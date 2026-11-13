package com.github.flowtrip.domain.usecase

import com.github.flowtrip.domain.model.Destination
import com.github.flowtrip.domain.model.PlatformResult
import com.github.flowtrip.domain.model.DataSource
import com.github.flowtrip.domain.model.ServiceStatus
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

    suspend fun course(
        stops: List<Destination>,
        userConfirmed: Boolean,
    ): PlatformResult {
        if (stops.isEmpty() || stops.size > 3) return PlatformResult.Failure("코스는 목적지를 포함해 1~3곳이어야 합니다.")
        if (!userConfirmed) return PlatformResult.Failure("목적지 변경 승인이 필요합니다.")
        if (!ux.state.value.allowsInteraction) return PlatformResult.Failure("차량 UX 제한으로 목적지를 변경할 수 없습니다.")
        if (navigation.state.value.source == DataSource.PLEOS) {
            if (stops.any { it.source != DataSource.PLEOS }) return PlatformResult.Failure(
                "Mock 장소는 실제 내비게이션으로 전송할 수 없습니다. 실제 장소 데이터 연동이 필요합니다.")
            if (stops.any { it.poiId.isNullOrBlank() || it.poiSubId.isNullOrBlank() }) return PlatformResult.Failure(
                "실제 장소의 POI 식별 정보가 필요합니다.")
            if (navigation.state.value.service != ServiceStatus.READY) return PlatformResult.Failure(
                "Pleos 내비게이션이 준비되지 않았습니다.")
        }
        return if (stops.size == 1) navigation.requestRoute(stops.last(), true)
        else navigation.requestRouteWithWaypoints(stops.last(), stops.dropLast(1), true)
    }
}
