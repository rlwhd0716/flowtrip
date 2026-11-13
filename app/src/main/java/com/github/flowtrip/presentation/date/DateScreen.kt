package com.github.flowtrip.presentation.date

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.github.flowtrip.core.util.AvailableTimeUnit
import com.github.flowtrip.domain.model.DataSource
import com.github.flowtrip.domain.model.TravelMode
import com.github.flowtrip.presentation.common.*

private val moodChoices = dateMoods.map { label -> FormChoice(label, when (label) {
    "로맨틱" -> FlowIconAsset.ROMANTIC
    "편안한 힐링" -> FlowIconAsset.RELAX
    "특별한 경험" -> FlowIconAsset.SPECIAL
    "활동적인 데이트" -> FlowIconAsset.ACTIVE
    else -> FlowIconAsset.MORE
}) }
private val typeChoices = datePlaceTypes.map { label -> FormChoice(label, when (label) {
    "맛집" -> FlowIconAsset.DATE_FOOD
    "야경·전망" -> FlowIconAsset.DATE_NIGHT
    "카페·디저트" -> FlowIconAsset.DATE_CAFE
    "자연·산책" -> FlowIconAsset.DATE_NATURE
    "문화·예술" -> FlowIconAsset.DATE_ART
    "쇼핑" -> FlowIconAsset.DATE_SHOPPING
    "액티비티·체험" -> FlowIconAsset.DATE_ACTIVITY
    "드라이브 코스" -> FlowIconAsset.DATE_DRIVING
    "역사·전시" -> FlowIconAsset.DATE_HISTORY
    else -> FlowIconAsset.MORE
}) }

@Composable
fun DateScreen(state: DateUiState, onTime: (Int) -> Unit, onUnit: (AvailableTimeUnit) -> Unit,
    onMood: (String) -> Unit, onPlaceType: (String) -> Unit, onNewPlaces: (Boolean) -> Unit,
    onStart: () -> Unit, onBack: () -> Unit) {
    FlowFormLayout(TravelMode.DATE, "오늘은 새로운 데이트를 떠나볼까요?", "지금 있는 곳에서, 주어진 시간 안에 즐길 수 있는 데이트 코스를 추천해 드릴게요.",
        onBack, "데이트 코스 추천하기", true, onStart) {
        InputSection("지금 있는 곳", FlowIconAsset.DATE_LOCATION, inline = true) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.currentLocation, fontSize = 19.sp)
                Text(if (state.locationSource == DataSource.MOCK) "실제 GPS가 아닌 시연용 위치입니다."
                    else "차량 위치는 앱 사용 중 업데이트에 따라 달라질 수 있습니다.", fontSize = 13.sp)
            }
        }
        InputSection("데이트 가능 시간", FlowIconAsset.DATE_DURATION, inline = true) {
            TimeSelector(state.availableMinutes, state.timeUnit, onTime, onUnit)
        }
        InputSection("오늘의 분위기", FlowIconAsset.DATE_MOOD, "원하는 데이트 분위기를 선택해 주세요. (${state.moods.size}/2)") {
            ChoiceGrid(moodChoices, state.moods, 2, onMood)
        }
        InputSection("가고 싶은 장소", FlowIconAsset.DATE_PLACES, "함께 가고 싶은 장소 유형을 선택해 주세요. (복수 선택 가능)") {
            ChoiceGrid(typeChoices, state.placeTypes, datePlaceTypes.size, onPlaceType, maxColumns = 5)
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .toggleable(value = state.preferNewPlaces, role = Role.Switch, onValueChange = onNewPlaces),
            verticalAlignment = Alignment.CenterVertically) {
            Text("새로운 장소 위주로 추천", Modifier.weight(1f), fontSize = 16.sp)
            Switch(checked = state.preferNewPlaces, onCheckedChange = null)
        }
    }
}

@Preview(name = "데이트 입력 · 가로", widthDp = 1100, heightDp = 720, locale = "ko")
@Preview(name = "데이트 입력 · 분할", widthDp = 480, heightDp = 780, locale = "ko")
@Composable
private fun DateScreenPreview() = DateScreen(DateUiState(currentLocation = "서울 성수동 · 예시 위치"), {}, {}, {}, {}, {}, {}, {})
