// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 02.12.01, 01.14.01
// PURPOSE: The conversation between a campsite's owner and a camper. Both sides open this same
//  screen; your messages are green on the right, theirs are on the left.
// Outstanding issues: none.
package com.example.trails.ui.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.fake.TestMessages
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Message
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.ui.theme.Ember
import com.example.trails.ui.theme.Night
import com.example.trails.ui.theme.TrailGreen
import com.example.trails.ui.theme.TrailsTheme
import java.util.Date

/**
 * Loads one conversation and lets the user reply.
 *
 * @param navController used to go back.
 * @param campsiteId the campsite the conversation is about.
 * @param camperId the camper in the conversation.
 * @param modifier applied to the screen.
 * @param viewModel loads the conversation and sends messages.
 */
@Composable
fun ChatScreen(
    navController: NavController,
    campsiteId: String,
    camperId: String,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel { ChatViewModel(campsiteId, camperId) }
) {
    val state by viewModel.uiState.collectAsState()
    val draft by viewModel.draft.collectAsState()
    ChatContent(
        state = state,
        draft = draft,
        onDraftChange = viewModel::onDraftChange,
        onSend = viewModel::send,
        onBack = { navController.popBackStack() },
        modifier = modifier
    )
}

/**
 * Stateless chat UI, split out so previews and UI tests can drive it directly.
 *
 * @param state what to show.
 * @param draft text in the message field.
 * @param onDraftChange called as the user types.
 * @param onSend called when Send is tapped.
 * @param onBack called when the back arrow is tapped.
 * @param modifier applied to the screen.
 * @param now the current time, used to format message times.
 */
@Composable
fun ChatContent(
    state: ChatUiState,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    now: Date = remember { Date() }
) {
    val ready = state as? ChatUiState.Ready
    val title = when {
        ready == null -> ""
        ready.otherName != null -> ready.otherName
        ready.iAmOwner -> stringResource(R.string.messaging_guest_fallback)
        else -> stringResource(R.string.messaging_owner_fallback)
    }
    Column(
        modifier
            .fillMaxSize()
            .imePadding()
            .testTag(ChatTags.SCREEN)
    ) {
        TrailsTopBar(
            title = title,
            subtitle = ready?.campsiteName,
            onBack = onBack,
            backTestTag = ChatTags.BACK,
            backContentDescription = stringResource(R.string.messaging_back)
        )
        when (state) {
            ChatUiState.Loading -> Centered(Modifier.weight(1f)) { CircularProgressIndicator() }
            ChatUiState.NotFound -> Centered(Modifier.weight(1f)) {
                Notice(stringResource(R.string.messaging_not_found), ChatTags.NOT_FOUND)
            }
            ChatUiState.NotAllowed -> Centered(Modifier.weight(1f)) {
                Notice(stringResource(R.string.messaging_not_allowed), ChatTags.NOT_ALLOWED)
            }
            is ChatUiState.Ready -> {
                MessageList(state, now, Modifier.weight(1f))
                if (state.sendState == SendState.FAILED) {
                    Text(
                        stringResource(R.string.messaging_send_failed),
                        color = Ember,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .testTag(ChatTags.SEND_ERROR)
                    )
                }
                Composer(
                    draft = draft,
                    canSend = draft.isNotBlank() && state.sendState != SendState.SENDING,
                    onDraftChange = onDraftChange,
                    onSend = onSend
                )
            }
        }
    }
}

/**
 * The messages, oldest at the top; scrolls to the newest when one arrives.
 *
 * @param state the conversation.
 * @param now the current time, used to format message times.
 * @param modifier applied to the list.
 */
@Composable
private fun MessageList(state: ChatUiState.Ready, now: Date, modifier: Modifier = Modifier) {
    if (state.messages.isEmpty()) {
        Centered(modifier) {
            Notice(
                stringResource(if (state.iAmOwner) R.string.messaging_chat_empty_owner else R.string.messaging_chat_empty_camper),
                ChatTags.EMPTY
            )
        }
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) { listState.scrollToItem(state.messages.lastIndex) }
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .testTag(ChatTags.LIST),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(state.messages, key = { it.id }) { message ->
            Bubble(message, mine = message.senderId == state.myId, now = now)
        }
    }
}

/**
 * One message.
 *
 * @param message the message.
 * @param mine true when the user wrote it.
 * @param now the current time, used to format the message time.
 */
@Composable
private fun Bubble(message: Message, mine: Boolean, now: Date) {
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (mine) 16.dp else 4.dp,
        bottomEnd = if (mine) 4.dp else 16.dp
    )
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(if (mine) TrailGreen else MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag(if (mine) ChatTags.MINE else ChatTags.THEIRS),
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start
        ) {
            Text(
                message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (mine) Night else MaterialTheme.colorScheme.onSurface
            )
            message.sentAt?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    formatMessageTime(it.toDate(), now),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (mine) Night.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Message field and Send button.
 *
 * @param draft text in the field.
 * @param canSend whether Send is enabled.
 * @param onDraftChange called as the user types.
 * @param onSend called when Send is tapped.
 */
@Composable
private fun Composer(draft: String, canSend: Boolean, onDraftChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            placeholder = { Text(stringResource(R.string.messaging_input_hint)) },
            maxLines = 4,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .testTag(ChatTags.INPUT)
        )
        IconButton(onClick = onSend, enabled = canSend, modifier = Modifier.testTag(ChatTags.SEND)) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.messaging_send),
                tint = if (canSend) TrailGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Centered(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun Notice(text: String, tag: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag(tag)
    )
}

/** Test tags for the chat screen. */
object ChatTags {
    const val SCREEN = "chat_screen"
    const val BACK = "chat_back"
    const val LIST = "chat_list"
    const val MINE = "chat_message_mine"
    const val THEIRS = "chat_message_theirs"
    const val EMPTY = "chat_empty"
    const val INPUT = "chat_input"
    const val SEND = "chat_send"
    const val SEND_ERROR = "chat_send_error"
    const val NOT_FOUND = "chat_not_found"
    const val NOT_ALLOWED = "chat_not_allowed"
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun ChatPreview() {
    TrailsTheme {
        ChatContent(
            state = ChatUiState.Ready(
                campsiteName = "Tunnel Mountain Site 14",
                ownerId = TestUsers.OWNER_ID,
                myId = TestUsers.OWNER_ID,
                iAmOwner = true,
                otherName = "Priya Sharma",
                messages = TestMessages.all
            ),
            draft = "",
            onDraftChange = {},
            onSend = {},
            onBack = {}
        )
    }
}
