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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.flowtrip.core.designsystem.FlowInfoCard
import com.github.flowtrip.core.designsystem.FlowPrimaryButton
import com.github.flowtrip.domain.model.RecommendationStage
import com.github.flowtrip.presentation.common.AdaptivePanels
import com.github.flowtrip.presentation.common.ScreenLayout

@Composable
fun RecommendationScreen(state: RecommendationUiState, onSelect: () -> Unit, onAnother: () -> Unit,
    onSkip: () -> Unit, onRetry: () -> Unit, onHome: () -> Unit, onBack: () -> Unit) {
    ScreenLayout(state.request.mode.title, onBack) {
        Text("${state.request.destination} · ${state.request.availableMinutes}분 · ${state.request.preference}",
            style = MaterialTheme.typography.bodyLarge)
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
                    FlowInfoCard {
                        Text(place.name, style = MaterialTheme.typography.headlineMedium)
                        Text(place.description, style = MaterialTheme.typography.bodyLarge)
                        Text("이 장소를 추천하는 이유", style = MaterialTheme.typography.titleLarge)
                        Text(place.reason, style = MaterialTheme.typography.bodyLarge)
                        Text("이동 ${place.distanceKm} km · 약 ${place.travelMinutes}분 (Mock)",
                            style = MaterialTheme.typography.labelLarge)
                    }
                }, second = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        FlowInfoCard {
                            Text("마음에 드는 장소인가요?", style = MaterialTheme.typography.titleLarge)
                            Text("선택하거나 건너뛰면 다음 단계로 이동해요. 다른 장소 추천은 현재 단계의 후보를 바꿔요.",
                                style = MaterialTheme.typography.bodyLarge)
                            Text("선택 ${state.selectedPlaces.size}곳 · 건너뛰기 ${state.skippedPlaces.size}곳")
                        }
                        FlowPrimaryButton("이 장소 선택", onSelect)
                        OutlinedButton(onClick = onAnother, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                            Text("다른 장소 추천")
                        }
                        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                            Text("건너뛰기")
                        }
                    }
                })
            }
        }
    }
}
