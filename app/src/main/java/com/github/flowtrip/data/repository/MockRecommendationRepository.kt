package com.github.flowtrip.data.repository

import com.github.flowtrip.data.model.MockRecommendationDataSource
import com.github.flowtrip.domain.model.*
import com.github.flowtrip.domain.repository.NavigationTestCatalog
import com.github.flowtrip.domain.repository.RecommendationRepository
import javax.inject.Inject

class MockRecommendationRepository @Inject constructor(
    private val testCatalog: NavigationTestCatalog,
) : RecommendationRepository {
    constructor() : this(OfficialNavigationTestCatalog())
    override suspend fun getRecommendations(request: RecommendationRequest): List<Place> =
        MockRecommendationDataSource.places.filter { it.stage in request.mode.stages }.map { place ->
            place.copy(
                reason = "${place.reason} · ${request.preference} · 예시 동선",
                // Explicit fixture mapping; never infer a destination from list position or fake coordinates.
                destination = place.demoRoute.destinationId?.let { id -> testCatalog.destinations.find { it.id == id } },
            )
        }
}
