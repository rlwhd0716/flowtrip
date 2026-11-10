package com.github.flowtrip.presentation.recommendation

import androidx.lifecycle.SavedStateHandle
import com.github.flowtrip.MainDispatcherRule
import com.github.flowtrip.data.repository.MockRecommendationRepository
import com.github.flowtrip.domain.model.Place
import com.github.flowtrip.domain.model.RecommendationRequest
import com.github.flowtrip.domain.model.RecommendationStage
import com.github.flowtrip.domain.model.TravelMode
import com.github.flowtrip.domain.repository.RecommendationRepository
import com.github.flowtrip.domain.usecase.GetRecommendationsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import com.github.flowtrip.data.repository.MockNavigationRepository
import com.github.flowtrip.data.repository.MockLocationRepository
import com.github.flowtrip.data.repository.MockUxRestrictionsRepository
import com.github.flowtrip.domain.usecase.RequestNavigationUseCase
import com.github.flowtrip.domain.usecase.ObserveLocationUseCase
import kotlinx.coroutines.test.TestScope

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val request = RecommendationRequest(TravelMode.DATE, "성수동", 120, "조용한 장소", true)
    private val navigation = MockNavigationRepository().apply { start() }
    private val ux = MockUxRestrictionsRepository().apply { start() }
    private val observeLocation = ObserveLocationUseCase(MockLocationRepository())
    private fun requestState() = SavedStateHandle(mapOf(
        "mode" to request.mode.name,
        "destination" to request.destination,
        "minutes" to request.availableMinutes,
        "preference" to request.preference,
        "newPlaces" to request.preferNewPlaces,
    ))

    private fun create(handle: SavedStateHandle = requestState()) = RecommendationViewModel(
        handle, GetRecommendationsUseCase(MockRecommendationRepository()), navigation,
        RequestNavigationUseCase(navigation, ux), observeLocation,
    )

    private fun TestScope.choose(vm: RecommendationViewModel) {
        vm.selectPlace()
        vm.confirmDestination()
        advanceUntilIdle()
    }

    @Test fun `map failure leaves recommendation and current route unchanged`() = runTest {
        val vm = create()
        advanceUntilIdle()
        val before = vm.uiState.value
        vm.openMap()
        advanceUntilIdle()
        val after = vm.uiState.value
        assertFalse(after.isOpeningMap)
        assertTrue(after.mapMessage!!.contains("Pleos"))
        assertEquals(before.stageIndex, after.stageIndex)
        assertEquals(before.place, after.place)
        assertEquals(before.selectedPlaces, after.selectedPlaces)
        assertEquals(before.navigation, after.navigation)
        assertNull(after.destinationConfirmation)
    }

    @Test fun `loading ignores actions and then shows first restaurant`() = runTest {
        val vm = create()
        assertTrue(vm.uiState.value.isLoading)
        assertEquals(request, vm.uiState.value.request)
        vm.selectPlace()
        vm.skipPlace()
        vm.recommendAnother()
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.stageIndex)
        assertEquals(RecommendationStage.RESTAURANT, vm.uiState.value.place?.stage)
        assertTrue(vm.uiState.value.selectedPlaces.isEmpty())
        assertTrue(vm.uiState.value.place!!.reason.contains(request.preference))
    }

    @Test fun `another recommendation changes candidate in same stage and wraps`() = runTest {
        val vm = create()
        advanceUntilIdle()
        val initial = vm.uiState.value.place
        vm.recommendAnother()
        assertNotEquals(initial, vm.uiState.value.place)
        assertEquals(0, vm.uiState.value.stageIndex)
        vm.recommendAnother()
        assertEquals(initial, vm.uiState.value.place)
        assertTrue(vm.uiState.value.selectedPlaces.isEmpty())
        assertTrue(vm.uiState.value.skippedPlaces.isEmpty())
    }

    @Test fun `select and skip advance all stages and completion is terminal`() = runTest {
        val vm = create()
        advanceUntilIdle()
        val restaurant = vm.uiState.value.place
        choose(vm)
        assertEquals(RecommendationStage.NIGHT_VIEW, vm.uiState.value.place?.stage)
        val view = vm.uiState.value.place
        vm.skipPlace()
        assertEquals(RecommendationStage.DESSERT, vm.uiState.value.place?.stage)
        choose(vm)
        val completed = vm.uiState.value
        assertTrue(completed.isComplete)
        assertNull(completed.place)
        assertEquals(2, completed.selectedPlaces.size)
        assertEquals(restaurant, completed.selectedPlaces.first())
        assertEquals(listOf(view), completed.skippedPlaces)
        vm.selectPlace()
        vm.skipPlace()
        vm.recommendAnother()
        assertEquals(completed, vm.uiState.value)
    }

    @Test fun `saved state restores candidate and decisions in new ViewModel`() = runTest {
        val handle = requestState()
        val original = create(handle)
        advanceUntilIdle()
        choose(original)
        original.skipPlace()
        original.recommendAnother()
        val snapshot = handle.keys().associateWith { handle.get<Any?>(it) }
        val restoredHandle = SavedStateHandle(snapshot)
        val restored = create(restoredHandle)
        advanceUntilIdle()
        assertEquals(original.uiState.value, restored.uiState.value)
        choose(restored)
        val completed = create(SavedStateHandle(restoredHandle.keys().associateWith { restoredHandle.get<Any?>(it) }))
        advanceUntilIdle()
        assertTrue(completed.uiState.value.isComplete)
        assertEquals(restored.uiState.value, completed.uiState.value)
    }

    @Test fun `repository failure supports retry without advancing stage`() = runTest {
        var fail = true
        val repository = object : RecommendationRepository {
            override suspend fun getRecommendations(request: RecommendationRequest): List<Place> {
                if (fail) error("offline")
                return MockRecommendationRepository().getRecommendations(request)
            }
        }
        val vm = RecommendationViewModel(requestState(), GetRecommendationsUseCase(repository), navigation,
            RequestNavigationUseCase(navigation, ux), observeLocation)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
        vm.skipPlace()
        assertEquals(0, vm.uiState.value.stageIndex)
        fail = false
        vm.load()
        advanceUntilIdle()
        assertNull(vm.uiState.value.error)
        assertNotNull(vm.uiState.value.place)
    }

    @Test fun `incomplete repository response is an error`() = runTest {
        val repository = object : RecommendationRepository {
            override suspend fun getRecommendations(request: RecommendationRequest) = emptyList<Place>()
        }
        val vm = RecommendationViewModel(requestState(), GetRecommendationsUseCase(repository), navigation,
            RequestNavigationUseCase(navigation, ux), observeLocation)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.place)
    }

    @Test fun `selection needs separate consent and failure never advances`() = runTest {
        val vm = create()
        advanceUntilIdle()
        vm.selectPlace()
        assertNotNull(vm.uiState.value.destinationConfirmation)
        assertNull(navigation.state.value.destination)
        vm.dismissDestination()
        assertNull(vm.uiState.value.destinationConfirmation)
        assertTrue(vm.uiState.value.selectedPlaces.isEmpty())
        navigation.failNextRoute = true
        choose(vm)
        assertEquals(0, vm.uiState.value.stageIndex)
        assertTrue(vm.uiState.value.selectedPlaces.isEmpty())
        assertNotNull(vm.uiState.value.navigationError)
        choose(vm)
        assertEquals(1, vm.uiState.value.stageIndex)
        assertEquals(1, vm.uiState.value.selectedPlaces.size)
    }

    @Test fun `arrival is delivered to recommendation state without visit completion or auto advance`() = runTest {
        val vm = create()
        advanceUntilIdle()
        choose(vm)
        val before = vm.uiState.value
        navigation.simulateArrival()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.arrivalSignal)
        assertEquals(before.stageIndex, vm.uiState.value.stageIndex)
        assertEquals(before.selectedPlaces, vm.uiState.value.selectedPlaces)
        navigation.simulateExternalDestinationChange(null)
        advanceUntilIdle()
        assertNull(vm.uiState.value.navigation.destination)
    }
}
