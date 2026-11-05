// ViewModel for the Waitlist domain (US 01.01.06, 01.02.01, 01.02.02, 02.09.01, 02.11.01).
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
package com.example.trails.ui.waitlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.Lottery
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.repository.LotteryRepository
import com.example.trails.data.repository.RulesNotAcceptedException
import com.example.trails.data.repository.WaitlistFullException
import com.example.trails.data.repository.WaitlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sealed interface representing all possible UI states of the Waitlist screen.
 */
sealed interface WaitlistUiState {
    /** Initial loading state while waiting for lottery and entry data. */
    data object Loading : WaitlistUiState

    /** Entrant is not currently on the waitlist for this lottery. */
    data class NotWaitlisted(
        val lottery: Lottery? = null,
        val isFull: Boolean = false,
        val entrantCount: Int = 0,
        val isRulesAccepted: Boolean = false,
        val entries: List<WaitlistEntry> = emptyList()
    ) : WaitlistUiState

    /** Entrant is currently on the waitlist. */
    data class Waitlisted(
        val entry: WaitlistEntry,
        val lottery: Lottery? = null,
        val entrantCount: Int = 0,
        val entries: List<WaitlistEntry> = emptyList()
    ) : WaitlistUiState

    /** Transient success state after a waitlist action completes. */
    data class Success(
        val message: String,
        val entry: WaitlistEntry? = null
    ) : WaitlistUiState

    /** Error state displaying a user-facing failure message. */
    data class Error(
        val message: String,
        val isFull: Boolean = false,
        val entries: List<WaitlistEntry> = emptyList()
    ) : WaitlistUiState
}

/**
 * Controller for Waitlist operations: joining, leaving, rules acceptance, and observing entrants.
 *
 * @param lotteryId The id of the lottery being viewed or joined.
 * @param userId The current user's device id (defaults to CurrentUser.id()).
 * @param waitlistRepository The repository for waitlist persistence.
 * @param lotteryRepository The repository for lottery details.
 */
class WaitlistViewModel(
    private val lotteryId: String,
    private val userId: String = CurrentUser.id(),
    private val waitlistRepository: WaitlistRepository = AppContainer.waitlistRepository,
    private val lotteryRepository: LotteryRepository = AppContainer.lotteryRepository
) : ViewModel() {

    private val _userRulesAccepted = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _lottery = MutableStateFlow<Lottery?>(null)

    private val _uiState = MutableStateFlow<WaitlistUiState>(WaitlistUiState.Loading)

    /** Strictly immutable StateFlow exposed to the UI to prevent data leaks. */
    val uiState: StateFlow<WaitlistUiState> = _uiState.asStateFlow()

    /** All entrants on the waitlist, sorted by joined date. */
    val entrants: StateFlow<List<WaitlistEntry>> = waitlistRepository.observeForLottery(lotteryId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        loadLottery()
        observeWaitlist()
    }

    private fun loadLottery() {
        viewModelScope.launch {
            try {
                val lottery = lotteryRepository.getById(lotteryId)
                _lottery.value = lottery
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load lottery information."
            }
        }
    }

    private fun observeWaitlist() {
        viewModelScope.launch {
            combine(
                waitlistRepository.observeEntry(lotteryId, userId),
                waitlistRepository.observeForLottery(lotteryId),
                _lottery,
                _userRulesAccepted,
                _errorMessage
            ) { userEntry, allEntries, lottery, rulesAccepted, error ->
                val entrantCount = allEntries.size
                val isFull = lottery?.waitlistCapacity?.let { entrantCount >= it } ?: false

                if (error != null) {
                    WaitlistUiState.Error(
                        message = error,
                        isFull = isFull,
                        entries = allEntries
                    )
                } else if (userEntry != null) {
                    WaitlistUiState.Waitlisted(
                        entry = userEntry,
                        lottery = lottery,
                        entrantCount = entrantCount,
                        entries = allEntries
                    )
                } else {
                    WaitlistUiState.NotWaitlisted(
                        lottery = lottery,
                        isFull = isFull,
                        entrantCount = entrantCount,
                        isRulesAccepted = rulesAccepted,
                        entries = allEntries
                    )
                }
            }.catch { e ->
                _uiState.value = WaitlistUiState.Error(
                    message = e.message ?: "Network error. Failed to load waitlist data."
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /**
     * Toggles or sets the rules agreement checkbox (US 02.11.01).
     */
    fun onRulesAgreementChanged(accepted: Boolean) {
        _userRulesAccepted.value = accepted
        if (accepted && _errorMessage.value?.contains("rules", ignoreCase = true) == true) {
            _errorMessage.value = null
        }
    }

    /**
     * Joins the waitlist for this lottery (US 01.01.06, 01.02.01).
     */
    fun joinWaitlist(
        userName: String = "Current User",
        partySize: Int = 1,
        rulesAccepted: Boolean = _userRulesAccepted.value
    ) {
        if (!rulesAccepted) {
            _errorMessage.value = "You must review and accept the campsite rules before joining the waitlist."
            return
        }

        viewModelScope.launch {
            try {
                _errorMessage.value = null
                val lottery = _lottery.value ?: lotteryRepository.getById(lotteryId)
                val maxCapacity = lottery?.waitlistCapacity

                waitlistRepository.joinWaitlist(
                    lotteryId = lotteryId,
                    userId = userId,
                    userName = userName,
                    partySize = partySize,
                    rulesAccepted = true,
                    maxCapacity = maxCapacity
                )
            } catch (e: WaitlistFullException) {
                _errorMessage.value = e.message ?: "The waitlist for this lottery has reached maximum capacity."
            } catch (e: RulesNotAcceptedException) {
                _errorMessage.value = e.message ?: "You must review and accept the campsite rules."
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to join waitlist. Please check your network connection."
            }
        }
    }

    /**
     * Leaves the waitlist for this lottery (US 01.02.02).
     */
    fun leaveWaitlist() {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                waitlistRepository.leaveWaitlist(lotteryId, userId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to leave waitlist. Please check your network connection."
            }
        }
    }

    /**
     * Clears any active error message and returns to the current status state.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Reloads the lottery details and waitlist state.
     */
    fun refresh() {
        _errorMessage.value = null
        loadLottery()
    }
}
