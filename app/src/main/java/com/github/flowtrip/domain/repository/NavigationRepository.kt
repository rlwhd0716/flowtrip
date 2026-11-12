package com.github.flowtrip.domain.repository

import com.github.flowtrip.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface NavigationRepository {
    val state: StateFlow<NavigationState>
    val events: Flow<NavigationEvent>
    fun start()
    fun stop()
    fun refreshDestination()
    /** Explicit foreground/diagnostic request, NOT split setup. Never call from route controls.
     * Success only means dispatch. Kept separate from the regular recommendation UI. */
    suspend fun openMap(): PlatformResult
    /** Success means routing was confirmed, not simply that the SDK accepted the call. */
    suspend fun requestRoute(destination: Destination, userConfirmed: Boolean): PlatformResult
    suspend fun requestRouteWithWaypoints(destination: Destination, waypoints: List<Destination>, userConfirmed: Boolean): PlatformResult
}
