// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 03.12.01
// PURPOSE: The admin's read-only log of owner-to-camper messages, opened from the Message log
//  tile on the admin home. No reply field and no delete.
// Outstanding issues: none.
package com.example.trails.ui.messaging

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.fake.TestMessages
import com.example.trails.data.fake.TestUsers
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.ui.theme.TrailsTheme
import java.util.Date

/**
 * Loads the log and lets the admin search it.
 *
 * @param navController used to go back.
 * @param modifier applied to the screen.
 * @param viewModel loads the log.
 */
@Composable
fun AdminMessageLogScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: AdminMessageLogViewModel = viewModel { AdminMessageLogViewModel() }
) {
    val state by viewModel.uiState.collectAsState()
    val query by viewModel.query.collectAsState()
    AdminMessageLogContent(
        state = state,
        query = query,
        onQueryChange = viewModel::onQueryChange,
        onBack = { navController.popBackStack() },
        modifier = modifier
    )
}

/**
 * Stateless message log, split out so previews and UI tests can drive it directly.
 *
 * @param state what to show.
 * @param query text in the search box.
 * @param onQueryChange called as the admin types in the search box.
 * @param onBack called when the back arrow is tapped.
 * @param modifier applied to the screen.
 * @param now the current time, used to format message times.
 */
@Composable
fun AdminMessageLogContent(
    state: AdminMessageLogUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    now: Date = remember { Date() }
) {
    Column(
        modifier
            .fillMaxSize()
            .testTag(AdminMessageLogTags.SCREEN)
    ) {
        TrailsTopBar(
            title = stringResource(R.string.messaging_log_title),
            subtitle = stringResource(R.string.messaging_log_subtitle),
            onBack = onBack,
            backTestTag = AdminMessageLogTags.BACK,
            backContentDescription = stringResource(R.string.messaging_back)
        )
        when (state) {
            AdminMessageLogUiState.Loading -> Centered(Modifier.weight(1f)) {
                CircularProgressIndicator(Modifier.testTag(AdminMessageLogTags.LOADING))
            }
            AdminMessageLogUiState.NotAllowed -> Centered(Modifier.weight(1f)) {
                Notice(stringResource(R.string.messaging_log_not_allowed), AdminMessageLogTags.NOT_ALLOWED)
            }
            is AdminMessageLogUiState.Loaded -> {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text(stringResource(R.string.messaging_log_search)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag(AdminMessageLogTags.SEARCH)
                )
                Text(
                    pluralStringResource(R.plurals.messaging_log_count, state.rows.size, state.rows.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag(AdminMessageLogTags.COUNT)
                )
                if (state.rows.isEmpty()) {
                    val text = if (state.totalCount == 0 || query.isBlank()) {
                        stringResource(R.string.messaging_log_empty)
                    } else {
                        stringResource(R.string.messaging_log_no_match, query.trim())
                    }
                    Centered(Modifier.weight(1f)) { Notice(text, AdminMessageLogTags.EMPTY) }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.rows, key = { it.id }) { LogRow(it, now) }
                    }
                }
            }
        }
    }
}

/**
 * One logged message: owner → camper, the campsite, the text and when it was sent.
 *
 * @param row the message.
 * @param now the current time, used to format the message time.
 */
@Composable
private fun LogRow(row: MessageLogRow, now: Date) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(AdminMessageLogTags.ROW)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(
                    R.string.messaging_log_sender_line,
                    row.ownerName ?: stringResource(R.string.messaging_owner_fallback),
                    row.camperName ?: stringResource(R.string.messaging_guest_fallback)
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            row.sentAt?.let {
                Spacer(Modifier.width(8.dp))
                Text(
                    formatMessageTime(it.toDate(), now),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(row.campsiteName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(row.text, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Tile on the admin home that opens the message log.
 *
 * @param onClick opens the log.
 * @param modifier applied to the tile.
 */
@Composable
fun MessageLogTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = modifier
            .fillMaxWidth()
            .testTag(AdminMessageLogTags.TILE)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.messaging_log_tile), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.messaging_log_tile_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

/** Test tags for the admin message log. */
object AdminMessageLogTags {
    const val SCREEN = "message_log_screen"
    const val BACK = "message_log_back"
    const val SEARCH = "message_log_search"
    const val COUNT = "message_log_count"
    const val ROW = "message_log_row"
    const val EMPTY = "message_log_empty"
    const val LOADING = "message_log_loading"
    const val NOT_ALLOWED = "message_log_not_allowed"
    const val TILE = "admin_home_message_log"
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun AdminMessageLogPreview() {
    val names = TestUsers.all.associate { it.deviceId to it.name }
    TrailsTheme {
        AdminMessageLogContent(
            state = AdminMessageLogUiState.Loaded(adminMessageLog(TestMessages.all, names), totalCount = 1),
            query = "",
            onQueryChange = {},
            onBack = {}
        )
    }
}
