package com.github.flowtrip.presentation.recommendation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.github.flowtrip.core.designsystem.*
import com.github.flowtrip.core.designsystem.preview.FlowTripScreenPreviews
import com.github.flowtrip.presentation.common.preview.FlowTripPreviewData
import com.github.flowtrip.domain.model.*
import com.github.flowtrip.presentation.common.AdaptivePanels
import com.github.flowtrip.presentation.common.ScreenLayout
import com.github.flowtrip.presentation.common.PlatformStatusCard

@Composable
fun RecommendationScreen(
    state: RecommendationUiState, onSelect: () -> Unit, onAnother: () -> Unit,
    onNextCourse: () -> Unit, onSkip: () -> Unit, onRetry: () -> Unit, onRetryRoute: () -> Unit,
    onCourseRoute: () -> Unit, onHome: () -> Unit, onBack: () -> Unit, onOpenMap: () -> Unit,
    onTestRoute: (String, Boolean) -> Unit,
) {
    val busy = state.isRequestingRoute || state.isOpeningMap
    ScreenLayout(state.request.mode.title, onBack = if (busy) null else onBack, compact = true) {
        Text("${state.request.destination} · ${state.request.availableMinutes}분 · ${state.request.preference}",
            style = MaterialTheme.typography.bodyMedium)
        NavigationTestPanel(state, onTestRoute, onRetryRoute, onOpenMap)
        when {
            state.isLoading -> CircularProgressIndicator()
            state.error != null -> FlowInfoCard {
                Text(state.error)
                FlowPrimaryButton("다시 시도", onRetry)
            }
            state.isComplete -> FlowInfoCard {
                Text("오늘의 코스를 살펴보세요", style = MaterialTheme.typography.headlineSmall)
                Text("선택 ${state.selectedPlaces.size}곳 · 건너뛰기 ${state.skippedPlaces.size}곳")
                state.selectedPlaces.forEachIndexed { index, place -> Text("${index + 1}. ${place.name} · ${place.stage.title}") }
                Text("코스 완료는 내비게이션 안내를 종료하지 않습니다.")
                FlowPrimaryButton("홈으로", onHome, enabled = !busy)
            }
            else -> state.place?.let { place ->
                Text("현재 코스 ${state.stageIndex + 1}/${RecommendationStage.entries.size} · ${place.stage.title}",
                    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                // In one Pleos window these are stacked. Expanded windows expose details beside controls.
                AdaptivePanels(first = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FlowInfoCard {
                            Text("추천 후보 ${state.candidateIndex + 1}/${state.candidates.size.coerceAtLeast(1)}",
                                style = MaterialTheme.typography.labelLarge)
                            Text(place.name, style = MaterialTheme.typography.headlineSmall)
                            Text(place.description)
                            Text("약 ${place.visitMinutes}분 · ${place.expectedSpend}")
                            Text(if (state.isCourseRoute) "안내 요청 범위: 남은 코스" else "안내 요청 범위: 현재 장소")
                        }
                        if (state.isRequestingRoute) Text("${state.pendingPlaceName} 경로 확인 중… 기존 추천을 유지합니다.")
                        state.navigationError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                            OutlinedButton(onClick = onRetryRoute, enabled = !busy,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("경로 다시 시도") }
                        }
                        if (state.navigationDiffers) Text("내비게이션의 현재 경로가 이 추천과 다릅니다. 안내 상태를 확인해 주세요.",
                            color = MaterialTheme.colorScheme.error)
                        FlowPrimaryButton("이 장소로 길안내", onSelect, enabled = !busy)
                        FlowPrimaryButton("다음 추천 경로", onAnother, enabled = !busy && state.candidates.size > 1)
                        OutlinedButton(onClick = onNextCourse, enabled = !busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                            Text(if (state.stageIndex == RecommendationStage.entries.lastIndex) "코스 완료" else "다음 코스")
                        }
                        Text("길안내 버튼은 현재 목적지를 변경합니다. 다음 코스는 현재 단계를 완료하고 다음 장소로 안내합니다.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }, second = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FlowInfoCard {
                            Text("장소 정보", style = MaterialTheme.typography.titleMedium)
                            place.destination?.address?.takeIf { it.isNotBlank() }?.let { Text(it) }
                            Text("주차 · ${place.parkingSummary}")
                            Text(place.reason)
                            Text("장소·비용·체류·주차 정보는 시연용 예시입니다.", style = MaterialTheme.typography.bodySmall)
                            if (state.stageIndex < RecommendationStage.entries.lastIndex) {
                                OutlinedButton(onClick = onCourseRoute, enabled = !busy,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("남은 코스 경유 안내") }
                                Text("경유 안내 순서: ${state.remainingCourse.joinToString(" → ") { it.name }}",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = onSkip, enabled = !busy,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("이 단계 건너뛰기") }
                        }
                        FlowInfoCard {
                            Text("Pleos 내비게이션", style = MaterialTheme.typography.titleMedium)
                            Text("두 앱의 위치와 크기는 Pleos window bar에서 조절해 주세요.")
                            OutlinedButton(onClick = onOpenMap,
                                enabled = !busy && state.navigation.source == DataSource.PLEOS,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                                Text(if (state.isOpeningMap) "화면 요청 중…" else "내비게이션 화면 요청")
                            }
                            state.mapMessage?.let { Text(it) }
                            if (state.navigation.source == DataSource.MOCK) Text("Mock에서는 경로 변경만 시뮬레이션합니다.")
                        }
                    }
                })
            }
        }
        state.lastConfirmedDestination?.let { Text("마지막 확인 목적지: ${it.name}", style = MaterialTheme.typography.bodySmall) }
        if (state.navigation.service != ServiceStatus.READY && state.previousRouteState != null)
            Text("이전 경로 기록이 있습니다. 현재 안내는 연결 후 다시 확인합니다.", style = MaterialTheme.typography.bodySmall)
        PlatformStatusCard(state.navigation, state.location, state.arrivalSignal)
    }
}

@FlowTripScreenPreviews
@Composable
private fun RecommendationScreenPreview() = RecommendationPreviewContent(FlowTripPreviewData.recommendation)

@Preview(name = "좁은 분할 창", widthDp = 360, heightDp = 720, locale = "ko")
@Composable
private fun NarrowPanelPreview() = RecommendationPreviewContent(FlowTripPreviewData.recommendation)

@Preview(name = "경로 변경 실패", widthDp = 480, heightDp = 960, locale = "ko")
@Composable
private fun RouteFailurePreview() = RecommendationPreviewContent(FlowTripPreviewData.recommendation.copy(
    navigationError = "새 경로 탐색에 실패했습니다. 기존 추천은 유지됩니다."))

@Preview(name = "경로 확인 중", widthDp = 480, heightDp = 960, locale = "ko")
@Composable
private fun RoutingPreview() = RecommendationPreviewContent(FlowTripPreviewData.recommendation.copy(
    isRequestingRoute = true, pendingPlaceName = "다음 추천 장소"))

@Preview(name = "코스 완료", widthDp = 1280, heightDp = 720, locale = "ko")
@Composable
private fun RecommendationCompletePreview() = RecommendationPreviewContent(FlowTripPreviewData.recommendation.copy(
    stageIndex = RecommendationStage.entries.size, place = null, selectedPlaces = listOf(FlowTripPreviewData.place)))

@Composable
private fun RecommendationPreviewContent(state: RecommendationUiState) {
    FlowTripTheme {
        RecommendationScreen(state, onSelect = {}, onAnother = {}, onNextCourse = {}, onSkip = {},
            onRetry = {}, onRetryRoute = {}, onCourseRoute = {}, onHome = {}, onBack = {}, onOpenMap = {}, onTestRoute = { _, _ -> })
    }
}
