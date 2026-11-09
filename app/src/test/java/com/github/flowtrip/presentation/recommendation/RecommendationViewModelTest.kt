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

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val request = RecommendationRequest(TravelMode.DATE, "성수동", 120, "조용한 장소", true)
    private fun requestState() = SavedStateHandle(mapOf(
        "mode" to request.mode.name,
        "destination" to request.destination,
        "minutes" to request.availableMinutes,
        "preference" to request.preference,
        "newPlaces" to request.preferNewPlaces,
    ))

    private fun create(handle: SavedStateHandle = requestState()) = RecommendationViewModel(
        handle, GetRecommendationsUseCase(MockRecommendationRepository()),
    )

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
        vm.selectPlace()
        assertEquals(RecommendationStage.NIGHT_VIEW, vm.uiState.value.place?.stage)
        val view = vm.uiState.value.place
        vm.skipPlace()
        assertEquals(RecommendationStage.DESSERT, vm.uiState.value.place?.stage)
        vm.selectPlace()
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
        original.selectPlace()
        original.skipPlace()
        original.recommendAnother()
        val snapshot = handle.keys().associateWith { handle.get<Any?>(it) }
        val restoredHandle = SavedStateHandle(snapshot)
        val restored = create(restoredHandle)
        advanceUntilIdle()
        assertEquals(original.uiState.value, restored.uiState.value)
        restored.selectPlace()
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
        val vm = RecommendationViewModel(requestState(), GetRecommendationsUseCase(repository))
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
        val vm = RecommendationViewModel(requestState(), GetRecommendationsUseCase(repository))
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.place)
    }
}
