package com.github.flowtrip.presentation.recommendation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.github.flowtrip.core.designsystem.FlowTripTheme
import com.github.flowtrip.core.designsystem.preview.FlowTripScreenPreviews
import com.github.flowtrip.presentation.common.preview.FlowTripPreviewData
import com.github.flowtrip.core.designsystem.FlowInfoCard
import com.github.flowtrip.core.designsystem.FlowPrimaryButton
import com.github.flowtrip.domain.model.RecommendationStage
import com.github.flowtrip.presentation.common.AdaptivePanels
import com.github.flowtrip.presentation.common.ScreenLayout
import com.github.flowtrip.presentation.common.PlatformStatusCard

@Composable
fun RecommendationScreen(state: RecommendationUiState, onSelect: () -> Unit, onAnother: () -> Unit,
    onSkip: () -> Unit, onRetry: () -> Unit, onConfirmDestination: () -> Unit,
    onDismissDestination: () -> Unit, onHome: () -> Unit, onBack: () -> Unit, onOpenMap: () -> Unit) {
    state.destinationConfirmation?.let { place ->
        AlertDialog(onDismissRequest = onDismissDestination,
            title = { Text("이 장소를 목적지로 설정할까요?") },
            text = { Text("${place.name}\n현재 목적지: ${state.navigation.destination?.name ?: "없음"}\n" +
                if (state.navigation.source == com.github.flowtrip.domain.model.DataSource.MOCK)
                    "Mock 내비게이션에서만 시뮬레이션합니다." else "실제 내비게이션 목적지가 변경됩니다. Mock 장소 전송은 차단됩니다.") },
            confirmButton = { TextButton(onClick = onConfirmDestination, modifier = Modifier.heightIn(min = 64.dp)) { Text("목적지로 설정") } },
            dismissButton = { TextButton(onClick = onDismissDestination, modifier = Modifier.heightIn(min = 64.dp)) { Text("취소") } })
    }
    ScreenLayout(state.request.mode.title, onBack, compact = true) {
        Text("${state.request.destination} · ${state.request.availableMinutes}분 · ${state.request.preference}",
            style = MaterialTheme.typography.bodyLarge)
        state.navigationError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge) }
        if (state.isRequestingRoute) Text("경로 탐색 결과를 기다리고 있습니다.", style = MaterialTheme.typography.bodyLarge)
        when {
            state.isLoading -> CircularProgressIndicator()
            state.error != null -> FlowInfoCard {
                Text(state.error, style = MaterialTheme.typography.bodyLarge)
                FlowPrimaryButton("다시 시도", onRetry)
            }
            state.isComplete -> FlowInfoCard {
                Text("오늘의 코스를 살펴보세요", style = MaterialTheme.typography.headlineMedium)
                Text("선택 ${state.selectedPlaces.size}곳 · 건너뛰기 ${state.skippedPlaces.size}곳",
                    style = MaterialTheme.typography.bodyLarge)
                if (state.selectedPlaces.isEmpty()) Text("선택한 장소가 없어요. 다른 설정으로 다시 시작해 보세요.")
                state.selectedPlaces.forEachIndexed { index, place ->
                    Text("${index + 1}. ${place.name} · ${place.stage.title}", style = MaterialTheme.typography.bodyLarge)
                }
                FlowPrimaryButton("홈으로", onHome)
            }
            else -> state.place?.let { place ->
                Text("STEP ${state.stageIndex + 1} / ${RecommendationStage.entries.size} · ${place.stage.title}",
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                AdaptivePanels(first = {
                    RecommendationMap(state, onOpenMap)
                }, second = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        FlowInfoCard {
                            Text(place.name, style = MaterialTheme.typography.headlineMedium)
                            Text(place.description, style = MaterialTheme.typography.bodyLarge)
                            place.destination?.address?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodyLarge)
                            }
                            Text("머무는 시간 약 ${place.visitMinutes}분 · ${place.expectedSpend}",
                                style = MaterialTheme.typography.labelLarge)
                            Text("주차 · ${place.parkingSummary}", style = MaterialTheme.typography.bodyLarge)
                            Text("이 장소를 추천하는 이유", style = MaterialTheme.typography.titleLarge)
                            Text(place.reason, style = MaterialTheme.typography.bodyLarge)
                            Text("장소·비용·체류·주차 정보는 시연용 예시입니다. 영업 여부는 확인되지 않았습니다.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FlowInfoCard {
                            Text("마음에 드는 장소인가요?", style = MaterialTheme.typography.titleLarge)
                            Text("선택 후 목적지 변경을 승인해 주세요. 경로 확인 후 다음 추천을 표시하며, 방문 완료로 기록하지 않습니다.",
                                style = MaterialTheme.typography.bodyLarge)
                            Text("선택 ${state.selectedPlaces.size}곳 · 건너뛰기 ${state.skippedPlaces.size}곳")
                        }
                        FlowPrimaryButton("이 장소 선택", onSelect, enabled = !state.isRequestingRoute)
                        OutlinedButton(onClick = onAnother, enabled = !state.isRequestingRoute, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                            Text("다른 장소 추천")
                        }
                        TextButton(onClick = onSkip, enabled = !state.isRequestingRoute, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                            Text("건너뛰기")
                        }
                    }
                })
            }
        }
        PlatformStatusCard(state.navigation, state.location, state.arrivalSignal)
    }
}

@FlowTripScreenPreviews
@Composable
private fun RecommendationScreenPreview() {
    RecommendationPreviewContent(FlowTripPreviewData.recommendation)
}

@Preview(name = "불러오는 중", group = "추천 화면 상태", widthDp = 1280, heightDp = 720, locale = "ko")
@Composable
private fun RecommendationLoadingPreview() {
    RecommendationPreviewContent(FlowTripPreviewData.recommendation.copy(isLoading = true, place = null))
}

@Preview(name = "불러오기 실패", group = "추천 화면 상태", widthDp = 1280, heightDp = 720, locale = "ko")
@Composable
private fun RecommendationErrorPreview() {
    RecommendationPreviewContent(FlowTripPreviewData.recommendation.copy(place = null,
        error = "추천 장소를 불러오지 못했어요. 다시 시도해 주세요."))
}

@Preview(name = "목적지 변경 승인", group = "추천 화면 상태", widthDp = 1280, heightDp = 720, locale = "ko")
@Composable
private fun RecommendationApprovalPreview() {
    RecommendationPreviewContent(FlowTripPreviewData.recommendation.copy(destinationConfirmation = FlowTripPreviewData.place))
}

@Preview(name = "코스 완료", group = "추천 화면 상태", widthDp = 1280, heightDp = 720, locale = "ko")
@Composable
private fun RecommendationCompletePreview() {
    RecommendationPreviewContent(FlowTripPreviewData.recommendation.copy(
        stageIndex = RecommendationStage.entries.size, place = null, selectedPlaces = listOf(FlowTripPreviewData.place)))
}

@Composable
private fun RecommendationPreviewContent(state: RecommendationUiState) {
    FlowTripTheme {
        RecommendationScreen(
            state = state,
            onSelect = {}, onAnother = {}, onSkip = {}, onRetry = {},
            onConfirmDestination = {}, onDismissDestination = {},
            onHome = {}, onBack = {}, onOpenMap = {},
        )
    }
}
