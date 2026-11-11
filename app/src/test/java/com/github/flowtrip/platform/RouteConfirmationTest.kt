package com.github.flowtrip.platform

import com.github.flowtrip.data.mapper.*
import com.github.flowtrip.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class RouteConfirmationTest {
    private val expected = Destination("a", "승인 장소", Coordinates(37.5, 127.0), DataSource.PLEOS, poiId = "a")
    @Test fun `cancelling previous guidance requires new completion and fresh matching snapshots`() {
        val confirmation = RouteConfirmation(expected)
        confirmation.onRoutingCompleted()
        confirmation.onDestination(expected)
        confirmation.onRouteCancelled()
        confirmation.onWaypoints(emptyList())
        confirmation.onRouteState(false, true)
        assertEquals(RoutingDecision.WAITING, confirmation.decision)
        confirmation.onRoutingCompleted()
        confirmation.onWaypoints(emptyList())
        confirmation.onDestination(expected)
        confirmation.onRouteState(false, true)
        assertEquals(RoutingDecision.CONFIRMED, confirmation.decision)
    }
    @Test fun `acceptance completion and old snapshots cannot by themselves confirm routing`() {
        val confirmation = RouteConfirmation(expected)
        confirmation.onDestination(expected)
        confirmation.onRouteState(false, true)
        assertEquals(RoutingDecision.WAITING, confirmation.decision)
        confirmation.onRoutingCompleted()
        confirmation.onWaypoints(emptyList())
        assertEquals(RoutingDecision.WAITING, confirmation.decision)
        confirmation.onDestination(expected)
        assertEquals(RoutingDecision.WAITING, confirmation.decision)
        confirmation.onRouteState(true, false)
        assertEquals(RoutingDecision.WAITING, confirmation.decision)
        confirmation.onRouteState(false, true)
        assertEquals(RoutingDecision.CONFIRMED, confirmation.decision)
    }
    @Test fun `fresh callbacks can arrive in reverse order`() {
        val confirmation = RouteConfirmation(expected)
        confirmation.onRoutingCompleted()
        confirmation.onWaypoints(emptyList())
        confirmation.onRouteState(false, true)
        confirmation.onDestination(expected)
        assertEquals(RoutingDecision.CONFIRMED, confirmation.decision)
    }
    @Test fun `different destination and non guiding snapshots never confirm a route`() {
        val different = RouteConfirmation(expected)
        different.onRoutingCompleted()
        different.onDestination(expected.copy(coordinates = Coordinates(35.0, 129.0)))
        different.onRouteState(false, true)
        assertEquals(RoutingDecision.WAITING, different.decision)
        val failed = RouteConfirmation(expected)
        failed.onRoutingCompleted()
        failed.onDestination(expected)
        failed.onRouteState(false, false)
        assertEquals(RoutingDecision.WAITING, failed.decision)
    }
    @Test fun `waypoint order and complete list must match after completion`() {
        val first = expected.copy(id = "w1", poiId = "w1", coordinates = Coordinates(37.6, 127.1))
        val second = expected.copy(id = "w2", poiId = "w2", coordinates = Coordinates(37.7, 127.2))
        val confirmation = RouteConfirmation(expected, listOf(first, second))
        confirmation.onWaypoints(listOf(first, second))
        confirmation.onRoutingCompleted()
        confirmation.onDestination(expected)
        confirmation.onRouteState(false, true)
        assertEquals(RoutingDecision.WAITING, confirmation.decision)
        confirmation.onWaypoints(listOf(first, second))
        assertEquals(RoutingDecision.CONFIRMED, confirmation.decision)
        val wrong = RouteConfirmation(expected, listOf(first, second))
        wrong.onRoutingCompleted()
        wrong.onWaypoints(listOf(second, first))
        assertEquals(RoutingDecision.WAITING, wrong.decision)
    }
    @Test fun `transient old snapshots can settle without weakening positive confirmation checks`() {
        val confirmation = RouteConfirmation(expected)
        confirmation.onRoutingCompleted()
        confirmation.onDestination(expected.copy(poiId = "previous"))
        confirmation.onWaypoints(listOf(expected))
        confirmation.onRouteState(false, false)
        assertEquals(RoutingDecision.WAITING, confirmation.decision)
        confirmation.onDestination(expected)
        confirmation.onWaypoints(emptyList())
        assertEquals(RoutingDecision.WAITING, confirmation.decision)
        confirmation.onRouteState(false, true)
        assertEquals(RoutingDecision.CONFIRMED, confirmation.decision)
    }
    @Test fun `POI ID and data source mismatch cannot match`() {
        assertFalse(expected.matches(expected.copy(poiId = "other")))
        assertFalse(expected.matches(expected.copy(source = DataSource.MOCK)))
        assertFalse(expected.copy(poiSubId = "0").matches(expected.copy(poiSubId = "1")))
    }
}
