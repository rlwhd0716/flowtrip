package com.github.flowtrip.domain.repository

import com.github.flowtrip.domain.model.Place
import com.github.flowtrip.domain.model.RecommendationRequest

interface RecommendationRepository {
    val usesRealPlaces: Boolean get() = false
    fun recordVisit(place: Place) {}
    fun recentVisits(): List<Place> = emptyList()
    suspend fun getRecommendations(request: RecommendationRequest): List<Place>
}
