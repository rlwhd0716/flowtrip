package com.github.flowtrip.data.mapper

import com.github.flowtrip.domain.model.Destination
import kotlin.math.abs

internal fun Destination.matches(other: Destination): Boolean =
    (source == other.source || (source != com.github.flowtrip.domain.model.DataSource.MOCK && other.source != com.github.flowtrip.domain.model.DataSource.MOCK)) &&
    abs(coordinates.latitude - other.coordinates.latitude) < 0.0001 &&
    abs(coordinates.longitude - other.coordinates.longitude) < 0.0001 &&
    (poiId.isNullOrBlank() || other.poiId.isNullOrBlank() || poiId == other.poiId) &&
    (poiSubId.isNullOrBlank() || other.poiSubId.isNullOrBlank() || poiSubId == other.poiSubId)
