package com.github.flowtrip.domain.usecase

import com.github.flowtrip.domain.model.RecommendationRequest
import com.github.flowtrip.domain.repository.RecommendationRepository

class GetRecommendationsUseCase(private val repository: RecommendationRepository) {
    suspend operator fun invoke(request: RecommendationRequest) = repository.getRecommendations(request)
}
