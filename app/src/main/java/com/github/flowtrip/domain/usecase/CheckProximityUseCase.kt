package com.github.flowtrip.domain.usecase

import com.github.flowtrip.domain.model.*
import kotlin.math.*

enum class Proximity { UNKNOWN, NEAR, FAR }

/** Straight-line proximity is only a hint, never proof of arrival or a completed visit. */
class CheckProximityUseCase {
    operator fun invoke(location: LocationState, destination: Destination?, nowMillis: Long): Proximity {
        val fix = location.location ?: return Proximity.UNKNOWN
        if (location.status != LocationStatus.AVAILABLE || !fix.isFresh(nowMillis) ||
            destination == null || fix.source != destination.source) return Proximity.UNKNOWN
        val lat1 = Math.toRadians(fix.coordinates.latitude)
        val lat2 = Math.toRadians(destination.coordinates.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(destination.coordinates.longitude - fix.coordinates.longitude)
        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val meters = 6_371_000 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
        return if (meters <= 150) Proximity.NEAR else Proximity.FAR
    }
}
