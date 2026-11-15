package com.github.flowtrip.core.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.github.flowtrip.domain.model.TravelMode
import com.github.flowtrip.presentation.date.DateScreen
import com.github.flowtrip.presentation.date.DateViewModel
import com.github.flowtrip.presentation.home.HomeScreen
import com.github.flowtrip.presentation.home.HomeViewModel
import com.github.flowtrip.presentation.recommendation.RecommendationScreen
import com.github.flowtrip.presentation.recommendation.RecommendationViewModel
import com.github.flowtrip.presentation.trip.TripScreen
import com.github.flowtrip.presentation.trip.TripViewModel

private object Routes {
    const val HOME = "home"
    const val TRIP = "trip"
    const val DATE = "date"
    const val RECOMMENDATION = "recommendation/{mode}?destination={destination}&minutes={minutes}&preference={preference}&newPlaces={newPlaces}&place={place}&dayInput={dayInput}"

    fun recommendation(request: RecommendationRequest, placeId: String = "") =
        "recommendation/${request.mode.name}?destination=${Uri.encode(request.destination)}" +
            "&minutes=${request.availableMinutes}&preference=${Uri.encode(request.preference)}&newPlaces=${request.preferNewPlaces}&place=${Uri.encode(placeId)}&dayInput=${request.dayInput}"
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
            val vm: HomeViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            HomeScreen(onTrip = { navController.navigate(Routes.TRIP) { launchSingleTop = true } },
                onDate = { navController.navigate(Routes.DATE) { launchSingleTop = true } },
                state = state, onReuse = { place ->
                    val mode = TravelMode.entries.first { place.stage in it.stages }
                    val region = if (place.provider == com.github.flowtrip.domain.model.DataSource.KAKAO)
                        place.destination?.address?.split(' ')?.take(2)?.joinToString(" ").orEmpty() else "이전 선택 장소 주변"
                    val request = RecommendationRequest(mode, region, 120, "다시 이용하기")
                    navController.navigate(Routes.recommendation(request, place.id)) { launchSingleTop = true }
                })
        }
        composable(Routes.TRIP) {
            val vm: TripViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            TripScreen(state, vm::selectProvince, vm::selectDistrict, vm::setTime, vm::setTimeUnit, vm::togglePreference,
                onStart = { vm.createRequest()?.let { request ->
                    navController.navigate(Routes.recommendation(request)) { launchSingleTop = true }
                } }, onBack = { navController.popBackStack() })
        }
        composable(Routes.DATE) {
            val vm: DateViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            DateScreen(state, vm::setTime, vm::setTimeUnit, vm::toggleMood, vm::togglePlaceType, vm::setPreferNewPlaces,
                onStart = { navController.navigate(Routes.recommendation(vm.createRequest())) { launchSingleTop = true } },
                onBack = { navController.popBackStack() })
        }
        composable(Routes.RECOMMENDATION, arguments = listOf(
            navArgument("mode") { type = NavType.StringType },
            navArgument("destination") { type = NavType.StringType },
            navArgument("minutes") { type = NavType.IntType },
            navArgument("preference") { type = NavType.StringType },
            navArgument("newPlaces") { type = NavType.BoolType },
            navArgument("place") { type = NavType.StringType; defaultValue = "" },
            navArgument("dayInput") { type = NavType.BoolType; defaultValue = false },
        )) { entry ->
            val vm: RecommendationViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            val homeEntry = remember(entry) { navController.getBackStackEntry(Routes.HOME) }
            val homeVm: HomeViewModel = hiltViewModel(homeEntry)
            LaunchedEffect(state.selectedPlaces) { homeVm.recordSelections(state.selectedPlaces) }
            RecommendationScreen(state,
                onSelect = vm::selectPlace,
                onAnother = vm::recommendAnother,
                onNextCourse = vm::nextCourse, onRetry = vm::load,
                onRetryRoute = vm::retryRoute,
                onHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onBack = { navController.popBackStack() },
                onCandidate = vm::chooseCandidate, onMode = { mode ->
                    if (state.realPlaces && mode != state.request.mode)
                        navController.navigate(if (mode == TravelMode.TRIP) Routes.TRIP else Routes.DATE) { launchSingleTop = true }
                    else vm.switchMode(mode)
                }, onRestart = vm::restartCourse,
                onTestRoute = vm::requestTestRoute)
        }
    }
}
