package com.github.flowtrip.presentation.recommendation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.flowtrip.domain.model.*
import com.github.flowtrip.domain.repository.NavigationRepository
import com.github.flowtrip.domain.repository.NavigationTestCatalog
import com.github.flowtrip.domain.usecase.GetRecommendationsUseCase
import com.github.flowtrip.domain.usecase.RequestNavigationUseCase
import com.github.flowtrip.domain.usecase.ObserveLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecommendationUiState(
    val request: RecommendationRequest,
    val stageIndex: Int = 0,
    val place: Place? = null,
    val candidates: List<Place> = emptyList(),
    val remainingCourse: List<Place> = emptyList(),
    val candidateIndex: Int = 0,
    val selectedPlaces: List<Place> = emptyList(),
    val skippedPlaces: List<Place> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val navigation: NavigationState = NavigationState(DataSource.MOCK),
    val location: LocationState = LocationState(DataSource.MOCK),
    val isRequestingRoute: Boolean = false,
    val pendingPlaceName: String? = null,
    val navigationError: String? = null,
    val arrivalSignal: NavigationEvent.Arrived? = null,
    val lastConfirmedDestination: Destination? = null,
    val previousRouteState: RouteState? = null,
    val isCourseRoute: Boolean = false,
    val isOpeningMap: Boolean = false,
    val mapMessage: String? = null,
    val testDestinations: List<Destination> = emptyList(),
    val testRouteMessage: String? = null,
    val isTestRouteRetry: Boolean = false,
) {
    val isComplete: Boolean get() = stageIndex >= RecommendationStage.entries.size
    val navigationDiffers: Boolean get() {
        val target = place?.destination ?: return false
        val active = listOfNotNull(navigation.destination) + navigation.waypoints
        return navigation.route != RouteState.SEARCHING && active.isNotEmpty() && active.none {
            it.source == target.source &&
                kotlin.math.abs(it.coordinates.latitude - target.coordinates.latitude) < 0.0001 &&
                kotlin.math.abs(it.coordinates.longitude - target.coordinates.longitude) < 0.0001
        }
    }
}

@HiltViewModel
class RecommendationViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val getRecommendations: GetRecommendationsUseCase,
    private val navigationRepository: NavigationRepository,
    private val requestNavigation: RequestNavigationUseCase,
    observeLocation: ObserveLocationUseCase,
    testCatalog: NavigationTestCatalog,
) : ViewModel() {
    private val request = RecommendationRequest(
        mode = TravelMode.valueOf(requireNotNull(savedState.get<String>("mode"))),
        destination = requireNotNull(savedState.get<String>("destination")),
        availableMinutes = requireNotNull(savedState.get<Int>("minutes")),
        preference = requireNotNull(savedState.get<String>("preference")),
        preferNewPlaces = requireNotNull(savedState.get<Boolean>("newPlaces")),
    )
    // Restore the actual candidates; regenerating them could change the user's chosen course.
    private var places = savedState.get<ArrayList<Place>>("recommendations")?.toList().orEmpty()
    private var stageIndex: Int = (savedState["stageIndex"] ?: 0).coerceIn(0, RecommendationStage.entries.size)
    private var candidateIndex: Int = savedState["candidateIndex"] ?: 0
    private var courseRoute: Boolean = savedState["courseRoute"] ?: false
    private val selectedIds = (savedState.get<ArrayList<String>>("selectedIds") ?: arrayListOf()).toMutableList()
    private val skippedIds = (savedState.get<ArrayList<String>>("skippedIds") ?: arrayListOf()).toMutableList()
    private val _uiState = MutableStateFlow(RecommendationUiState(
        request = request, navigation = navigationRepository.state.value,
        testDestinations = testCatalog.destinations,
        testRouteMessage = savedState["testRouteMessage"],
        isTestRouteRetry = savedState.get<String>("retryTestId") != null,
        lastConfirmedDestination = savedState["confirmedDestination"],
        previousRouteState = savedState.get<String>("observedRouteState")?.let { runCatching { RouteState.valueOf(it) }.getOrNull() },
        navigationError = if (savedState.get<Boolean>("routePending") == true)
            "이전 경로 요청이 중단되었습니다. 현재 내비게이션 상태를 확인한 뒤 다시 요청해 주세요."
        else savedState["navigationError"],
    ))
    val uiState = _uiState.asStateFlow()
    private var loadStarted = false

    init {
        savedState["routePending"] = false // Never replay a destination change after process death.
        load()
        viewModelScope.launch {
            navigationRepository.state.collect { value ->
                savedState["observedRouteState"] = value.route.name
                val confirmed = value.destination.takeIf { value.route == RouteState.GUIDING || value.route == RouteState.ARRIVED }
                if (confirmed != null) savedState["confirmedDestination"] = confirmed
                _uiState.update { it.copy(navigation = value, lastConfirmedDestination = confirmed ?: it.lastConfirmedDestination) }
            }
        }
        viewModelScope.launch { observeLocation().collect { value -> _uiState.update { it.copy(location = value) } } }
        viewModelScope.launch {
            navigationRepository.events.collect { event ->
                when (event) {
                    is NavigationEvent.Arrived -> _uiState.update { it.copy(arrivalSignal = event) }
                    is NavigationEvent.DestinationChanged -> _uiState.update { it.copy(arrivalSignal = null) }
                    is NavigationEvent.Failure -> _uiState.update { it.copy(navigationError = event.message) }
                }
            }
        }
        navigationRepository.refreshDestination()
    }

    fun load() {
        if (_uiState.value.isLoading && loadStarted) return
        if (_uiState.value.isRequestingRoute) return
        loadStarted = true
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                if (places.isEmpty()) {
                    val result = getRecommendations(request)
                    places = RecommendationStage.entries.flatMap { stage -> result.filter { it.stage == stage }.take(3) }
                }
                check(RecommendationStage.entries.all { stage -> places.any { it.stage == stage } })
                saveAndPublish()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                // An incomplete response must not become a permanent cached failure on retry.
                places = emptyList()
                savedState.remove<ArrayList<Place>>("recommendations")
                _uiState.update { it.copy(isLoading = false, error = "추천 장소를 불러오지 못했어요. 다시 시도해 주세요.") }
            }
        }
    }

    // Clearly labelled route buttons are explicit destination-change approval.
    fun selectPlace() = changeRoute(stageIndex, candidateIndex, courseRoute)
    fun recommendAnother() {
        if (!canAct()) return
        changeRoute(stageIndex, (candidateIndex + 1) % candidates(stageIndex).size, courseRoute)
    }
    fun requestRemainingCourse() = changeRoute(stageIndex, candidateIndex, true)
    fun retryRoute() {
        savedState.get<String>("retryTestId")?.let {
            requestTestRoute(it, savedState["retryTestCourse"] ?: false)
            return
        }
        if (!canAct()) return
        changeRoute(savedState["retryStage"] ?: stageIndex, savedState["retryIndex"] ?: candidateIndex,
            savedState["retryCourse"] ?: courseRoute)
    }

    /** Does not change the user's recommendation candidates, stage or visit records. */
    fun requestTestRoute(id: String, viaOther: Boolean = false) {
        val state = _uiState.value
        if (state.isRequestingRoute || state.isOpeningMap) return
        val destination = state.testDestinations.find { it.id == id } ?: return
        val stops = if (viaOther) state.testDestinations.filter { it.id != id } + destination else listOf(destination)
        savedState["retryTestId"] = id
        savedState["retryTestCourse"] = viaOther
        savedState["routePending"] = true
        _uiState.update { it.copy(isRequestingRoute = true, pendingPlaceName = destination.name,
            navigationError = null, isTestRouteRetry = true, testRouteMessage = "${stops.joinToString(" → ") { stop -> stop.name }} 경로 확인 중…") }
        viewModelScope.launch {
            try {
                val result = try { requestNavigation.course(stops, userConfirmed = true) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { PlatformResult.Failure("테스트 목적지 경로 요청에 실패했습니다.") }
                when (result) {
                    PlatformResult.Success -> {
                        clearRetry()
                        val message = "${stops.joinToString(" → ") { it.name }} 안내 상태를 SDK에서 확인했습니다."
                        savedState["confirmedDestination"] = destination
                        savedState["testRouteMessage"] = message
                        _uiState.update { it.copy(testRouteMessage = message, lastConfirmedDestination = destination, arrivalSignal = null) }
                    }
                    is PlatformResult.Failure -> {
                        savedState["navigationError"] = result.message
                        savedState["testRouteMessage"] = result.message
                        _uiState.update { it.copy(navigationError = result.message, testRouteMessage = result.message) }
                        navigationRepository.refreshDestination()
                    }
                }
            } finally {
                savedState["routePending"] = false
                _uiState.update { it.copy(isRequestingRoute = false, pendingPlaceName = null) }
            }
        }
    }

    fun nextCourse() {
        if (!canAct()) return
        if (stageIndex == RecommendationStage.entries.lastIndex) {
            _uiState.value.place?.let { selectedIds.add(it.id) }
            stageIndex++
            saveAndPublish()
        } else changeRoute(stageIndex + 1, 0, courseRoute)
    }

    // Retain the existing ability to omit a stage without claiming to change navigation.
    fun skipPlace() {
        if (!canAct()) return
        _uiState.value.place?.let { skippedIds.add(it.id) }
        stageIndex++
        candidateIndex = 0
        clearRetry()
        _uiState.update { it.copy(arrivalSignal = null) }
        saveAndPublish()
    }

    private fun changeRoute(targetStage: Int, targetIndex: Int, entireCourse: Boolean) {
        if (!canAct()) return
        val place = candidates(targetStage).getOrNull(targetIndex) ?: return
        val stops = if (entireCourse) listOf(place) +
            RecommendationStage.entries.drop(targetStage + 1).mapNotNull { stage -> places.firstOrNull { it.stage == stage } }
        else listOf(place)
        val destinations = stops.mapNotNull { it.destination }
        if (destinations.size != stops.size) {
            _uiState.update { it.copy(navigationError = "이 코스에 검증된 목적지 좌표가 없는 장소가 있습니다.") }
            return
        }
        savedState["retryStage"] = targetStage
        savedState.remove<String>("retryTestId")
        savedState.remove<Boolean>("retryTestCourse")
        savedState["retryIndex"] = targetIndex
        savedState["retryCourse"] = entireCourse
        savedState["routePending"] = true
        _uiState.update { it.copy(isRequestingRoute = true, pendingPlaceName = place.name, navigationError = null,
            isTestRouteRetry = false, testRouteMessage = null) }
        viewModelScope.launch {
            try {
                val result = try { requestNavigation.course(destinations, userConfirmed = true) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { PlatformResult.Failure("경로 요청을 처리하지 못했습니다.") }
                when (result) {
                    PlatformResult.Success -> {
                        if (targetStage != stageIndex) _uiState.value.place?.let { selectedIds.add(it.id) }
                        stageIndex = targetStage
                        candidateIndex = targetIndex
                        courseRoute = entireCourse
                        clearRetry()
                        savedState["confirmedDestination"] = destinations.last()
                        _uiState.update { it.copy(lastConfirmedDestination = destinations.last(), arrivalSignal = null) }
                        saveAndPublish()
                    }
                    is PlatformResult.Failure -> {
                        savedState["navigationError"] = result.message
                        _uiState.update { it.copy(navigationError = result.message) }
                        navigationRepository.refreshDestination()
                    }
                }
            } finally {
                savedState["routePending"] = false
                _uiState.update { it.copy(isRequestingRoute = false, pendingPlaceName = null) }
            }
        }
    }

    fun openMap() {
        val state = _uiState.value
        if (state.isOpeningMap || state.isLoading || state.isRequestingRoute) return
        _uiState.update { it.copy(isOpeningMap = true, mapMessage = null) }
        viewModelScope.launch {
            try {
                val result = requestNavigation.openMap()
                _uiState.update { it.copy(mapMessage = when (result) {
                    PlatformResult.Success -> "내비게이션 화면을 요청했습니다. 두 앱 배치는 Pleos window bar에서 조절해 주세요."
                    is PlatformResult.Failure -> result.message
                }) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(mapMessage = "내비게이션 화면 요청에 실패했습니다.") } }
            finally { _uiState.update { it.copy(isOpeningMap = false) } }
        }
    }

    private fun canAct() = !_uiState.value.isLoading && _uiState.value.error == null && !_uiState.value.isComplete &&
        !_uiState.value.isRequestingRoute && !_uiState.value.isOpeningMap
    private fun candidates(stage: Int) = places.filter { it.stage == RecommendationStage.entries.getOrNull(stage) }
    private fun clearRetry() {
        savedState.remove<String>("retryTestId")
        savedState.remove<Boolean>("retryTestCourse")
        savedState.remove<Int>("retryStage")
        savedState.remove<Int>("retryIndex")
        savedState.remove<Boolean>("retryCourse")
        savedState.remove<String>("navigationError")
        _uiState.update { it.copy(navigationError = null, isTestRouteRetry = false) }
    }
    private fun saveAndPublish() {
        val candidates = candidates(stageIndex)
        candidateIndex = if (candidates.isEmpty()) 0 else candidateIndex.coerceIn(candidates.indices)
        savedState["stageIndex"] = stageIndex
        savedState["candidateIndex"] = candidateIndex
        savedState["selectedIds"] = ArrayList(selectedIds)
        savedState["skippedIds"] = ArrayList(skippedIds)
        savedState["recommendations"] = ArrayList(places)
        savedState["courseRoute"] = courseRoute
        savedState["navigationError"] = _uiState.value.navigationError
        _uiState.update { it.copy(
            stageIndex = stageIndex, place = candidates.getOrNull(candidateIndex), candidates = candidates,
            remainingCourse = if (stageIndex >= RecommendationStage.entries.size) emptyList() else
                listOfNotNull(candidates.getOrNull(candidateIndex)) + RecommendationStage.entries.drop(stageIndex + 1)
                    .mapNotNull { stage -> places.firstOrNull { it.stage == stage } },
            candidateIndex = candidateIndex, isCourseRoute = courseRoute,
            selectedPlaces = selectedIds.mapNotNull { id -> places.find { it.id == id } },
            skippedPlaces = skippedIds.mapNotNull { id -> places.find { it.id == id } },
            isLoading = false, error = null,
        ) }
    }
}
