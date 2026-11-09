package com.github.flowtrip.platform

import com.github.flowtrip.data.repository.MockLocationRepository
import com.github.flowtrip.domain.model.*
import com.github.flowtrip.domain.usecase.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationTest {
    private val coordinates = Coordinates(37.5445, 127.0557)
    @Test fun `location expires without a new callback and stop clears location`() = runTest {
        val repository = MockLocationRepository { 1_000_000 + testScheduler.currentTime }
        val observed = mutableListOf<LocationState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveLocationUseCase(repository) { 1_000_000 + testScheduler.currentTime }().collect { observed.add(it) }
        }
        repository.start()
        runCurrent()
        assertEquals(LocationStatus.AVAILABLE, observed.last().status)
        advanceTimeBy(LOCATION_MAX_AGE_MILLIS)
        runCurrent()
        assertEquals(LocationStatus.STALE, observed.last().status)
        repository.stop()
        runCurrent()
        assertEquals(LocationStatus.UNAVAILABLE, observed.last().status)
        assertNull(observed.last().location)
    }

    @Test fun `null unsupported denied and error states retain no coordinates`() {
        val repository = MockLocationRepository()
        repository.simulateLocation(null)
        assertEquals(LocationStatus.UNAVAILABLE, repository.state.value.status)
        listOf(LocationStatus.UNSUPPORTED, LocationStatus.PERMISSION_DENIED, LocationStatus.ERROR).forEach {
            repository.simulateFailure(it)
            assertEquals(it, repository.state.value.status)
            assertNull(repository.state.value.location)
        }
    }

    @Test fun `future missing and old measurement timestamps are not fresh`() {
        val now = 100_000L
        assertFalse(LocationInfo(coordinates, 0, DataSource.PLEOS).isFresh(now))
        assertFalse(LocationInfo(coordinates, now + 1, DataSource.PLEOS).isFresh(now))
        assertFalse(LocationInfo(coordinates, now - LOCATION_MAX_AGE_MILLIS, DataSource.PLEOS).isFresh(now))
        assertTrue(LocationInfo(coordinates, now, DataSource.PLEOS).isFresh(now))
    }

    @Test fun `proximity ignores stale and mixed sources and never completes visits`() {
        val destination = Destination("a", "장소", coordinates, DataSource.PLEOS)
        val current = LocationState(DataSource.PLEOS, LocationStatus.AVAILABLE, LocationInfo(coordinates, 100_000, DataSource.PLEOS))
        val proximity = CheckProximityUseCase()
        assertEquals(Proximity.NEAR, proximity(current, destination, 100_000))
        assertEquals(Proximity.FAR, proximity(current, destination.copy(coordinates = Coordinates(35.0, 129.0)), 100_000))
        assertEquals(Proximity.UNKNOWN, proximity(current, destination.copy(source = DataSource.MOCK), 100_000))
        assertEquals(Proximity.UNKNOWN, proximity(current.copy(status = LocationStatus.STALE), destination, 100_000))
        assertEquals(Proximity.UNKNOWN, proximity(current, destination, 200_000))
    }

    @Test fun `invalid coordinates are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { Coordinates(91.0, 127.0) }
        assertThrows(IllegalArgumentException::class.java) { Coordinates(Double.NaN, 127.0) }
    }
}
