package com.github.flowtrip.presentation.recommendation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.flowtrip.core.designsystem.FlowInfoCard
import com.github.flowtrip.core.designsystem.FlowPrimaryButton
import com.github.flowtrip.domain.model.DataSource

@Composable
internal fun NavigationTestPanel(state: RecommendationUiState, onRequest: (String, Boolean) -> Unit,
    onRetry: () -> Unit, onOpenMap: () -> Unit) {
    if (state.testDestinations.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    val busy = state.isRequestingRoute || state.isOpeningMap
    OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
        Text(if (expanded) "실제 장소 테스트 접기" else "실제 장소 길안내 테스트")
    }
    if (!expanded) return
    FlowInfoCard {
        Text("공식 SDK 샘플 목적지", style = MaterialTheme.typography.titleMedium)
        Text(if (state.navigation.source == DataSource.PLEOS)
            "아래 버튼은 실제 내비게이션 목적지를 변경합니다. 추천 코스와 방문 기록은 유지됩니다."
        else "Mock 빌드에서는 시뮬레이션합니다. 실제 안내는 Pleos SDK 빌드에서 시험해 주세요.")
        state.testDestinations.forEach { destination ->
            Text("${destination.name} · ${destination.address}")
            FlowPrimaryButton("${destination.name} 길안내", { onRequest(destination.id, false) }, enabled = !busy)
        }
        if (state.testDestinations.size == 2) {
            val final = state.testDestinations.first()
            OutlinedButton(onClick = { onRequest(final.id, true) }, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                Text("${state.testDestinations.last().name} 경유 → ${final.name}")
            }
        }
        state.testRouteMessage?.let { Text(it) }
        if (state.navigationError != null && state.isTestRouteRetry) {
            OutlinedButton(onClick = onRetry, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("테스트 경로 다시 시도") }
        }
        OutlinedButton(onClick = onOpenMap, enabled = !busy && state.navigation.source == DataSource.PLEOS,
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("테스트 내비게이션 화면 요청") }
        state.mapMessage?.let { Text(it) }
        Text("창 배치는 Pleos window bar에서 조절해 주세요. 현재 지도에서의 장소 유효성은 경로 결과로 확인합니다.",
            style = MaterialTheme.typography.bodySmall)
    }
}
