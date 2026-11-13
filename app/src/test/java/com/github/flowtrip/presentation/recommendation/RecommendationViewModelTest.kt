package com.github.flowtrip.presentation.recommendation

import androidx.lifecycle.SavedStateHandle
import com.github.flowtrip.MainDispatcherRule
import com.github.flowtrip.data.repository.*
import com.github.flowtrip.domain.model.*
import com.github.flowtrip.domain.repository.*
import com.github.flowtrip.domain.usecase.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Updated course behavior: browsing never dispatches a route. Not a real SDK/device test. */
@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val navigation = MockNavigationRepository().apply { start() }
    private val ux = MockUxRestrictionsRepository().apply { start() }
    private fun handle() = SavedStateHandle(mapOf(
        "mode" to "DATE", "destination" to "성수동", "minutes" to 180,
        "preference" to "조용한 장소", "newPlaces" to true,
    ))
    private fun create(saved: SavedStateHandle = handle(), repository: RecommendationRepository = MockRecommendationRepository(),
        platform: NavigationRepository = navigation) = RecommendationViewModel(
        saved, GetRecommendationsUseCase(repository), platform, RequestNavigationUseCase(platform, ux),
        ObserveLocationUseCase(MockLocationRepository()), OfficialNavigationTestCatalog(),
    )
    private fun snapshot(saved: SavedStateHandle) = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) })

    @Test fun `browsing all modes stages and candidates never sends a route`() = runTest {
        val vm = create(); advanceUntilIdle()
        for (mode in TravelMode.entries) {
            vm.switchMode(mode); advanceUntilIdle()
            for (stage in mode.stages) {
                assertEquals(stage, vm.uiState.value.place!!.stage)
                assertEquals(3, vm.uiState.value.candidates.size)
                val ids = vm.uiState.value.candidates.map { it.id }
                repeat(3) { index ->
                    vm.recommendAnother()
                    assertEquals(ids[(index + 1) % 3], vm.uiState.value.place!!.id)
                    assertNull(navigation.state.value.destination)
                }
                vm.nextCourse()
            }
            assertTrue(vm.uiState.value.isComplete)
            assertEquals(3, vm.uiState.value.selectedPlaces.size)
            val completed = vm.uiState.value
            vm.nextCourse(); vm.recommendAnother(); vm.selectPlace()
            assertEquals(completed, vm.uiState.value)
        }
    }

    @Test fun `explicit mappings survive reordered source data and third candidate cannot route`() = runTest {
        val repository = object : RecommendationRepository {
            override suspend fun getRecommendations(request: RecommendationRequest) =
                MockRecommendationRepository().getRecommendations(request).reversed()
        }
        val vm = create(repository = repository); advanceUntilIdle()
        assertEquals(DemoRouteMapping.UNSET, vm.uiState.value.place!!.demoRoute)
        vm.selectPlace(); advanceUntilIdle()
        assertNull(navigation.state.value.destination)
        assertNotNull(vm.uiState.value.navigationError)
        vm.chooseCandidate(1); vm.selectPlace(); advanceUntilIdle()
        assertEquals("pleos-sample-software-dream-center", navigation.state.value.destination!!.id)
        vm.chooseCandidate(2); vm.selectPlace(); advanceUntilIdle()
        assertEquals("pleos-sample-42dot", navigation.state.value.destination!!.id)
        assertEquals(0, vm.uiState.value.stageIndex)
    }

    @Test fun `mode snapshots and active selection survive process recreation without backend reload`() = runTest {
        val saved = handle(); val vm = create(saved); advanceUntilIdle()
        vm.nextCourse(); vm.chooseCandidate(2)
        val date = vm.uiState.value
        vm.switchMode(TravelMode.TRIP); advanceUntilIdle()
        vm.nextCourse(); vm.nextCourse(); vm.chooseCandidate(1)
        val trip = vm.uiState.value
        val noReload = object : RecommendationRepository {
            override suspend fun getRecommendations(request: RecommendationRequest): List<Place> = error("must restore")
        }
        val restored = create(snapshot(saved), noReload); advanceUntilIdle()
        assertEquals(TravelMode.TRIP, restored.uiState.value.request.mode)
        assertEquals(trip.place, restored.uiState.value.place)
        assertEquals(trip.selectedPlaces, restored.uiState.value.selectedPlaces)
        restored.switchMode(TravelMode.DATE); advanceUntilIdle()
        assertEquals(date.place, restored.uiState.value.place)
        assertEquals(date.stageIndex, restored.uiState.value.stageIndex)
        assertEquals(date.selectedPlaces, restored.uiState.value.selectedPlaces)
        restored.switchMode(TravelMode.TRIP); advanceUntilIdle()
        assertEquals(trip.place, restored.uiState.value.place)
        assertNull(navigation.state.value.destination)
    }

    @Test fun `success error retry and arrival never advance or rewrite the selected place`() = runTest {
        val vm = create(); advanceUntilIdle()
        vm.selectPlace(); advanceUntilIdle()
        val first = vm.uiState.value.place
        assertEquals("42dot", navigation.state.value.destination!!.name)
        assertEquals(0, vm.uiState.value.stageIndex)
        navigation.simulateArrival(); advanceUntilIdle()
        assertNotNull(vm.uiState.value.arrivalSignal)
        assertEquals(first, vm.uiState.value.place)
        vm.chooseCandidate(1)
        val second = vm.uiState.value.place
        navigation.failNextRoute = true
        vm.selectPlace(); advanceUntilIdle()
        assertEquals(second, vm.uiState.value.place)
        assertNotNull(vm.uiState.value.navigationError)
        vm.retryRoute(); advanceUntilIdle()
        assertEquals("소프트웨어드림센터", navigation.state.value.destination!!.name)
        assertNull(vm.uiState.value.navigationError)
        assertEquals(0, vm.uiState.value.stageIndex)
        assertTrue(vm.uiState.value.selectedPlaces.isEmpty())
    }

    @Test fun `pending route blocks mode candidate restart and course actions`() = runTest {
        val vm = create(); advanceUntilIdle()
        vm.selectPlace()
        val before = vm.uiState.value.place
        repeat(5) {
            vm.switchMode(TravelMode.TRIP); vm.chooseCandidate(1); vm.recommendAnother()
            vm.nextCourse(); vm.skipPlace(); vm.restartCourse(); vm.selectPlace(); vm.openMap()
        }
        assertTrue(vm.uiState.value.isRequestingRoute)
        advanceUntilIdle()
        assertEquals(before, vm.uiState.value.place)
        assertEquals(TravelMode.DATE, vm.uiState.value.request.mode)
        assertEquals(0, vm.uiState.value.stageIndex)
    }

    @Test fun `pending process restoration does not resend but permits explicit retry`() = runTest {
        val saved = handle(); val vm = create(saved); advanceUntilIdle()
        vm.chooseCandidate(1); vm.selectPlace()
        val copy = snapshot(saved)
        advanceUntilIdle()
        val fresh = MockNavigationRepository().apply { start() }
        val restored = create(copy, platform = fresh); advanceUntilIdle()
        assertNull(fresh.state.value.destination)
        assertTrue(restored.uiState.value.navigationError!!.contains("중단"))
        assertEquals(1, restored.uiState.value.candidateIndex)
        restored.retryRoute(); advanceUntilIdle()
        assertEquals("소프트웨어드림센터", fresh.state.value.destination!!.name)
    }

    @Test fun `restricted and unknown UX cannot send a route`() = runTest {
        val vm = create(); advanceUntilIdle()
        for (access in listOf(UxAccess.RESTRICTED, UxAccess.UNKNOWN)) {
            ux.simulate(access)
            vm.selectPlace(); advanceUntilIdle()
            assertNull(navigation.state.value.destination)
            assertNotNull(vm.uiState.value.navigationError)
        }
    }

    @Test fun `restart resets only current mode without touching navigation`() = runTest {
        val vm = create(); advanceUntilIdle()
        vm.selectPlace(); advanceUntilIdle()
        val route = navigation.state.value
        vm.nextCourse(); vm.chooseCandidate(2)
        val datePlace = vm.uiState.value.place
        vm.switchMode(TravelMode.TRIP); advanceUntilIdle()
        vm.nextCourse(); vm.restartCourse(); advanceUntilIdle()
        assertEquals(0, vm.uiState.value.stageIndex)
        assertEquals(0, vm.uiState.value.candidateIndex)
        assertTrue(vm.uiState.value.selectedPlaces.isEmpty())
        assertEquals(route, navigation.state.value)
        vm.switchMode(TravelMode.DATE); advanceUntilIdle()
        assertEquals(datePlace, vm.uiState.value.place)
    }

    @Test fun `incomplete response supports manual retry and ignores actions while loading`() = runTest {
        var fail = true
        val repository = object : RecommendationRepository {
            override suspend fun getRecommendations(request: RecommendationRequest): List<Place> {
                val result = MockRecommendationRepository().getRecommendations(request)
                return if (fail) result.take(1) else result
            }
        }
        val vm = create(repository = repository)
        vm.chooseCandidate(1); vm.nextCourse(); vm.switchMode(TravelMode.TRIP)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
        fail = false; vm.load(); advanceUntilIdle()
        assertNull(vm.uiState.value.error)
        assertEquals(3, vm.uiState.value.candidates.size)
    }

    @Test fun `saved mode snapshots support Android Java serialization`() = runTest {
        val saved = handle(); val vm = create(saved); advanceUntilIdle()
        vm.switchMode(TravelMode.TRIP); advanceUntilIdle()
        for (key in listOf("recommendations", "course_DATE")) {
            val original = saved.get<Any>(key)
            val bytes = java.io.ByteArrayOutputStream()
            java.io.ObjectOutputStream(bytes).use { it.writeObject(original) }
            val copy = java.io.ObjectInputStream(java.io.ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() }
            assertEquals(original, copy)
        }
    }

    @Test fun `external navigation change leaves recommendations intact`() = runTest {
        val vm = create(); advanceUntilIdle()
        val place = vm.uiState.value.place!!
        navigation.simulateExternalDestinationChange(place.destination!!.copy(coordinates = Coordinates(35.0, 129.0)))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.navigationDiffers)
        assertEquals(place, vm.uiState.value.place)
    }

    @Test fun `retained diagnostic waypoint request does not rewrite the course`() = runTest {
        val vm = create(); advanceUntilIdle()
        val before = vm.uiState.value.place
        val targets = vm.uiState.value.testDestinations
        vm.requestTestRoute(targets.first().id, viaOther = true); advanceUntilIdle()
        assertEquals(targets.first(), navigation.state.value.destination)
        assertEquals(listOf(targets.last()), navigation.state.value.waypoints)
        assertEquals(before, vm.uiState.value.place)
        assertEquals(0, vm.uiState.value.stageIndex)
    }

    @Test fun `home reuse opens requested stage and candidate without routing`() = runTest {
        val saved = handle().apply { set("place", "view-2") }
        val vm = create(saved); advanceUntilIdle()
        assertEquals(1, vm.uiState.value.stageIndex)
        assertEquals(1, vm.uiState.value.candidateIndex)
        assertEquals("강변 산책로", vm.uiState.value.place!!.name)
        assertNull(navigation.state.value.destination)
        assertTrue(vm.uiState.value.selectedPlaces.isEmpty())
        val restored = create(snapshot(saved)); advanceUntilIdle()
        assertEquals(vm.uiState.value.place, restored.uiState.value.place)
        assertNull(navigation.state.value.destination)
    }
}
