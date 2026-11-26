// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 02.12.01, 01.14.01
// PURPOSE: Inside of the Messages tab: the user's conversations, then the guests an owner can
//  message (US 02.12.01) or the owners a camper can message (US 01.14.01).
// Outstanding issues: none.
package com.example.trails.ui.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestMessages
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Role
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.ui.shell.MessagesContent
import com.example.trails.ui.shell.MessagesTags
import com.example.trails.ui.theme.TrailsTheme
import java.util.Date

/**
 * The Messages tab for the signed-in user. Tapping a conversation or a contact opens the chat.
 *
 * @param navController used to open a chat.
 * @param modifier applied to the screen.
 * @param viewModel loads the inbox.
 */
@Composable
fun MessagesInboxScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: MessagesInboxViewModel = viewModel { MessagesInboxViewModel() }
) {
    val state by viewModel.uiState.collectAsState()
    MessagesInboxContent(
        state = state,
        onOpenChat = { campsiteId, camperId -> navController.navigate(MessagingRoutes.chat(campsiteId, camperId)) },
        modifier = modifier
    )
}

/**
 * Stateless Messages tab. Shows the shell's empty state when there is nothing to list.
 *
 * @param state what to show.
 * @param onOpenChat called with the campsite id and camper id of the chat to open.
 * @param modifier applied to the screen.
 * @param now the current time, used to format message times.
 */
@Composable
fun MessagesInboxContent(
    state: MessagesInboxUiState,
    onOpenChat: (campsiteId: String, camperId: String) -> Unit,
    modifier: Modifier = Modifier,
    now: Date = remember { Date() }
) {
    if (state is MessagesInboxUiState.Loaded && state.inbox.isEmpty) {
        MessagesContent(modifier)
        return
    }
    Column(
        modifier
            .fillMaxSize()
            .testTag(MessagesTags.SCREEN)
    ) {
        TrailsTopBar(title = stringResource(R.string.shell_messages_title))
        when (state) {
            MessagesInboxUiState.Loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.testTag(MessagesInboxTags.LOADING))
            }
            is MessagesInboxUiState.Loaded -> InboxList(state, onOpenChat, now, Modifier.weight(1f))
        }
    }
}

@Composable
private fun InboxList(
    state: MessagesInboxUiState.Loaded,
    onOpenChat: (String, String) -> Unit,
    now: Date,
    modifier: Modifier = Modifier
) {
    val inbox = state.inbox
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (inbox.conversations.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.messaging_section_conversations), hint = null) }
            items(inbox.conversations, key = { "c-" + it.conversationId }) { summary ->
                ConversationRow(summary, now) { onOpenChat(summary.campsiteId, summary.camperId) }
            }
        }
        if (inbox.contacts.isNotEmpty()) {
            item {
                if (state.mode == Role.OWNER) {
                    SectionHeader(
                        stringResource(R.string.messaging_section_guests),
                        stringResource(R.string.messaging_section_guests_hint)
                    )
                } else {
                    SectionHeader(
                        stringResource(R.string.messaging_section_owners),
                        stringResource(R.string.messaging_section_owners_hint)
                    )
                }
            }
            items(inbox.contacts, key = { "p-" + it.conversationId }) { contact ->
                ContactRow(contact, iAmOwner = state.mode == Role.OWNER) {
                    onOpenChat(contact.campsiteId, contact.camperId)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, hint: String?) {
    Column(Modifier.padding(top = 12.dp, bottom = 2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * One conversation: the other person, the campsite and the latest message.
 *
 * @param summary the conversation.
 * @param now the current time, used to format the message time.
 * @param onClick opens the chat.
 */
@Composable
private fun ConversationRow(summary: ConversationSummary, now: Date, onClick: () -> Unit) {
    val name = summary.otherName ?: stringResource(
        if (summary.iAmOwner) R.string.messaging_guest_fallback else R.string.messaging_owner_fallback
    )
    val preview = if (summary.lastFromMe) stringResource(R.string.messaging_you, summary.lastText) else summary.lastText
    RowCard(onClick, Modifier.testTag(MessagesInboxTags.conversation(summary.conversationId))) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(summary.campsiteName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Text(
                preview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        summary.lastSentAt?.let {
            Spacer(Modifier.width(8.dp))
            Text(
                formatMessageTime(it.toDate(), now),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Someone the user can start a conversation with, and a Message button.
 *
 * @param contact the guest or owner.
 * @param iAmOwner true in owner mode, where contacts are guests.
 * @param onMessage opens the chat.
 */
@Composable
private fun ContactRow(contact: MessageContact, iAmOwner: Boolean, onMessage: () -> Unit) {
    val name = contact.otherName ?: stringResource(
        if (iAmOwner) R.string.messaging_guest_fallback else R.string.messaging_owner_fallback
    )
    val status = stringResource(
        when (contact.status) {
            GuestStatus.RESERVED -> R.string.messaging_status_reserved
            GuestStatus.WON -> R.string.messaging_status_won
            GuestStatus.WAITLISTED -> R.string.messaging_status_waitlisted
        }
    )
    RowCard(onMessage, Modifier.testTag(MessagesInboxTags.contact(contact.conversationId))) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.messaging_contact_line, contact.campsiteName, status),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = onMessage) { Text(stringResource(R.string.messaging_action_message)) }
    }
}

@Composable
private fun RowCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.primary, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** Test tags for the Messages tab; the whole screen keeps [MessagesTags.SCREEN]. */
object MessagesInboxTags {
    const val LOADING = "messages_loading"

    /** A conversation row. */
    fun conversation(conversationId: String) = "messages_conversation_$conversationId"

    /** A guest or owner row. */
    fun contact(conversationId: String) = "messages_contact_$conversationId"
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun MessagesInboxPreview() {
    val names = TestUsers.all.associate { it.deviceId to it.name }
    TrailsTheme {
        MessagesInboxContent(
            state = MessagesInboxUiState.Loaded(
                Role.OWNER,
                buildInboxState(
                    myId = TestUsers.OWNER_ID,
                    mode = Role.OWNER,
                    messages = TestMessages.all,
                    contacts = listOf(
                        MessageContact(TestCampsites.HINTON_ID, "Wildhorse Lake Site 3", TestUsers.ADMIN_ID, "Morgan Lee", GuestStatus.WAITLISTED)
                    ),
                    names = names
                )
            ),
            onOpenChat = { _, _ -> }
        )
    }
}
