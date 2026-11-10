package com.github.flowtrip.presentation.recommendation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.github.flowtrip.core.designsystem.FlowInfoCard
import com.github.flowtrip.core.designsystem.FlowPrimaryButton
import com.github.flowtrip.domain.model.DataSource
import com.github.flowtrip.domain.model.ServiceStatus

@Composable
internal fun RecommendationMap(state: RecommendationUiState, onOpenMap: () -> Unit) {
    val navigation = state.navigation
    val available = navigation.source == DataSource.PLEOS && navigation.service == ServiceStatus.READY
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
        FlowPrimaryButton(if (state.isOpeningMap) "지도 요청 중…" else "Pleos 지도 열기", onOpenMap,
            enabled = available && !state.isOpeningMap && !state.isRequestingRoute && state.destinationConfirmation == null)
        state.mapMessage?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
    }
}
