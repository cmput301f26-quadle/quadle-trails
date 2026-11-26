// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 02.12.01, 01.14.01
// PURPOSE: Feeds the Messages tab: the user's conversations plus the guests (owner mode) or
//  campsite owners (camper mode) they can start one with.
// Outstanding issues: none.
package com.example.trails.ui.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.Role
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.MessageRepository
import com.example.trails.data.repository.ReservationRepository
import com.example.trails.data.repository.UserRepository
import com.example.trails.data.repository.WaitlistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Watches everything the Messages tab lists for one user.
 *
 * @param userId the signed-in user.
 * @param messageRepository where messages are stored.
 * @param userRepository supplies the user's mode and everyone's names.
 * @param campsiteRepository supplies campsites and their active lotteries.
 * @param reservationRepository supplies reservations.
 * @param waitlistRepository supplies lottery waitlist entries.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessagesInboxViewModel(
    private val userId: String = CurrentUser.id(),
    messageRepository: MessageRepository = AppContainer.messageRepository,
    userRepository: UserRepository = AppContainer.userRepository,
    private val campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    private val reservationRepository: ReservationRepository = AppContainer.reservationRepository,
    private val waitlistRepository: WaitlistRepository = AppContainer.waitlistRepository
) : ViewModel() {

    private val names: Flow<Map<String, String>> = userRepository.observeAll()
        .map { users -> users.filter { it.name.isNotBlank() }.associate { it.deviceId to it.name } }
        .catch { emit(emptyMap()) }

    private val mode: Flow<Role> = userRepository.observe(userId)
        .map { it?.activeMode ?: Role.ENTRANT }
        .catch { emit(Role.ENTRANT) }

    private val messages = messageRepository.observeForUser(userId).catch { emit(emptyList()) }

    /** The inbox; [MessagesInboxUiState.Loading] until the mode and messages arrive. */
    val uiState: StateFlow<MessagesInboxUiState> = mode
        .flatMapLatest { m ->
            val contacts = when (m) {
                Role.OWNER -> ownerGuests()
                Role.ENTRANT -> camperContacts()
                Role.ADMIN -> flowOf(emptyList())
            }
            combine(messages, contacts, names) { msgs, contactList, nameMap ->
                MessagesInboxUiState.Loaded(m, buildInboxState(userId, m, msgs, contactList, nameMap))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MessagesInboxUiState.Loading)

    private fun ownerGuests(): Flow<List<MessageContact>> = campsiteRepository.observeByOwner(userId)
        .catch { emit(emptyList()) }
        .flatMapLatest { sites ->
            if (sites.isEmpty()) return@flatMapLatest flowOf(emptyList())
            val perSite = sites.map { site ->
                combine(
                    reservationRepository.observeForCampsite(site.id).catch { emit(emptyList()) },
                    entriesFor(site.activeLotteryId)
                ) { reservations, entries -> Triple(site.id, reservations, entries) }
            }
            combine(combine(perSite) { it.toList() }, names) { rows, nameMap ->
                ownerGuests(
                    ownerId = userId,
                    campsites = sites,
                    reservationsByCampsite = rows.associate { it.first to it.second },
                    entriesByCampsite = rows.associate { it.first to it.third },
                    names = nameMap
                )
            }
        }

    private fun camperContacts(): Flow<List<MessageContact>> = combine(
        campsiteRepository.observeAll().catch { emit(emptyList()) },
        reservationRepository.observeForUser(userId).catch { emit(emptyList<Reservation>()) }
    ) { sites, reservations -> sites to reservations }
        .flatMapLatest { (sites, reservations) ->
            val lotteries = sites.mapNotNull { it.activeLotteryId?.takeIf(String::isNotBlank) }.distinct()
            val entries: Flow<Map<String, WaitlistEntry?>> =
                if (lotteries.isEmpty()) flowOf(emptyMap())
                else combine(lotteries.map { id -> myEntry(id).map { id to it } }) { it.toMap() }
            combine(entries, names) { entryMap, nameMap ->
                camperContacts(userId, sites, reservations, entryMap, nameMap)
            }
        }

    private fun entriesFor(lotteryId: String?): Flow<List<WaitlistEntry>> =
        if (lotteryId.isNullOrBlank()) flowOf(emptyList())
        else waitlistRepository.observeForLottery(lotteryId).catch { emit(emptyList()) }

    private fun myEntry(lotteryId: String): Flow<WaitlistEntry?> =
        waitlistRepository.observeEntry(lotteryId, userId).catch { emit(null) }
}

/** What the Messages tab shows. */
sealed interface MessagesInboxUiState {
    /** Still reading the user's mode and messages. */
    data object Loading : MessagesInboxUiState

    /**
     * Ready to show.
     *
     * @property mode the user's current mode.
     * @property inbox conversations and contacts.
     */
    data class Loaded(val mode: Role, val inbox: MessagesInboxState) : MessagesInboxUiState
}
