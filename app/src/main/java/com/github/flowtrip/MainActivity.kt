package com.github.flowtrip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.flowtrip.core.designsystem.FlowTripTheme
import com.github.flowtrip.core.navigation.FlowTripNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FlowTripApplication).container
        setContent {
            FlowTripTheme {
                FlowTripNavHost(container)
            }
        }
    }
}
