package com.github.flowtrip.presentation

import androidx.lifecycle.SavedStateHandle
import com.github.flowtrip.domain.model.TravelMode
import com.github.flowtrip.presentation.date.DateViewModel
import com.github.flowtrip.presentation.trip.TripViewModel
import org.junit.Assert.*
import org.junit.Test

class FormViewModelTest {
    @Test fun `trip requires destination and request uses current inputs`() {
        val handle = SavedStateHandle()
        val vm = TripViewModel(handle)
        assertNull(vm.createRequest())
        vm.setDestination("   ")
        assertFalse(vm.uiState.value.canStart)
        vm.setDestination(" 강릉 & 바다/카페? ")
        vm.setTime(180)
        vm.setPreference("맛집 탐방")
        val request = vm.createRequest()!!
        assertEquals(TravelMode.TRIP, request.mode)
        assertEquals("강릉 & 바다/카페?", request.destination)
        assertEquals(180, request.availableMinutes)
        assertEquals("맛집 탐방", request.preference)
        val restored = TripViewModel(SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }))
        assertEquals(vm.uiState.value, restored.uiState.value)
    }

    @Test fun `date choices survive restoration and invalid choices are ignored`() {
        val handle = SavedStateHandle()
        val vm = DateViewModel(handle)
        vm.setTime(240)
        vm.setMood("활동적인 장소")
        vm.setPreferNewPlaces(false)
        vm.setTime(-1)
        vm.setMood("invalid")
        val restored = DateViewModel(SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) }))
        assertEquals(vm.uiState.value, restored.uiState.value)
        assertEquals(TravelMode.DATE, restored.createRequest().mode)
        assertEquals(240, restored.createRequest().availableMinutes)
        assertEquals("활동적인 장소", restored.createRequest().preference)
        assertFalse(restored.createRequest().preferNewPlaces)
    }
}
