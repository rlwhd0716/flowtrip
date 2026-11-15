package com.github.flowtrip

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import java.net.HttpURLConnection
import java.net.URL
import com.github.flowtrip.data.remote.KakaoPlaceSearch
import com.github.flowtrip.domain.model.*
import com.github.flowtrip.domain.repository.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit opt-in network check. An upstream 403 is observable, never a successful venue search. */
class PlaceProxySmokeTest {
    private fun fixtureLocation(coordinates: Coordinates): LocationRepository = object : LocationRepository {
        // Search-pipeline fixtures only; never change the device's or a vehicle's location.
        override val state = MutableStateFlow(LocationState(DataSource.PLEOS, LocationStatus.AVAILABLE,
            LocationInfo(coordinates, System.currentTimeMillis(), DataSource.PLEOS)))
        override fun start() {}
        override fun stop() {}
        override fun refresh() {}
    }

    @Test fun domesticSearchMapsRealApiPlacesAndReturnsThreeRestaurantCandidates() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("runPlaceProxy") == "true")
        assumeTrue(BuildConfig.DEBUG && BuildConfig.ALLOW_LOCAL_PLACE_PROXY)
        val center = Coordinates(37.41275833, 127.09481944)
        val search = KakaoPlaceSearch(InstrumentationRegistry.getInstrumentation().targetContext, fixtureLocation(center))
        val places = search.search(RecommendationRequest(TravelMode.DATE, "현재 위치 주변", 300, "로맨틱 · 편안한 힐링 · 맛집"))
        val restaurants = places.filter { it.stage == RecommendationStage.RESTAURANT }
        assertEquals(3, restaurants.size)
        assertTrue(places.all { it.provider == DataSource.KAKAO && it.id.startsWith("kakao:") })
        assertTrue(restaurants.all { it.destination != null && it.travelMinutes + it.visitMinutes <= 300 })
        assertTrue(restaurants.all { it.destination!!.poiId == null })
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
            putString("stream", "\nReal domestic search pipeline: ${restaurants.size} restaurant candidates, ${places.size} total.\n")
        })
    }

    @Test fun overseasEmulatorLocationReportsLocationProblemInsteadOfNoMatchingPlaces() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("runPlaceProxy") == "true")
        assumeTrue(BuildConfig.DEBUG && BuildConfig.ALLOW_LOCAL_PLACE_PROXY)
        val search = KakaoPlaceSearch(InstrumentationRegistry.getInstrumentation().targetContext,
            fixtureLocation(Coordinates(37.421998, -122.084)))
        val failure = runCatching { search.search(RecommendationRequest(TravelMode.DATE, "현재 위치 주변", 300, "맛집")) }.exceptionOrNull()
        assertTrue(failure?.message.orEmpty().contains("국내 장소 검색 범위 밖"))
    }

    @Test fun appReachesLocalProxyAndPreservesKakaoResponse() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("runPlaceProxy") == "true")
        assumeTrue(BuildConfig.DEBUG && BuildConfig.ALLOW_LOCAL_PLACE_PROXY)
        val connection = URL(BuildConfig.PLACE_PROXY_URL.trimEnd('/') +
            "/v2/local/search/keyword.json?query=cafe&size=3").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5000
            connection.readTimeout = 10000
            val status = connection.responseCode
            val stream = if (status == 200) connection.inputStream else connection.errorStream
            val body = JSONObject(stream.bufferedReader().use { it.readText() })
            assertEquals("nosniff", connection.getHeaderField("x-content-type-options"))
            assertTrue("Unexpected proxy status: $status", status in listOf(200, 401, 403, 429))
            if (status == 200) assertTrue(body.getJSONArray("documents").length() > 0)
            else {
                assertTrue(body.getString("code").startsWith("KAKAO_"))
                assertTrue(!body.has("documents"))
            }
            InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
                putString("stream", "\nPlace proxy HTTP $status; realSearchSucceeded=${status == 200}; code=${body.optString("code")}\n")
            })
        } finally { connection.disconnect() }
    }
}
