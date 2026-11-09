package com.github.flowtrip.data.repository

import android.content.Context
import android.content.pm.PackageManager
import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import com.github.flowtrip.core.platform.PleosPermissions
import com.github.flowtrip.data.mapper.toDomain
import com.github.flowtrip.domain.model.*
import com.github.flowtrip.domain.repository.LocationRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class PleosLocationRepository(private val context: Context, private val navigation: PleosNavigationRepository) : LocationRepository {
    private val mutableState = MutableStateFlow(LocationState(DataSource.PLEOS))
    override val state = mutableState.asStateFlow()
    private var sdk: FusedLocation? = null
    private var listener: FusedLocationListener? = null
    private var scope: CoroutineScope? = null
    private var usingFallback = false

    override fun start() {
        if (scope != null) return
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            mutableState.value = LocationState(DataSource.PLEOS, LocationStatus.UNSUPPORTED, message = "Pleos 차량 환경이 아닙니다.")
            return
        }
        val session = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = session
        try {
            val client = FusedLocation(context)
            if (!client.isSupported()) {
                usingFallback = true
                mutableState.value = LocationState(DataSource.PLEOS, LocationStatus.UNSUPPORTED, message = "FusedLocation 미지원 · NaviHelper 대체 위치 대기")
                session.launch {
                    navigation.fallbackLocation.collect { fallback ->
                        if (fallback.location != null) mutableState.value = fallback
                    }
                }
                navigation.refreshCurrentLocation()
                return
            }
            if (!PleosPermissions.granted(context, PleosPermissions.FUSED_LOCATION)) {
                mutableState.value = LocationState(DataSource.PLEOS, LocationStatus.PERMISSION_DENIED, message = "차량 위치 권한이 필요합니다.")
                return
            }
            sdk = client
            val callbacks = object : FusedLocationListener {
                override fun onChanged(location: LocationData?) { session.launch {
                    if (sdk !== client) return@launch
                    if (!PleosPermissions.granted(context, PleosPermissions.FUSED_LOCATION)) {
                        stop()
                        mutableState.value = LocationState(DataSource.PLEOS, LocationStatus.PERMISSION_DENIED, message = "위치 권한이 취소되었습니다.")
                        return@launch
                    }
                    mutableState.value = try {
                        if (location == null) LocationState(DataSource.PLEOS, message = "위치를 사용할 수 없습니다.")
                        else LocationState(DataSource.PLEOS, LocationStatus.AVAILABLE, location.toDomain())
                    } catch (_: IllegalArgumentException) { LocationState(DataSource.PLEOS, LocationStatus.ERROR, message = "위치 좌표가 유효하지 않습니다.") }
                } }
                override fun onError(exception: Exception) { session.launch {
                    if (sdk === client) mutableState.value = LocationState(DataSource.PLEOS,
                        if (exception is SecurityException) LocationStatus.PERMISSION_DENIED else LocationStatus.ERROR,
                        message = "Pleos 위치 조회 실패")
                } }
            }
            listener = callbacks
            client.initialize()
            client.getLastKnownLocation(callbacks)
            client.registerFusedLocationCallback(callbacks)
        } catch (_: SecurityException) {
            mutableState.value = LocationState(DataSource.PLEOS, LocationStatus.PERMISSION_DENIED, message = "위치 권한이 거부되었습니다.")
        } catch (_: Exception) {
            mutableState.value = LocationState(DataSource.PLEOS, LocationStatus.ERROR, message = "Pleos 위치 초기화 실패")
        }
    }
    override fun refresh() {
        if (usingFallback) navigation.refreshCurrentLocation()
        else listener?.let { callbacks -> try { sdk?.getLastKnownLocation(callbacks) }
            catch (_: SecurityException) { mutableState.value = LocationState(DataSource.PLEOS, LocationStatus.PERMISSION_DENIED) }
            catch (_: Exception) { mutableState.value = LocationState(DataSource.PLEOS, LocationStatus.ERROR) } }
    }
    override fun stop() {
        val client = sdk
        sdk = null
        listener?.let { runCatching { client?.unregisterFusedLocationCallback(it) } }
        runCatching { client?.release() }
        scope?.cancel()
        scope = null
        listener = null
        usingFallback = false
        mutableState.value = LocationState(DataSource.PLEOS)
    }
}
