package com.github.flowtrip.presentation.date

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.flowtrip.core.util.availableTimeOptions
import com.github.flowtrip.domain.model.RecommendationRequest
import com.github.flowtrip.domain.model.TravelMode
import com.github.flowtrip.domain.model.LocationStatus
import com.github.flowtrip.domain.model.DataSource
import com.github.flowtrip.domain.usecase.ObserveLocationUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

val dateMoods = listOf("새로운 장소", "조용한 장소", "활동적인 장소", "추천에 맡기기")

data class DateUiState(
    val currentLocation: String = "위치 확인 중",
    val locationSource: DataSource = DataSource.MOCK,
    val availableMinutes: Int = 120,
    val mood: String = dateMoods.first(),
    val preferNewPlaces: Boolean = true,
)

@HiltViewModel
class DateViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    observeLocation: ObserveLocationUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DateUiState(
        availableMinutes = savedState["minutes"] ?: 120,
        mood = savedState["mood"] ?: dateMoods.first(),
        preferNewPlaces = savedState["newPlaces"] ?: true,
    ))
    val uiState = _uiState.asStateFlow()
    init {
        viewModelScope.launch {
            observeLocation().collect { location ->
                val display = when {
                    location.status == LocationStatus.AVAILABLE && location.source == DataSource.MOCK -> "서울 성수동 (Mock)"
                    location.status == LocationStatus.AVAILABLE -> location.location?.coordinates?.let { "위도 ${it.latitude} · 경도 ${it.longitude}" } ?: "위치 없음"
                    location.status == LocationStatus.STALE -> "최신 위치 확인 필요"
                    location.status == LocationStatus.PERMISSION_DENIED -> "위치 권한이 필요합니다."
                    location.status == LocationStatus.UNSUPPORTED -> "차량 위치 기능을 지원하지 않습니다."
                    else -> location.message ?: "위치를 사용할 수 없습니다."
                }
                _uiState.update { it.copy(currentLocation = display, locationSource = location.source) }
            }
        }
    }

    fun setTime(value: Int) { if (value in availableTimeOptions) update(_uiState.value.copy(availableMinutes = value)) }
    fun setMood(value: String) { if (value in dateMoods) update(_uiState.value.copy(mood = value)) }
    fun setPreferNewPlaces(value: Boolean) = update(_uiState.value.copy(preferNewPlaces = value))
    fun createRequest() = _uiState.value.let {
        RecommendationRequest(TravelMode.DATE, if (it.locationSource == DataSource.MOCK) "서울 성수동 (Mock)" else "현재 위치 주변",
            it.availableMinutes, it.mood, it.preferNewPlaces)
    }

    private fun update(state: DateUiState) {
        savedState["minutes"] = state.availableMinutes
        savedState["mood"] = state.mood
        savedState["newPlaces"] = state.preferNewPlaces
        _uiState.value = state
    }
}
