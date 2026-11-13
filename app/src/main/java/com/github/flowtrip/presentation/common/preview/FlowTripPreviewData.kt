package com.github.flowtrip.presentation.common.preview

import com.github.flowtrip.data.model.MockRecommendationDataSource
import com.github.flowtrip.data.repository.OfficialNavigationTestCatalog
import com.github.flowtrip.domain.model.*
import com.github.flowtrip.presentation.recommendation.RecommendationUiState

/** Bundled preview fixtures only: no SDK, network, location or ViewModel subscriptions. */
internal object FlowTripPreviewData {
    private val targets = OfficialNavigationTestCatalog().destinations
    private fun places(mode: TravelMode) = MockRecommendationDataSource.places
        .filter { it.stage in mode.stages }.map { place ->
            place.copy(destination = targets.find { it.id == place.demoRoute.destinationId })
        }
    val place = places(TravelMode.DATE).first()
    private fun recommendation(mode: TravelMode): RecommendationUiState {
        val all = places(mode)
        return RecommendationUiState(
            request = RecommendationRequest(mode, "예시 여행 지역", 180, "조용한 장소", true),
            place = all.first(), candidates = all.filter { it.stage == mode.stages.first() },
            remainingCourse = mode.stages.map { stage -> all.first { it.stage == stage } },
            isLoading = false,
            navigation = NavigationState(DataSource.MOCK, service = ServiceStatus.READY),
        )
    }
    val recommendation = recommendation(TravelMode.DATE)
    val tripRecommendation = recommendation(TravelMode.TRIP)
}
