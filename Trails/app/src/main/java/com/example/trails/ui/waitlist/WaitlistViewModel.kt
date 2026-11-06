// ViewModel controller orchestrating waitlist state streams, rules acceptance, and entry operations (MVVM pattern).
// implements US 01.01.06, US 01.02.01, US 01.02.02, US 02.09.01, US 02.11.01
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
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
import com.example.trails.data.repository.UserRepository
import com.example.trails.data.repository.WaitlistFullException
import com.example.trails.data.repository.WaitlistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ui states for the waitlist screens
// ai helped scaffold this boilerplate ui state
/**
 * UI states rendered by waitlist screens.
 */
sealed interface WaitlistUiState {
    // initial loading state
    /**
     * Initial loading state while campsite and lottery metadata are being retrieved.
     */
    data object Loading : WaitlistUiState

    // entrant is not on the waitlist yet
    /**
     * State when the user is not currently enrolled in the waitlist.
     *
     * @property lottery the associated lottery details, or null if not in a lottery.
     * @property campsite the campsite information.
     * @property isFull whether the waitlist has reached its maximum capacity.
     * @property entrantCount number of entrants currently on the waitlist.
     * @property isRulesAccepted whether the user has toggled acceptance of campsite rules.
     * @property entries list of all active waitlist entries.
     */
    data class NotWaitlisted(
        val lottery: Lottery? = null,
        val campsite: Campsite? = null,
        val isFull: Boolean = false,
        val entrantCount: Int = 0,
        val isRulesAccepted: Boolean = false,
        val entries: List<WaitlistEntry> = emptyList()
    ) : WaitlistUiState

    // entrant has an active waitlist entry
    /**
     * State when the user has an active waitlist entry in the lottery.
     *
     * @property entry the user's personal [WaitlistEntry].
     * @property lottery the associated lottery details.
     * @property campsite the campsite information.
     * @property entrantCount total number of entrants enrolled.
     * @property entries list of all active waitlist entries.
     */
    data class Waitlisted(
        val entry: WaitlistEntry,
        val lottery: Lottery? = null,
        val campsite: Campsite? = null,
        val entrantCount: Int = 0,
        val entries: List<WaitlistEntry> = emptyList()
    ) : WaitlistUiState

    // transient success state after an action
    /**
     * Transient success state emitted following a user action.
     *
     * @property message description of the completed action.
     * @property entry the resulting [WaitlistEntry], if applicable.
     */
    data class Success(
        val message: String,
        val entry: WaitlistEntry? = null
    ) : WaitlistUiState

    // error state with user facing message; keeps the campsite and lottery so the screen does not go blank
    /**
     * Error state conveying a user-facing failure message while retaining screen context.
     *
     * @property message description of the error encountered.
     * @property isFull whether the error resulted from reaching waitlist capacity.
     * @property entries list of active waitlist entries.
     * @property campsite retained campsite metadata to prevent UI blanking.
     * @property lottery retained lottery metadata to prevent UI blanking.
     */
    data class Error(
        val message: String,
        val isFull: Boolean = false,
        val entries: List<WaitlistEntry> = emptyList(),
        val campsite: Campsite? = null,
        val lottery: Lottery? = null
    ) : WaitlistUiState
}

// container holding lottery and campsite details together
private data class LotteryInfo(
    val lottery: Lottery? = null,
    val campsite: Campsite? = null
)

/**
 * ViewModel controller coordinating waitlist screen UI states and actions.
 *
 * Combines lottery, campsite, and waitlist streams into a single [uiState]
 * flow and exposes methods to agree to rules, join, and leave waitlists.
 *
 * @param lotteryId optional initial lottery identifier.
 * @param userId the device-based user identifier of the current entrant.
 * @param waitlistRepository repository providing waitlist read/write operations.
 * @param lotteryRepository repository providing lottery lookup.
 * @param campsiteRepository repository providing campsite lookup.
 * @param userRepository repository providing user profile lookup.
 * @param campsiteId optional initial campsite identifier.
 */
class WaitlistViewModel(
    private val lotteryId: String = "",
    private val userId: String = CurrentUser.id(),
    private val waitlistRepository: WaitlistRepository = AppContainer.waitlistRepository,
    private val lotteryRepository: LotteryRepository = AppContainer.lotteryRepository,
    private val campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    private val userRepository: UserRepository = AppContainer.userRepository,
    private val campsiteId: String? = null
) : ViewModel() {

    private val _userRulesAccepted = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _lottery = MutableStateFlow<Lottery?>(null)
    private val _campsite = MutableStateFlow<Campsite?>(null)
    private val _activeLotteryId = MutableStateFlow(lotteryId)

    private val _uiState = MutableStateFlow<WaitlistUiState>(WaitlistUiState.Loading)

    // immutable stateflow exposed to prevent data leaks
    // based on kotlin stateflow documentation
    val uiState: StateFlow<WaitlistUiState> = _uiState.asStateFlow()

    // exposed lottery details
    val lottery: StateFlow<Lottery?> = _lottery.asStateFlow()

    // exposed campsite details
    val campsite: StateFlow<Campsite?> = _campsite.asStateFlow()

    // all entrants on the waitlist sorted by join date
    @OptIn(ExperimentalCoroutinesApi::class)
    val entrants: StateFlow<List<WaitlistEntry>> = _activeLotteryId
        .flatMapLatest { id ->
            if (id.isBlank()) flowOf(emptyList())
            else waitlistRepository.observeForLottery(id).catch { emit(emptyList()) }
        }
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
                var currentLotteryId = lotteryId
                var targetCampsiteId = campsiteId

                // resolve campsite if campsite id is provided
                if (!targetCampsiteId.isNullOrBlank()) {
                    val campsite = campsiteRepository.getById(targetCampsiteId)
                    _campsite.value = campsite
                    if (currentLotteryId.isBlank()) {
                        currentLotteryId = campsite?.activeLotteryId.orEmpty()
                    }
                }

                // load lottery details
                if (currentLotteryId.isNotBlank()) {
                    val lottery = lotteryRepository.getById(currentLotteryId)
                    _lottery.value = lottery

                    if (_campsite.value == null) {
                        val fallbackCampsiteId = lottery?.campsiteIds?.firstOrNull()
                        if (!fallbackCampsiteId.isNullOrBlank()) {
                            _campsite.value = campsiteRepository.getById(fallbackCampsiteId)
                        }
                    }
                    _activeLotteryId.value = currentLotteryId
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load lottery info."
            }
        }
    }

    // referenced stackoverflow for combining multiple stateflows
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeWaitlist() {
        val lotteryInfoFlow = combine(_lottery, _campsite) { lottery, campsite ->
            LotteryInfo(lottery, campsite)
        }

        viewModelScope.launch {
            _activeLotteryId.flatMapLatest { currentLotteryId ->
                if (currentLotteryId.isBlank()) {
                    combine(
                        flowOf<WaitlistEntry?>(null),
                        flowOf<List<WaitlistEntry>>(emptyList()),
                        lotteryInfoFlow,
                        _userRulesAccepted,
                        _errorMessage
                    ) { userEntry, allEntries, info, rulesAccepted, error ->
                        createUiState(userEntry, allEntries, info, rulesAccepted, error)
                    }
                } else {
                    combine(
                        waitlistRepository.observeEntry(currentLotteryId, userId)
                            .catch { e ->
                                _errorMessage.value = e.message ?: "Network error: Failed to load waitlist data."
                                emit(null)
                            },
                        waitlistRepository.observeForLottery(currentLotteryId)
                            .catch { e ->
                                _errorMessage.value = e.message ?: "Network error: Failed to load waitlist data."
                                emit(emptyList())
                            },
                        lotteryInfoFlow,
                        _userRulesAccepted,
                        _errorMessage
                    ) { userEntry, allEntries, info, rulesAccepted, error ->
                        createUiState(userEntry, allEntries, info, rulesAccepted, error)
                    }
                }
            }.catch { e ->
                _uiState.value = WaitlistUiState.Error(
                    message = e.message ?: "Network error: Failed to load waitlist data.",
                    campsite = _campsite.value,
                    lottery = _lottery.value
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun createUiState(
        userEntry: WaitlistEntry?,
        allEntries: List<WaitlistEntry>,
        info: LotteryInfo,
        rulesAccepted: Boolean,
        error: String?
    ): WaitlistUiState {
        val entrantCount = allEntries.size
        val isFull = info.lottery?.waitlistCapacity?.let { entrantCount >= it } ?: false

        return if (error != null) {
            WaitlistUiState.Error(
                message = error,
                isFull = isFull,
                entries = allEntries,
                campsite = info.campsite,
                lottery = info.lottery
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
    }

    // toggles the campsite rules acceptance checkbox
    /**
     * Updates the user's rules agreement checkbox selection.
     *
     * @param accepted true if the user accepts the campsite rules, false otherwise.
     */
    fun onRulesAgreementChanged(accepted: Boolean) {
        _userRulesAccepted.value = accepted
        if (accepted && _errorMessage.value?.contains("rules", ignoreCase = true) == true) {
            _errorMessage.value = null
        }
    }

    // joins the waitlist
    /**
     * Enrolls the current user onto the active lottery's waitlist.
     *
     * @param userName optional display name for the entrant.
     * @param partySize number of guests in the party.
     * @param rulesAccepted whether the entrant has agreed to campsite rules.
     */
    fun joinWaitlist(
        userName: String? = null,
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
                val targetLotteryId = _activeLotteryId.value.ifBlank {
                    _lottery.value?.id?.ifBlank { null } ?: lotteryId
                }
                // guard against joining when no active lottery exists for the campsite
                if (targetLotteryId.isBlank()) {
                    _errorMessage.value = "This campsite is not in a lottery right now."
                    return@launch
                }
                val currentLottery = _lottery.value ?: lotteryRepository.getById(targetLotteryId)
                val maxCapacity = currentLottery?.waitlistCapacity

                // resolve user display name from user repository or param
                val resolvedUserName = if (!userName.isNullOrBlank()) {
                    userName
                } else {
                    userRepository.getById(userId)?.name?.ifBlank { null } ?: "Entrant"
                }

                waitlistRepository.joinWaitlist(
                    lotteryId = targetLotteryId,
                    userId = userId,
                    userName = resolvedUserName,
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
    /**
     * Removes the current user from the active lottery's waitlist.
     */
    fun leaveWaitlist() {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                val targetLotteryId = _activeLotteryId.value.ifBlank {
                    _lottery.value?.id?.ifBlank { null } ?: lotteryId
                }
                if (targetLotteryId.isBlank()) {
                    _errorMessage.value = "This campsite is not in a lottery right now."
                    return@launch
                }
                waitlistRepository.leaveWaitlist(targetLotteryId, userId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to leave waitlist. Please check your network connection."
            }
        }
    }

    // clears current error message
    /**
     * Clears any active error message surfaced in the UI state.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    // reloads lottery details and state
    /**
     * Reloads campsite and lottery data from repositories.
     */
    fun refresh() {
        _errorMessage.value = null
        loadData()
    }
}
