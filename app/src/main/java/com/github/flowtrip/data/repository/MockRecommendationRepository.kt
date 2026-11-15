package com.github.flowtrip.data.repository

import com.github.flowtrip.data.model.MockRecommendationDataSource
import com.github.flowtrip.data.remote.KakaoPlaceSearch
import com.github.flowtrip.domain.model.*
import com.github.flowtrip.domain.repository.NavigationTestCatalog
import com.github.flowtrip.domain.repository.RecommendationRepository
import javax.inject.Inject

class MockRecommendationRepository private constructor(
    private val testCatalog: NavigationTestCatalog,
    private val realSearch: KakaoPlaceSearch?,
    override val usesRealPlaces: Boolean,
) : RecommendationRepository {
    @Inject constructor(testCatalog: NavigationTestCatalog, realSearch: KakaoPlaceSearch) :
        this(testCatalog, realSearch, com.github.flowtrip.BuildConfig.REAL_PLACES)
    constructor(testCatalog: NavigationTestCatalog) : this(testCatalog, null, false)
    constructor() : this(OfficialNavigationTestCatalog())
    override fun recordVisit(place: Place) { realSearch?.recordVisit(place) }
    override fun recentVisits(): List<Place> = realSearch?.recentVisits().orEmpty()
    override suspend fun getRecommendations(request: RecommendationRequest): List<Place> =
        if (usesRealPlaces) requireNotNull(realSearch).search(request) else
        MockRecommendationDataSource.places.filter { it.stage in request.mode.stages }.map { place ->
            place.copy(
                reason = "${place.reason} · ${request.preference} · 예시 동선",
                // Explicit fixture mapping; never infer a destination from list position or fake coordinates.
                destination = place.demoRoute.destinationId?.let { id -> testCatalog.destinations.find { it.id == id } },
            )
        }
}
