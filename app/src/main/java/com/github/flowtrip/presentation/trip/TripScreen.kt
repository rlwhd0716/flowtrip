package com.github.flowtrip.presentation.trip

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.github.flowtrip.core.util.AvailableTimeUnit
import com.github.flowtrip.data.model.DemoRegionCatalog
import com.github.flowtrip.domain.model.TravelMode
import com.github.flowtrip.presentation.common.*

private val tripChoices = tripPreferences.map { label -> FormChoice(label, when (label) {
    "자연·경관" -> FlowIconAsset.NATURE
    "관광·명소" -> FlowIconAsset.CAMERA
    "맛집" -> FlowIconAsset.FOOD
    "카페·디저트" -> FlowIconAsset.CAFE
    "역사·문화" -> FlowIconAsset.HISTORY
    "예술·전시" -> FlowIconAsset.ART
    "쇼핑" -> FlowIconAsset.SHOPPING
    "액티비티·체험" -> FlowIconAsset.CAMPING
    else -> FlowIconAsset.MORE
}) }

@Composable
fun TripScreen(state: TripUiState, onProvince: (String) -> Unit, onDistrict: (String) -> Unit,
    onTime: (Int) -> Unit, onUnit: (AvailableTimeUnit) -> Unit, onPreference: (String) -> Unit,
    onStart: () -> Unit, onBack: () -> Unit) {
    FlowFormLayout(TravelMode.TRIP, "여행을 시작해 볼까요?", "간단한 정보만으로, 지금 떠날 수 있는 여행 코스를 추천해 드릴게요.",
        onBack, "여행 코스 추천하기", state.canStart, onStart) {
        InputSection("여행 목적지", FlowIconAsset.TRIP_LOCATION, inline = true) {
            BoxWithConstraints {
                if (maxWidth >= 400.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RegionDropdown("시/도 선택", state.province, DemoRegionCatalog.districts.keys.toList(), true, "province-select", onProvince, Modifier.weight(1f))
                        FlowIcon(FlowIconAsset.NEXT, Modifier.size(22.dp))
                        RegionDropdown("시/군/구 선택", state.district, DemoRegionCatalog.districts[state.province].orEmpty(),
                            state.province.isNotBlank(), "district-select", onDistrict, Modifier.weight(1f))
                    }
                } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RegionDropdown("시/도 선택", state.province, DemoRegionCatalog.districts.keys.toList(), true, "province-select", onProvince)
                    RegionDropdown("시/군/구 선택", state.district, DemoRegionCatalog.districts[state.province].orEmpty(),
                        state.province.isNotBlank(), "district-select", onDistrict)
                }
            }
        }
        InputSection("여행 가능 시간", FlowIconAsset.TRIP_DURATION, inline = true) {
            TimeSelector(state.availableMinutes, state.timeUnit, onTime, onUnit)
        }
        InputSection("여행 관심사", FlowIconAsset.TRIP_INTERESTS, "챙겨보고 싶은 여행 테마를 선택해 주세요. (${state.preferences.size}/3)") {
            ChoiceGrid(tripChoices, state.preferences, 3, onPreference)
        }
        Text("선택 가능한 지역은 시연용 주요 지역 목록입니다. 코스와 장소 정보는 예시 데이터로 제공됩니다.", fontSize = 13.sp)
    }
}

@Preview(name = "여행 입력 · 가로", widthDp = 1100, heightDp = 720, locale = "ko")
@Preview(name = "여행 입력 · 분할", widthDp = 480, heightDp = 780, locale = "ko")
@Composable
private fun TripScreenPreview() = TripScreen(TripUiState(province = "부산광역시", district = "해운대구"), {}, {}, {}, {}, {}, {}, {})
