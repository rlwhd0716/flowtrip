package com.github.flowtrip.domain.model

enum class DataSource { MOCK, PLEOS }
enum class ServiceStatus { STOPPED, CONNECTING, READY, UNSUPPORTED, PERMISSION_DENIED, ERROR }
enum class RouteState { IDLE, SEARCHING, GUIDING, ARRIVED, FAILED }
enum class LocationStatus { UNAVAILABLE, AVAILABLE, STALE, UNSUPPORTED, PERMISSION_DENIED, ERROR }
enum class UxAccess { UNKNOWN, RESTRICTED, ALLOWED, SIMULATED }

data class Coordinates(val latitude: Double, val longitude: Double) : java.io.Serializable {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }
}

data class Destination(
    val id: String,
    val name: String,
    val coordinates: Coordinates,
    val source: DataSource,
    val address: String = "",
    val poiId: String? = null,
    val poiSubId: String? = null,
    val poiProvider: String? = null,
) : java.io.Serializable

data class LocationInfo(val coordinates: Coordinates, val timestampMillis: Long, val source: DataSource)
data class LocationState(
    val source: DataSource,
    val status: LocationStatus = LocationStatus.UNAVAILABLE,
    val location: LocationInfo? = null,
    val message: String? = null,
)

data class NavigationState(
    val source: DataSource,
    val service: ServiceStatus = ServiceStatus.STOPPED,
    val route: RouteState = RouteState.IDLE,
    val destination: Destination? = null,
    val message: String? = null,
    val waypoints: List<Destination> = emptyList(),
)

sealed interface NavigationEvent {
    data class DestinationChanged(val destination: Destination?) : NavigationEvent
    // Arrival is a signal, never confirmation of a venue visit.
    data class Arrived(val name: String, val destination: Destination?, val isWaypoint: Boolean = false) : NavigationEvent
    data class Failure(val message: String) : NavigationEvent
}

sealed interface PlatformResult {
    data object Success : PlatformResult
    data class Failure(val message: String) : PlatformResult
}

data class UxState(val access: UxAccess = UxAccess.UNKNOWN, val message: String = "차량 UX 제한 확인 중") {
    val allowsInteraction: Boolean get() = access == UxAccess.ALLOWED || access == UxAccess.SIMULATED
}
