// OWNER: Aarav (aarav-kapoor-cs) — Owner notifications
// USER STORIES: 02.10.02 (view a list of all notifications I have sent)
// PURPOSE: Sent notifications (All history / This week / This month) and one notification's
//  details with its recipients (Figma Scenario 6 · Sent notifications, Details).
// Outstanding issues: None currently known.
// AI assistance: Used AI to help build the sent-notification list and details screens from the Figma screens.
package com.example.trails.ui.ownernotify

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.model.SentNotification
import com.example.trails.data.model.SentRecipient
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.ui.profile.InitialsAvatar
import com.example.trails.ui.theme.TrailsTheme
import com.google.firebase.Timestamp
import java.util.GregorianCalendar

/**
 * Connects [SentNotificationsViewModel] to navigation and renders [SentNotificationsContent].
 *
 * @param navController used to go back and to open a notification's details.
 * @param modifier modifier for the screen.
 * @param viewModel the screen's controller.
 */
@Composable
fun SentNotificationsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SentNotificationsViewModel = viewModel { SentNotificationsViewModel() }
) {
    val state by viewModel.state.collectAsState()
    SentNotificationsContent(
        state = state,
        onBack = { navController.popBackStack() },
        onFilterChange = viewModel::onFilterChange,
        onOpen = { id -> navController.navigate(OwnerNotifyRoutes.sentDetails(id)) },
        modifier = modifier
    )
}

/**
 * Stateless Sent notifications list, laid out as in Figma.
 *
 * @param state what to show.
 * @param onBack called from the back arrow.
 * @param onFilterChange called when a chip is tapped.
 * @param onOpen called with a notification's id when its card is tapped.
 * @param modifier modifier for the screen.
 */
@Composable
fun SentNotificationsContent(
    state: SentListUiState,
    onBack: () -> Unit,
    onFilterChange: (SentFilter) -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().testTag(SentTags.SCREEN)) {
        TrailsTopBar(title = stringResource(R.string.sent_title), onBack = onBack)
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SentFilter.entries.forEach { chip ->
                    FilterChip(
                        selected = chip == state.filter,
                        onClick = { onFilterChange(chip) },
                        label = { Text(stringResource(chip.label())) },
                        modifier = Modifier.testTag(SentTags.chip(chip))
                    )
                }
            }
            if (!state.isLoading && state.error == null) {
                Text(
                    pluralStringResource(R.plurals.sent_count, state.items.size, state.items.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp).testTag(SentTags.COUNT)
                )
            }
        }
        when {
            state.isLoading -> Centered { CircularProgressIndicator() }
            state.error != null -> Centered { Text(stringResource(R.string.sent_load_failed), textAlign = TextAlign.Center) }
            state.items.isEmpty() -> Centered {
                Text(stringResource(R.string.sent_empty), textAlign = TextAlign.Center, modifier = Modifier.testTag(SentTags.EMPTY))
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.items, key = { it.id }) { item -> SentCard(item) { onOpen(item.id) } }
            }
        }
    }
}

/** One sent notification: bell, title, who it went to, a preview and when. */
@Composable
private fun SentCard(item: SentNotification, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag(SentTags.card(item.id))
    ) {
        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.subject, style = MaterialTheme.typography.titleSmall)
            Text(toLine(item), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                item.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            formatSentAt(item.sentAt)?.let {
                Text(
                    stringResource(R.string.sent_when, it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** "To: 3 entrants · Grande Cache Ridge". */
@Composable
private fun toLine(item: SentNotification): String {
    val to = pluralStringResource(R.plurals.notify_to, item.recipients.size, item.recipients.size)
    return listOf(to, item.campsiteName).filter { it.isNotBlank() }.joinToString(" · ")
}

/**
 * Connects [SentNotificationDetailsViewModel] to navigation and renders [SentNotificationDetailsContent].
 *
 * @param navController used to go back.
 * @param id the notification to show.
 * @param modifier modifier for the screen.
 * @param viewModel the screen's controller.
 */
@Composable
fun SentNotificationDetailsScreen(
    navController: NavController,
    id: String,
    modifier: Modifier = Modifier,
    viewModel: SentNotificationDetailsViewModel = viewModel(key = "sent-$id") { SentNotificationDetailsViewModel(id) }
) {
    val state by viewModel.state.collectAsState()
    SentNotificationDetailsContent(state, onBack = { navController.popBackStack() }, onRetry = viewModel::load, modifier = modifier)
}

/**
 * Stateless details of one sent notification, laid out as in Figma.
 *
 * @param state what to show.
 * @param onBack called from the back arrow.
 * @param onRetry called from "Try again" after a loading error.
 * @param modifier modifier for the screen.
 */
@Composable
fun SentNotificationDetailsContent(
    state: SentDetailsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().testTag(SentTags.DETAILS)) {
        TrailsTopBar(title = stringResource(R.string.sent_details_title), onBack = onBack)
        when (state) {
            SentDetailsUiState.Loading -> Centered { CircularProgressIndicator() }
            SentDetailsUiState.NotFound -> Centered { Text(stringResource(R.string.sent_not_found), textAlign = TextAlign.Center) }
            is SentDetailsUiState.Error -> Centered {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.sent_load_failed), textAlign = TextAlign.Center)
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.notify_retry)) }
                }
            }
            is SentDetailsUiState.Loaded -> DetailsBody(state.notification)
        }
    }
}

/** SENT badge and time, title, waitlist, the message, then the recipients. */
@Composable
private fun DetailsBody(item: SentNotification) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.sent_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(Modifier.weight(1f))
                formatSentAt(item.sentAt)?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(item.subject, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag(SentTags.SUBJECT))
            Text(
                waitlistLabel(item.campsiteName, item.lotteryName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(stringResource(R.string.notify_message), style = MaterialTheme.typography.titleSmall)
        Text(
            item.message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(12.dp)
                .testTag(SentTags.MESSAGE)
        )

        Text(
            stringResource(R.string.sent_recipients, item.recipients.size),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.testTag(SentTags.RECIPIENTS)
        )
        item.recipients.forEach { RecipientRow(it) }
    }
}

/** A recipient's initials, name and "Waitlist entrant · Party of 2". */
@Composable
private fun RecipientRow(recipient: SentRecipient) {
    val name = recipient.name.ifBlank { stringResource(R.string.notify_unknown_entrant) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp)
            .testTag(SentTags.recipient(recipient.userId)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InitialsAvatar(name = name, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(name, style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.sent_recipient_party, recipient.partySize),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Centres [content] in the remaining space. */
@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

/** The chip's label. */
@StringRes
private fun SentFilter.label(): Int = when (this) {
    SentFilter.ALL -> R.string.sent_filter_all
    SentFilter.WEEK -> R.string.sent_filter_week
    SentFilter.MONTH -> R.string.sent_filter_month
}

/** Test tags used by UI tests to find elements on these screens. */
object SentTags {
    const val SCREEN = "sent_list"
    const val COUNT = "sent_count"
    const val EMPTY = "sent_empty"
    const val DETAILS = "sent_details"
    const val SUBJECT = "sent_details_subject"
    const val MESSAGE = "sent_details_message"
    const val RECIPIENTS = "sent_details_recipients"

    /** Tag of a filter chip. */
    fun chip(filter: SentFilter) = "sent_chip_${filter.name.lowercase()}"

    /** Tag of a sent notification's card. */
    fun card(id: String) = "sent_card_$id"

    /** Tag of a recipient's row. */
    fun recipient(userId: String) = "sent_recipient_$userId"
}

/** Preview: the owner's history from Figma. */
@Preview(showBackground = true, backgroundColor = 0xFF0F1512, heightDp = 700)
@Composable
private fun SentNotificationsPreview() {
    val item = SentNotification(
        id = "1", subject = "Lottery draw date update", campsiteName = "Grande Cache Ridge", lotteryName = "June 2027",
        message = "The June draw is now on May 24, 2027. Your entry stays active.",
        recipients = listOf(SentRecipient("a", "Maya Singh", 2), SentRecipient("b", "Daniel Kim", 4), SentRecipient("c", "Lena Fischer", 3)),
        sentAt = Timestamp(GregorianCalendar(2027, 4, 1, 9, 24).time)
    )
    TrailsTheme {
        SentNotificationsContent(SentListUiState(isLoading = false, items = listOf(item)), onBack = {}, onFilterChange = {}, onOpen = {})
    }
}
