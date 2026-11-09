package com.github.flowtrip.presentation.recommendation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.flowtrip.domain.model.Place
import com.github.flowtrip.domain.model.RecommendationRequest
import com.github.flowtrip.domain.model.RecommendationStage
import com.github.flowtrip.domain.model.TravelMode
import com.github.flowtrip.domain.usecase.GetRecommendationsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.github.flowtrip.domain.model.NavigationState
import com.github.flowtrip.domain.model.LocationState
import com.github.flowtrip.domain.model.NavigationEvent
import com.github.flowtrip.domain.model.DataSource
import com.github.flowtrip.domain.model.PlatformResult
import com.github.flowtrip.domain.repository.NavigationRepository
import com.github.flowtrip.domain.usecase.RequestNavigationUseCase
import com.github.flowtrip.domain.usecase.ObserveLocationUseCase
import kotlinx.coroutines.flow.update

data class RecommendationUiState(
    val request: RecommendationRequest,
    val stageIndex: Int = 0,
    val place: Place? = null,
    val selectedPlaces: List<Place> = emptyList(),
    val skippedPlaces: List<Place> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val navigation: NavigationState = NavigationState(DataSource.MOCK),
    val location: LocationState = LocationState(DataSource.MOCK),
    val destinationConfirmation: Place? = null,
    val isRequestingRoute: Boolean = false,
    val navigationError: String? = null,
    val arrivalSignal: NavigationEvent.Arrived? = null,
) {
    val isComplete: Boolean get() = stageIndex >= RecommendationStage.entries.size
}

@HiltViewModel
class RecommendationViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val getRecommendations: GetRecommendationsUseCase,
    private val navigationRepository: NavigationRepository,
    private val requestNavigation: RequestNavigationUseCase,
    observeLocation: ObserveLocationUseCase,
) : ViewModel() {
    private val request = RecommendationRequest(
        mode = TravelMode.valueOf(requireNotNull(savedState.get<String>("mode"))),
        destination = requireNotNull(savedState.get<String>("destination")),
        availableMinutes = requireNotNull(savedState.get<Int>("minutes")),
        preference = requireNotNull(savedState.get<String>("preference")),
        preferNewPlaces = requireNotNull(savedState.get<Boolean>("newPlaces")),
    )
    private var places: List<Place> = emptyList()
    private var stageIndex: Int = (savedState["stageIndex"] ?: 0).coerceIn(0, RecommendationStage.entries.size)
    private var candidateIndex: Int = savedState["candidateIndex"] ?: 0
    private val selectedIds = (savedState.get<ArrayList<String>>("selectedIds") ?: arrayListOf()).toMutableList()
    private val skippedIds = (savedState.get<ArrayList<String>>("skippedIds") ?: arrayListOf()).toMutableList()
    private val _uiState = MutableStateFlow(RecommendationUiState(request = request, navigation = navigationRepository.state.value))
    val uiState = _uiState.asStateFlow()
    private var loadStarted = false

    init {
        load()
        viewModelScope.launch { navigationRepository.state.collect { value -> _uiState.update { it.copy(navigation = value) } } }
        viewModelScope.launch { observeLocation().collect { value -> _uiState.update { it.copy(location = value) } } }
        viewModelScope.launch {
            navigationRepository.events.collect { event ->
                if (event is NavigationEvent.Arrived) {
                    // Notify recommendation orchestration, without marking a visit or advancing a stage.
                    _uiState.update { it.copy(arrivalSignal = event) }
                }
            }
        }
    }

    fun load() {
        if (_uiState.value.isLoading && loadStarted) return
        loadStarted = true
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                places = getRecommendations(_uiState.value.request)
                check(RecommendationStage.entries.all { stage -> places.any { it.stage == stage } })
                publish()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "추천 장소를 불러오지 못했어요. 다시 시도해 주세요.")
            }
        }
    }

    fun recommendAnother() {
        if (!canAct()) return
        candidateIndex = (candidateIndex + 1) % candidates().size
        saveAndPublish()
    }

    fun selectPlace() {
        if (!canAct()) return
        _uiState.update { it.copy(destinationConfirmation = it.place, navigationError = null) }
    }

    fun dismissDestination() {
        if (!_uiState.value.isRequestingRoute) _uiState.update { it.copy(destinationConfirmation = null) }
    }

    fun confirmDestination() {
        val place = _uiState.value.destinationConfirmation ?: return
        if (_uiState.value.isRequestingRoute) return
        val destination = place.destination
        if (destination == null) {
            _uiState.update { it.copy(destinationConfirmation = null, navigationError = "이 장소의 검증된 목적지 좌표가 없습니다.") }
            return
        }
        _uiState.update { it.copy(isRequestingRoute = true, destinationConfirmation = null, navigationError = null) }
        viewModelScope.launch {
            val result = try { requestNavigation(destination, userConfirmed = true) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { PlatformResult.Failure("경로 요청을 처리하지 못했습니다.") }
            _uiState.update { it.copy(isRequestingRoute = false) }
            when (result) {
                PlatformResult.Success -> if (_uiState.value.place?.id == place.id) advance(selected = true)
                is PlatformResult.Failure -> _uiState.update { it.copy(navigationError = result.message) }
            }
        }
    }
    fun skipPlace() = advance(selected = false)

    private fun advance(selected: Boolean) {
        if (!canAct()) return
        val place = _uiState.value.place ?: return
        if (selected) selectedIds.add(place.id) else skippedIds.add(place.id)
        stageIndex++
        candidateIndex = 0
        saveAndPublish()
    }

    private fun canAct() = !_uiState.value.isLoading && _uiState.value.error == null && !_uiState.value.isComplete &&
        !_uiState.value.isRequestingRoute && _uiState.value.destinationConfirmation == null
    private fun candidates() = places.filter { it.stage == RecommendationStage.entries.getOrNull(stageIndex) }

    private fun saveAndPublish() {
        savedState["stageIndex"] = stageIndex
        savedState["candidateIndex"] = candidateIndex
        savedState["selectedIds"] = ArrayList(selectedIds)
        savedState["skippedIds"] = ArrayList(skippedIds)
        publish()
    }

    private fun publish() {
        val candidates = candidates()
        candidateIndex = if (candidates.isEmpty()) 0 else candidateIndex.coerceIn(candidates.indices)
        _uiState.value = _uiState.value.copy(
            stageIndex = stageIndex,
            place = candidates.getOrNull(candidateIndex),
            selectedPlaces = selectedIds.mapNotNull { id -> places.find { it.id == id } },
            skippedPlaces = skippedIds.mapNotNull { id -> places.find { it.id == id } },
            isLoading = false,
            error = null,
        )
    }
}
