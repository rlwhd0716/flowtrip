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
    /** Success means routing was confirmed, not simply that the SDK accepted the call. */
    suspend fun requestRoute(destination: Destination, userConfirmed: Boolean): PlatformResult
}
