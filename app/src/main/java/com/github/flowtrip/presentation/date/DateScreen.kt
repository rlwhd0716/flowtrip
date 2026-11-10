package com.github.flowtrip.presentation.date

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.github.flowtrip.core.designsystem.FlowInfoCard
import com.github.flowtrip.core.designsystem.FlowPrimaryButton
import com.github.flowtrip.core.util.availableTimeOptions
import com.github.flowtrip.core.util.formatAvailableTime
import com.github.flowtrip.presentation.common.AdaptivePanels
import com.github.flowtrip.presentation.common.ChoiceGroup
import com.github.flowtrip.presentation.common.ScreenLayout
import com.github.flowtrip.core.designsystem.FlowTripTheme
import com.github.flowtrip.core.designsystem.preview.FlowTripScreenPreviews

@Composable
fun DateScreen(state: DateUiState, onTime: (Int) -> Unit, onMood: (String) -> Unit,
    onNewPlaces: (Boolean) -> Unit, onStart: () -> Unit, onBack: () -> Unit) {
    ScreenLayout("오늘은 새로운 데이트", onBack) {
        AdaptivePanels(first = {
            FlowInfoCard {
                Text("지금 있는 곳", style = MaterialTheme.typography.titleLarge)
                Text(state.currentLocation, style = MaterialTheme.typography.headlineMedium)
                Text(if (state.locationSource == com.github.flowtrip.domain.model.DataSource.MOCK)
                    "실제 위치를 수집하지 않는 예시 위치입니다." else "차량 위치는 앱 사용 중 메모리에서만 처리합니다.", style = MaterialTheme.typography.bodyMedium)
            }
        }, second = {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                FlowInfoCard {
                    ChoiceGroup("데이트 가능 시간", availableTimeOptions, state.availableMinutes, ::formatAvailableTime, onTime)
                    ChoiceGroup("오늘의 분위기", dateMoods, state.mood, { it }, onMood)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.heightIn(min = 56.dp)
                        .toggleable(value = state.preferNewPlaces, role = Role.Switch, onValueChange = onNewPlaces)) {
                        Text("새로운 장소 위주로 추천", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = state.preferNewPlaces, onCheckedChange = null)
                    }
                }
                FlowPrimaryButton("데이트 시작", onStart)
            }
        })
    }
}

@FlowTripScreenPreviews
@Composable
private fun DateScreenPreview() {
    var state by remember { mutableStateOf(DateUiState(currentLocation = "서울 성수동 (Mock)", availableMinutes = 180)) }
    FlowTripTheme {
        DateScreen(
            state = state,
            onTime = { state = state.copy(availableMinutes = it) },
            onMood = { state = state.copy(mood = it) },
            onNewPlaces = { state = state.copy(preferNewPlaces = it) },
            onStart = {}, onBack = {},
        )
    }
}
