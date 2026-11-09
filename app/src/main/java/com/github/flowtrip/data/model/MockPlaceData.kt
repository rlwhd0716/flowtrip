package com.github.flowtrip.data.model

import com.github.flowtrip.domain.model.Place
import com.github.flowtrip.domain.model.RecommendationStage

/** Fictional fixture data. No real venue, routing or location lookup is performed. */
internal data class MockPlaceData(
    val id: String,
    val stage: RecommendationStage,
    val name: String,
    val description: String,
    val reason: String,
    val distanceKm: Double,
    val travelMinutes: Int,
) {
    fun toDomain(context: String) = Place(
        id, stage, name, description, "$reason · $context", distanceKm, travelMinutes,
    )
}

internal val mockPlaces = listOf(
    MockPlaceData("food-1", RecommendationStage.RESTAURANT, "골목의 작은 식탁", "계절 메뉴를 나누며 여유롭게 식사하는 작은 레스토랑", "새로운 식당에서 코스를 시작해 보세요", 2.4, 8),
    MockPlaceData("food-2", RecommendationStage.RESTAURANT, "온기 키친", "따뜻한 한 접시와 편안한 대화가 있는 식당", "색다른 메뉴를 함께 즐기기 좋은 첫 장소예요", 3.1, 11),
    MockPlaceData("view-1", RecommendationStage.NIGHT_VIEW, "달빛 전망길", "도시의 불빛을 바라보며 쉬어가는 산책 명소", "식사 후 풍경을 즐기며 잠시 쉬어가세요", 4.2, 14),
    MockPlaceData("view-2", RecommendationStage.NIGHT_VIEW, "별빛 언덕", "탁 트인 전망과 짧은 산책을 즐기는 언덕", "야경을 보며 오늘의 장면을 남기기 좋아요", 5.0, 17),
    MockPlaceData("cafe-1", RecommendationStage.DESSERT, "느린 오후", "디저트와 차 한 잔으로 마무리하는 카페", "마지막은 달콤한 디저트와 함께해 보세요", 1.8, 6),
    MockPlaceData("cafe-2", RecommendationStage.DESSERT, "구름 디저트룸", "작은 케이크와 커피가 있는 아늑한 공간", "오늘의 이야기를 나누며 코스를 마무리해요", 2.2, 7),
)
