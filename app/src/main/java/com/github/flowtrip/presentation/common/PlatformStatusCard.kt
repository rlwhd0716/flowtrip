package com.github.flowtrip.presentation.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.flowtrip.core.designsystem.FlowInfoCard
import com.github.flowtrip.domain.model.*
import com.github.flowtrip.domain.usecase.CheckProximityUseCase
import com.github.flowtrip.domain.usecase.Proximity

@Composable
fun PlatformStatusCard(navigation: NavigationState, location: LocationState, arrival: NavigationEvent.Arrived?) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    navigation.destination?.let { Text("현재 목적지: ${it.name}", style = MaterialTheme.typography.bodyMedium) }
    if (arrival != null) Text("${arrival.name} ${if (arrival.isWaypoint) "경유지" else "목적지"} 도착",
        style = MaterialTheme.typography.bodyMedium)
    if (navigation.service == ServiceStatus.ERROR || navigation.service == ServiceStatus.PERMISSION_DENIED ||
        navigation.service == ServiceStatus.UNSUPPORTED) {
        Text(navigation.message ?: "내비게이션 연결을 확인해 주세요.", color = MaterialTheme.colorScheme.error)
    }
    TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
        Text(if (expanded) "연결 정보 접기" else "연결 정보 보기")
    }
    if (!expanded) return
    FlowInfoCard {
        Text(if (navigation.source == DataSource.MOCK) "내비게이션 · Mock" else "내비게이션 · Pleos",
            style = MaterialTheme.typography.titleMedium)
        Text(navigation.message ?: "연결 상태 확인 중")
        if (navigation.service != ServiceStatus.READY) Text("수신된 목적지와 경로는 최신 상태가 아닐 수 있습니다.")
        if (navigation.destination == null) Text("확인된 목적지 없음")
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
            Text("목적지 근처입니다. 방문 완료는 별도로 확인해 주세요.")
        }
    }
}
