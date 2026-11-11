package com.github.flowtrip.data.repository

import com.github.flowtrip.domain.model.*
import com.github.flowtrip.domain.repository.NavigationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** In-memory simulator. Does not move the vehicle or automatically simulate arrival. */
class MockNavigationRepository : NavigationRepository {
    private val mutableState = MutableStateFlow(NavigationState(DataSource.MOCK))
    override val state = mutableState.asStateFlow()
    private val mutableEvents = MutableSharedFlow<NavigationEvent>(extraBufferCapacity = 16)
    override val events = mutableEvents.asSharedFlow()
    private val routeMutex = Mutex()
    var failNextRoute = false
    private var generation = 0

    override fun start() { mutableState.value = state.value.copy(service = ServiceStatus.READY, message = "Mock 내비게이션") }
    override fun stop() {
        generation++
        mutableState.value = state.value.copy(service = ServiceStatus.STOPPED,
            route = if (state.value.route == RouteState.SEARCHING) RouteState.FAILED else state.value.route)
    }
    override fun refreshDestination() { /* Current mock destination is already in StateFlow. */ }
    override suspend fun openMap(): PlatformResult =
        PlatformResult.Failure("Pleos 지도는 실제 SDK 빌드와 Pleos Connect 환경에서 사용할 수 있습니다.")
    override suspend fun requestRoute(destination: Destination, userConfirmed: Boolean): PlatformResult =
        route(destination, emptyList(), userConfirmed)
    override suspend fun requestRouteWithWaypoints(destination: Destination, waypoints: List<Destination>, userConfirmed: Boolean): PlatformResult {
        if (waypoints.size !in 1..2) return PlatformResult.Failure("경유지는 1~2곳이어야 합니다.")
        return route(destination, waypoints, userConfirmed)
    }
    private suspend fun route(destination: Destination, waypoints: List<Destination>, userConfirmed: Boolean): PlatformResult = routeMutex.withLock {
        if (!userConfirmed) return@withLock PlatformResult.Failure("목적지 변경 승인이 필요합니다.")
        if (state.value.service != ServiceStatus.READY) return@withLock PlatformResult.Failure("Mock 내비게이션이 준비되지 않았습니다.")
        val previous = state.value
        val requestGeneration = generation
        mutableState.value = state.value.copy(route = RouteState.SEARCHING, message = "Mock 경로 탐색 중")
        try { delay(150) } catch (cancelled: CancellationException) {
            if (requestGeneration == generation) mutableState.value = state.value.copy(
                route = previous.route.takeIf { it == RouteState.GUIDING || it == RouteState.ARRIVED } ?: RouteState.FAILED,
                message = "Mock 경로 요청 중단")
            throw cancelled
        }
        if (requestGeneration != generation) return@withLock PlatformResult.Failure("경로 요청이 중단되었습니다.")
        if (failNextRoute) {
            failNextRoute = false
            mutableState.value = state.value.copy(
                route = previous.route.takeIf { it == RouteState.GUIDING || it == RouteState.ARRIVED } ?: RouteState.FAILED,
                message = "Mock 경로 탐색 실패 · 기존 상태 유지")
            mutableEvents.tryEmit(NavigationEvent.Failure("Mock 경로 탐색 실패"))
            return@withLock PlatformResult.Failure("Mock 경로 탐색 실패")
        }
        mutableState.value = state.value.copy(destination = destination, waypoints = waypoints.toList(), route = RouteState.GUIDING, message = "Mock 경로 탐색 완료")
        mutableEvents.tryEmit(NavigationEvent.DestinationChanged(destination))
        PlatformResult.Success
    }

    fun simulateArrival(isWaypoint: Boolean = false) {
        val destination = state.value.destination ?: return
        mutableEvents.tryEmit(NavigationEvent.Arrived(destination.name, destination, isWaypoint))
        if (!isWaypoint) mutableState.value = state.value.copy(route = RouteState.ARRIVED)
    }
    fun simulateExternalDestinationChange(destination: Destination?) {
        generation++
        mutableState.value = state.value.copy(destination = destination, waypoints = emptyList(), route = if (destination == null) RouteState.IDLE else RouteState.GUIDING)
        mutableEvents.tryEmit(NavigationEvent.DestinationChanged(destination))
    }
}
