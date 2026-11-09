package com.github.flowtrip.platform

import com.github.flowtrip.data.repository.MockNavigationRepository
import com.github.flowtrip.data.repository.MockUxRestrictionsRepository
import com.github.flowtrip.domain.model.*
import com.github.flowtrip.domain.usecase.RequestNavigationUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationRepositoryTest {
    private val destination = Destination("test", "테스트 장소", Coordinates(37.54, 127.05), DataSource.MOCK)

    @Test fun `no approval and restricted UX cannot mutate destination`() = runTest {
        val navigation = MockNavigationRepository().apply { start() }
        val ux = MockUxRestrictionsRepository().apply { start() }
        val request = RequestNavigationUseCase(navigation, ux)
        assertTrue(request(destination, false) is PlatformResult.Failure)
        assertNull(navigation.state.value.destination)
        ux.simulate(UxAccess.RESTRICTED)
        assertTrue(request(destination, true) is PlatformResult.Failure)
        ux.simulate(UxAccess.UNKNOWN)
        assertTrue(request(destination, true) is PlatformResult.Failure)
        assertNull(navigation.state.value.destination)
    }

    @Test fun `routing requires async result and supports failure then retry`() = runTest {
        val navigation = MockNavigationRepository().apply { start(); failNextRoute = true }
        val failed = async { navigation.requestRoute(destination, true) }
        runCurrent()
        assertEquals(RouteState.SEARCHING, navigation.state.value.route)
        assertNull(navigation.state.value.destination)
        advanceUntilIdle()
        assertTrue(failed.await() is PlatformResult.Failure)
        assertEquals(RouteState.FAILED, navigation.state.value.route)
        assertEquals(PlatformResult.Success, navigation.requestRoute(destination, true))
        assertEquals(destination, navigation.state.value.destination)
    }

    @Test fun `stopping or external destination change invalidates in flight request`() = runTest {
        val navigation = MockNavigationRepository().apply { start() }
        val stopped = async { navigation.requestRoute(destination, true) }
        runCurrent()
        navigation.stop()
        advanceUntilIdle()
        assertTrue(stopped.await() is PlatformResult.Failure)
        assertNull(navigation.state.value.destination)
        navigation.start()
        val changed = async { navigation.requestRoute(destination, true) }
        runCurrent()
        val external = destination.copy(id = "external", name = "사용자 변경 목적지")
        navigation.simulateExternalDestinationChange(external)
        advanceUntilIdle()
        assertTrue(changed.await() is PlatformResult.Failure)
        assertEquals(external, navigation.state.value.destination)
    }

    @Test fun `automotive mock never asserts parked or unrestricted`() {
        val ux = MockUxRestrictionsRepository(isAutomotive = true)
        ux.start()
        assertEquals(UxAccess.UNKNOWN, ux.state.value.access)
        assertFalse(ux.state.value.allowsInteraction)
    }

    @Test fun `cancelling route collection cannot leave simulator stuck searching`() = runTest {
        val navigation = MockNavigationRepository().apply { start() }
        val job = async { navigation.requestRoute(destination, true) }
        runCurrent()
        job.cancel()
        advanceUntilIdle()
        assertEquals(RouteState.FAILED, navigation.state.value.route)
        assertNull(navigation.state.value.destination)
        assertEquals(PlatformResult.Success, navigation.requestRoute(destination, true))
    }
}
