package com.github.flowtrip.core.util

// Legacy presets remain for callers that still present quick choices.
val availableTimeOptions = listOf(60, 120, 180, 240)

enum class AvailableTimeUnit(val title: String, val minutes: Int, val maxAmount: Int) {
    HOURS("시간", 60, 24), DAYS("일", 1440, 7)
}

fun formatAvailableTime(minutes: Int): String = if (minutes >= 1440 && minutes % 1440 == 0)
    "${minutes / 1440}일" else "${minutes / 60}시간"

fun validTime(minutes: Int, unit: AvailableTimeUnit) =
    minutes % unit.minutes == 0 && minutes / unit.minutes in 1..unit.maxAmount

fun convertTimeUnit(minutes: Int, from: AvailableTimeUnit, to: AvailableTimeUnit): Int =
    (minutes / from.minutes).coerceIn(1, to.maxAmount) * to.minutes
