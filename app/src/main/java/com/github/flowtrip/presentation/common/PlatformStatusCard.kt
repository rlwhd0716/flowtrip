package com.github.flowtrip.presentation.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.github.flowtrip.core.designsystem.FlowInfoCard
import com.github.flowtrip.domain.model.*
import com.github.flowtrip.domain.usecase.CheckProximityUseCase
import com.github.flowtrip.domain.usecase.Proximity

@Composable
fun PlatformStatusCard(navigation: NavigationState, location: LocationState, arrival: NavigationEvent.Arrived?) {
    FlowInfoCard {
        val source = if (navigation.source == DataSource.MOCK) "Mock" else "Pleos"
        Text("내비게이션 · $source", style = MaterialTheme.typography.titleLarge)
        Text(navigation.message ?: "연결 상태 확인 중")
        if (navigation.service != ServiceStatus.READY) Text("연결 재확인 필요 · 목적지와 경로는 마지막 수신 정보입니다.")
        Text("현재 목적지: ${navigation.destination?.name ?: "확인된 목적지 없음"}")
        if (navigation.waypoints.isNotEmpty()) Text("경유지: ${navigation.waypoints.joinToString(" → ") { it.name }}")
        Text("경로 상태: ${when (navigation.route) {
            RouteState.IDLE -> "안내 없음"
            RouteState.SEARCHING -> "탐색 중"
            RouteState.GUIDING -> "안내 확인됨"
            RouteState.ARRIVED -> "도착"
            RouteState.FAILED -> "요청 실패 · 현재 안내 재확인 필요"
        }}")
        Text("위치: " + when (location.status) {
            LocationStatus.AVAILABLE -> if (location.source == DataSource.MOCK) "Mock 위치" else "차량 위치 수신 중"
            LocationStatus.STALE -> "최신 위치 확인 필요"
            LocationStatus.PERMISSION_DENIED -> "권한 필요"
            LocationStatus.UNSUPPORTED -> "미지원"
            else -> location.message ?: "사용 가능한 위치 없음"
        })
        if (CheckProximityUseCase()(location, navigation.destination, System.currentTimeMillis()) == Proximity.NEAR) {
            Text("목적지 근처입니다. 위치 근접만으로 방문 완료를 판단하지 않습니다.")
        }
        if (arrival != null) Text("${arrival.name} ${if (arrival.isWaypoint) "경유지" else "목적지"} 도착 신호 수신 · 방문 완료는 별도 확인이 필요합니다.")
    }
}
