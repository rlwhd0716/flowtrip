package com.github.flowtrip.core.platform

import com.github.flowtrip.domain.model.UxState
import com.github.flowtrip.domain.repository.UxRestrictionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UnavailableUxRestrictionsRepository : UxRestrictionsRepository {
    override val state = MutableStateFlow(UxState(message = "차량 UX 제한을 확인할 수 없는 환경입니다.")).asStateFlow()
    override fun start() = Unit
    override fun stop() = Unit
}
