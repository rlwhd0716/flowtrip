package com.github.flowtrip.domain.usecase

import com.github.flowtrip.domain.model.*
import kotlin.math.*

/** Product estimates, not road ETA or venue facts. Keep tuning in one place. */
data class RecommendationWeights(
    val distancePenalty: Double = 1.0,
    val visitedPenalty: Double = 20.0,
    val assumedSpeedKmh: Double = 25.0,
    val roadFactor: Double = 1.4,
    val transferMinutes: Int = 5,
)

data class CandidateAssessment(
    val places: List<Place>,
    val availableCount: Int,
    val minimumRequiredMinutes: Int?,
) {
    fun emptyMessage(minutes: Int): String = if (availableCount == 0)
        "주변 검색 결과가 모두 이미 추천했거나 제외한 장소입니다. 새 코스를 시작하거나 조건을 변경해 주세요."
    else "검색된 ${availableCount}곳의 추정 이동·체류 시간이 남은 ${minutes}분을 초과합니다. 가장 짧은 후보도 약 ${minimumRequiredMinutes}분이 필요합니다. 시간을 늘리거나 다른 장소 유형을 선택해 주세요."
}

object RecommendationRules {
    // Coarse service-area sanity check, not an administrative boundary or GPS substitution.
    fun isDomesticSearchArea(coordinates: Coordinates): Boolean =
        coordinates.latitude in 32.0..39.0 && coordinates.longitude in 124.0..132.0

    fun activityBudget(minutes: Int, dayInput: Boolean): Int =
        if (dayInput) 8 * 60 else minutes.coerceAtLeast(0)

    fun remaining(budget: Int, started: Long, now: Long): Int =
        (budget - ((now - started).coerceAtLeast(0) / 60_000).toInt()).coerceAtLeast(0)

    fun distance(a: Coordinates, b: Coordinates): Double {
        val lat = Math.toRadians(b.latitude - a.latitude)
        val lon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(lat / 2).pow(2) + cos(Math.toRadians(a.latitude)) *
            cos(Math.toRadians(b.latitude)) * sin(lon / 2).pow(2)
        return 6371.0 * 2 * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    fun rank(places: List<Place>, origin: Coordinates, minutes: Int, excluded: Set<String>,
        visited: Set<String>, preferNew: Boolean, weights: RecommendationWeights = RecommendationWeights()): List<Place> =
        assess(places, origin, minutes, excluded, visited, preferNew, weights).places

    fun assess(places: List<Place>, origin: Coordinates, minutes: Int, excluded: Set<String>,
        visited: Set<String>, preferNew: Boolean, weights: RecommendationWeights = RecommendationWeights()): CandidateAssessment {
        val estimated = places.distinctBy { it.id }.filter { it.id !in excluded && it.destination != null }.map { place ->
            val km = distance(origin, place.destination!!.coordinates)
            place.copy(distanceKm = km, travelMinutes = ceil(km * weights.roadFactor / weights.assumedSpeedKmh * 60).toInt() + weights.transferMinutes)
        }
        val ranked = estimated.filter { it.travelMinutes + it.visitMinutes <= minutes }
            .sortedWith(compareBy<Place> { it.distanceKm * weights.distancePenalty +
                if (preferNew && it.id in visited) weights.visitedPenalty else 0.0 }.thenBy { it.id })
        return CandidateAssessment(ranked, estimated.size, estimated.minOfOrNull { it.travelMinutes + it.visitMinutes })
    }
}
