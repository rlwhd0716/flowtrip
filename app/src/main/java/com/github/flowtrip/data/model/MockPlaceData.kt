package com.github.flowtrip.data.model

import com.github.flowtrip.domain.model.Place
import com.github.flowtrip.domain.model.RecommendationStage
import com.github.flowtrip.domain.model.Coordinates
import com.github.flowtrip.domain.model.DataSource
import com.github.flowtrip.domain.model.Destination

/** Fictional fixture data. No real venue, routing or location lookup is performed. */
internal data class MockPlaceData(
    val id: String,
    val stage: RecommendationStage,
    val name: String,
    val description: String,
    val reason: String,
    val distanceKm: Double,
    val travelMinutes: Int,
    val coordinates: Coordinates,
    val address: String,
    val visitMinutes: Int,
    val expectedSpend: String,
    val parkingSummary: String,
) {
    fun toDomain(context: String) = Place(
        id, stage, name, description, "$reason · $context", distanceKm, travelMinutes,
        Destination(id, name, coordinates, DataSource.MOCK, address = address),
        visitMinutes, expectedSpend, parkingSummary,
    )
}

internal val mockPlaces = listOf(
    MockPlaceData("food-1", RecommendationStage.RESTAURANT, "골목의 작은 식탁", "계절 메뉴를 나누며 여유롭게 식사하는 작은 레스토랑", "새로운 식당에서 코스를 시작해 보세요", 2.4, 8,
        Coordinates(37.5435, 127.0552), "서울 성동구 성수동 · 예시 위치", 60, "1인 2~3만원", "인근 공영주차장 이용"),
    MockPlaceData("food-2", RecommendationStage.RESTAURANT, "온기 키친", "따뜻한 한 접시와 편안한 대화가 있는 식당", "색다른 메뉴를 함께 즐기기 좋은 첫 장소예요", 3.1, 11,
        Coordinates(37.5480, 127.0640), "서울 성동구 성수역 인근 · 예시 위치", 50, "1인 1~2만원", "주차 공간이 적어요"),
    MockPlaceData("view-1", RecommendationStage.NIGHT_VIEW, "달빛 전망길", "도시의 불빛을 바라보며 쉬어가는 산책 명소", "식사 후 풍경을 즐기며 잠시 쉬어가세요", 4.2, 14,
        Coordinates(37.5310, 127.0660), "서울 광진구 한강변 · 예시 위치", 35, "입장료 없음", "한강공원 주차장 이용"),
    MockPlaceData("view-2", RecommendationStage.NIGHT_VIEW, "별빛 언덕", "탁 트인 전망과 짧은 산책을 즐기는 언덕", "야경을 보며 오늘의 장면을 남기기 좋아요", 5.0, 17,
        Coordinates(37.5572, 127.0765), "서울 광진구 어린이대공원 인근 · 예시 위치", 40, "입장료 없음", "인근 공영주차장 이용"),
    MockPlaceData("cafe-1", RecommendationStage.DESSERT, "느린 오후", "디저트와 차 한 잔으로 마무리하는 카페", "마지막은 달콤한 디저트와 함께해 보세요", 1.8, 6,
        Coordinates(37.5412, 127.0495), "서울 성동구 서울숲 인근 · 예시 위치", 45, "1인 1~2만원", "인근 유료주차장 이용"),
    MockPlaceData("cafe-2", RecommendationStage.DESSERT, "구름 디저트룸", "작은 케이크와 커피가 있는 아늑한 공간", "오늘의 이야기를 나누며 코스를 마무리해요", 2.2, 7,
        Coordinates(37.5463, 127.0578), "서울 성동구 성수동 · 예시 위치", 40, "1인 1~2만원", "전용 주차장 없음"),
    MockPlaceData("food-3", RecommendationStage.RESTAURANT, "초록 테이블", "제철 재료로 준비한 편안한 저녁", "함께 나눠 먹기 좋은 메뉴예요", 3.5, 12,
        Coordinates(37.5490, 127.0520), "서울 성동구 · 예시 위치", 55, "1인 2~3만원", "인근 유료주차장 이용"),
    MockPlaceData("view-3", RecommendationStage.NIGHT_VIEW, "강빛 산책길", "강변 불빛을 즐기는 짧은 산책 코스", "차분하게 풍경을 감상해 보세요", 4.0, 13,
        Coordinates(37.5320, 127.0580), "서울 한강변 · 예시 위치", 30, "입장료 없음", "공원 주차장 이용"),
    MockPlaceData("cafe-3", RecommendationStage.DESSERT, "작은 다과점", "차와 구움과자로 마무리하는 공간", "가볍게 쉬어가기 좋아요", 2.8, 9,
        Coordinates(37.5420, 127.0590), "서울 성동구 · 예시 위치", 40, "1인 1~2만원", "전용 주차장 없음"),
)
