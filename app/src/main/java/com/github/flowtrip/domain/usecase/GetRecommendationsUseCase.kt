package com.github.flowtrip.domain.usecase

import com.github.flowtrip.domain.model.RecommendationRequest
import com.github.flowtrip.domain.repository.RecommendationRepository

class GetRecommendationsUseCase(private val repository: RecommendationRepository) {
    val usesRealPlaces get() = repository.usesRealPlaces
    fun recordVisit(place: com.github.flowtrip.domain.model.Place) = repository.recordVisit(place)
    fun recentVisits() = repository.recentVisits()
    suspend operator fun invoke(request: RecommendationRequest) = repository.getRecommendations(request)
}
