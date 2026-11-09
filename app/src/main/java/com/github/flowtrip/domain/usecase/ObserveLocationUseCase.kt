package com.github.flowtrip.domain.usecase

import com.github.flowtrip.domain.model.*
import com.github.flowtrip.domain.repository.LocationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.transformLatest

const val LOCATION_MAX_AGE_MILLIS = 60_000L

fun LocationInfo.isFresh(nowMillis: Long): Boolean =
    timestampMillis > 0 && nowMillis >= timestampMillis && nowMillis - timestampMillis < LOCATION_MAX_AGE_MILLIS

class ObserveLocationUseCase(
    private val repository: LocationRepository,
    private val now: () -> Long = System::currentTimeMillis,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke() = repository.state.transformLatest { state ->
        val fix = state.location
        if (state.status != LocationStatus.AVAILABLE || fix == null) {
            emit(state)
        } else if (!fix.isFresh(now())) {
            emit(state.copy(status = LocationStatus.STALE, message = "위치가 오래되었거나 시간 정보가 올바르지 않습니다."))
        } else {
            emit(state)
            delay((fix.timestampMillis + LOCATION_MAX_AGE_MILLIS - now()).coerceAtLeast(1))
            emit(state.copy(status = LocationStatus.STALE, message = "최근 위치 업데이트가 없습니다."))
        }
    }
}
