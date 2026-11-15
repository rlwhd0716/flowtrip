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
import com.github.flowtrip.domain.usecase.RecommendationRules
import com.github.flowtrip.domain.usecase.isFresh
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
    val realPlaces: Boolean = false,
    val remainingMinutes: Int? = null,
) {
    val isComplete: Boolean get() = stageIndex >= request.mode.stages.size
    val navigationDiffers: Boolean get() {
        val target = place?.destination ?: return false
        val active = listOfNotNull(navigation.destination) + navigation.waypoints
        return navigation.route != RouteState.SEARCHING && active.isNotEmpty() && active.none {
            (it.source == target.source || (it.source == DataSource.PLEOS && target.source == DataSource.KAKAO)) &&
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
    private var request = RecommendationRequest(
        mode = TravelMode.valueOf(savedState.get<String>("activeMode") ?: requireNotNull(savedState.get<String>("mode"))),
        destination = requireNotNull(savedState.get<String>("destination")),
        availableMinutes = requireNotNull(savedState.get<Int>("minutes")),
        preference = requireNotNull(savedState.get<String>("preference")),
        preferNewPlaces = requireNotNull(savedState.get<Boolean>("newPlaces")),
        dayInput = savedState["dayInput"] ?: false,
    )
    private val sourceChanged = (savedState.get<Boolean>("realPlaces") ?: false) != getRecommendations.usesRealPlaces
    // Reject legacy snapshots containing fictional coordinates or date-only travel courses.
    private var places = if (savedState.get<Int>("courseSchema") == 2 &&
        !sourceChanged)
        savedState.get<ArrayList<Place>>("recommendations")?.toList().orEmpty() else emptyList()
    private var stageIndex: Int = if (sourceChanged) 0 else (savedState["stageIndex"] ?: 0).coerceIn(0, request.mode.stages.size)
    private var candidateIndex: Int = if (sourceChanged) 0 else savedState["candidateIndex"] ?: 0
    private var courseRoute: Boolean = !sourceChanged && (savedState["courseRoute"] ?: false)
    private val selectedIds = (if (sourceChanged) emptyList() else savedState.get<ArrayList<String>>("selectedIds").orEmpty()).toMutableList()
    private val skippedIds = (if (sourceChanged) emptyList() else savedState.get<ArrayList<String>>("skippedIds").orEmpty()).toMutableList()
    private val _uiState = MutableStateFlow(RecommendationUiState(
        request = request, navigation = navigationRepository.state.value, realPlaces = getRecommendations.usesRealPlaces,
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
    private var startedAt: Long = (if (sourceChanged) null else savedState.get<Long>("startedAt"))
        ?: System.currentTimeMillis().also { savedState["startedAt"] = it }
    private val recommendedIds = (if (sourceChanged) emptyList() else savedState.get<ArrayList<String>>("recommendedIds").orEmpty()).toMutableSet()
    private fun remainingMinutes() = RecommendationRules.remaining(
        RecommendationRules.activityBudget(request.availableMinutes, request.dayInput), startedAt, System.currentTimeMillis())

    init {
        if (sourceChanged) {
            savedState.remove<ArrayList<Place>>("recommendations")
            TravelMode.entries.forEach { savedState.remove<CourseSnapshot>("course_" + it.name) }
            savedState["stageIndex"] = 0
            savedState["candidateIndex"] = 0
            savedState["selectedIds"] = arrayListOf<String>()
            savedState["skippedIds"] = arrayListOf<String>()
            savedState["recommendedIds"] = arrayListOf<String>()
            savedState["courseRoute"] = false
        }
        savedState["realPlaces"] = getRecommendations.usesRealPlaces
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
        if (getRecommendations.usesRealPlaces && _uiState.value.error != null && places.isNotEmpty()) {
            refreshRealCandidates(); return
        }
        loadStarted = true
        _uiState.update { it.copy(isLoading = true, error = null,
            remainingMinutes = if (getRecommendations.usesRealPlaces) remainingMinutes() else null) }
        viewModelScope.launch {
            try {
                if (places.isEmpty()) {
                    val result = getRecommendations(request.copy(startStage = stageIndex,
                        remainingMinutes = if (getRecommendations.usesRealPlaces) remainingMinutes() else null,
                        excludedIds = selectedIds.toSet() + skippedIds.toSet() + recommendedIds))
                    places = request.mode.stages.flatMap { stage -> result.filter { it.stage == stage }.take(3) }
                    // Home reuse selects a detail only. It never dispatches a route.
                    if (savedState.get<Int>("courseSchema") != 2) {
                        places.find { it.id == savedState.get<String>("place") }?.let { initial ->
                            stageIndex = request.mode.stages.indexOf(initial.stage).coerceAtLeast(0)
                            candidateIndex = candidates(stageIndex).indexOfFirst { it.id == initial.id }.coerceAtLeast(0)
                        }
                    }
                }
                if (getRecommendations.usesRealPlaces) check(isStageAvailable()) { "남은 시간과 검색 조건에 맞는 장소가 없습니다. 조건을 변경해 주세요." }
                else check(request.mode.stages.all { stage -> places.any { it.stage == stage } })
                saveAndPublish()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                // An incomplete response must not become a permanent cached failure on retry.
                places = emptyList()
                savedState.remove<ArrayList<Place>>("recommendations")
                _uiState.update { it.copy(isLoading = false, error = if (getRecommendations.usesRealPlaces)
                    error.message ?: "실제 장소 검색에 실패했습니다." else "추천 장소를 불러오지 못했어요. 다시 시도해 주세요.") }
            }
        }
    }

    // Clearly labelled route buttons are explicit destination-change approval.
    fun selectPlace() = changeRoute(stageIndex, candidateIndex, false)
    fun chooseCandidate(index: Int) {
        if (!canAct() || index !in candidates(stageIndex).indices) return
        candidateIndex = index
        clearRetry()
        _uiState.update { it.copy(testRouteMessage = null, arrivalSignal = null) }
        saveAndPublish()
    }
    fun recommendAnother() {
        if (!canAct()) return
        if (getRecommendations.usesRealPlaces) { refreshRealCandidates(excludeShown = true); return }
        chooseCandidate((candidateIndex + 1) % candidates(stageIndex).size)
    }

    private fun isStageAvailable() = stageIndex >= request.mode.stages.size || candidates(stageIndex).isNotEmpty()

    /** Refresh only the active stage. Keep visited snapshots and never dispatch guidance while browsing. */
    private fun refreshRealCandidates(excludeShown: Boolean = false) {
        if (excludeShown) recommendedIds.addAll(candidates(stageIndex).map { it.id })
        savedState["recommendedIds"] = ArrayList(recommendedIds)
        clearRetry()
        _uiState.update { it.copy(isLoading = true, error = null, arrivalSignal = null, remainingMinutes = remainingMinutes()) }
        viewModelScope.launch {
            try {
                val result = getRecommendations(request.copy(startStage = stageIndex, remainingMinutes = remainingMinutes(),
                    excludedIds = selectedIds.toSet() + skippedIds.toSet() + recommendedIds))
                val current = result.filter { it.stage == request.mode.stages.getOrNull(stageIndex) }.take(3)
                check(current.isNotEmpty()) { "남은 시간·현재 위치 조건에 맞는 새 장소가 없습니다. 조건을 변경하거나 새 코스를 시작해 주세요." }
                places = places.filter { it.stage in request.mode.stages.take(stageIndex) } + result
                candidateIndex = 0
                saveAndPublish()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { _uiState.update { it.copy(isLoading = false, error = error.message ?: "실제 장소 검색 실패") } }
        }
    }

    fun switchMode(mode: TravelMode) {
        if (mode == request.mode || _uiState.value.isLoading || _uiState.value.isRequestingRoute || _uiState.value.isOpeningMap) return
        savedState["course_" + request.mode.name] = CourseSnapshot(ArrayList(places), stageIndex, candidateIndex,
            ArrayList(selectedIds), ArrayList(skippedIds))
        val restored = savedState.get<CourseSnapshot>("course_" + mode.name)
        request = request.copy(mode = mode)
        savedState["activeMode"] = mode.name
        places = restored?.places.orEmpty()
        stageIndex = restored?.stage ?: 0
        candidateIndex = restored?.candidate ?: 0
        selectedIds.clear(); selectedIds.addAll(restored?.selected.orEmpty())
        skippedIds.clear(); skippedIds.addAll(restored?.skipped.orEmpty())
        courseRoute = false
        clearRetry()
        _uiState.update { it.copy(request = request, place = null, candidates = emptyList(),
            stageIndex = stageIndex, arrivalSignal = null, testRouteMessage = null, mapMessage = null) }
        saveAndPublish()
        load()
    }

    fun restartCourse() {
        if (_uiState.value.isLoading || _uiState.value.isRequestingRoute || _uiState.value.isOpeningMap) return
        places = emptyList(); stageIndex = 0; candidateIndex = 0
        selectedIds.clear(); skippedIds.clear(); courseRoute = false
        recommendedIds.clear(); savedState["recommendedIds"] = arrayListOf<String>()
        startedAt = System.currentTimeMillis(); savedState["startedAt"] = startedAt
        clearRetry()
        _uiState.update { it.copy(testRouteMessage = null, arrivalSignal = null) }
        saveAndPublish()
        load()
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
                        val message = if (navigationRepository.state.value.source == DataSource.PLEOS)
                            "${stops.joinToString(" → ") { it.name }} 안내 상태를 SDK에서 확인했습니다."
                        else "Mock 길안내 확인 · 실제 SDK 요청 아님"
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
        if (getRecommendations.usesRealPlaces) recommendedIds.addAll(candidates(stageIndex).map { it.id })
        _uiState.value.place?.let { selectedIds.add(it.id); getRecommendations.recordVisit(it) }
        stageIndex++
        candidateIndex = 0
        clearRetry()
        _uiState.update { it.copy(arrivalSignal = null, testRouteMessage = null) }
        saveAndPublish()
        if (getRecommendations.usesRealPlaces && !_uiState.value.isComplete) refreshRealCandidates()
    }

    // Retain the existing ability to omit a stage without claiming to change navigation.
    fun skipPlace() {
        if (!canAct()) return
        if (getRecommendations.usesRealPlaces) recommendedIds.addAll(candidates(stageIndex).map { it.id })
        _uiState.value.place?.let { skippedIds.add(it.id) }
        stageIndex++
        candidateIndex = 0
        clearRetry()
        _uiState.update { it.copy(arrivalSignal = null) }
        saveAndPublish()
        if (getRecommendations.usesRealPlaces && !_uiState.value.isComplete) refreshRealCandidates()
    }

    private fun changeRoute(targetStage: Int, targetIndex: Int, entireCourse: Boolean) {
        if (!canAct()) return
        val place = candidates(targetStage).getOrNull(targetIndex) ?: return
        val stops = if (entireCourse) listOf(place) +
            request.mode.stages.drop(targetStage + 1).mapNotNull { stage -> places.firstOrNull { it.stage == stage } }
        else listOf(place)
        val destinations = stops.mapNotNull { it.destination }
        if (destinations.size != stops.size) {
            _uiState.update { it.copy(navigationError = "이 코스에 검증된 목적지 좌표가 없는 장소가 있습니다.") }
            return
        }
        if (getRecommendations.usesRealPlaces) {
            val location = _uiState.value.location
            val fix = location.location?.takeIf { location.status == LocationStatus.AVAILABLE && it.isFresh(System.currentTimeMillis()) }
            val estimates = if (fix != null) {
                var origin = fix.coordinates
                stops.sumOf { stop ->
                    val estimate = RecommendationRules.rank(listOf(stop), origin, Int.MAX_VALUE, emptySet(), emptySet(), false).single()
                    origin = stop.destination!!.coordinates
                    estimate.travelMinutes + estimate.visitMinutes
                }
            } else stops.sumOf { it.travelMinutes + it.visitMinutes }
            if (estimates > remainingMinutes()) {
                _uiState.update { it.copy(navigationError = "추정 이동·체류 시간이 남은 시간을 초과합니다. 다음 추천으로 현재 조건을 갱신해 주세요.") }
                return
            }
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
                        courseRoute = entireCourse
                        clearRetry()
                        _uiState.update { it.copy(testRouteMessage = if (it.navigation.source == DataSource.PLEOS)
                            "${destinations.last().name} 경로 안내 상태 확인됨" else "Mock 길안내 확인 · 실제 SDK 요청 아님") }
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
    private fun candidates(stage: Int) = places.filter { it.stage == request.mode.stages.getOrNull(stage) }
    private fun clearRetry() {
        savedState.remove<String>("retryTestId")
        savedState.remove<Boolean>("retryTestCourse")
        savedState.remove<Int>("retryStage")
        savedState.remove<Int>("retryIndex")
        savedState.remove<Boolean>("retryCourse")
        savedState.remove<String>("navigationError")
        savedState.remove<String>("testRouteMessage")
        _uiState.update { it.copy(navigationError = null, isTestRouteRetry = false, testRouteMessage = null) }
    }
    private fun saveAndPublish() {
        val candidates = candidates(stageIndex)
        candidateIndex = if (candidates.isEmpty()) 0 else candidateIndex.coerceIn(candidates.indices)
        savedState["courseSchema"] = 2
        savedState["realPlaces"] = getRecommendations.usesRealPlaces
        savedState["stageIndex"] = stageIndex
        savedState["candidateIndex"] = candidateIndex
        savedState["selectedIds"] = ArrayList(selectedIds)
        savedState["recommendedIds"] = ArrayList(recommendedIds)
        savedState["skippedIds"] = ArrayList(skippedIds)
        savedState["recommendations"] = ArrayList(places)
        savedState["courseRoute"] = courseRoute
        savedState["navigationError"] = _uiState.value.navigationError
        _uiState.update { it.copy(
            realPlaces = getRecommendations.usesRealPlaces,
            remainingMinutes = if (getRecommendations.usesRealPlaces) remainingMinutes() else null,
            stageIndex = stageIndex, place = candidates.getOrNull(candidateIndex), candidates = candidates,
            remainingCourse = if (stageIndex >= request.mode.stages.size) emptyList() else
                listOfNotNull(candidates.getOrNull(candidateIndex)) + request.mode.stages.drop(stageIndex + 1)
                    .mapNotNull { stage -> places.firstOrNull { it.stage == stage } },
            candidateIndex = candidateIndex, isCourseRoute = courseRoute,
            selectedPlaces = selectedIds.mapNotNull { id -> places.find { it.id == id } },
            skippedPlaces = skippedIds.mapNotNull { id -> places.find { it.id == id } },
            isLoading = false, error = null,
        ) }
    }
}

/** Per-mode state; restoration never replays a navigation request. */
private data class CourseSnapshot(
    val places: ArrayList<Place>, val stage: Int, val candidate: Int,
    val selected: ArrayList<String>, val skipped: ArrayList<String>,
) : java.io.Serializable
