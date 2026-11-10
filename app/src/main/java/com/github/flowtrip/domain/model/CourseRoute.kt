package com.github.flowtrip.domain.model

/** Ordered map stops. The first stop is the route's current-location origin. */
data class CourseStop(
    val id: String,
    val label: String,
    val coordinates: Coordinates,
    val source: DataSource,
)

/** One driving segment, for example current location -> stop 1 or stop 1 -> stop 2. */
data class CourseLeg(
    val fromStopId: String,
    val toStopId: String,
    val roadGeometry: List<Coordinates>,
    val distanceMeters: Int,
    val durationSeconds: Int,
)

data class CourseRoute(val stops: List<CourseStop>, val legs: List<CourseLeg>)

sealed interface CourseRouteResult {
    data class Success(val route: CourseRoute) : CourseRouteResult
    data class Failure(val message: String) : CourseRouteResult
}
