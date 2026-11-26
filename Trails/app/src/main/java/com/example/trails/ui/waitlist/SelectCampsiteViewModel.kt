// ViewModel controller loading lottery and multi-campsite options for winner selection (MVVM pattern).
// implements US 01.04.02, US 02.04.02
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.ui.waitlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.LotteryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI states rendered by the campsite selector screen.
 */
sealed interface SelectCampsiteUiState {
    /**
     * Initial loading state while retrieving lottery and campsite data.
     */
    data object Loading : SelectCampsiteUiState

    /**
     * Successfully loaded lottery and associated campsites.
     *
     * @property lottery the winning lottery metadata.
     * @property campsites list of campsites included in this lottery.
     */
    data class Success(
        val lottery: Lottery,
        val campsites: List<Campsite>
    ) : SelectCampsiteUiState

    /**
     * Error state when loading lottery or campsites fails.
     *
     * @property message description of the failure.
     */
    data class Error(val message: String) : SelectCampsiteUiState
}

/**
 * ViewModel coordinating data fetching for winner multi-campsite selection.
 *
 * @param lotteryId the identifier of the lottery won.
 * @param lotteryRepository repository for lottery lookup.
 * @param campsiteRepository repository for campsite lookup.
 */
class SelectCampsiteViewModel(
    private val lotteryId: String,
    private val lotteryRepository: LotteryRepository = AppContainer.lotteryRepository,
    private val campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SelectCampsiteUiState>(SelectCampsiteUiState.Loading)
    val uiState: StateFlow<SelectCampsiteUiState> = _uiState.asStateFlow()

    init {
        loadCampsites()
    }

    /**
     * Loads the lottery document and resolves all campsite documents in [Lottery.campsiteIds].
     */
    fun loadCampsites() {
        viewModelScope.launch {
            _uiState.value = SelectCampsiteUiState.Loading
            try {
                if (lotteryId.isBlank()) {
                    _uiState.value = SelectCampsiteUiState.Error("Lottery identifier cannot be blank.")
                    return@launch
                }
                val lottery = lotteryRepository.getById(lotteryId)
                if (lottery == null) {
                    _uiState.value = SelectCampsiteUiState.Error("Lottery not found.")
                    return@launch
                }
                val sites = lottery.campsiteIds.mapNotNull { id ->
                    campsiteRepository.getById(id)
                }
                if (sites.isEmpty()) {
                    _uiState.value = SelectCampsiteUiState.Error("No campsites found for this lottery.")
                } else {
                    _uiState.value = SelectCampsiteUiState.Success(lottery, sites)
                }
            } catch (e: Exception) {
                _uiState.value = SelectCampsiteUiState.Error(e.message ?: "Failed to load campsites.")
            }
        }
    }
}
