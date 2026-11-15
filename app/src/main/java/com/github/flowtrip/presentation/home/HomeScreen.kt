package com.github.flowtrip.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.github.flowtrip.core.designsystem.FlowTripTheme
import com.github.flowtrip.domain.model.Place
import com.github.flowtrip.domain.model.TravelMode
import com.github.flowtrip.presentation.common.*
import com.github.flowtrip.presentation.common.preview.FlowTripPreviewData

@Composable
fun HomeScreen(onTrip: () -> Unit, onDate: () -> Unit, state: HomeUiState = HomeUiState(), onReuse: (Place) -> Unit = {}) = FlowTripTheme(darkTheme = false) {
    Surface(Modifier.fillMaxSize(), color = Color(0xFFF8FBFA), contentColor = FlowInk) {
        BoxWithConstraints(Modifier.safeDrawingPadding().padding(20.dp)) {
            val wide = maxWidth >= 720.dp
            val compact = maxHeight < 560.dp
            val recentColumns = if (maxWidth >= 1000.dp) 4 else if (maxWidth >= 560.dp) 2 else 1
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 24.dp)) {
                FlowBrand(fontSize = 29.sp)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("다음 순간이 기대되는 여정", fontSize = if (wide) 34.sp else 27.sp, fontWeight = FontWeight.Bold)
                    Text("지금, 어떤 여정을 떠나고 싶으신가요? 원하는 모드를 선택해 시작해 보세요.", fontSize = 17.sp)
                }
                if (wide) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ModeCard(TravelMode.TRIP, onTrip, Modifier.weight(1f), compact)
                    ModeCard(TravelMode.DATE, onDate, Modifier.weight(1f), compact)
                } else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModeCard(TravelMode.TRIP, onTrip, compact = compact)
                    ModeCard(TravelMode.DATE, onDate, compact = compact)
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FlowIcon(FlowIconAsset.RECENT, Modifier.size(30.dp))
                        Text("최근 가본 장소", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(if (state.isExample) "예시 장소입니다. 실제 방문 기록이 아닙니다."
                        else "최근 코스 선택 기록입니다. 실제 방문 여부는 확인하지 않습니다.", fontSize = 14.sp)
                    if (state.recentPlaces.isEmpty()) Text("코스에서 선택한 장소가 여기에 표시됩니다.", fontSize = 16.sp)
                    state.recentPlaces.chunked(recentColumns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { place -> RecentPlaceCard(place, { onReuse(place) }, Modifier.weight(1f)) }
                            repeat(recentColumns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeCard(mode: TravelMode, onClick: () -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    Card(onClick = onClick, shape = RoundedCornerShape(24.dp), modifier = modifier.fillMaxWidth().height(if (compact) 225.dp else 248.dp),
        colors = CardDefaults.cardColors(containerColor = FlowMint)) {
        Box(Modifier.fillMaxSize()) {
            ReferencePhoto(if (mode == TravelMode.TRIP) ReferenceImage.HOME_TRIP else ReferenceImage.HOME_DATE, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF5E9F5EF), Color(0xE6EFF7F1), Color(0x00FFFFFF)))))
            Column(Modifier.fillMaxSize().padding(if (compact) 16.dp else 22.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)) {
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xCCCEEADF)).padding(8.dp)) {
                        FlowIcon(if (mode == TravelMode.TRIP) FlowIconAsset.NATURE else FlowIconAsset.ROMANTIC, Modifier.size(if (compact) 26.dp else 30.dp))
                    }
                    Text(mode.title, fontSize = if (compact) 24.sp else 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                    Text(if (mode == TravelMode.TRIP) "목적지를 정하고,\n여정 속 새로운 장소를 만나세요."
                        else "지금 있는 곳에서,\n함께 갈 새로운 장소를 찾아보세요.", fontSize = 16.sp, lineHeight = 22.sp)
                }
                Box(Modifier.clip(RoundedCornerShape(24.dp)).background(FlowGreen).padding(horizontal = 26.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("시작하기", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        FlowIcon(FlowIconAsset.NEXT, Modifier.size(22.dp), tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentPlaceCard(place: Place, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(onClick = onClick, shape = RoundedCornerShape(18.dp), modifier = modifier.testTag("reuse-${place.id}"),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (place.provider == com.github.flowtrip.domain.model.DataSource.MOCK)
                PlacePhoto(place.image, Modifier.size(width = 68.dp, height = 88.dp).clip(RoundedCornerShape(12.dp)))
            else Box(Modifier.size(width = 68.dp, height = 88.dp), contentAlignment = Alignment.Center) { Text("사진 미제공", fontSize = 12.sp) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(place.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(place.stage.title, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FlowIcon(FlowIconAsset.REFRESH, Modifier.size(18.dp))
                    Text("다시 이용하기", color = FlowGreen, fontSize = 14.sp)
                }
            }
        }
    }
}

@Preview(name = "메인 · 가로", widthDp = 1100, heightDp = 720, locale = "ko")
@Preview(name = "메인 · 분할", widthDp = 480, heightDp = 780, locale = "ko")
@Composable
private fun HomeScreenPreview() = HomeScreen({}, {}, HomeUiState(
    FlowTripPreviewData.recommendation.remainingCourse + listOfNotNull(FlowTripPreviewData.tripRecommendation.place)))
