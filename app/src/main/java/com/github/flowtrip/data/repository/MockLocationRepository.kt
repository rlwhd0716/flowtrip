package com.github.flowtrip.data.repository

import com.github.flowtrip.domain.model.*
import com.github.flowtrip.domain.repository.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockLocationRepository(private val now: () -> Long = System::currentTimeMillis) : LocationRepository {
    private val mutableState = MutableStateFlow(LocationState(DataSource.MOCK))
    override val state = mutableState.asStateFlow()
    override fun start() = refresh()
    override fun refresh() = simulateLocation(Coordinates(37.5445, 127.0557))
    override fun stop() { mutableState.value = LocationState(DataSource.MOCK) }
    fun simulateLocation(coordinates: Coordinates?, timestampMillis: Long = now()) {
        mutableState.value = LocationState(DataSource.MOCK,
            status = if (coordinates == null) LocationStatus.UNAVAILABLE else LocationStatus.AVAILABLE,
            location = coordinates?.let { LocationInfo(it, timestampMillis, DataSource.MOCK) })
    }
    fun simulateFailure(status: LocationStatus) {
        require(status in listOf(LocationStatus.UNSUPPORTED, LocationStatus.PERMISSION_DENIED, LocationStatus.ERROR))
        mutableState.value = LocationState(DataSource.MOCK, status, message = "Mock 위치 상태: $status")
    }
}
