package com.github.flowtrip.presentation.trip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.flowtrip.core.util.availableTimeOptions
import com.github.flowtrip.domain.model.RecommendationRequest
import com.github.flowtrip.domain.model.TravelMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

val tripPreferences = listOf("풍경과 산책", "맛집 탐방", "문화와 명소", "추천에 맡기기")

data class TripUiState(
    val destination: String = "",
    val availableMinutes: Int = 120,
    val preference: String = tripPreferences.first(),
) {
    val canStart: Boolean get() = destination.isNotBlank()
}

@HiltViewModel
class TripViewModel @Inject constructor(private val savedState: SavedStateHandle) : ViewModel() {
    private val _uiState = MutableStateFlow(TripUiState(
        savedState["destination"] ?: "",
        savedState["minutes"] ?: 120,
        savedState["preference"] ?: tripPreferences.first(),
    ))
    val uiState = _uiState.asStateFlow()

    fun setDestination(value: String) = update(_uiState.value.copy(destination = value.take(100)))
    fun setTime(value: Int) { if (value in availableTimeOptions) update(_uiState.value.copy(availableMinutes = value)) }
    fun setPreference(value: String) { if (value in tripPreferences) update(_uiState.value.copy(preference = value)) }
    fun createRequest(): RecommendationRequest? = _uiState.value.takeIf { it.canStart }?.let {
        RecommendationRequest(TravelMode.TRIP, it.destination.trim(), it.availableMinutes, it.preference)
    }

    private fun update(state: TripUiState) {
        savedState["destination"] = state.destination
        savedState["minutes"] = state.availableMinutes
        savedState["preference"] = state.preference
        _uiState.value = state
    }
}
