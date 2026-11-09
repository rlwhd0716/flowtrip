package com.github.flowtrip.domain.repository

import com.github.flowtrip.domain.model.UxState
import kotlinx.coroutines.flow.StateFlow

interface UxRestrictionsRepository {
    val state: StateFlow<UxState>
    fun start()
    fun stop()
}
