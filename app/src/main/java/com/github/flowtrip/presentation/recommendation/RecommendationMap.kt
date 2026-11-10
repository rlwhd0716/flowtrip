package com.github.flowtrip.presentation.recommendation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.github.flowtrip.core.designsystem.FlowInfoCard
import com.github.flowtrip.core.designsystem.FlowPrimaryButton
import com.github.flowtrip.domain.model.DataSource
import com.github.flowtrip.domain.model.ServiceStatus
import com.github.flowtrip.domain.model.RouteState

@Composable
internal fun RecommendationMap(state: RecommendationUiState, onOpenMap: () -> Unit) {
    val navigation = state.navigation
    val pleos = navigation.source == DataSource.PLEOS
    val available = pleos && navigation.service == ServiceStatus.READY
    val canRequest = pleos && navigation.service != ServiceStatus.UNSUPPORTED
    FlowInfoCard {
        Text("Pleos 내비게이션 지도", style = MaterialTheme.typography.titleLarge)
        Text("Pleos Connect 내비게이션 화면에서 지도와 현재 경로를 확인하세요.",
            style = MaterialTheme.typography.bodyLarge)
        Text("지도 열기는 현재 목적지를 변경하지 않습니다. 추천 장소는 아직 Mock 데이터입니다.",
            style = MaterialTheme.typography.bodyMedium)
        if (!available) {
            Text(if (navigation.source == DataSource.MOCK)
                "지도는 Pleos SDK 빌드와 Pleos Connect 환경에서 사용할 수 있습니다."
            else navigation.message ?: "Pleos 내비게이션 연결을 확인해 주세요.",
                style = MaterialTheme.typography.bodyLarge)
        }
        FlowPrimaryButton(when {
            state.isOpeningMap -> "연결 및 지도 요청 중…"
            canRequest && !available -> "연결하고 지도 열기"
            else -> "Pleos 지도 열기"
        }, onOpenMap,
            enabled = canRequest && !state.isOpeningMap && !state.isRequestingRoute && state.destinationConfirmation == null)
        if (navigation.route == RouteState.FAILED) {
            navigation.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        state.mapMessage?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
    }
}
