// OWNER: Skanda (skandarajeev) — Lottery Setup
// USER STORIES: 02.04.01, 02.04.02 (campsites), 02.04.03, 02.04.04 (dates), 02.04.05, 02.04.07 (winners)
// PURPOSE: The New lottery pages: Campsites (F07–F09), Dates (F10–F14), Winners (F15, F16)
//  and Review (F17), plus the period sheets shared with the edit screen.
// Outstanding issues: none.
package com.example.trails.ui.lottery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trails.R

/** Scrolling page body with the Figma content padding (24dp sides, 8dp top). */
@Composable
internal fun StepBody(modifier: Modifier, spacing: Int = 14, content: @Composable () -> Unit) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp)
    ) { content() }
}

/** Step 1: tick one or more campsites (US 02.04.01, 02.04.02). */
@Composable
internal fun CampsitesStep(state: LotteryFormUiState, actions: LotteryFormActions, modifier: Modifier) {
    StepBody(modifier, spacing = 16) {
        StepProgress(1, R.string.lottery_step_campsites)
        StepHeading(stringResource(R.string.lottery_campsites_heading), stringResource(R.string.lottery_campsites_hint))
        CampsiteChoices(state, actions.onToggleCampsite)
    }
}

/** The campsite rows, shared by step 1 and the edit screen's Campsites sheet. */
@Composable
internal fun CampsiteChoices(state: LotteryFormUiState, onToggle: (String) -> Unit) {
    if (state.campsites.isEmpty()) {
        Text(stringResource(R.string.lottery_campsites_none), style = LotteryType.Body, color = LotteryColors.Muted)
    }
    state.campsites.forEach { option ->
        val c = option.campsite
        val selected = c.id in state.draft.campsiteIds
        CampsiteSelectRow(
            imageUrl = c.imageUrl,
            name = c.name,
            subtitle = option.takenBy?.let { stringResource(R.string.lottery_campsite_taken, it) }
                ?: stringResource(R.string.lottery_campsite_location_guests, c.locationName, c.maxOccupancy),
            selected = selected,
            enabled = option.available || selected,
            onClick = { onToggle(c.id) },
            tag = LotteryTags.campsite(c.id)
        )
    }
}

/** Which period sheet is open, if any. */
internal enum class PeriodSheet { REGISTRATION, BOOKING }

/** Step 2: registration and booking periods, with the timeline on top (US 02.04.03, 02.04.04). */
@Composable
internal fun DatesStep(state: LotteryFormUiState, actions: LotteryFormActions, modifier: Modifier) {
    var sheet by rememberSaveable { mutableStateOf<PeriodSheet?>(null) }
    val draft = state.draft
    val regProblem = state.problems[LotteryField.REGISTRATION]?.takeUnless { it == LotteryProblem.RegistrationNotSet }
    val bookProblem = state.problems[LotteryField.BOOKING]?.takeUnless { it == LotteryProblem.BookingNotSet }
    val regSet = draft.registrationStart != null && draft.registrationEnd != null
    val bookSet = draft.bookingStart != null && draft.bookingEnd != null

    StepBody(modifier) {
        StepProgress(2, R.string.lottery_step_dates)
        StepHeading(stringResource(R.string.lottery_dates_heading), stringResource(R.string.lottery_dates_hint))
        WizardTimeline(draft, regSet, bookSet && bookProblem == null, bookSet)
        PeriodCard(
            title = stringResource(R.string.lottery_registration_period),
            value = if (regSet) rangeText(draft.registrationStart!!, draft.registrationEnd!!) else null,
            placeholder = stringResource(R.string.lottery_registration_choose),
            note = regProblem?.let { problemText(it) }
                ?: draft.drawTime?.let { stringResource(R.string.lottery_registration_draw_note, formatDateTime(it)) },
            error = regProblem != null,
            enabled = true,
            onClick = { sheet = PeriodSheet.REGISTRATION },
            tag = LotteryTags.REGISTRATION_CARD
        )
        PeriodCard(
            title = stringResource(R.string.lottery_booking_period),
            value = if (bookSet) rangeText(draft.bookingStart!!, draft.bookingEnd!!) else null,
            placeholder = stringResource(R.string.lottery_booking_choose),
            note = when {
                !regSet -> stringResource(R.string.lottery_booking_needs_registration)
                bookProblem != null -> problemText(bookProblem)
                bookSet -> stringResource(R.string.lottery_booking_time_to_book, spanText(spanBetween(draft.bookingStart!!, draft.bookingEnd!!)))
                else -> stringResource(R.string.lottery_booking_hint)
            },
            error = bookProblem != null,
            enabled = regSet,
            onClick = { sheet = PeriodSheet.BOOKING },
            tag = LotteryTags.BOOKING_CARD
        )
    }
    PeriodSheets(state, sheet, onClose = { sheet = null }, actions)
}

/** "Apr 1, 09:00  →  Apr 8, 09:00". */
@Composable
private fun rangeText(start: Long, end: Long) = stringResource(R.string.lottery_range, formatDateTime(start), formatDateTime(end))

/** Timeline while setting up: stops turn green as each period is set (Figma F10–F14). */
@Composable
private fun WizardTimeline(draft: LotteryDraft, regSet: Boolean, bookingValid: Boolean, bookingSet: Boolean) {
    val white = Color.White
    val muted = LotteryColors.Muted
    LotteryTimeline(
        registration = TimelineStop(
            stringResource(R.string.lottery_timeline_registration),
            if (regSet) formatDayRange(draft.registrationStart!!, draft.registrationEnd!!) else stringResource(R.string.lottery_not_set),
            dotOn = regSet,
            labelColour = if (regSet) white else muted
        ),
        draw = TimelineStop(
            stringResource(R.string.lottery_timeline_draw),
            draft.drawTime?.let { formatDateTime(it) } ?: stringResource(R.string.lottery_dash),
            dotOn = regSet,
            labelColour = if (regSet) white else muted
        ),
        booking = TimelineStop(
            stringResource(R.string.lottery_timeline_booking),
            when {
                bookingValid -> formatDayRange(draft.bookingStart!!, draft.bookingEnd!!)
                bookingSet -> stringResource(R.string.lottery_check_dates)
                else -> stringResource(R.string.lottery_not_set)
            },
            dotOn = bookingValid,
            labelColour = if (bookingValid) white else muted
        ),
        firstLineOn = regSet,
        secondLineOn = bookingValid
    )
}

/**
 * Registration / Booking card: a prompt until set, then the period with "Change" (green card),
 * or a red card with the problem.
 */
@Composable
private fun PeriodCard(
    title: String,
    value: String?,
    placeholder: String,
    note: String?,
    error: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val set = value != null
    LotteryCard(
        highlighted = set && !error,
        error = error,
        onClick = if (enabled) onClick else null,
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.55f)
            .testTag(tag)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = LotteryType.BodyStrong, modifier = Modifier.weight(1f))
            if (set) Text(stringResource(R.string.lottery_change), style = LotteryType.Action, color = MaterialTheme.colorScheme.primary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                painterResource(R.drawable.lottery_ic_calendar),
                contentDescription = null,
                tint = if (error) LotteryColors.Error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                value ?: placeholder,
                style = if (set) LotteryType.CardTitle else LotteryType.Body,
                color = if (set) Color.White else LotteryColors.Muted,
                modifier = Modifier.weight(1f)
            )
            Icon(painterResource(R.drawable.lottery_ic_chevron_right), contentDescription = null, tint = LotteryColors.Muted, modifier = Modifier.size(16.dp))
        }
        if (note != null) {
            if (error) ErrorText(note) else Text(note, style = LotteryType.Small, color = LotteryColors.Muted)
        }
    }
}

/** The registration and booking sheets, shared by the Dates step and the edit screen. */
@Composable
internal fun PeriodSheets(state: LotteryFormUiState, sheet: PeriodSheet?, onClose: () -> Unit, actions: LotteryFormActions) {
    val draft = state.draft
    val today = CalendarDay.of(state.now)
    when (sheet) {
        PeriodSheet.REGISTRATION -> DateRangeSheet(
            title = stringResource(R.string.lottery_registration_period),
            confirmLabel = stringResource(R.string.lottery_set_registration),
            initialStart = draft.registrationStart,
            initialEnd = draft.registrationEnd,
            defaultStartMinute = 9 * 60,
            defaultEndMinute = 9 * 60,
            minDay = today,
            lockedStart = state.openedAt,
            summary = { s, e -> stringResource(R.string.lottery_sheet_registration_summary, spanText(spanBetween(s, e))) },
            onConfirm = { s, e -> actions.onRegistrationChange(s, e); onClose() },
            onDismiss = onClose
        )
        PeriodSheet.BOOKING -> {
            val closes = draft.registrationEnd
            DateRangeSheet(
                title = stringResource(R.string.lottery_booking_period),
                confirmLabel = stringResource(R.string.lottery_set_booking),
                initialStart = draft.bookingStart ?: closes,
                initialEnd = draft.bookingEnd,
                defaultStartMinute = closes?.let { minuteOfDay(it) } ?: (9 * 60),
                defaultEndMinute = 18 * 60,
                minDay = closes?.let { CalendarDay.of(it) } ?: today,
                lockedStart = null,
                summary = { s, e ->
                    val span = spanText(spanBetween(s, e))
                    if (s == closes) stringResource(R.string.lottery_sheet_booking_summary_at_close, span)
                    else stringResource(R.string.lottery_sheet_booking_summary, span)
                },
                onConfirm = { s, e -> actions.onBookingChange(s, e); onClose() },
                onDismiss = onClose
            )
        }
        null -> Unit
    }
}

/** Step 3: how many winners, and the optional waiting list limit (US 02.04.05, 02.04.07). */
@Composable
internal fun WinnersStep(state: LotteryFormUiState, actions: LotteryFormActions, modifier: Modifier) {
    val winners = state.draft.numWinners
    StepBody(modifier) {
        StepProgress(3, R.string.lottery_step_winners)
        StepHeading(stringResource(R.string.lottery_winners_heading), stringResource(R.string.lottery_winners_hint))
        LotteryCard(highlighted = true, padding = 16.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StepButton("−", stringResource(R.string.lottery_decrease), { actions.onWinnersChange(winners - 1) },
                    size = 44.dp, primary = false, enabled = winners > 1, tag = LotteryTags.WINNERS_DECREASE)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(winners.toString(), style = LotteryType.BigNumber, modifier = Modifier.testTag(LotteryTags.WINNERS_VALUE))
                    Text(pluralStringResource(R.plurals.lottery_winners_unit, winners), style = LotteryType.Hint, color = LotteryColors.Muted)
                }
                StepButton("+", stringResource(R.string.lottery_increase), { actions.onWinnersChange(winners + 1) },
                    size = 44.dp, primary = true, enabled = winners < MAX_WINNERS, tag = LotteryTags.WINNERS_INCREASE)
            }
        }
        Text(pluralStringResource(R.plurals.lottery_winners_fewer, winners, winners), style = LotteryType.Small, color = LotteryColors.Muted)
        WaitlistLimitCard(state, actions)
    }
}

/** "Limit the waiting list" switch; when on, a Maximum people stepper (Figma F15, F16). */
@Composable
internal fun WaitlistLimitCard(state: LotteryFormUiState, actions: LotteryFormActions) {
    val draft = state.draft
    val on = draft.limitWaitlist
    val capacity = draft.waitlistCapacity
    LotteryCard(highlighted = on, spacing = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.lottery_limit_title), style = LotteryType.BodyStrong)
                Text(
                    stringResource(if (on) R.string.lottery_limit_on_hint else R.string.lottery_limit_off_hint),
                    style = LotteryType.Small,
                    color = LotteryColors.Muted
                )
            }
            Switch(
                checked = on,
                onCheckedChange = actions.onWaitlistLimitChange,
                colors = lotterySwitchColors(),
                modifier = Modifier.testTag(LotteryTags.WAITLIST_SWITCH)
            )
        }
        if (on) {
            CardDivider()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.lottery_limit_max), style = LotteryType.Hint, color = LotteryColors.Muted, modifier = Modifier.weight(1f))
                StepButton("−", stringResource(R.string.lottery_decrease), { actions.onWaitlistCapacityChange(capacity - WAITLIST_STEP) },
                    size = 34.dp, primary = false, enabled = capacity > draft.numWinners, tag = LotteryTags.WAITLIST_DECREASE)
                Text(capacity.toString(), style = LotteryType.Header.copy(fontSize = 20.sp), modifier = Modifier.testTag(LotteryTags.WAITLIST_VALUE))
                StepButton("+", stringResource(R.string.lottery_increase), { actions.onWaitlistCapacityChange(capacity + WAITLIST_STEP) },
                    size = 34.dp, primary = true, enabled = capacity < MAX_WAITLIST, tag = LotteryTags.WAITLIST_INCREASE)
            }
            state.problems[LotteryField.WAITLIST]?.let { ErrorText(problemText(it)) }
        }
    }
}

/** Review: name the lottery and check every setting, each with Edit (Figma F17). */
@Composable
internal fun ReviewStep(state: LotteryFormUiState, actions: LotteryFormActions, showNameError: Boolean, modifier: Modifier) {
    val draft = state.draft
    val dash = stringResource(R.string.lottery_dash)
    val edit = stringResource(R.string.lottery_edit)
    StepBody(modifier) {
        StepHeading(stringResource(R.string.lottery_review_heading), stringResource(R.string.lottery_review_hint))
        LotteryNameField(draft.name, actions.onNameChange, showNameError)
        LotteryCard {
            SummaryRow(stringResource(R.string.lottery_row_campsites),
                state.campsiteNames.joinToString(stringResource(R.string.lottery_list_separator)).ifEmpty { dash },
                edit, { actions.onEditStep(LotteryStep.CAMPSITES) }, LotteryTags.editRow("campsites"))
            CardDivider()
            SummaryRow(stringResource(R.string.lottery_row_registration),
                draft.registrationStart?.let { s -> draft.registrationEnd?.let { e -> compactRange(s, e) } } ?: dash,
                edit, { actions.onEditStep(LotteryStep.DATES) }, LotteryTags.editRow("registration"))
            CardDivider()
            SummaryRow(stringResource(R.string.lottery_row_draw),
                draft.drawTime?.let { stringResource(R.string.lottery_draw_automatic, formatDateTime(it)) } ?: dash,
                stringResource(R.string.lottery_auto), null, LotteryTags.editRow("draw"))
            CardDivider()
            SummaryRow(stringResource(R.string.lottery_row_booking),
                draft.bookingStart?.let { s -> draft.bookingEnd?.let { e -> compactRange(s, e) } } ?: dash,
                edit, { actions.onEditStep(LotteryStep.DATES) }, LotteryTags.editRow("booking"))
            CardDivider()
            SummaryRow(stringResource(R.string.lottery_row_winners), draft.numWinners.toString(),
                edit, { actions.onEditStep(LotteryStep.WINNERS) }, LotteryTags.editRow("winners"))
            CardDivider()
            SummaryRow(stringResource(R.string.lottery_row_waitlist),
                waitlistText(if (draft.limitWaitlist) draft.waitlistCapacity else null),
                edit, { actions.onEditStep(LotteryStep.WINNERS) }, LotteryTags.editRow("waitlist"))
        }
    }
}

/** "Apr 1, 09:00 → Apr 8, 09:00". */
@Composable
internal fun compactRange(start: Long, end: Long) = stringResource(R.string.lottery_range_compact, formatDateTime(start), formatDateTime(end))
