package com.github.flowtrip.data.mapper

import com.github.flowtrip.domain.model.Destination

internal enum class RoutingDecision { WAITING, CONFIRMED }

/** SDK acceptance/completion alone cannot confirm a route to the approved destination. */
internal class RouteConfirmation(private val expected: Destination, private val expectedWaypoints: List<Destination> = emptyList()) {
    private var completed = false
    private var matchingDestination = false
    private var guiding = false
    private var matchingWaypoints = false
    val hasCompleted: Boolean get() = completed
    val waitingReason: String get() = when {
        !completed -> "탐색 완료 신호 대기"
        !matchingDestination -> "요청 목적지 일치 확인 대기"
        !matchingWaypoints -> "경유지 목록 일치 확인 대기"
        !guiding -> "안내 모드 확인 대기"
        else -> "확인 완료"
    }
    val decision: RoutingDecision get() = when {
        completed && matchingDestination && matchingWaypoints && guiding -> RoutingDecision.CONFIRMED
        else -> RoutingDecision.WAITING
    }
    fun onRoutingCompleted() {
        completed = true
        matchingDestination = false
        guiding = false
        matchingWaypoints = false
    }
    fun onRouteCancelled() {
        // A replacement can cancel the old route first. Require a new completion
        // and fresh snapshots; cancellation itself can never confirm success.
        completed = false
        matchingDestination = false
        matchingWaypoints = false
        guiding = false
    }
    fun onWaypoints(waypoints: List<Destination>) {
        if (!completed) return
        matchingWaypoints = waypoints.size == expectedWaypoints.size &&
            expectedWaypoints.zip(waypoints).all { (expected, actual) -> expected.matches(actual) }
    }
    fun onDestination(destination: Destination?) {
        if (completed) {
            matchingDestination = destination != null && expected.matches(destination)
        }
    }
    fun onRouteState(isRouting: Boolean, isGuiding: Boolean) {
        if (completed) {
            guiding = !isRouting && isGuiding
        }
    }
}
