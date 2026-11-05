// controller for waitlist screens and operations
// owner: chitkarsh (chitkarshrathi) - waitlist
package com.example.trails.ui.waitlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.repository.CampsiteRepository
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ui states for the waitlist screens
sealed interface WaitlistUiState {
    // initial loading state
    data object Loading : WaitlistUiState

    // entrant is not on the waitlist yet
    data class NotWaitlisted(
        val lottery: Lottery? = null,
        val campsite: Campsite? = null,
        val isFull: Boolean = false,
        val entrantCount: Int = 0,
        val isRulesAccepted: Boolean = false,
        val entries: List<WaitlistEntry> = emptyList()
    ) : WaitlistUiState

    // entrant has an active waitlist entry
    data class Waitlisted(
        val entry: WaitlistEntry,
        val lottery: Lottery? = null,
        val campsite: Campsite? = null,
        val entrantCount: Int = 0,
        val entries: List<WaitlistEntry> = emptyList()
    ) : WaitlistUiState

    // transient success state after an action
    data class Success(
        val message: String,
        val entry: WaitlistEntry? = null
    ) : WaitlistUiState

    // error state with user facing message
    data class Error(
        val message: String,
        val isFull: Boolean = false,
        val entries: List<WaitlistEntry> = emptyList()
    ) : WaitlistUiState
}

// container holding lottery and campsite details together
private data class LotteryInfo(
    val lottery: Lottery? = null,
    val campsite: Campsite? = null
)

class WaitlistViewModel(
    private val lotteryId: String,
    private val userId: String = CurrentUser.id(),
    private val waitlistRepository: WaitlistRepository = AppContainer.waitlistRepository,
    private val lotteryRepository: LotteryRepository = AppContainer.lotteryRepository,
    private val campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    private val campsiteId: String? = null
) : ViewModel() {

    private val _userRulesAccepted = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _lottery = MutableStateFlow<Lottery?>(null)
    private val _campsite = MutableStateFlow<Campsite?>(null)

    private val _uiState = MutableStateFlow<WaitlistUiState>(WaitlistUiState.Loading)

    // immutable stateflow exposed to prevent data leaks
    val uiState: StateFlow<WaitlistUiState> = _uiState.asStateFlow()

    // exposed lottery details
    val lottery: StateFlow<Lottery?> = _lottery.asStateFlow()

    // exposed campsite details
    val campsite: StateFlow<Campsite?> = _campsite.asStateFlow()

    // all entrants on the waitlist sorted by join date
    val entrants: StateFlow<List<WaitlistEntry>> = waitlistRepository.observeForLottery(lotteryId)
        .map { list -> list.sortedWith(compareBy(nullsLast()) { it.joinedAt?.toDate() }) }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        loadData()
        observeWaitlist()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val lottery = lotteryRepository.getById(lotteryId)
                _lottery.value = lottery
                val targetCampsiteId = campsiteId ?: lottery?.campsiteIds?.firstOrNull()
                if (targetCampsiteId != null) {
                    _campsite.value = campsiteRepository.getById(targetCampsiteId)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load lottery info."
            }
        }
    }

    private fun observeWaitlist() {
        val lotteryInfoFlow = combine(_lottery, _campsite) { lottery, campsite ->
            LotteryInfo(lottery, campsite)
        }

        viewModelScope.launch {
            combine(
                waitlistRepository.observeEntry(lotteryId, userId),
                waitlistRepository.observeForLottery(lotteryId),
                lotteryInfoFlow,
                _userRulesAccepted,
                _errorMessage
            ) { userEntry, allEntries, info, rulesAccepted, error ->
                val entrantCount = allEntries.size
                val isFull = info.lottery?.waitlistCapacity?.let { entrantCount >= it } ?: false

                if (error != null) {
                    WaitlistUiState.Error(
                        message = error,
                        isFull = isFull,
                        entries = allEntries
                    )
                } else if (userEntry != null) {
                    WaitlistUiState.Waitlisted(
                        entry = userEntry,
                        lottery = info.lottery,
                        campsite = info.campsite,
                        entrantCount = entrantCount,
                        entries = allEntries
                    )
                } else {
                    WaitlistUiState.NotWaitlisted(
                        lottery = info.lottery,
                        campsite = info.campsite,
                        isFull = isFull,
                        entrantCount = entrantCount,
                        isRulesAccepted = rulesAccepted,
                        entries = allEntries
                    )
                }
            }.catch { e ->
                _uiState.value = WaitlistUiState.Error(
                    message = e.message ?: "Network error: Failed to load waitlist data."
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    // toggles the campsite rules acceptance checkbox
    fun onRulesAgreementChanged(accepted: Boolean) {
        _userRulesAccepted.value = accepted
        if (accepted && _errorMessage.value?.contains("rules", ignoreCase = true) == true) {
            _errorMessage.value = null
        }
    }

    // joins the waitlist
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

    // leaves the waitlist
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

    // clears current error message
    fun clearError() {
        _errorMessage.value = null
    }

    // reloads lottery details and state
    fun refresh() {
        _errorMessage.value = null
        loadData()
    }
}
