package com.github.flowtrip.core.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material3.Text
import com.github.flowtrip.domain.model.UxState
import com.github.flowtrip.presentation.common.ScreenLayout
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.flowtrip.domain.model.RecommendationRequest
import com.github.flowtrip.presentation.date.DateScreen
import com.github.flowtrip.presentation.date.DateViewModel
import com.github.flowtrip.presentation.home.HomeScreen
import com.github.flowtrip.presentation.recommendation.RecommendationScreen
import com.github.flowtrip.presentation.recommendation.RecommendationViewModel
import com.github.flowtrip.presentation.trip.TripScreen
import com.github.flowtrip.presentation.trip.TripViewModel

private object Routes {
    const val HOME = "home"
    const val TRIP = "trip"
    const val DATE = "date"
    const val RECOMMENDATION = "recommendation/{mode}?destination={destination}&minutes={minutes}&preference={preference}&newPlaces={newPlaces}"

    fun recommendation(request: RecommendationRequest) =
        "recommendation/${request.mode.name}?destination=${Uri.encode(request.destination)}" +
            "&minutes=${request.availableMinutes}&preference=${Uri.encode(request.preference)}&newPlaces=${request.preferNewPlaces}"
}

@Composable
fun FlowTripNavHost(ux: UxState) {
    val navController = rememberNavController()
    val focus = LocalFocusManager.current
    LaunchedEffect(ux.allowsInteraction) { if (!ux.allowsInteraction) focus.clearFocus(force = true) }
    if (!ux.allowsInteraction) {
        ScreenLayout("정차 후 사용해 주세요") { Text(ux.message) }
        return
    }
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onTrip = { navController.navigate(Routes.TRIP) { launchSingleTop = true } },
                onDate = { navController.navigate(Routes.DATE) { launchSingleTop = true } })
        }
        composable(Routes.TRIP) {
            val vm: TripViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            TripScreen(state, vm::setDestination, vm::setTime, vm::setPreference,
                onStart = { vm.createRequest()?.let { request ->
                    navController.navigate(Routes.recommendation(request)) { launchSingleTop = true }
                } }, onBack = { navController.popBackStack() })
        }
        composable(Routes.DATE) {
            val vm: DateViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            DateScreen(state, vm::setTime, vm::setMood, vm::setPreferNewPlaces,
                onStart = { navController.navigate(Routes.recommendation(vm.createRequest())) { launchSingleTop = true } },
                onBack = { navController.popBackStack() })
        }
        composable(Routes.RECOMMENDATION, arguments = listOf(
            navArgument("mode") { type = NavType.StringType },
            navArgument("destination") { type = NavType.StringType },
            navArgument("minutes") { type = NavType.IntType },
            navArgument("preference") { type = NavType.StringType },
            navArgument("newPlaces") { type = NavType.BoolType },
        )) {
            val vm: RecommendationViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            RecommendationScreen(state, vm::selectPlace, vm::recommendAnother, vm::skipPlace, vm::load,
                onConfirmDestination = vm::confirmDestination, onDismissDestination = vm::dismissDestination,
                onHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onBack = { navController.popBackStack() })
        }
    }
}
