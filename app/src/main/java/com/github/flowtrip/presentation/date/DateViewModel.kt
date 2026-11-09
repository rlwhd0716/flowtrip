package com.github.flowtrip.presentation.date

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.flowtrip.core.util.availableTimeOptions
import com.github.flowtrip.domain.model.RecommendationRequest
import com.github.flowtrip.domain.model.TravelMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

val dateMoods = listOf("새로운 장소", "조용한 장소", "활동적인 장소", "추천에 맡기기")

data class DateUiState(
    val currentLocation: String = "서울 성수동 (Mock)",
    val availableMinutes: Int = 120,
    val mood: String = dateMoods.first(),
    val preferNewPlaces: Boolean = true,
)

@HiltViewModel
class DateViewModel @Inject constructor(private val savedState: SavedStateHandle) : ViewModel() {
    private val _uiState = MutableStateFlow(DateUiState(
        availableMinutes = savedState["minutes"] ?: 120,
        mood = savedState["mood"] ?: dateMoods.first(),
        preferNewPlaces = savedState["newPlaces"] ?: true,
    ))
    val uiState = _uiState.asStateFlow()

    fun setTime(value: Int) { if (value in availableTimeOptions) update(_uiState.value.copy(availableMinutes = value)) }
    fun setMood(value: String) { if (value in dateMoods) update(_uiState.value.copy(mood = value)) }
    fun setPreferNewPlaces(value: Boolean) = update(_uiState.value.copy(preferNewPlaces = value))
    fun createRequest() = _uiState.value.let {
        RecommendationRequest(TravelMode.DATE, it.currentLocation, it.availableMinutes, it.mood, it.preferNewPlaces)
    }

    private fun update(state: DateUiState) {
        savedState["minutes"] = state.availableMinutes
        savedState["mood"] = state.mood
        savedState["newPlaces"] = state.preferNewPlaces
        _uiState.value = state
    }
}
