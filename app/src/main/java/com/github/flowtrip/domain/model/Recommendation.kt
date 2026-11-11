package com.github.flowtrip.domain.model

enum class TravelMode(val title: String) { TRIP("여행 모드"), DATE("데이트 모드") }

enum class RecommendationStage(val title: String) {
    RESTAURANT("처음 가보는 식당"), NIGHT_VIEW("야경 명소"), DESSERT("디저트 카페")
}

data class RecommendationRequest(
    val mode: TravelMode,
    val destination: String,
    val availableMinutes: Int,
    val preference: String,
    val preferNewPlaces: Boolean = false,
)

data class Place(
    val id: String,
    val stage: RecommendationStage,
    val name: String,
    val description: String,
    val reason: String,
    val distanceKm: Double,
    val travelMinutes: Int,
    val destination: Destination? = null,
    val visitMinutes: Int = 40,
    val expectedSpend: String = "정보 없음",
    val parkingSummary: String = "주차 정보 확인 필요",
) : java.io.Serializable
