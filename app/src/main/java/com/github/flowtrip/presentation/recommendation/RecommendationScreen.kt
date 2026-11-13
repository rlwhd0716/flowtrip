package com.github.flowtrip.presentation.recommendation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.github.flowtrip.core.designsystem.FlowTripTheme
import com.github.flowtrip.domain.model.*
import com.github.flowtrip.presentation.common.PlacePhoto
import com.github.flowtrip.presentation.common.FlowIcon
import com.github.flowtrip.presentation.common.FlowIconAsset
import com.github.flowtrip.presentation.common.FlowBrand
import com.github.flowtrip.presentation.common.iconAsset
import com.github.flowtrip.presentation.common.detailIcon
import com.github.flowtrip.presentation.common.preview.FlowTripPreviewData

private val Green = Color(0xFF006B59)
private val Ink = Color(0xFF142F35)
private val Mint = Color(0xFFE8F3EF)

@Composable
fun RecommendationScreen(
    state: RecommendationUiState, onSelect: () -> Unit, onAnother: () -> Unit,
    onNextCourse: () -> Unit, onRetry: () -> Unit, onRetryRoute: () -> Unit,
    onHome: () -> Unit, onBack: () -> Unit,
    onCandidate: (Int) -> Unit = {}, onMode: (TravelMode) -> Unit = {}, onRestart: () -> Unit = {},
) = FlowTripTheme(darkTheme = false) {
    val busy = state.isRequestingRoute || state.isOpeningMap || state.isLoading
    // Apply only to the app window. Insets belong to the Pleos system bars.
    Surface(color = Color(0xFFF8FAF9), contentColor = Ink, modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.safeDrawingPadding().padding(12.dp)) {
            val wide = maxWidth >= 640.dp
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeSelector(state.request.mode, !busy, onMode, onBack, wide)
                CourseProgress(state, !busy, onRestart, wide)
                DemoModeNotice()
                when {
                    state.isLoading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Green) }
                    state.error != null -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Text(state.error)
                        Button(onClick = onRetry) { Text("다시 시도") }
                    }
                    state.isComplete -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("코스 완료", style = MaterialTheme.typography.headlineMedium)
                        Text("선택한 장소 ${state.selectedPlaces.size}곳 · 예시 코스")
                        state.selectedPlaces.forEachIndexed { index, place -> Text("${index + 1}. ${place.name}") }
                        Button(onClick = onRestart) { Text("새 코스 시작") }
                        OutlinedButton(onClick = onHome) { Text("홈으로") }
                    }
                    else -> state.place?.let { place ->
                        if (wide) {
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                RecommendationList(state, !busy, onCandidate, Modifier.weight(0.45f).fillMaxHeight())
                                PlaceDetail(place, Modifier.weight(0.55f).fillMaxHeight())
                            }
                        } else {
                            // Independently scrollable content keeps actions outside the scroll area.
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                RecommendationList(state, !busy, onCandidate, scrollable = false)
                                PlaceDetail(place, scrollable = false)
                            }
                        }
                        val message = state.navigationError ?: when {
                            state.isRequestingRoute -> "${place.destination?.name.orEmpty()} 경로 결과 확인 중…"
                            state.navigationDiffers -> "내비게이션은 다른 목적지를 안내 중입니다."
                            state.navigation.route == RouteState.GUIDING && state.navigation.destination != null ->
                                if (state.navigation.source == DataSource.PLEOS) "${state.navigation.destination.name} 안내 상태 확인됨"
                                else "Mock 길안내 확인 · 실제 SDK 요청 아님"
                            else -> state.testRouteMessage
                        }
                        message?.let {
                            Text(it, color = if (state.navigationError != null) MaterialTheme.colorScheme.error else Green,
                                fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        NavigationAction(place, busy, onSelect)
                        CourseAction(state, busy, onAnother, onNextCourse, onRetryRoute)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(mode: TravelMode, enabled: Boolean, onMode: (TravelMode) -> Unit, onBack: () -> Unit, wide: Boolean) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))) {
        PlacePhoto(if (mode == TravelMode.TRIP) PlaceImage.FOREST else PlaceImage.NIGHT,
            Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color.White, Color(0xE6FFFFFF), Color(0x55FFFFFF)))))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, enabled = enabled) {
                    FlowIcon(FlowIconAsset.BACK, tint = if (enabled) Green else Color.Gray, description = "뒤로")
                }
                FlowBrand(mode, Modifier.weight(1f), fontSize = if (wide) 24.sp else 22.sp, showMode = wide)
                if (wide) ModeButtons(mode, enabled, onMode)
            }
            if (!wide) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { ModeButtons(mode, enabled, onMode) }
        }
    }
}

@Composable
private fun ModeButtons(mode: TravelMode, enabled: Boolean, onMode: (TravelMode) -> Unit) {
    TravelMode.entries.forEach { item ->
        FilterChip(selected = mode == item, onClick = { onMode(item) }, enabled = enabled,
            label = { Text(item.title, fontSize = 15.sp) }, modifier = Modifier.padding(start = 6.dp).heightIn(min = 48.dp))
    }
}

@Composable
private fun CourseProgress(state: RecommendationUiState, enabled: Boolean, onRestart: () -> Unit, wide: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        state.request.mode.stages.forEachIndexed { index, stage ->
            val active = state.stageIndex == index
            Surface(color = if (active) Green else Mint, shape = RoundedCornerShape(24.dp), modifier = Modifier.weight(1f)) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (state.stageIndex > index) FlowIcon(FlowIconAsset.CHECK, Modifier.size(18.dp))
                    else Text("${index + 1}", color = if (active) Color.White else Ink, fontSize = 14.sp)
                    if (wide) FlowIcon(stage.iconAsset(), Modifier.size(22.dp), tint = if (active) Color.White else Ink)
                    val label = when (stage) {
                        RecommendationStage.RESTAURANT -> "식당"
                        RecommendationStage.NIGHT_VIEW -> "야경"
                        RecommendationStage.DESSERT -> "디저트"
                        else -> stage.title
                    }
                    Text(label, Modifier.weight(1f), color = if (active) Color.White else Ink,
                        fontSize = if (wide) 17.sp else 13.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                }
            }
        }
        OutlinedButton(onClick = onRestart, enabled = enabled, contentPadding = PaddingValues(10.dp), modifier = Modifier.heightIn(min = 48.dp)) {
            FlowIcon(FlowIconAsset.REFRESH, Modifier.size(20.dp), tint = if (enabled) Green else Color.Gray)
            Spacer(Modifier.width(6.dp))
            Text(if (wide) "코스 다시 추천" else "다시", fontSize = 14.sp)
        }
    }
}

@Composable
private fun DemoModeNotice() {
    Surface(color = Color(0xFFFFEEDD), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowIcon(FlowIconAsset.INFO, Modifier.size(20.dp), tint = Color(0xFF975016))
            Text("시연 모드 · 추천 장소와 실제 길안내 목적지가 다릅니다.",
                Modifier.weight(1f), color = Color(0xFF975016), fontSize = 14.sp)
        }
    }
}

@Composable
private fun RecommendationList(state: RecommendationUiState, enabled: Boolean, onCandidate: (Int) -> Unit,
    modifier: Modifier = Modifier, scrollable: Boolean = true) {
    val scrollState = rememberScrollState()
    Column(if (scrollable) modifier.verticalScroll(scrollState) else modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val listIcon = if (state.request.mode == TravelMode.TRIP) FlowIconAsset.TRIP_RESULT
                else if (state.place?.stage == RecommendationStage.RESTAURANT) FlowIconAsset.DATE_RESULT
                else state.place?.stage?.iconAsset() ?: FlowIconAsset.DATE_RESULT
            FlowIcon(listIcon, Modifier.size(30.dp))
            Text("추천 ${state.place?.stage?.title.orEmpty()}", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
        state.candidates.forEachIndexed { index, place ->
            RecommendationCard(place, state.candidateIndex == index, enabled, { onCandidate(index) })
        }
    }
}

@Composable
private fun RecommendationCard(place: Place, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(Modifier.fillMaxWidth().clip(shape).background(if (selected) Mint else Color.White)
        .border(if (selected) 2.dp else 1.dp, if (selected) Green else Color(0xFFDDE5E3), shape)
        .testTag("candidate-${place.id}")
        .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        PlacePhoto(place.image, Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(place.name, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Text("${place.stage.title} · 예시 정보", fontSize = 13.sp, color = Green)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FlowIcon(FlowIconAsset.TRIP_LOCATION, Modifier.size(16.dp))
                Text(place.destination?.let { "길안내 테스트: ${it.name}" } ?: "테스트 목적지 미설정", fontSize = 13.sp,
                    modifier = Modifier.weight(1f))
            }
        }
        FlowIcon(if (selected) FlowIconAsset.CHECK else FlowIconAsset.RADIO_OFF, Modifier.size(26.dp))
    }
}

@Composable
private fun PlaceDetail(place: Place, modifier: Modifier = Modifier, scrollable: Boolean = true) {
    val scrollState = rememberScrollState()
    val container = modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).padding(12.dp)
    Column(if (scrollable) container.verticalScroll(scrollState) else container,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().height(154.dp).clip(RoundedCornerShape(16.dp))) {
            PlacePhoto(place.image, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC102D29)))))
            Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                Text(place.name, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("detail-title"))
                Text("예시 이미지 · ${place.stage.title}", color = Color.White, fontSize = 13.sp)
            }
        }
        Text(place.description, fontSize = 16.sp)
        place.details.forEach { detail ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowIcon(detailIcon(detail.label), Modifier.size(22.dp), tint = Ink)
                Text(detail.label, Modifier.weight(0.4f), fontSize = 15.sp, color = Green)
                Text(detail.value, Modifier.weight(0.6f), fontSize = 15.sp)
            }
            HorizontalDivider(color = Color(0xFFE2E9E6))
        }
        Text(place.reason, fontSize = 14.sp, color = Green)
        Text("상세 정보와 이미지는 Mock 예시입니다. 실제 가격·운영·주차 정보는 방문 전 확인해 주세요.", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
private fun NavigationAction(place: Place, busy: Boolean, onSelect: () -> Unit) {
    Button(onClick = onSelect, enabled = !busy && place.destination != null,
        colors = ButtonDefaults.buttonColors(containerColor = Green), shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("navigation-action")) {
        FlowIcon(FlowIconAsset.NAVIGATE, Modifier.size(24.dp), tint = if (!busy && place.destination != null) Color.White else Color.Gray)
        Spacer(Modifier.width(10.dp))
        Text(place.destination?.let { "${it.name}${if (it.name == "42dot") "으로" else "로"} 길안내 테스트" }
            ?: "길안내 목적지 미설정", fontSize = 18.sp)
    }
}

@Composable
private fun CourseAction(state: RecommendationUiState, busy: Boolean, onAnother: () -> Unit, onNext: () -> Unit,
    onRetryRoute: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onAnother, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
            Text("다음 추천", fontSize = 16.sp)
            FlowIcon(FlowIconAsset.NEXT, Modifier.size(20.dp), tint = if (busy) Color.Gray else Ink)
        }
        OutlinedButton(onClick = onNext, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
            Text(if (state.stageIndex == state.request.mode.stages.lastIndex) "코스 완료" else "다음 코스", fontSize = 16.sp)
            FlowIcon(if (state.stageIndex == state.request.mode.stages.lastIndex) FlowIconAsset.CHECK else FlowIconAsset.NEXT,
                Modifier.padding(start = 4.dp).size(20.dp),
                tint = if (state.stageIndex == state.request.mode.stages.lastIndex) null else if (busy) Color.Gray else Green)
        }
        if (state.navigationError != null) OutlinedButton(onClick = onRetryRoute, enabled = !busy && state.place?.destination != null,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("재시도", fontSize = 16.sp) }
    }
}

@Preview(widthDp = 1100, heightDp = 720, locale = "ko")
@Preview(widthDp = 900, heightDp = 480, locale = "ko")
@Preview(widthDp = 480, heightDp = 780, locale = "ko")
@Composable
private fun RecommendationPreview() = FlowTripTheme(darkTheme = false) {
    RecommendationScreen(FlowTripPreviewData.recommendation, {}, {}, {}, {}, {}, {}, {})
}

@Preview(name = "여행 모드", widthDp = 1100, heightDp = 720, locale = "ko")
@Composable
private fun TravelRecommendationPreview() = FlowTripTheme(darkTheme = false) {
    RecommendationScreen(FlowTripPreviewData.tripRecommendation, {}, {}, {}, {}, {}, {}, {})
}
