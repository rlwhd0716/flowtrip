package com.github.flowtrip.data.model

import com.github.flowtrip.domain.model.*

/** All descriptions, amounts and times are demo fixtures, not verified venue facts. */
internal object MockRecommendationDataSource {
    private fun place(id: String, stage: RecommendationStage, name: String, description: String,
        reason: String, image: PlaceImage, mapping: DemoRouteMapping, vararg details: Pair<String, String>) = Place(
        id = id, stage = stage, name = name, description = description, reason = reason,
        distanceKm = 0.0, travelMinutes = 0,
        visitMinutes = details.firstOrNull { it.first in listOf("체류 시간", "예상 관람 시간") }
            ?.second?.filter { it.isDigit() }?.toIntOrNull() ?: 40,
        expectedSpend = details.firstOrNull { it.first in listOf("예상 가격", "입장료") }
            ?.second?.let { "예시 · $it" } ?: "예시 · 가격 확인 필요",
        parkingSummary = "예시 · 방문 전 확인 필요",
        details = details.map { PlaceDetail(it.first, it.second) } + PlaceDetail("주차 정보", "예시 · 방문 전 확인 필요"),
        image = image, demoRoute = mapping,
    )
    val places = listOf(
        place("food-1", RecommendationStage.RESTAURANT, "골목의 작은 식탁", "계절 재료로 준비한 아늑한 골목 속 레스토랑", "식사 후 야경 산책으로 이어지는 여유로운 시작", PlaceImage.RESTAURANT, DemoRouteMapping.DOT,
            "대표 메뉴" to "파스타 · 스테이크", "예상 가격" to "1인 2~3만원", "체류 시간" to "약 60분"),
        place("food-2", RecommendationStage.RESTAURANT, "정원 옆 레스토랑", "정원을 바라보며 대화를 나누는 식사", "정원 분위기에서 야경으로 이어지는 코스", PlaceImage.RESTAURANT, DemoRouteMapping.DREAM,
            "대표 메뉴" to "리소토 · 샐러드", "예상 가격" to "1인 3~4만원", "체류 시간" to "약 60분"),
        place("food-3", RecommendationStage.RESTAURANT, "밤의 식탁", "은은한 조명 아래 함께 나누는 저녁", "도시의 저녁 풍경을 즐기는 코스", PlaceImage.RESTAURANT, DemoRouteMapping.UNSET,
            "대표 메뉴" to "그릴 요리", "예상 가격" to "1인 2~4만원", "체류 시간" to "약 60분"),
        place("view-1", RecommendationStage.NIGHT_VIEW, "도시 야경 전망대", "도시의 불빛을 한눈에 담는 전망 공간", "식사 후 전망을 즐기고 카페로 이동해 보세요", PlaceImage.NIGHT, DemoRouteMapping.DOT,
            "관람 포인트" to "도시 스카이라인", "추천 방문 시간" to "해가 진 뒤", "체류 시간" to "약 40분"),
        place("view-2", RecommendationStage.NIGHT_VIEW, "강변 산책로", "강물에 비치는 불빛과 함께하는 산책", "가벼운 산책 뒤 따뜻한 차로 마무리", PlaceImage.NIGHT, DemoRouteMapping.DREAM,
            "관람 포인트" to "강변 조명과 산책길", "추천 방문 시간" to "저녁 시간", "체류 시간" to "약 40분"),
        place("view-3", RecommendationStage.NIGHT_VIEW, "루프탑 전망 명소", "탁 트인 공간에서 감상하는 야경", "저녁의 여운을 담아 디저트로 이어가세요", PlaceImage.NIGHT, DemoRouteMapping.UNSET,
            "관람 포인트" to "옥상 전망", "추천 방문 시간" to "일몰 이후", "체류 시간" to "약 30분"),
        place("cafe-1", RecommendationStage.DESSERT, "작은 디저트 카페", "차와 케이크를 나누는 아늑한 공간", "오늘의 이야기를 나누며 마무리", PlaceImage.CAFE, DemoRouteMapping.DOT,
            "대표 메뉴" to "케이크 · 차", "예상 가격" to "1인 1~2만원", "체류 시간" to "약 45분"),
        place("cafe-2", RecommendationStage.DESSERT, "정원 카페", "초록 정원과 달콤한 디저트", "편안한 정원에서 코스를 마무리", PlaceImage.CAFE, DemoRouteMapping.DREAM,
            "대표 메뉴" to "타르트 · 라테", "예상 가격" to "1인 1~2만원", "체류 시간" to "약 45분"),
        place("cafe-3", RecommendationStage.DESSERT, "야경 카페", "창밖의 도시 풍경과 함께하는 디저트", "야경의 여운을 이어가는 마지막 장소", PlaceImage.CAFE, DemoRouteMapping.UNSET,
            "대표 메뉴" to "구움과자 · 커피", "예상 가격" to "1인 1~2만원", "체류 시간" to "약 40분"),
        place("trip-view-1", RecommendationStage.ATTRACTION, "해운대 등대 전망대", "바다와 등대를 테마로 한 관광지 예시", "바다 감상 → 로컬 식사 → 전망 카페 동선", PlaceImage.COAST, DemoRouteMapping.DOT,
            "주요 볼거리" to "해안 풍경 · 등대", "예상 관람 시간" to "약 60분", "입장료" to "예시 · 무료"),
        place("trip-view-2", RecommendationStage.ATTRACTION, "경주 불국사", "전통 건축과 산책을 테마로 한 관광지 예시", "전통 풍경 → 로컬 식사 → 한옥 카페 동선", PlaceImage.TEMPLE, DemoRouteMapping.DREAM,
            "주요 볼거리" to "전통 건축 · 산책길", "예상 관람 시간" to "약 90분", "입장료" to "예시 · 현장 확인"),
        place("trip-view-3", RecommendationStage.ATTRACTION, "팔공산 둘레길", "숲과 자연을 테마로 한 관광지 예시", "숲 산책 → 든든한 식사 → 자연 전망 동선", PlaceImage.FOREST, DemoRouteMapping.UNSET,
            "주요 볼거리" to "숲길 · 산 풍경", "예상 관람 시간" to "약 90분", "입장료" to "예시 · 무료"),
        place("trip-food-1", RecommendationStage.LOCAL_FOOD, "여행지 로컬 맛집 A", "관광 후 쉬어가는 로컬 식당 예시", "관광과 카페 사이에서 여유로운 식사", PlaceImage.RESTAURANT, DemoRouteMapping.DOT,
            "대표 메뉴" to "생선구이 정식", "예상 가격" to "1인 1~2만원", "체류 시간" to "약 50분"),
        place("trip-food-2", RecommendationStage.LOCAL_FOOD, "여행지 맛집 B", "지역 음식을 테마로 한 식당 예시", "여행의 분위기를 이어가는 한 끼", PlaceImage.RESTAURANT, DemoRouteMapping.DREAM,
            "대표 메뉴" to "한식 정식", "예상 가격" to "1인 1~2만원", "체류 시간" to "약 50분"),
        place("trip-food-3", RecommendationStage.LOCAL_FOOD, "여행지 맛집 C", "산책 후 든든하게 먹는 식사 예시", "이동 중 쉬어가는 식사 코스", PlaceImage.RESTAURANT, DemoRouteMapping.UNSET,
            "대표 메뉴" to "버섯 전골", "예상 가격" to "1인 2~3만원", "체류 시간" to "약 60분"),
        place("trip-cafe-1", RecommendationStage.CAFE, "오션뷰 카페", "바다 전망을 테마로 한 카페 예시", "바다 풍경으로 여행을 마무리", PlaceImage.COAST, DemoRouteMapping.DOT,
            "주요 특징" to "바다 전망", "대표 메뉴" to "커피 · 케이크", "예상 가격" to "1인 1~2만원"),
        place("trip-cafe-2", RecommendationStage.CAFE, "한옥 카페", "전통 공간을 테마로 한 카페 예시", "차 한 잔으로 여행을 마무리", PlaceImage.TEMPLE, DemoRouteMapping.DREAM,
            "주요 특징" to "한옥 마당", "대표 메뉴" to "전통차 · 다과", "예상 가격" to "1인 1~2만원"),
        place("trip-cafe-3", RecommendationStage.CAFE, "자연 전망 카페", "초록 풍경을 테마로 한 카페 예시", "자연을 바라보며 여행을 마무리", PlaceImage.FOREST, DemoRouteMapping.UNSET,
            "주요 특징" to "숲 전망", "대표 메뉴" to "차 · 구움과자", "예상 가격" to "1인 1~2만원"),
    )
}
