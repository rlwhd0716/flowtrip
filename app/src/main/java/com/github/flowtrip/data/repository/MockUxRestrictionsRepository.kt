package com.github.flowtrip.data.repository

import com.github.flowtrip.domain.model.UxAccess
import com.github.flowtrip.domain.model.UxState
import com.github.flowtrip.domain.repository.UxRestrictionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockUxRestrictionsRepository(private val isAutomotive: Boolean = false) : UxRestrictionsRepository {
    private val mutableState = MutableStateFlow(UxState())
    override val state = mutableState.asStateFlow()
    override fun start() {
        mutableState.value = if (isAutomotive) UxState(UxAccess.UNKNOWN, "차량에서는 실제 UX 제한 연동 빌드가 필요합니다.")
            else UxState(UxAccess.SIMULATED, "개발용 Mock 환경 · 차량의 정차 상태를 나타내지 않습니다.")
    }
    override fun stop() { mutableState.value = UxState() }
    fun simulate(access: UxAccess) { mutableState.value = UxState(access, "Mock UX 제한: $access") }
}
