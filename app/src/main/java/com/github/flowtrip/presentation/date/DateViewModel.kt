package com.github.flowtrip.presentation.date

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.flowtrip.core.util.*
import com.github.flowtrip.domain.model.*
import com.github.flowtrip.domain.usecase.ObserveLocationUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.util.Locale

val dateMoods = listOf("로맨틱", "편안한 힐링", "특별한 경험", "활동적인 데이트")
val datePlaceTypes = listOf("맛집", "야경·전망", "카페·디저트", "자연·산책", "문화·예술", "쇼핑", "액티비티·체험", "드라이브 코스", "역사·전시", "기타")

data class DateUiState(
    val currentLocation: String = "위치 확인 중",
    val locationSource: DataSource = DataSource.MOCK,
    val availableMinutes: Int = 120,
    val timeUnit: AvailableTimeUnit = AvailableTimeUnit.HOURS,
    val moods: List<String> = listOf(dateMoods.first()),
    val placeTypes: List<String> = listOf("맛집"),
    val preferNewPlaces: Boolean = true,
) {
    val mood: String get() = moods.joinToString(" · ").ifBlank { "추천에 맡기기" }
}

@HiltViewModel
class DateViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    observeLocation: ObserveLocationUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DateUiState(
        availableMinutes = savedState["minutes"] ?: 120,
        timeUnit = savedState.get<String>("timeUnit")?.let { runCatching { AvailableTimeUnit.valueOf(it) }.getOrNull() } ?: AvailableTimeUnit.HOURS,
        moods = savedState.get<ArrayList<String>>("moods")?.filter { it in dateMoods }?.distinct()?.take(2) ?: listOf(dateMoods.first()),
        placeTypes = savedState.get<ArrayList<String>>("placeTypes")?.filter { it in datePlaceTypes }?.distinct() ?: listOf("맛집"),
        preferNewPlaces = savedState["newPlaces"] ?: true,
    ))
    val uiState = _uiState.asStateFlow()
    init {
        viewModelScope.launch {
            observeLocation().collect { location ->
                val display = when {
                    location.status == LocationStatus.AVAILABLE && location.source == DataSource.MOCK -> "서울 성수동 · 예시 위치"
                    location.status == LocationStatus.AVAILABLE -> location.location?.coordinates?.let {
                        String.format(Locale.KOREA, "위도 %.5f · 경도 %.5f", it.latitude, it.longitude)
                    } ?: "위치 없음"
                    location.status == LocationStatus.STALE -> "최신 위치 확인 필요"
                    location.status == LocationStatus.PERMISSION_DENIED -> "위치 권한이 필요합니다."
                    location.status == LocationStatus.UNSUPPORTED -> "차량 위치 기능을 지원하지 않습니다."
                    else -> location.message ?: "위치를 사용할 수 없습니다."
                }
                _uiState.update { it.copy(currentLocation = display, locationSource = location.source) }
            }
        }
    }
    fun setTime(value: Int) { if (validTime(value, _uiState.value.timeUnit)) update(_uiState.value.copy(availableMinutes = value)) }
    fun setTimeUnit(unit: AvailableTimeUnit) = update(_uiState.value.copy(
        availableMinutes = convertTimeUnit(_uiState.value.availableMinutes, _uiState.value.timeUnit, unit), timeUnit = unit))
    fun toggleMood(value: String) {
        val selected = _uiState.value.moods
        if (value !in dateMoods || (value !in selected && selected.size >= 2)) return
        update(_uiState.value.copy(moods = if (value in selected) selected - value else selected + value))
    }
    fun togglePlaceType(value: String) {
        if (value !in datePlaceTypes) return
        val selected = _uiState.value.placeTypes
        update(_uiState.value.copy(placeTypes = if (value in selected) selected - value else selected + value))
    }
    fun setPreferNewPlaces(value: Boolean) = update(_uiState.value.copy(preferNewPlaces = value))
    fun createRequest() = _uiState.value.let {
        RecommendationRequest(TravelMode.DATE, if (it.locationSource == DataSource.MOCK) "서울 성수동 (Mock)" else "현재 위치 주변",
            it.availableMinutes, "${it.mood} · ${it.placeTypes.joinToString(" · ").ifBlank { "장소 유형 무관" }}", it.preferNewPlaces)
    }
    private fun update(state: DateUiState) {
        savedState["minutes"] = state.availableMinutes
        savedState["timeUnit"] = state.timeUnit.name
        savedState["moods"] = ArrayList(state.moods)
        savedState["placeTypes"] = ArrayList(state.placeTypes)
        savedState["newPlaces"] = state.preferNewPlaces
        _uiState.value = state
    }
}
