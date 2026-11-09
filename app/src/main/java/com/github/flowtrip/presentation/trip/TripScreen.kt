package com.github.flowtrip.presentation.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.flowtrip.core.designsystem.FlowInfoCard
import com.github.flowtrip.core.designsystem.FlowPrimaryButton
import com.github.flowtrip.core.util.availableTimeOptions
import com.github.flowtrip.core.util.formatAvailableTime
import com.github.flowtrip.presentation.common.AdaptivePanels
import com.github.flowtrip.presentation.common.ChoiceGroup
import com.github.flowtrip.presentation.common.ScreenLayout

@Composable
fun TripScreen(state: TripUiState, onDestination: (String) -> Unit, onTime: (Int) -> Unit,
    onPreference: (String) -> Unit, onStart: () -> Unit, onBack: () -> Unit) {
    ScreenLayout("여행을 시작해 볼까요?", onBack) {
        AdaptivePanels(first = {
            FlowInfoCard {
                Text("어디로 떠나나요?", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = state.destination, onValueChange = onDestination,
                    label = { Text("여행 목적지") }, placeholder = { Text("예: 강릉") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("목적지를 직접 입력해 주세요. 실제 장소 검색은 연결하지 않았어요.", style = MaterialTheme.typography.bodyMedium)
            }
        }, second = {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                FlowInfoCard {
                    ChoiceGroup("여행 가능 시간", availableTimeOptions, state.availableMinutes, ::formatAvailableTime, onTime)
                    ChoiceGroup("어떤 여행이 좋나요?", tripPreferences, state.preference, { it }, onPreference)
                }
                FlowPrimaryButton("코스 추천 시작", onStart, enabled = state.canStart)
            }
        })
    }
}
