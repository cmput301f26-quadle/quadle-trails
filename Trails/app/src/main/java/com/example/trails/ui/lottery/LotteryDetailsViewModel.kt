// Controller for the owner's Lottery details page (US 02.04.06).
// OWNER: Skanda (skandarajeev) — Lottery Setup
// Outstanding issues: none.
package com.example.trails.ui.lottery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.LotteryRepository
import com.example.trails.data.repository.WaitlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Watches one lottery, its campsites and its waiting list, so the page updates by itself
 * after an edit or when someone joins.
 *
 * @param lotteryId the lottery to show.
 * @param lotteryRepository where lotteries are stored.
 * @param campsiteRepository used for the campsites' names.
 * @param waitlistRepository used to count entrants.
 * @param clock current time; tests pass a fixed one.
 */
class LotteryDetailsViewModel(
    private val lotteryId: String,
    private val lotteryRepository: LotteryRepository = AppContainer.lotteryRepository,
    private val campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    private val waitlistRepository: WaitlistRepository = AppContainer.waitlistRepository,
    clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _deleteState = MutableStateFlow(DeleteUiState())

    /** Progress of Delete lottery, offered on closed lotteries nobody entered. */
    val deleteState: StateFlow<DeleteUiState> = _deleteState.asStateFlow()

    /** What the page should show. */
    val uiState: StateFlow<LotteryDetailsUiState> =
        combine(
            lotteryRepository.observeById(lotteryId),
            campsiteRepository.observeAll(),
            waitlistRepository.observeForLottery(lotteryId)
        ) { lottery, campsites, entries ->
            buildLotteryDetailsState(lottery, campsites, entries.size, clock())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LotteryDetailsUiState.Loading)

    /** Deletes the lottery if its waiting list is still empty, and frees its campsites. */
    fun onDelete() {
        if (_deleteState.value.isDeleting) return
        _deleteState.update { it.copy(isDeleting = true, failed = false) }
        viewModelScope.launch {
            try {
                val result = deleteLotteryIfEmpty(lotteryId, lotteryRepository, campsiteRepository, waitlistRepository)
                _deleteState.update { it.copy(isDeleting = false, deleted = result == DeleteResult.Deleted) }
            } catch (e: Exception) {
                _deleteState.update { it.copy(isDeleting = false, failed = true) }
            }
        }
    }

    /** Hides the "Couldn't delete" message. */
    fun onDismissDeleteError() {
        _deleteState.update { it.copy(failed = false) }
    }
}

/**
 * Progress of deleting a lottery from its details page.
 *
 * @property isDeleting true while deleting.
 * @property deleted true once it is gone; the page closes.
 * @property failed true if the last attempt failed.
 */
data class DeleteUiState(val isDeleting: Boolean = false, val deleted: Boolean = false, val failed: Boolean = false)
