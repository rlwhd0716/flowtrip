package com.github.flowtrip.data

import com.github.flowtrip.data.repository.MockRecommendationRepository
import com.github.flowtrip.data.repository.OfficialNavigationTestCatalog
import com.github.flowtrip.domain.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MockRecommendationRepositoryTest {
    @Test fun `each mode has three distinct stages with three explicitly mapped candidates each`() = runTest {
        val catalog = OfficialNavigationTestCatalog()
        val repository = MockRecommendationRepository(catalog)
        val seenIds = mutableSetOf<String>()
        for (mode in TravelMode.entries) {
            val places = repository.getRecommendations(RecommendationRequest(mode, "예시 지역", 180, "풍경"))
            assertEquals(9, places.size)
            assertEquals(mode.stages.toSet(), places.map { it.stage }.toSet())
            for (stage in mode.stages) {
                val candidates = places.filter { it.stage == stage }
                assertEquals(3, candidates.size)
                assertEquals(listOf(DemoRouteMapping.DOT, DemoRouteMapping.DREAM, DemoRouteMapping.UNSET), candidates.map { it.demoRoute })
                assertEquals(catalog.destinations.first(), candidates[0].destination)
                assertEquals(catalog.destinations.last(), candidates[1].destination)
                assertNull(candidates[2].destination)
                candidates.forEach { place ->
                    assertTrue(seenIds.add(place.id))
                    assertTrue(place.description.isNotBlank())
                    assertTrue(place.details.size >= 4)
                    assertTrue(place.details.all { it.label.isNotBlank() && it.value.isNotBlank() })
                }
            }
        }
    }
}
