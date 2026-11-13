package com.github.flowtrip.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.flowtrip.R
import com.github.flowtrip.domain.model.RecommendationStage
import com.github.flowtrip.domain.model.TravelMode

/** User-supplied transparent artwork. Crops exclude accidentally extracted card borders. */
enum class FlowIconAsset(val resource: Int, val crop: IconRegion? = null) {
    LOGO(R.drawable.ft_icon_date_header_heart_logo),
    NATURE(R.drawable.ft_icon_category_nature_mountain),
    CAMERA(R.drawable.ft_icon_category_landmark_camera),
    FOOD(R.drawable.ft_icon_category_restaurant_utensils, IconRegion(116, 56, 148, 148)),
    CAFE(R.drawable.ft_icon_category_cafe_coffee),
    HISTORY(R.drawable.ft_icon_category_history_temple),
    ART(R.drawable.ft_icon_category_art_palette),
    SHOPPING(R.drawable.ft_icon_category_shopping_bag),
    CAMPING(R.drawable.ft_icon_category_activity_camping),
    TRIP_LOCATION(R.drawable.ft_icon_travel_location_pin),
    TRIP_DURATION(R.drawable.ft_icon_travel_duration_clock),
    TRIP_INTERESTS(R.drawable.ft_icon_travel_interests_sparkles),
    DATE_LOCATION(R.drawable.ft_icon_date_location_pin),
    DATE_DURATION(R.drawable.ft_icon_date_duration_clock),
    DATE_MOOD(R.drawable.ft_icon_date_mood_sparkles),
    DATE_PLACES(R.drawable.ft_icon_date_places_layers),
    ROMANTIC(R.drawable.ft_icon_mood_romantic_heart, IconRegion(72, 52, 132, 116)),
    RELAX(R.drawable.ft_icon_mood_relaxing_coffee),
    SPECIAL(R.drawable.ft_icon_mood_special_star, IconRegion(124, 56, 140, 140)),
    ACTIVE(R.drawable.ft_icon_mood_active_people),
    DATE_FOOD(R.drawable.ft_icon_date_category_food, IconRegion(108, 40, 112, 124)),
    DATE_NIGHT(R.drawable.ft_icon_date_category_night_camera),
    DATE_CAFE(R.drawable.ft_icon_date_category_cafe),
    DATE_NATURE(R.drawable.ft_icon_date_category_nature),
    DATE_ART(R.drawable.ft_icon_date_category_art),
    DATE_SHOPPING(R.drawable.ft_icon_date_category_shopping),
    DATE_ACTIVITY(R.drawable.ft_icon_date_category_activity_gamepad),
    DATE_DRIVING(R.drawable.ft_icon_date_category_driving_car),
    DATE_HISTORY(R.drawable.ft_icon_date_category_history),
    RECENT(R.drawable.ft_icon_recent_history_clock),
    TRIP_RESULT(R.drawable.ft_icon_result_travel_compass),
    DATE_RESULT(R.drawable.ft_icon_result_date_utensils),
    CHECK(R.drawable.ft_ui_check_circle),
    RADIO_OFF(R.drawable.ft_ui_radio_off),
    REFRESH(R.drawable.ft_ui_refresh),
    INFO(R.drawable.ft_ui_info),
    NEXT(R.drawable.ft_ui_chevron_right),
    BACK(R.drawable.ft_ui_chevron_left),
    DOWN(R.drawable.ft_ui_chevron_down),
    NAVIGATE(R.drawable.ft_ui_navigation),
    PLUS(R.drawable.ft_ui_plus),
    MINUS(R.drawable.ft_ui_minus),
    MORE(R.drawable.ft_ui_more),
    PRICE(R.drawable.ft_ui_price),
    MENU(R.drawable.ft_ui_menu),
}

data class IconRegion(val x: Int, val y: Int, val width: Int, val height: Int)

@Composable
fun FlowIcon(asset: FlowIconAsset, modifier: Modifier = Modifier, tint: Color? = null, description: String? = null) {
    val region = asset.crop
    val painter = if (region != null) {
        val bitmap = ImageBitmap.imageResource(asset.resource)
        BitmapPainter(bitmap, IntOffset(region.x, region.y), IntSize(region.width, region.height))
    } else painterResource(asset.resource)
    // Adjacent text/selection semantics provide the accessible label.
    Image(painter, contentDescription = description, modifier = modifier.size(28.dp), contentScale = ContentScale.Fit,
        colorFilter = tint?.let { ColorFilter.tint(it) })
}

@Composable
fun FlowBrand(mode: TravelMode? = null, modifier: Modifier = Modifier, fontSize: TextUnit = 26.sp, showMode: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowIcon(if (mode == null) FlowIconAsset.NATURE else FlowIconAsset.LOGO, Modifier.size(34.dp))
        Text(if (showMode && mode != null) "FlowTrip  |  ${mode.title}" else "FlowTrip",
            fontSize = fontSize, lineHeight = 30.sp, fontWeight = FontWeight.Bold, color = FlowInk,
            modifier = Modifier.weight(1f))
    }
}

fun RecommendationStage.iconAsset(): FlowIconAsset = when (this) {
    RecommendationStage.ATTRACTION -> FlowIconAsset.CAMERA
    RecommendationStage.LOCAL_FOOD -> FlowIconAsset.FOOD
    RecommendationStage.CAFE -> FlowIconAsset.CAFE
    RecommendationStage.RESTAURANT -> FlowIconAsset.DATE_FOOD
    RecommendationStage.NIGHT_VIEW -> FlowIconAsset.DATE_NIGHT
    RecommendationStage.DESSERT -> FlowIconAsset.DATE_CAFE
}

fun detailIcon(label: String): FlowIconAsset = when (label) {
    "대표 메뉴" -> FlowIconAsset.MENU
    "예상 가격", "입장료" -> FlowIconAsset.PRICE
    "주차 정보" -> FlowIconAsset.DATE_DRIVING
    "관람 포인트" -> FlowIconAsset.DATE_NIGHT
    "체류 시간", "예상 관람 시간", "추천 방문 시간" -> FlowIconAsset.TRIP_DURATION
    else -> FlowIconAsset.NATURE
}
