// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 02.12.01, 01.14.01
// PURPOSE: One conversation between a campsite's owner and a camper. The owner (US 02.12.01)
//  and the camper (US 01.14.01) open the same conversation and both can send.
// Outstanding issues: none.
package com.example.trails.ui.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Message
import com.example.trails.data.model.Role
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.MessageRepository
import com.example.trails.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Watches one conversation and sends messages in it.
 *
 * @param campsiteId the campsite the conversation is about.
 * @param camperId the camper in the conversation.
 * @param userId the signed-in user; must be the campsite's owner or the camper.
 * @param campsiteRepository supplies the campsite and so its owner.
 * @param messageRepository where messages are stored.
 * @param userRepository supplies the other person's name.
 */
class ChatViewModel(
    private val campsiteId: String,
    private val camperId: String,
    private val userId: String = CurrentUser.id(),
    campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    private val messageRepository: MessageRepository = AppContainer.messageRepository,
    userRepository: UserRepository = AppContainer.userRepository
) : ViewModel() {

    private val sendState = MutableStateFlow(SendState.IDLE)
    private val _draft = MutableStateFlow("")

    /** Text typed in the message field. */
    val draft: StateFlow<String> = _draft.asStateFlow()

    private val campsite = campsiteRepository.observeById(campsiteId).catch { emit(null) }

    private val names = userRepository.observeAll()
        .map { users -> users.filter { it.name.isNotBlank() }.associate { it.deviceId to it.name } }
        .catch { emit(emptyMap()) }

    private val messages = messageRepository
        .observeConversation(Message.conversationIdFor(campsiteId, camperId))
        .catch { emit(emptyList()) }

    /** The conversation, or why it can't be shown. */
    val uiState: StateFlow<ChatUiState> =
        combine(campsite, messages, names, sendState) { site, msgs, nameMap, send ->
            buildChatState(site, camperId, userId, msgs, nameMap, send)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatUiState.Loading)

    /**
     * Updates the message field.
     *
     * @param text the new text.
     */
    fun onDraftChange(text: String) {
        _draft.value = text.take(Message.MAX_LENGTH)
        if (sendState.value == SendState.FAILED) sendState.value = SendState.IDLE
    }

    /** Sends the typed message; the field clears once it is stored. Does nothing while sending. */
    fun send() {
        val ready = uiState.value as? ChatUiState.Ready ?: return
        val text = _draft.value.trim()
        if (text.isEmpty() || sendState.value == SendState.SENDING) return
        val fromOwner = userId == ready.ownerId
        val message = Message(
            conversationId = Message.conversationIdFor(campsiteId, camperId),
            campsiteId = campsiteId,
            campsiteName = ready.campsiteName,
            ownerId = ready.ownerId,
            camperId = camperId,
            senderId = userId,
            senderRole = if (fromOwner) Role.OWNER else Role.ENTRANT,
            text = text
        )
        sendState.value = SendState.SENDING
        viewModelScope.launch {
            try {
                messageRepository.send(message)
                _draft.value = ""
                sendState.value = SendState.IDLE
            } catch (e: Exception) {
                sendState.value = SendState.FAILED
            }
        }
    }
}

/** Where the last send stands. */
enum class SendState {
    /** Nothing in flight. */
    IDLE,
    /** Waiting for the message to be stored. */
    SENDING,
    /** The last send failed; the text is kept so it can be retried. */
    FAILED
}

/** What the chat screen shows. */
sealed interface ChatUiState {
    /** Still reading the campsite. */
    data object Loading : ChatUiState

    /** The campsite no longer exists. */
    data object NotFound : ChatUiState

    /** The user is neither the campsite's owner nor the camper. */
    data object NotAllowed : ChatUiState

    /**
     * The conversation.
     *
     * @property campsiteName the campsite's name.
     * @property ownerId the campsite's owner.
     * @property myId the signed-in user.
     * @property iAmOwner true when the user is the owner, false when they are the camper.
     * @property otherName the other person's name, or null if unknown.
     * @property messages the messages, oldest first.
     * @property sendState where the last send stands.
     */
    data class Ready(
        val campsiteName: String,
        val ownerId: String,
        val myId: String,
        val iAmOwner: Boolean,
        val otherName: String?,
        val messages: List<Message>,
        val sendState: SendState = SendState.IDLE
    ) : ChatUiState
}

/**
 * Decides what the chat shows. Only the campsite's owner and the camper may see it.
 *
 * @param campsite the campsite, or null if it doesn't exist.
 * @param camperId the camper in the conversation.
 * @param myId the signed-in user.
 * @param messages the conversation's messages, oldest first.
 * @param names display names by device id.
 * @param sendState where the last send stands.
 * @return the chat state.
 */
fun buildChatState(
    campsite: Campsite?,
    camperId: String,
    myId: String,
    messages: List<Message>,
    names: Map<String, String>,
    sendState: SendState
): ChatUiState {
    if (campsite == null) return ChatUiState.NotFound
    val iAmOwner = myId.isNotBlank() && myId == campsite.ownerId
    if (!iAmOwner && (myId.isBlank() || myId != camperId)) return ChatUiState.NotAllowed
    val otherId = if (iAmOwner) camperId else campsite.ownerId
    return ChatUiState.Ready(
        campsiteName = campsite.name,
        ownerId = campsite.ownerId,
        myId = myId,
        iAmOwner = iAmOwner,
        otherName = names[otherId],
        messages = messages,
        sendState = sendState
    )
}
