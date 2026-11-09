package com.github.flowtrip.data.repository

import com.github.flowtrip.data.model.mockPlaces
import com.github.flowtrip.domain.model.RecommendationRequest
import com.github.flowtrip.domain.repository.RecommendationRepository
import javax.inject.Inject

class MockRecommendationRepository @Inject constructor() : RecommendationRepository {
    override suspend fun getRecommendations(request: RecommendationRequest) = mockPlaces.map {
        it.toDomain("${request.destination} 기준 · ${request.availableMinutes}분 · ${request.preference}" +
            if (request.preferNewPlaces) " · 새로운 장소 우선" else "")
    }
}
