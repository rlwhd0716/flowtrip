package com.github.flowtrip.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.github.flowtrip.BuildConfig
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.*
import ai.pleos.playground.navi.constants.*
import com.github.flowtrip.core.platform.PleosPermissions
import com.github.flowtrip.data.mapper.RouteConfirmation
import com.github.flowtrip.data.mapper.RoutingDecision
import com.github.flowtrip.data.mapper.toDomain
import com.github.flowtrip.domain.model.*
import com.github.flowtrip.domain.repository.NavigationRepository
import com.github.flowtrip.domain.repository.UxRestrictionsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PleosNavigationRepository(
    private val context: Context,
    private val ux: UxRestrictionsRepository,
) : NavigationRepository {
    private val mutableState = MutableStateFlow(NavigationState(DataSource.PLEOS))
    override val state = mutableState.asStateFlow()
    private val mutableEvents = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 32)
    override val events = mutableEvents.asSharedFlow()
    private val mutableLocation = MutableStateFlow(LocationState(DataSource.PLEOS))
    internal val fallbackLocation = mutableLocation.asStateFlow()
    private var helper: NaviHelper? = null
    private var listener: NaviHelperEventListener? = null
    private var scope: CoroutineScope? = null
    private var pending: CompletableDeferred<PlatformResult>? = null
    private var confirmation: RouteConfirmation? = null
    private val mutex = Mutex()
    private fun trace(event: String) { if (BuildConfig.DEBUG) Log.i("FlowTrip.Navi", event) }

    override fun start() {
        if (helper != null) return
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            mutableState.value = state.value.copy(service = ServiceStatus.UNSUPPORTED, message = "Pleos 차량 환경이 아닙니다.")
            return
        }
        // Connecting and showing the map do not require information-query permission.
        // Each API checks its own permission; map requests use NAVI_CUSTOM_MAP.
        mutableState.value = state.value.copy(service = ServiceStatus.CONNECTING, message = "Pleos Navi Service 연결 중")
        val session = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = session
        try {
            val sdk = NaviHelper(context)
            helper = sdk
            fun dispatch(block: () -> Unit) { session.launch { if (helper === sdk) block() } }
            val callbacks = object : NaviHelperEventListener {
                override fun onDestinationInfo(destinationInfo: DestinationInfo) = dispatch {
                    trace("onDestinationInfo")
                    try {
                        val destination = destinationInfo.toDomain()
                        val changed = state.value.destination != destination
                        mutableState.value = state.value.copy(destination = destination)
                        if (changed) mutableEvents.tryEmit(NavigationEvent.DestinationChanged(destination))
                        confirmation?.onDestination(destination)
                        finishIfConfirmed()
                    } catch (_: IllegalArgumentException) { fail("목적지 좌표가 유효하지 않습니다.") }
                }
                override fun onRouteStarted(routeStartInfo: RouteStartInfo) = dispatch {
                    trace("onRouteStarted")
                    mutableState.value = state.value.copy(route = RouteState.SEARCHING, message = "경로 탐색 중")
                }
                override fun onRoutingCompleted() = dispatch {
                    trace("onRoutingCompleted")
                    confirmation?.onRoutingCompleted()
                    // This callback alone is not a verified route to our requested destination.
                    queryDestinationAndRoute()
                }
                @Suppress("OVERRIDE_DEPRECATION")
                override fun onRouteEnded() = dispatch {
                    // Public legacy completion callback retained in 3.0.5. Older Navi apps
                    // may emit this instead of the newly introduced onRoutingCompleted.
                    trace("onRouteEnded (legacy completion)")
                    confirmation?.onRoutingCompleted()
                    queryDestinationAndRoute()
                }
                override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) = dispatch {
                    trace("onRouteStateInfo mode=${routeStateInfo.naviMode} routing=${routeStateInfo.isRouting}")
                    val guiding = routeStateInfo.naviMode == NaviMode.ROUTE_GUIDING || routeStateInfo.naviMode == NaviMode.SIMULATION_GUIDING
                    if (pending != null) {
                        confirmation?.onRouteState(routeStateInfo.isRouting, guiding)
                        finishIfConfirmed()
                    } else if (pending == null) {
                        mutableState.value = state.value.copy(route = when {
                            routeStateInfo.isRouting -> RouteState.SEARCHING
                            guiding -> RouteState.GUIDING
                            else -> RouteState.IDLE
                        }, destination = if (guiding || routeStateInfo.isRouting) state.value.destination else null,
                            waypoints = if (guiding || routeStateInfo.isRouting) state.value.waypoints else emptyList())
                    }
                }
                override fun onDestinationChanged() = dispatch { queryDestinationAndRoute() }
                override fun onReRouteEnded() = dispatch { queryDestinationAndRoute() }
                override fun onWaypointChanged(waypointChangedInfo: WaypointChangedInfo) = dispatch { queryDestinationAndRoute() }
                override fun onWayPointInfo(waypointInfoList: List<WaypointInfo>) = dispatch {
                    trace("onWayPointInfo count=${waypointInfoList.size}")
                    try {
                        val waypoints = waypointInfoList.map { it.toDomain() }
                        mutableState.value = state.value.copy(waypoints = waypoints)
                        confirmation?.onWaypoints(waypoints)
                        finishIfConfirmed()
                    } catch (_: IllegalArgumentException) { fail("경유지 좌표가 유효하지 않습니다.") }
                }
                override fun onRouteCancelled() = dispatch {
                    trace("onRouteCancelled; replacementPending=${pending != null}")
                    confirmation?.onRouteCancelled()
                    // Do not treat cancellation of the old guidance as the outcome of
                    // its replacement. Wait for a verified new route, error or timeout.
                    mutableState.value = state.value.copy(destination = null, waypoints = emptyList(), route = RouteState.IDLE,
                        message = if (pending != null) "기존 안내 종료 · 새 경로 결과 확인 중" else "경로 안내 종료")
                    mutableEvents.tryEmit(NavigationEvent.DestinationChanged(null))
                }
                override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) = dispatch {
                    mutableState.value = state.value.copy(route = RouteState.ARRIVED)
                    mutableEvents.tryEmit(NavigationEvent.Arrived(destinationArrivedInfo.name,
                        state.value.destination?.takeIf { it.name == destinationArrivedInfo.name }))
                }
                override fun onWayPointArrived(waypointArrivedInfo: WaypointArrivedInfo) = dispatch {
                    mutableEvents.tryEmit(NavigationEvent.Arrived(waypointArrivedInfo.name, null, isWaypoint = true))
                }
                override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) = dispatch {
                    try {
                        // NaviHelper does not expose the measurement timestamp; never claim a fresh GPS fix.
                        mutableLocation.value = LocationState(DataSource.PLEOS, LocationStatus.STALE,
                            LocationInfo(Coordinates(currentLocationInfo.latitude, currentLocationInfo.longitude), 0, DataSource.PLEOS),
                            "NaviHelper 대체 위치 · 측정 시각 미제공")
                    } catch (_: IllegalArgumentException) { mutableLocation.value = LocationState(DataSource.PLEOS, LocationStatus.ERROR) }
                }
                override fun onError(errorCode: NaviErrorCode) = dispatch { fail("Pleos 내비게이션 오류: $errorCode") }
            }
            listener = callbacks
            sdk.initialize(onConnected = { dispatch {
                try {
                    sdk.addListener(callbacks)
                    trace("connected; listener registered")
                    mutableState.value = state.value.copy(service = ServiceStatus.READY, message = "Pleos Navi Service 연결됨")
                    queryDestinationAndRoute()
                    refreshCurrentLocation()
                } catch (_: SecurityException) { denied() } catch (_: Exception) { fail("내비게이션 리스너 등록 실패") }
            } }, onFailed = { dispatch {
                mutableState.value = state.value.copy(service = ServiceStatus.ERROR, message = "Pleos Navi Service 연결 실패")
                fail("Pleos Navi Service에 연결할 수 없습니다.")
            } })
        } catch (_: SecurityException) { denied() } catch (_: Exception) {
            mutableState.value = state.value.copy(service = ServiceStatus.ERROR, message = "Pleos 내비게이션 초기화 실패")
        }
    }

    private fun denied() {
        mutableState.value = state.value.copy(service = ServiceStatus.PERMISSION_DENIED)
        fail("Pleos 내비게이션 권한이 거부되었습니다.")
    }
    private fun fail(message: String) {
        trace("failure: $message")
        mutableState.value = state.value.copy(route = RouteState.FAILED, message = message)
        mutableEvents.tryEmit(NavigationEvent.Failure(message))
        pending?.complete(PlatformResult.Failure(message))
    }
    private fun finishIfConfirmed() {
        when (confirmation?.decision) {
            RoutingDecision.CONFIRMED -> {
                mutableState.value = state.value.copy(route = RouteState.GUIDING, message = "승인한 목적지의 경로 안내 확인됨")
                pending?.complete(PlatformResult.Success)
            }
            else -> Unit
        }
    }
    private fun queryDestinationAndRoute() {
        if (state.value.service != ServiceStatus.READY) return
        if (!PleosPermissions.granted(context, PleosPermissions.NAVI_ROUTE)) return
        try {
            trace("query destination/route/waypoints")
            helper?.getDestinationInfo(); helper?.getRouteStateInfo(); helper?.getWaypointInfo()
        }
        catch (_: SecurityException) { fail("내비게이션 정보 조회 권한이 거부되었습니다.") }
        catch (_: Exception) { fail("내비게이션 정보 조회 실패") }
    }
    override fun refreshDestination() = queryDestinationAndRoute()
    override suspend fun openMap(): PlatformResult = withContext(Dispatchers.Main.immediate) {
        if (!ux.state.value.allowsInteraction) return@withContext PlatformResult.Failure("차량 UX 제한으로 지도를 열 수 없습니다.")
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            return@withContext PlatformResult.Failure("Pleos Connect 환경에서 지도를 열어 주세요.")
        }
        if (!PleosPermissions.granted(context, PleosPermissions.NAVI_CUSTOM_MAP)) {
            return@withContext PlatformResult.Failure("Pleos 지도 화면 표시 권한(NAVI_CUSTOM_MAP)이 필요합니다.")
        }
        if (state.value.service != ServiceStatus.READY && state.value.service != ServiceStatus.CONNECTING) {
            stop()
            start()
        }
        val connected = withTimeoutOrNull(25_000) {
            state.first { it.service != ServiceStatus.CONNECTING }
        } ?: return@withContext PlatformResult.Failure("Pleos 내비게이션 연결 시간이 초과되었습니다. 다시 시도해 주세요.")
        val sdk = helper
        if (connected.service != ServiceStatus.READY || sdk == null) {
            return@withContext PlatformResult.Failure(connected.message ?: "Pleos 내비게이션에 연결할 수 없습니다.")
        }
        // UX can change while waiting for the service. Check again before opening another app.
        if (!ux.state.value.allowsInteraction) return@withContext PlatformResult.Failure("차량 UX 제한으로 지도 열기를 중단했습니다.")
        if (!PleosPermissions.granted(context, PleosPermissions.NAVI_CUSTOM_MAP)) {
            return@withContext PlatformResult.Failure("Pleos 지도 화면 표시 권한이 취소되었습니다.")
        }
        try {
            sdk.requestNavigationScreen(NavigationScreenRequest.Foreground)
            // No screen-display completion callback is provided. Do not claim the screen is visible.
            PlatformResult.Success
        } catch (_: SecurityException) {
            PlatformResult.Failure("Pleos 지도 화면 표시 권한이 거부되었습니다.")
        } catch (_: Exception) {
            PlatformResult.Failure("Pleos 지도 화면을 요청하지 못했습니다.")
        }
    }
    internal fun refreshCurrentLocation() {
        if (!PleosPermissions.granted(context, PleosPermissions.NAVI_ROUTE)) return
        if (state.value.service == ServiceStatus.READY) try { helper?.getCurrentLocationInfo() }
        catch (_: Exception) { mutableLocation.value = LocationState(DataSource.PLEOS, LocationStatus.ERROR, message = "대체 위치 조회 실패") }
    }

    override suspend fun requestRoute(destination: Destination, userConfirmed: Boolean): PlatformResult =
        route(destination, emptyList(), userConfirmed)

    override suspend fun requestRouteWithWaypoints(destination: Destination, waypoints: List<Destination>, userConfirmed: Boolean): PlatformResult {
        if (waypoints.size !in 1..2) return PlatformResult.Failure("NaviHelper 경유지는 1~2곳이어야 합니다.")
        return route(destination, waypoints, userConfirmed)
    }

    private suspend fun route(destination: Destination, waypoints: List<Destination>, userConfirmed: Boolean): PlatformResult = withContext(Dispatchers.Main.immediate) {
        if (mutex.isLocked) return@withContext PlatformResult.Failure("이전 경로 요청 결과를 확인 중입니다.")
        mutex.withLock {
            if (!userConfirmed) return@withLock PlatformResult.Failure("목적지 변경 승인이 필요합니다.")
            if (!ux.state.value.allowsInteraction) return@withLock PlatformResult.Failure("차량 UX 제한으로 목적지를 변경할 수 없습니다.")
            val stops = waypoints + destination
            if (stops.any { it.source != DataSource.PLEOS }) return@withLock PlatformResult.Failure("Mock 장소는 실제 내비게이션으로 전송할 수 없습니다. 실제 장소 데이터 연동이 필요합니다.")
            if (stops.any { it.poiId.isNullOrBlank() || it.poiSubId.isNullOrBlank() }) return@withLock PlatformResult.Failure("실제 장소의 POI 식별 정보가 필요합니다.")
            if (state.value.service != ServiceStatus.READY) return@withLock PlatformResult.Failure("Pleos 내비게이션이 준비되지 않았습니다.")
            if (!PleosPermissions.granted(context, PleosPermissions.NAVI_ROUTE_SEARCH)) return@withLock PlatformResult.Failure("경로 탐색 권한이 필요합니다.")
            if (!PleosPermissions.granted(context, PleosPermissions.NAVI_ROUTE)) return@withLock PlatformResult.Failure("경로 결과를 확인할 내비게이션 정보 조회 권한이 필요합니다.")
            val result = CompletableDeferred<PlatformResult>()
            val routeConfirmation = RouteConfirmation(destination, waypoints)
            confirmation = routeConfirmation
            pending = result
            mutableState.value = state.value.copy(route = RouteState.SEARCHING, message = "목적지 경로 요청 중")
            // The three get APIs are asynchronous and their snapshots are not atomic.
            // A pre-change response can arrive after completion. Keep all positive
            // checks, but retry reads rather than permanently rejecting one mismatch.
            val poll = launch {
                while (isActive && !result.isCompleted) {
                    delay(1_000)
                    if (routeConfirmation.hasCompleted) queryDestinationAndRoute()
                }
            }
            try {
                val routeInfo = RouteInfo(latitude = destination.coordinates.latitude, longitude = destination.coordinates.longitude,
                    displayLatitude = null, displayLongitude = null, poiName = destination.name,
                    poiId = requireNotNull(destination.poiId), poiSubId = requireNotNull(destination.poiSubId), address = destination.address,
                    poiProvider = destination.poiProvider.orEmpty())
                if (waypoints.isEmpty()) helper!!.requestRoute(routeInfo)
                else helper!!.requestRouteWithWaypoints(RouteWithWaypointsRequest(
                    destination = routeInfo,
                    waypoints = waypoints.mapIndexed { index, stop -> RequestWaypointInfo(
                        latitude = stop.coordinates.latitude, longitude = stop.coordinates.longitude,
                        poiName = stop.name, waypointIndex = if (index == 0) WaypointIndex.FIRST else WaypointIndex.SECOND,
                        poiId = requireNotNull(stop.poiId), address = stop.address,
                        poiSubId = requireNotNull(stop.poiSubId), poiProvider = stop.poiProvider.orEmpty(),
                    ) },
                ))
                trace("route dispatched; waypointCount=${waypoints.size}")
                withTimeoutOrNull(25_000) { result.await() } ?: PlatformResult.Failure(
                    "경로 요청은 전달했으나 안내 결과를 확인하지 못했습니다 (${routeConfirmation.waitingReason}). 실제 목적지는 내비게이션 화면에서 확인해 주세요.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SecurityException) { PlatformResult.Failure("경로 탐색 권한이 거부되었습니다.") }
            catch (_: Exception) { PlatformResult.Failure("Pleos 경로 요청 실패") }
            finally {
                poll.cancel()
                pending = null
                confirmation = null
                if (state.value.route == RouteState.SEARCHING) {
                    mutableState.value = state.value.copy(route = RouteState.FAILED, message = "경로 결과를 확인하지 못했습니다.")
                }
            }
        }
    }

    override fun stop() {
        val sdk = helper
        helper = null
        pending?.complete(PlatformResult.Failure("앱이 비활성화되어 경로 확인을 중단했습니다."))
        listener?.let { runCatching { sdk?.removeListener(it) } }
        runCatching { sdk?.release() }
        scope?.cancel()
        scope = null
        listener = null
        mutableLocation.value = LocationState(DataSource.PLEOS)
        mutableState.value = state.value.copy(service = ServiceStatus.STOPPED)
    }
}
