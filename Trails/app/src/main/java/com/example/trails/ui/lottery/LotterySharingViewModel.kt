package com.example.trails.ui.lottery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.*
import com.example.trails.data.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LotterySharingState(
    val loading: Boolean = true,
    val lottery: Lottery? = null,
    val invitations: List<LotteryInvitation> = emptyList(),
    val availableSites: List<Campsite> = emptyList(),
    val ownerNames: Map<String, String> = emptyMap(),
    val ownerId: String = "",
    val now: Long = 0,
    val busy: Boolean = false,
    val error: String? = null
)

/** US 02.04.08: one controller for the creator's invitations and the recipient's inbox. */
class LotterySharingViewModel(
    private val lotteryId: String? = null,
    private val collaboration: LotteryCollaborationRepository = AppContainer.lotteryCollaborationRepository,
    lotteries: LotteryRepository = AppContainer.lotteryRepository,
    campsites: CampsiteRepository = AppContainer.campsiteRepository,
    users: UserRepository = AppContainer.userRepository,
    private val ownerId: String = CurrentUser.id(),
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val action = MutableStateFlow(false to null as String?)
    private val data = combine(
        if (lotteryId == null) collaboration.observeIncoming(ownerId) else collaboration.observeForLottery(lotteryId),
        lotteries.observeAll(), campsites.observeAll(), users.observeAll(), lotteryClock(clock)
    ) { invitations, allLotteries, allSites, allUsers, now ->
        val lottery = allLotteries.find { it.id == lotteryId }
        val visible = invitations.filter { invite -> allLotteries.any { it.id == invite.lotteryId } }
        val available = campsiteOptions(allSites, allLotteries, lotteryId, now)
            .filter { it.available && it.campsite.ownerId != ownerId && it.campsite.id !in lottery?.campsiteIds.orEmpty() }
            .map { it.campsite }
        LotterySharingState(false, lottery, visible, available,
            allUsers.associate { it.deviceId to it.name }, ownerId, now)
    }.catch { emit(LotterySharingState(loading = false, ownerId = ownerId, error = "Could not load invitations. Reopen this screen to retry.")) }

    val state: StateFlow<LotterySharingState> = combine(data, action) { state, (busy, error) ->
        state.copy(busy = busy, error = error ?: state.error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LotterySharingState())

    fun invite(ids: List<String>) = perform { collaboration.invite(requireNotNull(lotteryId), ownerId, ids) }
    fun respond(id: String, accept: Boolean) = perform { collaboration.respond(id, ownerId, accept) }

    private fun perform(block: suspend () -> Unit) {
        if (action.value.first) return
        action.value = true to null
        viewModelScope.launch {
            try { block(); action.value = false to null }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { action.value = false to (e.message ?: "Could not save. Try again.") }
        }
    }
}
