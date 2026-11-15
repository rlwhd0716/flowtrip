package com.github.flowtrip.presentation.trip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.flowtrip.core.util.*
import com.github.flowtrip.data.model.DemoRegionCatalog
import com.github.flowtrip.domain.model.RecommendationRequest
import com.github.flowtrip.domain.model.TravelMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

val tripPreferences = listOf("자연·경관", "관광·명소", "맛집", "카페·디저트", "역사·문화", "예술·전시", "쇼핑", "액티비티·체험")

data class TripUiState(
    val province: String = "",
    val district: String = "",
    val availableMinutes: Int = 120,
    val timeUnit: AvailableTimeUnit = AvailableTimeUnit.HOURS,
    val preferences: List<String> = listOf(tripPreferences.first()),
) {
    val destination: String get() = listOf(province, district).filter { it.isNotBlank() }.joinToString(" ")
    val preference: String get() = preferences.joinToString(" · ").ifBlank { "추천에 맡기기" }
    val canStart: Boolean get() = district in DemoRegionCatalog.districts[province].orEmpty()
}

@HiltViewModel
class TripViewModel @Inject constructor(private val savedState: SavedStateHandle) : ViewModel() {
    private val _uiState = MutableStateFlow(TripUiState(
        province = savedState.get<String>("province").orEmpty().takeIf { it in DemoRegionCatalog.districts }.orEmpty(),
        district = savedState.get<String>("district").orEmpty().takeIf {
            it in DemoRegionCatalog.districts[savedState.get<String>("province").orEmpty()].orEmpty()
        }.orEmpty(),
        availableMinutes = savedState.get<Int>("minutes") ?: 120,
        timeUnit = savedState.get<String>("timeUnit")?.let { runCatching { AvailableTimeUnit.valueOf(it) }.getOrNull() } ?: AvailableTimeUnit.HOURS,
        preferences = savedState.get<ArrayList<String>>("preferences")?.filter { it in tripPreferences }?.distinct()?.take(3)
            ?: listOf(tripPreferences.first()),
    ))
    val uiState = _uiState.asStateFlow()

    fun selectProvince(value: String) {
        if (value in DemoRegionCatalog.districts && value != _uiState.value.province)
            update(_uiState.value.copy(province = value, district = ""))
    }
    fun selectDistrict(value: String) {
        if (value in DemoRegionCatalog.districts[_uiState.value.province].orEmpty()) update(_uiState.value.copy(district = value))
    }
    fun setTime(value: Int) { if (validTime(value, _uiState.value.timeUnit)) update(_uiState.value.copy(availableMinutes = value)) }
    fun setTimeUnit(unit: AvailableTimeUnit) = update(_uiState.value.copy(
        availableMinutes = convertTimeUnit(_uiState.value.availableMinutes, _uiState.value.timeUnit, unit), timeUnit = unit))
    fun togglePreference(value: String) {
        val selected = _uiState.value.preferences
        if (value !in tripPreferences || (value !in selected && selected.size >= 3)) return
        update(_uiState.value.copy(preferences = if (value in selected) selected - value else selected + value))
    }
    fun createRequest(): RecommendationRequest? = _uiState.value.takeIf { it.canStart }?.let {
        RecommendationRequest(TravelMode.TRIP, it.destination, it.availableMinutes, it.preference, dayInput = it.timeUnit == AvailableTimeUnit.DAYS)
    }
    private fun update(state: TripUiState) {
        savedState["province"] = state.province
        savedState["district"] = state.district
        savedState["minutes"] = state.availableMinutes
        savedState["timeUnit"] = state.timeUnit.name
        savedState["preferences"] = ArrayList(state.preferences)
        _uiState.value = state
    }
}
