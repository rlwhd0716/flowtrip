package com.github.flowtrip.data.mapper

import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.WaypointInfo
import ai.pleos.playground.location.fused.model.LocationData
import com.github.flowtrip.domain.model.*

internal fun DestinationInfo.toDomain(): Destination? {
    if (name.isBlank() && poiId.isNullOrBlank()) return null
    return Destination(
    id = poiId?.takeIf { it.isNotBlank() } ?: "$latitude,$longitude",
    name = name, coordinates = Coordinates(latitude, longitude), source = DataSource.PLEOS,
    address = address, poiId = poiId, poiSubId = poiSubId, poiProvider = poiProvider,
    )
}

internal fun LocationData.toDomain() = LocationInfo(Coordinates(latitude, longitude), timestampMillis, DataSource.PLEOS)

internal fun WaypointInfo.toDomain() = Destination(
    id = poiId?.takeIf { it.isNotBlank() } ?: "$latitude,$longitude",
    name = name, coordinates = Coordinates(latitude, longitude), source = DataSource.PLEOS,
    address = address, poiId = poiId, poiSubId = poiSubId, poiProvider = poiProvider,
)
