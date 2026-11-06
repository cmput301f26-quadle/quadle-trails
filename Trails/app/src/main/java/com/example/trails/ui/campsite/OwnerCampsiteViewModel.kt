// Controller for the owner's campsite page (US 02.01.04).
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Outstanding issues: none.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.LotteryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Watches one campsite and the lotteries, and exposes the page state.
 * Because it observes, the page updates by itself after the owner edits the campsite.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param campsiteId the campsite to show.
 * @param campsiteRepository where campsites are stored.
 * @param lotteryRepository where lotteries are stored.
 */
class OwnerCampsiteViewModel(
    campsiteId: String,
    campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    lotteryRepository: LotteryRepository = AppContainer.lotteryRepository
) : ViewModel() {

    /** The page state; starts as Loading. */
    val uiState: StateFlow<OwnerCampsiteUiState> =
        combine(
            campsiteRepository.observeById(campsiteId),
            lotteryRepository.observeAll(),
            ::buildOwnerCampsiteState
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OwnerCampsiteUiState.Loading)
}
