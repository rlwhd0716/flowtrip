package com.github.flowtrip.presentation.common.preview

import com.github.flowtrip.domain.model.Coordinates
import com.github.flowtrip.domain.model.DataSource
import com.github.flowtrip.domain.model.Destination
import com.github.flowtrip.domain.model.LocationInfo
import com.github.flowtrip.domain.model.LocationState
import com.github.flowtrip.domain.model.LocationStatus
import com.github.flowtrip.domain.model.NavigationState
import com.github.flowtrip.domain.model.Place
import com.github.flowtrip.domain.model.RecommendationRequest
import com.github.flowtrip.domain.model.RecommendationStage
import com.github.flowtrip.domain.model.ServiceStatus
import com.github.flowtrip.domain.model.TravelMode
import com.github.flowtrip.presentation.recommendation.RecommendationUiState

/** Static design fixtures: no ViewModel, Hilt, SDK, network, or location subscriptions. */
internal object FlowTripPreviewData {
    private val coordinates = Coordinates(37.5445, 127.0557)
    val place = Place(
        id = "preview-restaurant",
        stage = RecommendationStage.RESTAURANT,
        name = "골목의 작은 식탁",
        description = "계절 메뉴를 나누며 여유롭게 식사하는 작은 레스토랑",
        reason = "새로운 식당에서 코스를 시작해 보세요. 조용히 대화하기 좋은 공간이에요.",
        distanceKm = 2.4,
        travelMinutes = 8,
        destination = Destination("preview-restaurant", "골목의 작은 식탁", coordinates, DataSource.MOCK,
            address = "서울 성수동 · 미리보기용 예시 주소"),
        visitMinutes = 60,
        expectedSpend = "1인 약 2~3만 원",
        parkingSummary = "인근 공영주차장 이용 · 예시 정보",
    )
    val recommendation = RecommendationUiState(
        request = RecommendationRequest(TravelMode.DATE, "서울 성수동 (Mock)", 180, "조용한 장소", true),
        place = place,
        isLoading = false,
        navigation = NavigationState(DataSource.MOCK, service = ServiceStatus.READY, message = "Mock 내비게이션"),
        location = LocationState(DataSource.MOCK, LocationStatus.AVAILABLE,
            LocationInfo(coordinates, 1L, DataSource.MOCK)),
    )
}
