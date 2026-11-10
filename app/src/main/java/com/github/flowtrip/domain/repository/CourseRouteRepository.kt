package com.github.flowtrip.domain.repository

import com.github.flowtrip.domain.model.CourseRouteResult
import com.github.flowtrip.domain.model.CourseStop

/** Read-only route planning for FlowTrip's embedded map; never changes the vehicle's destination. */
interface CourseRouteRepository {
    /** Preserve stop order and return road geometry for every adjacent pair of stops. */
    suspend fun calculateRoute(stops: List<CourseStop>): CourseRouteResult
}
