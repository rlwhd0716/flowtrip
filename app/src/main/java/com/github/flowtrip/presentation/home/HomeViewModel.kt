package com.github.flowtrip.presentation.home

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.flowtrip.domain.model.*
import com.github.flowtrip.domain.usecase.GetRecommendationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(val recentPlaces: List<Place> = emptyList(), val isExample: Boolean = true)

/** Stores explicit course selections, never infers a physical visit from SDK guidance. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    getRecommendations: GetRecommendationsUseCase,
    @ApplicationContext context: Context,
) : ViewModel() {
    private val preferences = context.getSharedPreferences("flowtrip_recent_selections", Context.MODE_PRIVATE)
    private val restored = savedState.get<ArrayList<Place>>("recentPlaces").orEmpty()
    private val _uiState = MutableStateFlow(HomeUiState(restored, restored.isEmpty()))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val all = TravelMode.entries.flatMap { mode ->
                    getRecommendations(RecommendationRequest(mode, "예시 지역", 120, "추천에 맡기기"))
                }
                // Saved state may already have been updated by a visible recommendation screen.
                if (!_uiState.value.isExample) return@launch
                val ids = preferences.getString("placeIds", "").orEmpty().split('|')
                val recorded = ids.mapNotNull { id -> all.find { it.id == id } }.distinctBy { it.id }.take(4)
                val examples = listOf("cafe-1", "view-2", "view-1", "trip-food-1")
                    .mapNotNull { id -> all.find { it.id == id } }
                _uiState.value = HomeUiState(recorded.ifEmpty { examples }, recorded.isEmpty())
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Empty state keeps mode selection available. */ }
        }
    }

    fun recordSelections(places: List<Place>) {
        if (places.isEmpty()) return
        val previous = if (_uiState.value.isExample) emptyList() else _uiState.value.recentPlaces
        if (places.all { place -> previous.any { it.id == place.id } }) return
        val recent = (places.asReversed() + previous).distinctBy { it.id }.take(4)
        savedState["recentPlaces"] = ArrayList(recent)
        preferences.edit().putString("placeIds", recent.joinToString("|") { it.id }).apply()
        _uiState.value = HomeUiState(recent, false)
    }
}
