// OWNER: Skanda (skandarajeev) — Lottery Setup
// USER STORIES: supports 02.04.06
// PURPOSE: The owner's view of one lottery: status, what happens next, timeline and settings,
//  with Edit until the draw (Figma F19 Scheduled, F22 Changes saved, F23 Registration open,
//  F24 Winners drawn, F25 No entrants).
// Outstanding issues: the winners list on F24 belongs to US 02.05.01 (auto-draw), so the Draw
//  complete state shows the summary only.
package com.example.trails.ui.lottery

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestLotteries
import com.example.trails.ui.theme.TrailsTheme
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Connects [LotteryDetailsViewModel] to navigation and renders [LotteryDetailsContent].
 *
 * @param savedState this page's back stack state; the edit screen leaves its "Changes saved" text here.
 */
@Composable
fun LotteryDetailsScreen(
    navController: NavController,
    lotteryId: String,
    modifier: Modifier = Modifier,
    savedState: SavedStateHandle? = null,
    viewModel: LotteryDetailsViewModel = viewModel(key = "lottery_details_$lotteryId") { LotteryDetailsViewModel(lotteryId) }
) {
    val state by viewModel.uiState.collectAsState()
    val savedFlow = remember(savedState) { savedState?.getStateFlow<String?>(LOTTERY_SAVED_MESSAGE, null) ?: MutableStateFlow(null) }
    val savedMessage by savedFlow.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    LaunchedEffect(deleteState.deleted) {
        if (deleteState.deleted) navController.popBackStack()
    }
    LotteryDetailsContent(
        state = state,
        savedMessage = savedMessage,
        onBack = { navController.popBackStack() },
        onEdit = { navController.navigate(LotteryRoutes.lotteryForm(lotteryId = lotteryId)) },
        onReuse = { navController.navigate(LotteryRoutes.lotteryForm(reuseId = lotteryId)) },
        modifier = modifier,
        deleteState = deleteState,
        onDelete = viewModel::onDelete,
        onDismissDeleteError = viewModel::onDismissDeleteError
    )
}

/**
 * Stateless details page, split out so previews and UI tests can drive it directly.
 *
 * @param state what to show.
 * @param savedMessage "Changes saved…" banner text after an edit, or null.
 * @param onBack called when the back arrow is tapped.
 * @param onEdit called when Edit is tapped (only shown before the draw).
 * @param onReuse called when "Reuse settings in a new lottery" is tapped (no-entrants state).
 * @param deleteState progress of Delete lottery, offered when nobody entered a closed lottery.
 * @param onDelete called after the owner confirms Delete lottery.
 * @param onDismissDeleteError called when the "Couldn't delete" message is closed.
 */
@Composable
fun LotteryDetailsContent(
    state: LotteryDetailsUiState,
    savedMessage: String?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onReuse: () -> Unit,
    modifier: Modifier = Modifier,
    deleteState: DeleteUiState = DeleteUiState(),
    onDelete: () -> Unit = {},
    onDismissDeleteError: () -> Unit = {}
) {
    val loaded = state as? LotteryDetailsUiState.Loaded
    Scaffold(
        modifier = modifier.background(lotteryBackground()),
        containerColor = Color.Transparent,
        topBar = {
            LotteryHeader(
                title = stringResource(R.string.lottery_details_title),
                onBack = onBack,
                actionLabel = if (loaded?.canEdit == true) stringResource(R.string.lottery_edit) else null,
                onAction = onEdit
            )
        },
        bottomBar = {
            if (loaded?.status == LotteryStatus.NO_ENTRANTS) {
                LotteryFooter { LotteryButton(stringResource(R.string.lottery_reuse), onClick = onReuse, modifier = Modifier.testTag(LotteryTags.PRIMARY_BUTTON)) }
            }
        }
    ) { padding ->
        val inner = Modifier
            .padding(padding)
            .fillMaxSize()
        when (state) {
            LotteryDetailsUiState.Loading -> Box(inner, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.testTag(LotteryTags.LOADING))
            }
            LotteryDetailsUiState.NotFound -> Box(inner.padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.lottery_not_found), style = LotteryType.Body, color = LotteryColors.Muted, textAlign = TextAlign.Center)
            }
            is LotteryDetailsUiState.Loaded -> DetailsBody(state, savedMessage, deleteState.isDeleting, onDelete, inner)
        }
    }
    if (deleteState.failed) DeleteFailedDialog(onRetry = onDelete, onDismiss = onDismissDeleteError)
}

@Composable
private fun DetailsBody(
    state: LotteryDetailsUiState.Loaded,
    savedMessage: String?,
    isDeleting: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier
) {
    val lottery = state.lottery
    StepBody(modifier) {
        if (savedMessage != null) SavedBanner(savedMessage)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(lottery.name, style = LotteryType.Title)
            StatusBadge(state.status)
        }
        when (state.status) {
            LotteryStatus.SCHEDULED -> ScheduledCard(state)
            LotteryStatus.OPEN -> OpenCards(state)
            LotteryStatus.DRAWN -> DrawnCard(state)
            LotteryStatus.NO_ENTRANTS -> {
                NoEntrantsCards(state)
                // Closed lotteries hide Edit, so one nobody entered can be deleted from here.
                DeleteLotterySection(state.entrantCount, isDeleting, onConfirm = onDelete)
            }
        }
        if (state.status != LotteryStatus.NO_ENTRANTS) {
            DetailsTimeline(state)
            SettingsCard(state)
        }
    }
}

/** Green "✓ Changes saved. Winners updated to 6." (Figma F22). */
@Composable
private fun SavedBanner(message: String) {
    val shape = RoundedCornerShape(8.dp)
    val green = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, green, shape)
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .testTag(LotteryTags.SAVED_BANNER),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(painterResource(R.drawable.lottery_ic_check), contentDescription = null, tint = green, modifier = Modifier.size(14.dp))
        Text(message, style = LotteryType.Action, color = green)
    }
}

/** Card with a bold line and a grey line, e.g. "Registration opens in 12 days". */
@Composable
private fun NextStepCard(title: String, body: String, highlighted: Boolean = true) {
    LotteryCard(highlighted = highlighted, spacing = 4.dp) {
        Text(title, style = LotteryType.CardTitle.copy(fontSize = 16.sp))
        Text(body, style = LotteryType.Small, color = LotteryColors.Muted)
    }
}

/** "in 12 days", "tomorrow" or "today" from three strings. */
@Composable
private fun countdown(days: Long, @PluralsRes inDays: Int, @StringRes today: Int, @StringRes tomorrow: Int): String = when {
    days <= 0 -> stringResource(today)
    days == 1L -> stringResource(tomorrow)
    else -> pluralStringResource(inDays, days.toInt(), days.toInt())
}

@Composable
private fun ScheduledCard(state: LotteryDetailsUiState.Loaded) {
    val opens = state.lottery.registrationStart.millis() ?: return
    NextStepCard(
        countdown(calendarDaysUntil(opens, state.now), R.plurals.lottery_opens_in, R.string.lottery_opens_today, R.string.lottery_opens_tomorrow),
        stringResource(R.string.lottery_opens_body, formatDateTime(opens))
    )
}

/** Entrant and winner tiles, then when the draw runs (Figma F23). */
@Composable
private fun OpenCards(state: LotteryDetailsUiState.Loaded) {
    val lottery = state.lottery
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(state.entrantCount.toString(),
            lottery.waitlistCapacity?.let { stringResource(R.string.lottery_stat_entrants_of, it) } ?: stringResource(R.string.lottery_stat_entrants),
            Modifier.weight(1f))
        StatTile(lottery.numWinners.toString(), stringResource(R.string.lottery_stat_winners), Modifier.weight(1f))
    }
    val draw = lottery.registrationEnd.millis() ?: return
    NextStepCard(
        countdown(calendarDaysUntil(draw, state.now), R.plurals.lottery_draw_in, R.string.lottery_draw_today, R.string.lottery_draw_tomorrow),
        stringResource(R.string.lottery_draw_body, formatDateTime(draw))
    )
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier) {
    LotteryCard(highlighted = true, spacing = 2.dp, modifier = modifier) {
        Text(value, style = LotteryType.Title.copy(fontSize = 26.sp))
        Text(label, style = LotteryType.Small, color = LotteryColors.Muted)
    }
}

/** "5 winners drawn · From 24 entrants · Apr 8, 09:00 · booking open until Apr 10, 18:00". */
@Composable
private fun DrawnCard(state: LotteryDetailsUiState.Loaded) {
    val lottery = state.lottery
    val drawn = minOf(lottery.numWinners, state.entrantCount)
    val draw = lottery.registrationEnd.millis()?.let { formatDateTime(it) } ?: stringResource(R.string.lottery_dash)
    val bookingEnd = lottery.bookingEnd.millis()?.let { formatDateTime(it) } ?: stringResource(R.string.lottery_dash)
    NextStepCard(
        stringResource(R.string.lottery_drawn_title, pluralStringResource(R.plurals.lottery_count_winners, drawn, drawn)),
        stringResource(R.string.lottery_drawn_body,
            pluralStringResource(R.plurals.lottery_count_entrants, state.entrantCount, state.entrantCount), draw, bookingEnd)
    )
}

/** "No one entered" and a tip to try again (Figma F25). */
@Composable
private fun NoEntrantsCards(state: LotteryDetailsUiState.Loaded) {
    val closed = state.lottery.registrationEnd.millis()?.let { formatDay(it) } ?: stringResource(R.string.lottery_dash)
    NextStepCard(stringResource(R.string.lottery_no_one_title), stringResource(R.string.lottery_no_one_body, closed), highlighted = false)
    NextStepCard(stringResource(R.string.lottery_try_longer_title), stringResource(R.string.lottery_try_longer_body))
}

/** Timeline with the current phase's label in green. */
@Composable
private fun DetailsTimeline(state: LotteryDetailsUiState.Loaded) {
    val lottery = state.lottery
    val green = MaterialTheme.colorScheme.primary
    val closed = state.status.isClosed
    val dash = stringResource(R.string.lottery_dash)
    val regStart = lottery.registrationStart.millis()
    val regEnd = lottery.registrationEnd.millis()
    val bookStart = lottery.bookingStart.millis()
    val bookEnd = lottery.bookingEnd.millis()
    LotteryTimeline(
        registration = TimelineStop(
            stringResource(R.string.lottery_timeline_registration),
            if (regStart != null && regEnd != null) formatDayRange(regStart, regEnd) else dash,
            dotOn = true,
            labelColour = if (closed) Color.White else green
        ),
        draw = TimelineStop(
            stringResource(R.string.lottery_timeline_draw),
            regEnd?.let { formatDateTime(it) } ?: dash,
            dotOn = closed,
            labelColour = if (closed) green else Color.White
        ),
        booking = TimelineStop(
            stringResource(R.string.lottery_timeline_booking),
            if (bookStart != null && bookEnd != null) formatDayRange(bookStart, bookEnd) else dash,
            dotOn = false,
            labelColour = Color.White
        ),
        firstLineOn = closed,
        secondLineOn = false
    )
}

/** Campsites, winners, waiting list and booking; the open state shows only campsites and booking (Figma F23). */
@Composable
private fun SettingsCard(state: LotteryDetailsUiState.Loaded) {
    val lottery = state.lottery
    val names = state.campsites.joinToString(stringResource(R.string.lottery_list_separator)) { it.name }
    val booking = lottery.bookingStart.millis()?.let { s -> lottery.bookingEnd.millis()?.let { e -> compactRange(s, e) } }
        ?: stringResource(R.string.lottery_dash)
    LotteryCard {
        InfoRow(stringResource(R.string.lottery_row_campsites), names.ifEmpty { stringResource(R.string.lottery_dash) })
        if (state.status != LotteryStatus.OPEN) {
            InfoRow(stringResource(R.string.lottery_row_winners), lottery.numWinners.toString())
            InfoRow(stringResource(R.string.lottery_row_waitlist), waitlistText(lottery.waitlistCapacity))
        }
        InfoRow(stringResource(R.string.lottery_row_booking), booking)
    }
}

/** Design-time preview of the scheduled sample lottery (Figma F19). */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ScheduledPreview() {
    val lottery = TestLotteries.all.first { it.id == TestLotteries.SCHEDULED_LOTTERY_ID }
    TrailsTheme {
        LotteryDetailsContent(
            state = buildLotteryDetailsState(lottery, TestCampsites.all, 0, System.currentTimeMillis()),
            savedMessage = null,
            onBack = {}, onEdit = {}, onReuse = {}
        )
    }
}
