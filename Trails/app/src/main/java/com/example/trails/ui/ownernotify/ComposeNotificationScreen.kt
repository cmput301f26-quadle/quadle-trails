// OWNER: Aarav (aarav-kapoor-cs) — Owner notifications
// USER STORIES: 02.10.01 (send notifications to waitlist entrants)
// PURPOSE: New notification: recipients, subject and message, then the "Notification sent"
//  dialog (Figma Scenario 6 · Compose, Sent).
// Outstanding issues: The "To" card has no Edit link because the whole waitlist is always notified.
// AI assistance: Used AI to help build the New notification form and sent dialog from the Figma screens.
package com.example.trails.ui.ownernotify

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.model.Lottery
import com.example.trails.data.model.SentRecipient
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.ui.theme.Ember
import com.example.trails.ui.theme.TrailsTheme

/**
 * Connects [ComposeNotificationViewModel] to navigation and renders [ComposeNotificationContent].
 *
 * @param navController used to go back and to open Sent notifications.
 * @param lotteryId the lottery whose waitlist is notified.
 * @param modifier modifier for the screen.
 * @param viewModel the screen's controller.
 */
@Composable
fun ComposeNotificationScreen(
    navController: NavController,
    lotteryId: String,
    modifier: Modifier = Modifier,
    viewModel: ComposeNotificationViewModel = viewModel(key = "notify-$lotteryId") {
        ComposeNotificationViewModel(lotteryId)
    }
) {
    val state by viewModel.uiState.collectAsState()
    val form by viewModel.form.collectAsState()
    val errors by viewModel.errors.collectAsState()
    val sendState by viewModel.sendState.collectAsState()

    ComposeNotificationContent(
        state = state,
        form = form,
        errors = errors,
        sendState = sendState,
        onBack = { navController.popBackStack() },
        onHistory = { navController.navigate(OwnerNotifyRoutes.SENT) },
        onRetry = viewModel::load,
        onSubjectChange = viewModel::onSubjectChange,
        onMessageChange = viewModel::onMessageChange,
        onSend = viewModel::onSend,
        onDone = { navController.popBackStack() },
        modifier = modifier
    )
}

/**
 * Stateless New notification, laid out as in Figma.
 *
 * @param state the lottery and recipients.
 * @param form what the owner typed.
 * @param errors problems to show under the fields.
 * @param sendState progress of sending.
 * @param onBack called from the back arrow.
 * @param onHistory opens Sent notifications.
 * @param onRetry called from "Try again" after a loading error.
 * @param onSubjectChange called as the subject is typed.
 * @param onMessageChange called as the message is typed.
 * @param onSend called from "Review and send".
 * @param onDone called from "Done" in the sent dialog.
 * @param modifier modifier for the screen.
 */
@Composable
fun ComposeNotificationContent(
    state: ComposeUiState,
    form: NotifyForm,
    errors: NotifyFormErrors,
    sendState: SendState,
    onBack: () -> Unit,
    onHistory: () -> Unit,
    onRetry: () -> Unit,
    onSubjectChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().testTag(ComposeTags.SCREEN)) {
        TrailsTopBar(
            title = stringResource(R.string.notify_title),
            onBack = onBack,
            actionLabel = stringResource(R.string.notify_history),
            onAction = onHistory,
            actionTestTag = ComposeTags.HISTORY
        )
        when (state) {
            ComposeUiState.Loading -> Centered { CircularProgressIndicator() }
            ComposeUiState.NotFound -> Centered { Text(stringResource(R.string.notify_not_found), textAlign = TextAlign.Center) }
            is ComposeUiState.Error -> Centered {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.notify_load_failed), textAlign = TextAlign.Center)
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.notify_retry)) }
                }
            }
            is ComposeUiState.Ready -> ComposeForm(state, form, errors, sendState, onSubjectChange, onMessageChange, onSend)
        }
    }

    if (sendState is SendState.Sent) {
        SentDialog(sendState, onDone = onDone, onHistory = onHistory)
    }
}

/** The "To" card, Subject, Message with its counter, and "Review and send". */
@Composable
private fun ComposeForm(
    state: ComposeUiState.Ready,
    form: NotifyForm,
    errors: NotifyFormErrors,
    sendState: SendState,
    onSubjectChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val sending = sendState == SendState.Sending
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ToCard(state)

        Text(stringResource(R.string.notify_subject), style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = form.subject,
            onValueChange = onSubjectChange,
            placeholder = { Text(stringResource(R.string.notify_subject_hint)) },
            isError = errors.subject != null,
            supportingText = errors.subject?.let { { Text(stringResource(it.message()), color = Ember) } },
            singleLine = true,
            enabled = !sending,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag(ComposeTags.SUBJECT)
        )

        Text(stringResource(R.string.notify_message), style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = form.message,
            onValueChange = onMessageChange,
            placeholder = { Text(stringResource(R.string.notify_message_hint)) },
            isError = errors.message != null,
            supportingText = errors.message?.let { { Text(stringResource(it.message()), color = Ember) } },
            minLines = 5,
            enabled = !sending,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag(ComposeTags.MESSAGE)
        )
        Row(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.notify_limit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.notify_counter, form.message.length, MAX_MESSAGE),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(ComposeTags.COUNTER)
            )
        }

        if (sendState is SendState.Failed) {
            Text(stringResource(R.string.notify_send_failed), color = Ember, modifier = Modifier.testTag(ComposeTags.ERROR))
        }
        Button(
            onClick = onSend,
            enabled = state.recipients.isNotEmpty() && !sending,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag(ComposeTags.SEND)
        ) {
            Text(stringResource(if (sending) R.string.notify_sending else R.string.notify_send))
        }
    }
}

/** "To: 3 entrants", their names, and which waitlist. */
@Composable
private fun ToCard(state: ComposeUiState.Ready) {
    val count = state.recipients.size
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp)
            .testTag(ComposeTags.TO),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(pluralStringResource(R.plurals.notify_to, count, count), style = MaterialTheme.typography.titleSmall)
        if (count == 0) {
            Text(
                stringResource(R.string.notify_no_entrants),
                style = MaterialTheme.typography.bodySmall,
                color = Ember,
                modifier = Modifier.testTag(ComposeTags.NO_ENTRANTS)
            )
        } else {
            Text(
                recipientNames(state.recipients),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            waitlistLabel(state.campsiteName, state.lottery.name),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** "Maya Singh, Daniel Kim, Lena Fischer", with "Entrant" for anyone without a name. */
@Composable
private fun recipientNames(recipients: List<SentRecipient>): String {
    val unknown = stringResource(R.string.notify_unknown_entrant)
    return recipients.joinToString(", ") { it.name.ifBlank { unknown } }
}

/** "Notification sent" with Done and a link to the history (Figma · Sent). */
@Composable
private fun SentDialog(sent: SendState.Sent, onDone: () -> Unit, onHistory: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDone,
        modifier = Modifier.testTag(ComposeTags.SENT_DIALOG),
        title = { Text(stringResource(R.string.notify_sent_title)) },
        text = { Text(pluralStringResource(R.plurals.notify_sent_body, sent.count, sent.subject, sent.count, sent.waitlist)) },
        confirmButton = {
            Button(onClick = onDone, modifier = Modifier.testTag(ComposeTags.DONE)) {
                Text(stringResource(R.string.notify_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onHistory, modifier = Modifier.testTag(ComposeTags.VIEW_SENT)) {
                Text(stringResource(R.string.notify_view_sent))
            }
        }
    )
}

/** Centres [content] under the top bar. */
@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

/** The message shown under a field for each problem. */
@StringRes
private fun NotifyFieldError.message(): Int = when (this) {
    NotifyFieldError.SUBJECT_REQUIRED -> R.string.notify_error_subject_required
    NotifyFieldError.SUBJECT_TOO_LONG -> R.string.notify_error_subject_long
    NotifyFieldError.MESSAGE_REQUIRED -> R.string.notify_error_message_required
    NotifyFieldError.MESSAGE_TOO_LONG -> R.string.notify_error_message_long
}

/** Test tags used by UI tests to find elements on this screen. */
object ComposeTags {
    const val SCREEN = "notify_compose"
    const val HISTORY = "notify_history"
    const val TO = "notify_to"
    const val NO_ENTRANTS = "notify_no_entrants"
    const val SUBJECT = "notify_subject"
    const val MESSAGE = "notify_message"
    const val COUNTER = "notify_counter"
    const val SEND = "notify_send"
    const val ERROR = "notify_error"
    const val SENT_DIALOG = "notify_sent_dialog"
    const val DONE = "notify_done"
    const val VIEW_SENT = "notify_view_sent"
}

/** Preview: the June 2027 waitlist at Grande Cache Ridge, as in Figma. */
@Preview(showBackground = true, backgroundColor = 0xFF0F1512, heightDp = 800)
@Composable
private fun ComposeNotificationPreview() {
    TrailsTheme {
        ComposeNotificationContent(
            state = ComposeUiState.Ready(
                Lottery(id = "june", name = "June 2027"),
                "Grande Cache Ridge",
                listOf(SentRecipient("a", "Maya Singh", 2), SentRecipient("b", "Daniel Kim", 4), SentRecipient("c", "Lena Fischer", 3))
            ),
            form = NotifyForm("Lottery draw date update", "Hi! The June lottery draw for Grande Cache Ridge is now on May 24, 2027."),
            errors = NotifyFormErrors(),
            sendState = SendState.Idle,
            onBack = {}, onHistory = {}, onRetry = {}, onSubjectChange = {}, onMessageChange = {}, onSend = {}, onDone = {}
        )
    }
}
