// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 03.12.01
// PURPOSE: Feeds the admin's read-only log of every message an owner sent to a camper,
//  newest first, with a search box.
// Outstanding issues: none.
package com.example.trails.ui.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.Message
import com.example.trails.data.model.Role
import com.example.trails.data.repository.MessageRepository
import com.example.trails.data.repository.UserRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Date

/**
 * One owner-to-camper message in the admin log.
 *
 * @property id the message id.
 * @property campsiteName the campsite the conversation is about.
 * @property ownerId the owner who sent it.
 * @property ownerName the owner's name, or null if unknown.
 * @property camperId the camper it was sent to.
 * @property camperName the camper's name, or null if unknown.
 * @property text the message body.
 * @property sentAt when it was sent.
 */
data class MessageLogRow(
    val id: String,
    val campsiteName: String,
    val ownerId: String,
    val ownerName: String?,
    val camperId: String,
    val camperName: String?,
    val text: String,
    val sentAt: Timestamp?
)

/**
 * The admin log (US 03.12.01): only messages an owner sent to a camper, newest first.
 *
 * @param messages every message, in any order.
 * @param names display names by device id.
 * @param query search text; matches the message, campsite, owner or camper name, ignoring case.
 *  Blank shows everything.
 * @return the matching rows.
 */
fun adminMessageLog(messages: List<Message>, names: Map<String, String>, query: String = ""): List<MessageLogRow> {
    val needle = query.trim()
    return messages
        .filter { it.senderRole == Role.OWNER && it.senderId == it.ownerId }
        .sortedWith(compareByDescending<Message, Date?>(nullsLast()) { it.sentAt?.toDate() }.thenBy { it.id })
        .map {
            MessageLogRow(
                id = it.id,
                campsiteName = it.campsiteName,
                ownerId = it.ownerId,
                ownerName = names[it.ownerId],
                camperId = it.camperId,
                camperName = names[it.camperId],
                text = it.text,
                sentAt = it.sentAt
            )
        }
        .filter { row ->
            needle.isEmpty() || listOfNotNull(row.text, row.campsiteName, row.ownerName, row.camperName)
                .any { it.contains(needle, ignoreCase = true) }
        }
}

/** What the admin message log shows. */
sealed interface AdminMessageLogUiState {
    /** Still reading the user and the messages. */
    data object Loading : AdminMessageLogUiState

    /** The user doesn't hold the Admin role. */
    data object NotAllowed : AdminMessageLogUiState

    /**
     * The log.
     *
     * @property rows the matching owner-to-camper messages, newest first.
     * @property totalCount how many owner-to-camper messages exist before searching.
     */
    data class Loaded(val rows: List<MessageLogRow>, val totalCount: Int) : AdminMessageLogUiState
}

/**
 * Watches every message and exposes the owner-to-camper ones to admins only.
 *
 * @param userId the signed-in user; must hold [Role.ADMIN].
 * @param messageRepository where messages are stored.
 * @param userRepository supplies the user's roles and everyone's names.
 */
class AdminMessageLogViewModel(
    userId: String = CurrentUser.id(),
    messageRepository: MessageRepository = AppContainer.messageRepository,
    userRepository: UserRepository = AppContainer.userRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")

    /** Text in the search box. */
    val query: StateFlow<String> = _query

    private val isAdmin = userRepository.observe(userId)
        .map { user -> user != null && Role.ADMIN in user.roles }
        .catch { emit(false) }

    private val names = userRepository.observeAll()
        .map { users -> users.filter { it.name.isNotBlank() }.associate { it.deviceId to it.name } }
        .catch { emit(emptyMap()) }

    private val messages = messageRepository.observeAll().catch { emit(emptyList()) }

    /** The log; [AdminMessageLogUiState.Loading] until the user and messages arrive. */
    val uiState: StateFlow<AdminMessageLogUiState> =
        combine(isAdmin, messages, names, _query) { admin, msgs, nameMap, q ->
            if (!admin) AdminMessageLogUiState.NotAllowed
            else AdminMessageLogUiState.Loaded(
                rows = adminMessageLog(msgs, nameMap, q),
                totalCount = adminMessageLog(msgs, nameMap).size
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminMessageLogUiState.Loading)

    /**
     * Updates the search box.
     *
     * @param text the new search text.
     */
    fun onQueryChange(text: String) {
        _query.value = text
    }
}
