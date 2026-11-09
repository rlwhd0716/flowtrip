package com.github.flowtrip.domain.repository

import com.github.flowtrip.domain.model.LocationState
import kotlinx.coroutines.flow.StateFlow

interface LocationRepository {
    val state: StateFlow<LocationState>
    fun start()
    fun stop()
    fun refresh()
}
