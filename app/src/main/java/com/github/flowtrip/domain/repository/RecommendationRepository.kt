package com.github.flowtrip.domain.repository

import com.github.flowtrip.domain.model.Place
import com.github.flowtrip.domain.model.RecommendationRequest

interface RecommendationRepository {
    suspend fun getRecommendations(request: RecommendationRequest): List<Place>
}
