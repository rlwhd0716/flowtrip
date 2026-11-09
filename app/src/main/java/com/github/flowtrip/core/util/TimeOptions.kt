package com.github.flowtrip.core.util

val availableTimeOptions = listOf(60, 120, 180, 240)

fun formatAvailableTime(minutes: Int): String = "${minutes / 60}시간"
