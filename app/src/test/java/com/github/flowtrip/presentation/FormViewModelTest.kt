package com.github.flowtrip.presentation

import androidx.lifecycle.SavedStateHandle
import com.github.flowtrip.MainDispatcherRule
import com.github.flowtrip.core.util.AvailableTimeUnit
import com.github.flowtrip.data.repository.MockLocationRepository
import com.github.flowtrip.domain.model.TravelMode
import com.github.flowtrip.domain.usecase.ObserveLocationUseCase
import com.github.flowtrip.presentation.date.DateViewModel
import com.github.flowtrip.presentation.trip.TripViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FormViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private fun snapshot(handle: SavedStateHandle) = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })

    @Test fun `trip requires cascading region and province change clears district`() {
        val vm = TripViewModel(SavedStateHandle())
        assertNull(vm.createRequest())
        vm.selectDistrict("해운대구")
        assertFalse(vm.uiState.value.canStart)
        vm.selectProvince("부산광역시")
        vm.selectDistrict("해운대구")
        assertEquals("부산광역시 해운대구", vm.createRequest()!!.destination)
        vm.selectProvince("강원특별자치도")
        assertEquals("", vm.uiState.value.district)
        assertNull(vm.createRequest())
        vm.selectDistrict("강릉시")
        vm.selectProvince("unknown")
        vm.selectDistrict("unknown")
        assertEquals("강원특별자치도 강릉시", vm.createRequest()!!.destination)
    }

    @Test fun `trip multi selection cap unit conversion and restoration retain request`() {
        val handle = SavedStateHandle(); val vm = TripViewModel(handle)
        vm.selectProvince("부산광역시"); vm.selectDistrict("해운대구")
        vm.togglePreference("맛집"); vm.togglePreference("카페·디저트")
        vm.togglePreference("쇼핑")
        assertEquals(3, vm.uiState.value.preferences.size)
        assertFalse("쇼핑" in vm.uiState.value.preferences)
        vm.setTime(180); vm.setTimeUnit(AvailableTimeUnit.DAYS)
        assertEquals(3 * 1440, vm.uiState.value.availableMinutes)
        vm.setTime(8 * 1440)
        assertEquals(3 * 1440, vm.uiState.value.availableMinutes)
        val request = vm.createRequest()!!
        assertEquals(TravelMode.TRIP, request.mode)
        assertTrue(request.preference.contains("맛집"))
        val restored = TripViewModel(snapshot(handle))
        assertEquals(vm.uiState.value, restored.uiState.value)
        assertEquals(request, restored.createRequest())
    }

    @Test fun `date limits moods supports multiple types and preserves location independent of inputs`() = runTest {
        val handle = SavedStateHandle()
        val observe = ObserveLocationUseCase(MockLocationRepository())
        val vm = DateViewModel(handle, observe); advanceUntilIdle()
        val location = vm.uiState.value.currentLocation
        vm.toggleMood("특별한 경험"); vm.toggleMood("편안한 힐링")
        assertEquals(listOf("로맨틱", "특별한 경험"), vm.uiState.value.moods)
        vm.toggleMood("ロマンチック")
        vm.togglePlaceType("야경·전망"); vm.togglePlaceType("카페·디저트")
        vm.setTimeUnit(AvailableTimeUnit.DAYS); vm.setTime(2 * 1440)
        vm.setPreferNewPlaces(false)
        assertEquals(location, vm.uiState.value.currentLocation)
        val restored = DateViewModel(snapshot(handle), observe); advanceUntilIdle()
        assertEquals(vm.uiState.value, restored.uiState.value)
        val request = restored.createRequest()
        assertEquals(TravelMode.DATE, request.mode)
        assertEquals(2880, request.availableMinutes)
        assertTrue(request.preference.contains("특별한 경험"))
        assertTrue(request.preference.contains("야경·전망"))
        assertFalse(request.preferNewPlaces)
    }

    @Test fun `time minimum maximum and switching large hours to days are bounded`() {
        val vm = TripViewModel(SavedStateHandle())
        vm.setTime(0); assertEquals(120, vm.uiState.value.availableMinutes)
        vm.setTime(24 * 60)
        vm.setTime(25 * 60); assertEquals(24 * 60, vm.uiState.value.availableMinutes)
        vm.setTimeUnit(AvailableTimeUnit.DAYS)
        assertEquals(7 * 1440, vm.uiState.value.availableMinutes)
        vm.setTimeUnit(AvailableTimeUnit.HOURS)
        assertEquals(7 * 60, vm.uiState.value.availableMinutes)
        vm.setTime(-1); assertEquals(7 * 60, vm.uiState.value.availableMinutes)
    }
}
