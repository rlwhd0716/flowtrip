package com.github.flowtrip.platform

import com.github.flowtrip.data.mapper.*
import com.github.flowtrip.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class RouteConfirmationTest {
    private val expected = Destination("a", "승인 장소", Coordinates(37.5, 127.0), DataSource.PLEOS, poiId = "a")
    @Test fun `acceptance completion and old snapshots cannot by themselves confirm routing`() {
        val confirmation = RouteConfirmation(expected)
        confirmation.onDestination(expected)
        confirmation.onRouteState(false, true)
        assertEquals(RoutingDecision.WAITING, confirmation.decision)
        confirmation.onRoutingCompleted()
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
        confirmation.onRouteState(false, true)
        confirmation.onDestination(expected)
        assertEquals(RoutingDecision.CONFIRMED, confirmation.decision)
    }
    @Test fun `different destination and non guiding completion are rejected`() {
        val different = RouteConfirmation(expected)
        different.onRoutingCompleted()
        different.onDestination(expected.copy(coordinates = Coordinates(35.0, 129.0)))
        different.onRouteState(false, true)
        assertEquals(RoutingDecision.REJECTED, different.decision)
        val failed = RouteConfirmation(expected)
        failed.onRoutingCompleted()
        failed.onDestination(expected)
        failed.onRouteState(false, false)
        assertEquals(RoutingDecision.REJECTED, failed.decision)
    }
    @Test fun `POI ID and data source mismatch cannot match`() {
        assertFalse(expected.matches(expected.copy(poiId = "other")))
        assertFalse(expected.matches(expected.copy(source = DataSource.MOCK)))
    }
}
