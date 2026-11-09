package com.github.flowtrip

import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.flowtrip.core.designsystem.FlowTripTheme
import com.github.flowtrip.core.navigation.FlowTripNavHost
import dagger.hilt.android.AndroidEntryPoint
import com.github.flowtrip.core.platform.PleosPermissions
import com.github.flowtrip.domain.repository.NavigationRepository
import com.github.flowtrip.domain.repository.LocationRepository
import com.github.flowtrip.domain.repository.UxRestrictionsRepository
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var navigation: NavigationRepository
    @Inject lateinit var location: LocationRepository
    @Inject lateinit var ux: UxRestrictionsRepository
    private var permissionAttempted = false
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        // Re-check the actual grant state in each adapter, including partial denial.
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            location.stop()
            navigation.stop()
            navigation.start()
            location.start()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionAttempted = savedInstanceState?.getBoolean("pleosPermissionAttempted") ?: false
        enableEdgeToEdge()
        setContent {
            FlowTripTheme {
                val uxState by ux.state.collectAsStateWithLifecycle()
                FlowTripNavHost(uxState)
            }
        }
        if (BuildConfig.PLEOS_SDK_ENABLED &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    ux.state.collect { state ->
                        // Do not open a permission dialog while driving/UX state is unknown.
                        if (state.allowsInteraction && !permissionAttempted) {
                            permissionAttempted = true
                            val missing = PleosPermissions.missingDangerous(this@MainActivity)
                            if (missing.isNotEmpty()) permissionRequest.launch(missing.toTypedArray())
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("pleosPermissionAttempted", permissionAttempted)
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        ux.start()
        navigation.start()
        location.start()
    }

    override fun onStop() {
        location.stop()
        navigation.stop()
        ux.stop()
        super.onStop()
    }
}
