package com.github.flowtrip

import android.os.Build
import android.os.Bundle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.github.flowtrip.data.repository.OfficialNavigationTestCatalog
import com.github.flowtrip.domain.model.PlatformResult
import com.github.flowtrip.domain.model.DataSource
import com.github.flowtrip.domain.model.RouteState
import com.github.flowtrip.domain.model.ServiceStatus
import com.github.flowtrip.domain.model.UxAccess
import com.github.flowtrip.domain.usecase.RequestNavigationUseCase
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit opt-in only. Sends real routes on a Pleos emulator; never runs on a vehicle. */
class PleosNavigationSmokeTest {
    @Test fun officialDestinationsReachGuidingState() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("runRealNavigation") == "true")
        assumeTrue(BuildConfig.PLEOS_SDK_ENABLED)
        assumeTrue(Build.MODEL.contains("emulator", ignoreCase = true))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val reference = AtomicReference<MainActivity>()
            scenario.onActivity { reference.set(it) }
            val activity = reference.get()
            runBlocking {
                val ux = withTimeout(15_000) { activity.ux.state.first { it.access != UxAccess.UNKNOWN } }
                assertTrue("실제 차량 UX 제한: ${ux.message}", ux.allowsInteraction)
                val connected = withTimeout(25_000) { activity.navigation.state.first {
                    it.service != ServiceStatus.STOPPED && it.service != ServiceStatus.CONNECTING
                } }
                assertEquals("Navi Service: ${connected.message}", ServiceStatus.READY, connected.service)
                assertEquals(DataSource.PLEOS, connected.source)
                val request = RequestNavigationUseCase(activity.navigation, activity.ux)
                val destinations = OfficialNavigationTestCatalog().destinations
                // Start with a different destination if an earlier test left guidance active.
                delay(1_000)
                val currentId = activity.navigation.state.value.destination?.poiId
                val ordered = if (currentId == destinations.first().poiId) destinations.reversed() else destinations
                for (destination in ordered) {
                    val result = request(destination, userConfirmed = true)
                    val state = activity.navigation.state.value
                    InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
                        putString("stream", "\n${destination.name}: $result; service=${state.service}; route=${state.route}; destination=${state.destination?.name}\n")
                    })
                    assertEquals("${destination.name}: $result / ${state.message}", PlatformResult.Success, result)
                    assertEquals(RouteState.GUIDING, state.route)
                    assertEquals(destination.poiId, state.destination?.poiId)
                }
            }
        }
    }
}
