package com.github.flowtrip.core.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.flowtrip.AppContainer
import com.github.flowtrip.domain.model.RecommendationRequest
import com.github.flowtrip.domain.model.TravelMode
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
fun FlowTripNavHost(container: AppContainer) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onTrip = { navController.navigate(Routes.TRIP) { launchSingleTop = true } },
                onDate = { navController.navigate(Routes.DATE) { launchSingleTop = true } })
        }
        composable(Routes.TRIP) {
            val vm: TripViewModel = viewModel(factory = viewModelFactory {
                initializer { TripViewModel(createSavedStateHandle()) }
            })
            val state by vm.uiState.collectAsStateWithLifecycle()
            TripScreen(state, vm::setDestination, vm::setTime, vm::setPreference,
                onStart = { vm.createRequest()?.let { request ->
                    navController.navigate(Routes.recommendation(request)) { launchSingleTop = true }
                } }, onBack = { navController.popBackStack() })
        }
        composable(Routes.DATE) {
            val vm: DateViewModel = viewModel(factory = viewModelFactory {
                initializer { DateViewModel(createSavedStateHandle()) }
            })
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
        )) { entry ->
            val args = requireNotNull(entry.arguments)
            val request = RecommendationRequest(
                TravelMode.valueOf(requireNotNull(args.getString("mode"))),
                requireNotNull(args.getString("destination")), args.getInt("minutes"),
                requireNotNull(args.getString("preference")), args.getBoolean("newPlaces"),
            )
            val vm: RecommendationViewModel = viewModel(factory = viewModelFactory {
                initializer { RecommendationViewModel(createSavedStateHandle(), request, container.getRecommendations) }
            })
            val state by vm.uiState.collectAsStateWithLifecycle()
            RecommendationScreen(state, vm::selectPlace, vm::recommendAnother, vm::skipPlace, vm::load,
                onHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onBack = { navController.popBackStack() })
        }
    }
}
