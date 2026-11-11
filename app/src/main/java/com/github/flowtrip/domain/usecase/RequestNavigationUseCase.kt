package com.github.flowtrip.domain.usecase

import com.github.flowtrip.domain.model.Destination
import com.github.flowtrip.domain.model.PlatformResult
import com.github.flowtrip.domain.repository.NavigationRepository
import com.github.flowtrip.domain.repository.UxRestrictionsRepository

class RequestNavigationUseCase(
    private val navigation: NavigationRepository,
    private val ux: UxRestrictionsRepository,
) {
    suspend fun openMap(): PlatformResult {
        if (!ux.state.value.allowsInteraction) return PlatformResult.Failure("차량 UX 제한으로 지도를 열 수 없습니다.")
        return navigation.openMap()
    }

    suspend operator fun invoke(destination: Destination, userConfirmed: Boolean): PlatformResult {
        if (!userConfirmed) return PlatformResult.Failure("목적지 변경 승인이 필요합니다.")
        if (!ux.state.value.allowsInteraction) return PlatformResult.Failure("차량 UX 제한으로 목적지를 변경할 수 없습니다.")
        return navigation.requestRoute(destination, userConfirmed = true)
    }

    suspend fun course(stops: List<Destination>, userConfirmed: Boolean): PlatformResult {
        if (stops.isEmpty() || stops.size > 3) return PlatformResult.Failure("코스는 목적지를 포함해 1~3곳이어야 합니다.")
        if (!userConfirmed) return PlatformResult.Failure("목적지 변경 승인이 필요합니다.")
        if (!ux.state.value.allowsInteraction) return PlatformResult.Failure("차량 UX 제한으로 목적지를 변경할 수 없습니다.")
        return if (stops.size == 1) navigation.requestRoute(stops.last(), true)
        else navigation.requestRouteWithWaypoints(stops.last(), stops.dropLast(1), true)
    }
}
