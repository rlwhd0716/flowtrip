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

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendationViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val request = RecommendationRequest(TravelMode.DATE, "성수동", 120, "조용한 장소", true)
    private val navigation = MockNavigationRepository().apply { start() }
    private val ux = MockUxRestrictionsRepository().apply { start() }
    private val observeLocation = ObserveLocationUseCase(MockLocationRepository())
    private fun requestState() = SavedStateHandle(mapOf(
        "mode" to request.mode.name, "destination" to request.destination, "minutes" to request.availableMinutes,
        "preference" to request.preference, "newPlaces" to request.preferNewPlaces,
    ))
    private fun create(handle: SavedStateHandle = requestState(), repository: RecommendationRepository = MockRecommendationRepository(),
        platform: NavigationRepository = navigation) = RecommendationViewModel(
        handle, GetRecommendationsUseCase(repository), platform, RequestNavigationUseCase(platform, ux), observeLocation,
        OfficialNavigationTestCatalog(),
    )
    private fun snapshot(handle: SavedStateHandle) = SavedStateHandle(handle.keys().associateWith { handle.get<Any?>(it) })

    @Test fun `official real test requests preserve recommendation and support both targets and waypoints`() = runTest {
        val vm = create(); advanceUntilIdle()
        val before = vm.uiState.value
        val targets = before.testDestinations
        assertEquals(2, targets.size)
        assertTrue(targets.all { it.source == DataSource.PLEOS && !it.poiId.isNullOrBlank() && !it.poiSubId.isNullOrBlank() })
        for (target in targets) {
            vm.requestTestRoute(target.id)
            vm.nextCourse(); vm.recommendAnother()
            advanceUntilIdle()
            assertEquals(target, navigation.state.value.destination)
            assertEquals(before.place, vm.uiState.value.place)
            assertEquals(before.stageIndex, vm.uiState.value.stageIndex)
            assertTrue(vm.uiState.value.selectedPlaces.isEmpty())
        }
        vm.requestTestRoute(targets.first().id, viaOther = true); advanceUntilIdle()
        assertEquals(targets.first(), navigation.state.value.destination)
        assertEquals(listOf(targets.last()), navigation.state.value.waypoints)
        assertTrue(vm.uiState.value.testRouteMessage!!.contains("SDK"))
    }

    @Test fun `failed real test request restores retry target after recreation without automatic resend`() = runTest {
        val handle = requestState(); val vm = create(handle); advanceUntilIdle()
        val target = vm.uiState.value.testDestinations.last()
        navigation.failNextRoute = true
        vm.requestTestRoute(target.id); advanceUntilIdle()
        assertTrue(vm.uiState.value.isTestRouteRetry)
        assertNotNull(vm.uiState.value.navigationError)
        val restored = create(snapshot(handle)); advanceUntilIdle()
        assertNull(navigation.state.value.destination)
        restored.retryRoute(); advanceUntilIdle()
        assertEquals(target, navigation.state.value.destination)
        assertFalse(restored.uiState.value.isTestRouteRetry)
        assertNull(restored.uiState.value.navigationError)
    }

    @Test fun `UX restriction prevents real test routing too`() = runTest {
        val vm = create(); advanceUntilIdle()
        ux.simulate(UxAccess.RESTRICTED)
        vm.requestTestRoute(vm.uiState.value.testDestinations.first().id); advanceUntilIdle()
        assertNull(navigation.state.value.destination)
        assertNotNull(vm.uiState.value.navigationError)
    }

    @Test fun `loading ignores actions then reuses request and three original ordered candidates`() = runTest {
        val vm = create()
        vm.selectPlace(); vm.nextCourse(); vm.recommendAnother(); vm.skipPlace()
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(request, state.request)
        assertEquals(0, state.stageIndex)
        assertEquals(listOf("food-1", "food-2", "food-3"), state.candidates.map { it.id })
        assertTrue(state.place!!.reason.contains(request.preference))
        assertNull(navigation.state.value.destination)
    }

    @Test fun `single action routes A B C A and does not open navigation or advance stage`() = runTest {
        var opens = 0
        val platform = object : NavigationRepository by navigation {
            override suspend fun openMap(): PlatformResult { opens++; return PlatformResult.Success }
        }
        val vm = create(platform = platform)
        advanceUntilIdle()
        vm.selectPlace(); advanceUntilIdle()
        assertEquals("food-1", vm.uiState.value.place!!.id)
        repeat(3) { index ->
            vm.recommendAnother()
            // The existing card stays until routing has completed.
            assertEquals(listOf("food-1", "food-2", "food-3")[index], vm.uiState.value.place!!.id)
            advanceUntilIdle()
            assertEquals(listOf("food-2", "food-3", "food-1")[index], vm.uiState.value.place!!.id)
            assertEquals(vm.uiState.value.place!!.destination, navigation.state.value.destination)
            assertEquals(0, vm.uiState.value.stageIndex)
        }
        assertEquals(0, opens)
        assertTrue(vm.uiState.value.selectedPlaces.isEmpty())
    }

    @Test fun `failure retains candidate stage and last confirmed destination and retry targets B`() = runTest {
        val vm = create(); advanceUntilIdle()
        vm.selectPlace(); advanceUntilIdle()
        val before = vm.uiState.value
        navigation.failNextRoute = true
        vm.recommendAnother(); advanceUntilIdle()
        assertEquals(before.place, vm.uiState.value.place)
        assertEquals(before.lastConfirmedDestination, vm.uiState.value.lastConfirmedDestination)
        assertEquals(before.navigation.destination, vm.uiState.value.navigation.destination)
        assertEquals(RouteState.GUIDING, vm.uiState.value.navigation.route)
        assertNotNull(vm.uiState.value.navigationError)
        vm.retryRoute(); advanceUntilIdle()
        assertEquals("food-2", vm.uiState.value.place!!.id)
        assertNull(vm.uiState.value.navigationError)
    }

    @Test fun `rapid route stage skip and screen actions cannot overlap an outstanding request`() = runTest {
        val vm = create(); advanceUntilIdle()
        vm.recommendAnother()
        repeat(10) { vm.recommendAnother(); vm.nextCourse(); vm.selectPlace(); vm.skipPlace(); vm.openMap() }
        assertTrue(vm.uiState.value.isRequestingRoute)
        advanceUntilIdle()
        assertEquals("food-2", vm.uiState.value.place!!.id)
        assertEquals(0, vm.uiState.value.stageIndex)
        assertNull(vm.uiState.value.mapMessage)
    }

    @Test fun `arrival never auto advances and next course commits only after routing success`() = runTest {
        val vm = create(); advanceUntilIdle()
        vm.selectPlace(); advanceUntilIdle()
        navigation.simulateArrival(); advanceUntilIdle()
        assertNotNull(vm.uiState.value.arrivalSignal)
        assertEquals(0, vm.uiState.value.stageIndex)
        navigation.failNextRoute = true
        vm.nextCourse(); advanceUntilIdle()
        assertEquals(0, vm.uiState.value.stageIndex)
        assertTrue(vm.uiState.value.selectedPlaces.isEmpty())
        vm.retryRoute(); advanceUntilIdle()
        assertEquals(1, vm.uiState.value.stageIndex)
        assertEquals("view-1", vm.uiState.value.place!!.id)
        assertEquals(listOf("food-1"), vm.uiState.value.selectedPlaces.map { it.id })
        assertNull(vm.uiState.value.arrivalSignal)
        vm.nextCourse(); advanceUntilIdle()
        vm.nextCourse()
        assertTrue(vm.uiState.value.isComplete)
        val complete = vm.uiState.value
        vm.selectPlace(); vm.recommendAnother(); vm.nextCourse(); vm.skipPlace()
        assertEquals(complete, vm.uiState.value)
    }

    @Test fun `remaining course sends ordered waypoints and candidate change updates that course`() = runTest {
        val vm = create(); advanceUntilIdle()
        assertEquals(listOf("food-1", "view-1", "cafe-1"), vm.uiState.value.remainingCourse.map { it.id })
        vm.requestRemainingCourse(); advanceUntilIdle()
        assertTrue(vm.uiState.value.isCourseRoute)
        assertEquals(listOf("food-1", "view-1"), navigation.state.value.waypoints.map { it.id })
        assertEquals("cafe-1", navigation.state.value.destination!!.id)
        assertFalse(vm.uiState.value.navigationDiffers)
        vm.recommendAnother(); advanceUntilIdle()
        assertEquals(listOf("food-2", "view-1"), navigation.state.value.waypoints.map { it.id })
        navigation.simulateArrival(isWaypoint = true); advanceUntilIdle()
        assertEquals(0, vm.uiState.value.stageIndex)
        assertTrue(vm.uiState.value.arrivalSignal!!.isWaypoint)
        vm.nextCourse(); advanceUntilIdle()
        assertEquals(listOf("view-1"), navigation.state.value.waypoints.map { it.id })
    }

    @Test fun `external destination sync does not rewrite recommendations or auto route back`() = runTest {
        val vm = create(); advanceUntilIdle()
        vm.selectPlace(); advanceUntilIdle()
        val before = vm.uiState.value.place
        val external = before!!.destination!!.copy(id = "external", name = "외부 목적지", coordinates = Coordinates(35.0, 129.0))
        navigation.simulateExternalDestinationChange(external); advanceUntilIdle()
        assertEquals(external, vm.uiState.value.navigation.destination)
        assertEquals(external, vm.uiState.value.lastConfirmedDestination)
        assertEquals(before, vm.uiState.value.place)
        assertTrue(vm.uiState.value.navigationDiffers)
        navigation.simulateExternalDestinationChange(null); advanceUntilIdle()
        assertNull(vm.uiState.value.navigation.destination)
    }

    @Test fun `restored snapshot retains actual candidates without calling recommendation backend`() = runTest {
        val handle = requestState(); val vm = create(handle); advanceUntilIdle()
        vm.recommendAnother(); advanceUntilIdle()
        vm.nextCourse(); advanceUntilIdle()
        val noReload = object : RecommendationRepository {
            override suspend fun getRecommendations(request: RecommendationRequest): List<Place> = error("must restore")
        }
        val restored = create(snapshot(handle), noReload); advanceUntilIdle()
        assertEquals(vm.uiState.value.place, restored.uiState.value.place)
        assertEquals(vm.uiState.value.candidates, restored.uiState.value.candidates)
        assertEquals(vm.uiState.value.selectedPlaces, restored.uiState.value.selectedPlaces)
        assertEquals(vm.uiState.value.lastConfirmedDestination, restored.uiState.value.lastConfirmedDestination)
        assertEquals(vm.uiState.value.stageIndex, restored.uiState.value.stageIndex)
        assertNull(restored.uiState.value.error)
    }

    @Test fun `interrupted process snapshot does not replay route and explicit retry keeps pending target`() = runTest {
        val handle = requestState(); val vm = create(handle); advanceUntilIdle()
        vm.recommendAnother()
        val saved = snapshot(handle)
        advanceUntilIdle()
        val freshPlatform = MockNavigationRepository().apply { start() }
        val restored = create(saved, platform = freshPlatform); advanceUntilIdle()
        assertEquals("food-1", restored.uiState.value.place!!.id)
        assertNull(freshPlatform.state.value.destination)
        assertTrue(restored.uiState.value.navigationError!!.contains("중단"))
        restored.retryRoute(); advanceUntilIdle()
        assertEquals("food-2", restored.uiState.value.place!!.id)
    }

    @Test fun `saved candidates survive Java serialization used by Android saved state`() = runTest {
        val handle = requestState(); create(handle); advanceUntilIdle()
        val bytes = java.io.ByteArrayOutputStream()
        java.io.ObjectOutputStream(bytes).use { it.writeObject(handle.get<ArrayList<Place>>("recommendations")) }
        val restored = java.io.ObjectInputStream(java.io.ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() }
        assertEquals(handle.get<ArrayList<Place>>("recommendations"), restored)
    }

    @Test fun `restricted or unknown UX blocks every route button without changing candidates`() = runTest {
        val vm = create(); advanceUntilIdle()
        val before = vm.uiState.value.place
        listOf(UxAccess.RESTRICTED, UxAccess.UNKNOWN).forEach {
            ux.simulate(it)
            vm.selectPlace(); advanceUntilIdle()
            vm.recommendAnother(); advanceUntilIdle()
            vm.nextCourse(); advanceUntilIdle()
            vm.requestRemainingCourse(); advanceUntilIdle()
            assertEquals(before, vm.uiState.value.place)
            assertEquals(0, vm.uiState.value.stageIndex)
            assertNull(navigation.state.value.destination)
        }
    }

    @Test fun `failed loading supports retry and incomplete repository response reports error`() = runTest {
        var fail = true
        val repo = object : RecommendationRepository {
            override suspend fun getRecommendations(request: RecommendationRequest): List<Place> {
                if (fail) error("offline")
                return MockRecommendationRepository().getRecommendations(request)
            }
        }
        val vm = create(repository = repo); advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
        vm.skipPlace(); assertEquals(0, vm.uiState.value.stageIndex)
        fail = false; vm.load(); advanceUntilIdle()
        assertNull(vm.uiState.value.error)
        var incomplete = true
        val partial = object : RecommendationRepository {
            override suspend fun getRecommendations(request: RecommendationRequest): List<Place> {
                val all = MockRecommendationRepository().getRecommendations(request)
                return if (incomplete) all.take(1) else all
            }
        }
        val bad = create(repository = partial); advanceUntilIdle()
        assertNotNull(bad.uiState.value.error)
        incomplete = false
        bad.load(); advanceUntilIdle()
        assertNull(bad.uiState.value.error)
        assertEquals(3, bad.uiState.value.candidates.size)
    }

    @Test fun `map failure and skipping never change current navigation`() = runTest {
        val vm = create(); advanceUntilIdle()
        vm.selectPlace(); advanceUntilIdle()
        val before = navigation.state.value
        vm.openMap(); advanceUntilIdle()
        assertTrue(vm.uiState.value.mapMessage!!.contains("Pleos"))
        assertFalse(vm.uiState.value.isOpeningMap)
        vm.skipPlace()
        assertEquals(before, navigation.state.value)
        assertEquals(1, vm.uiState.value.stageIndex)
        assertEquals(listOf("food-1"), vm.uiState.value.skippedPlaces.map { it.id })
    }
}
