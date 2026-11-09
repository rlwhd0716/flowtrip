package com.github.flowtrip

import android.app.Application
import com.github.flowtrip.data.repository.MockRecommendationRepository
import com.github.flowtrip.domain.usecase.GetRecommendationsUseCase

class FlowTripApplication : Application() {
    val container by lazy { AppContainer() }
}

class AppContainer {
    private val recommendationRepository = MockRecommendationRepository()
    val getRecommendations = GetRecommendationsUseCase(recommendationRepository)
}
