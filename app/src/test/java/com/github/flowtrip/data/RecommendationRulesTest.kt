package com.github.flowtrip.data

import com.github.flowtrip.domain.model.*
import com.github.flowtrip.domain.usecase.RecommendationRules
import org.junit.Assert.*
import org.junit.Test

class RecommendationRulesTest {
    @Test fun `overseas default emulator position is outside the domestic search area`() {
        assertFalse(RecommendationRules.isDomesticSearchArea(Coordinates(37.421998, -122.084)))
        assertFalse(RecommendationRules.isDomesticSearchArea(Coordinates(0.0, 0.0)))
        assertTrue(RecommendationRules.isDomesticSearchArea(Coordinates(37.41275833, 127.09481944)))
        assertTrue(RecommendationRules.isDomesticSearchArea(Coordinates(33.4, 126.5)))
        assertTrue(RecommendationRules.isDomesticSearchArea(Coordinates(37.24, 131.86)))
    }
    private val origin = Coordinates(37.5, 127.0)
    private fun place(id: String, lat: Double, stay: Int = 40) = Place(id, RecommendationStage.CAFE,
        id, "", "", 0.0, 0, Destination(id, id, Coordinates(lat, 127.0), DataSource.KAKAO), visitMinutes = stay, provider = DataSource.KAKAO)

    @Test fun `empty candidates distinguish time budget from already recommended places`() {
        val near = place("near", 37.5, 60)
        val tooShort = RecommendationRules.assess(listOf(near), origin, 60, emptySet(), emptySet(), false)
        assertTrue(tooShort.places.isEmpty())
        assertEquals(65, tooShort.minimumRequiredMinutes)
        assertTrue(tooShort.emptyMessage(60).contains("65분"))
        val excluded = RecommendationRules.assess(listOf(near), origin, 180, setOf("near"), emptySet(), false)
        assertEquals(0, excluded.availableCount)
        assertNull(excluded.minimumRequiredMinutes)
        assertTrue(excluded.emptyMessage(180).contains("이미 추천"))
    }

    @Test fun `distance estimate filters impossible venues and exclusions without inventing ratings`() {
        val near = place("near", 37.501)
        val far = place("far", 38.5)
        val excluded = place("excluded", 37.5)
        val longStay = place("long", 37.5, 120)
        val ranked = RecommendationRules.rank(listOf(far, near, excluded, longStay, near), origin, 60, setOf("excluded"), emptySet(), true)
        assertEquals(listOf("near"), ranked.map { it.id })
        assertTrue(ranked.single().distanceKm in 0.10..0.12)
        assertTrue(ranked.single().travelMinutes >= 5)
        assertTrue(RecommendationRules.rank(listOf(near), origin, 0, emptySet(), emptySet(), false).isEmpty())
    }

    @Test fun `new place preference demotes history but does not fabricate quality scores`() {
        val visited = place("visited", 37.5001)
        val fresh = place("fresh", 37.501)
        assertEquals("fresh", RecommendationRules.rank(listOf(visited, fresh), origin, 90, emptySet(), setOf("visited"), true).first().id)
        assertEquals("visited", RecommendationRules.rank(listOf(visited, fresh), origin, 90, emptySet(), setOf("visited"), false).first().id)
    }

    @Test fun `elapsed time persists across background periods and day input covers first activity day only`() {
        assertEquals(480, RecommendationRules.activityBudget(3 * 1440, true))
        assertEquals(1440, RecommendationRules.activityBudget(1440, false))
        assertEquals(118, RecommendationRules.remaining(120, 1_000, 121_000))
        assertEquals(120, RecommendationRules.remaining(120, 2_000, 1_000))
        assertEquals(0, RecommendationRules.remaining(120, 1_000, 8_000_000))
    }
}
