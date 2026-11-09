package com.github.flowtrip.data.mapper

import com.github.flowtrip.domain.model.Destination

internal enum class RoutingDecision { WAITING, CONFIRMED, REJECTED }

/** SDK acceptance/completion alone cannot confirm a route to the approved destination. */
internal class RouteConfirmation(private val expected: Destination) {
    private var completed = false
    private var matchingDestination = false
    private var guiding = false
    private var rejected = false
    val decision: RoutingDecision get() = when {
        rejected -> RoutingDecision.REJECTED
        completed && matchingDestination && guiding -> RoutingDecision.CONFIRMED
        else -> RoutingDecision.WAITING
    }
    fun onRoutingCompleted() {
        completed = true
        matchingDestination = false
        guiding = false
    }
    fun onDestination(destination: Destination?) {
        if (completed) {
            matchingDestination = destination != null && expected.matches(destination)
            if (!matchingDestination) rejected = true
        }
    }
    fun onRouteState(isRouting: Boolean, isGuiding: Boolean) {
        if (completed) {
            guiding = !isRouting && isGuiding
            if (!isRouting && !isGuiding) rejected = true
        }
    }
}
