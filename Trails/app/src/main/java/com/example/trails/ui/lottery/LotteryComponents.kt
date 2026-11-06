// Small building blocks shared by the lottery screens, matching the Figma components
//  (Navigation / Header, Button / Primary, cards, badges, timeline, steppers).
// OWNER: Skanda (skandarajeev) — Lottery Setup
package com.example.trails.ui.lottery

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import com.example.trails.R

/** Test tags used by UI tests to find elements on the lottery screens. */
object LotteryTags {
    const val BACK = "lottery_back"
    const val HEADER_ACTION = "lottery_header_action"
    const val PRIMARY_BUTTON = "lottery_primary_button"
    const val NAME_FIELD = "lottery_name_field"
    const val WINNERS_DECREASE = "lottery_winners_decrease"
    const val WINNERS_INCREASE = "lottery_winners_increase"
    const val WINNERS_VALUE = "lottery_winners_value"
    const val WAITLIST_SWITCH = "lottery_waitlist_switch"
    const val WAITLIST_DECREASE = "lottery_waitlist_decrease"
    const val WAITLIST_INCREASE = "lottery_waitlist_increase"
    const val WAITLIST_VALUE = "lottery_waitlist_value"
    const val REGISTRATION_CARD = "lottery_registration_card"
    const val BOOKING_CARD = "lottery_booking_card"
    const val SHEET_CONFIRM = "lottery_sheet_confirm"
    const val SHEET_OPENS = "lottery_sheet_opens"
    const val SHEET_CLOSES = "lottery_sheet_closes"
    const val DIALOG_BUTTON = "lottery_dialog_button"
    const val SAVED_BANNER = "lottery_saved_banner"
    const val LIST = "lottery_list"
    const val CREATE = "lottery_create"
    const val EMPTY = "lottery_empty"
    const val LOADING = "lottery_loading"
    const val DIALOG_CANCEL = "lottery_dialog_cancel"
    const val DELETE = "lottery_delete"
    const val DELETE_BLOCKED = "lottery_delete_blocked"

    /** Tag for a campsite row in the picker. */
    fun campsite(id: String) = "lottery_campsite_$id"

    /** Tag for a summary row's Edit link on the review screen. */
    fun editRow(field: String) = "lottery_edit_$field"

    /** Tag for a settings row on the edit screen. */
    fun settingsRow(field: String) = "lottery_settings_$field"

    /** Tag for a filter chip on the Lotteries tab. */
    fun filter(filter: LotteryFilter) = "lottery_filter_${filter.name.lowercase()}"

    /** Tag for a lottery card on the Lotteries tab. */
    fun card(id: String) = "lottery_card_$id"

    /** Tag for a day in the date picker, e.g. "lottery_day_2027_4_8". */
    fun day(day: CalendarDay) = "lottery_day_${day.year}_${day.month}_${day.day}"
}

/** Back chevron, title and an optional green action, e.g. "Edit" (Figma "Navigation / Header", Type=Back). */
@Composable
internal fun LotteryHeader(title: String, onBack: () -> Unit, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 7.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 40dp touch target around the 22dp chevron, placed so the chevron sits 16dp from the edge.
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .testTag(LotteryTags.BACK),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(R.drawable.lottery_ic_chevron_left),
                contentDescription = stringResource(R.string.lottery_back),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(3.dp))
        Text(title, style = LotteryType.Header, modifier = Modifier.weight(1f))
        if (actionLabel != null) {
            Text(
                actionLabel,
                style = LotteryType.Header.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onAction)
                    .padding(4.dp)
                    .testTag(LotteryTags.HEADER_ACTION)
            )
        }
    }
}

/** Full-width 44dp button: green when enabled, dark green when disabled, red when [destructive]. */
@Composable
internal fun LotteryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContainerColor = LotteryColors.DisabledFill,
            disabledContentColor = LotteryColors.DisabledText
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Text(text, style = LotteryType.Button)
    }
}

/** Bottom area holding the main button (Figma "Footer": 24dp sides, 12dp top and bottom). */
@Composable
internal fun LotteryFooter(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

/** "Step 2 of 3 · Dates" and the three progress bars. */
@Composable
internal fun StepProgress(number: Int, @StringRes name: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.lottery_step, number, stringResource(name)),
            style = LotteryType.Small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
            color = LotteryColors.Muted
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i < number) MaterialTheme.colorScheme.primary else LotteryColors.Line)
                )
            }
        }
    }
}

/** Bold question and the grey explanation under it, e.g. "When does it run?". */
@Composable
internal fun StepHeading(title: String, hint: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = LotteryType.Heading)
        Text(hint, style = LotteryType.Hint, color = LotteryColors.Muted)
    }
}

/**
 * Rounded card: dark with a muted border, or dark green with a green border when [highlighted],
 * or with a red border when [error].
 */
@Composable
internal fun LotteryCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    error: Boolean = false,
    onClick: (() -> Unit)? = null,
    padding: Dp = 14.dp,
    spacing: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val border = when {
        error -> BorderStroke(1.5.dp, LotteryColors.Error)
        highlighted -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        else -> BorderStroke(1.dp, LotteryColors.Line)
    }
    Column(
        modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
            .clip(shape)
            .background(if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
            .border(border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

/** Thin divider between rows inside a card. */
@Composable
internal fun CardDivider() {
    HorizontalDivider(thickness = 1.dp, color = LotteryColors.Line)
}

/** Outlined status badge: amber Scheduled, green Registration open, grey once closed. */
@Composable
internal fun StatusBadge(status: LotteryStatus, modifier: Modifier = Modifier) {
    val (label, colour) = when (status) {
        LotteryStatus.SCHEDULED -> R.string.lottery_badge_scheduled to LotteryColors.Amber
        LotteryStatus.OPEN -> R.string.lottery_badge_open to MaterialTheme.colorScheme.primary
        LotteryStatus.DRAWN -> R.string.lottery_badge_drawn to LotteryColors.Muted
        LotteryStatus.NO_ENTRANTS -> R.string.lottery_badge_no_entrants to LotteryColors.Muted
    }
    val shape = RoundedCornerShape(6.dp)
    Text(
        stringResource(label),
        style = LotteryType.TinyStrong,
        color = colour,
        modifier = modifier
            .clip(shape)
            .background(colour.copy(alpha = 0.12f))
            .border(1.dp, colour.copy(alpha = 0.8f), shape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

/**
 * One stop on the Registration → Draw → Booking timeline.
 *
 * @property label e.g. "Registration".
 * @property value e.g. "Apr 1 – 8" or "Not set".
 * @property dotOn true for a green dot, false for a muted one.
 * @property labelColour colour of [label].
 */
internal data class TimelineStop(val label: String, val value: String, val dotOn: Boolean, val labelColour: Color)

/** The three-stop timeline card (Figma "Timeline"). */
@Composable
internal fun LotteryTimeline(registration: TimelineStop, draw: TimelineStop, booking: TimelineStop, firstLineOn: Boolean, secondLineOn: Boolean) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, LotteryColors.Line, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TimelineStopView(registration, Alignment.Start)
        TimelineLine(firstLineOn, Modifier.weight(1f))
        TimelineStopView(draw, Alignment.CenterHorizontally)
        TimelineLine(secondLineOn, Modifier.weight(1f))
        TimelineStopView(booking, Alignment.End)
    }
}

@Composable
private fun TimelineStopView(stop: TimelineStop, alignment: Alignment.Horizontal) {
    Column(horizontalAlignment = alignment, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (stop.dotOn) MaterialTheme.colorScheme.primary else LotteryColors.Line)
        )
        Text(stop.label, style = LotteryType.SmallStrong, color = stop.labelColour)
        Text(stop.value, style = LotteryType.Tiny, color = LotteryColors.Muted)
    }
}

@Composable
private fun TimelineLine(on: Boolean, modifier: Modifier) {
    Box(modifier.padding(top = 4.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (on) MaterialTheme.colorScheme.primary else LotteryColors.Line)
        )
    }
}

/** Round − / + button for the steppers: dark outlined, or solid green when [primary]. */
@Composable
internal fun StepButton(
    symbol: String,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp,
    primary: Boolean,
    enabled: Boolean,
    tag: String
) {
    val green = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(if (primary) green else MaterialTheme.colorScheme.surfaceContainer)
            .then(if (primary) Modifier else Modifier.border(1.dp, LotteryColors.Line, CircleShape))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            symbol,
            style = LotteryType.Header.copy(fontSize = if (size > 40.dp) 22.sp else 18.sp),
            color = if (primary) MaterialTheme.colorScheme.onPrimary else Color.White
        )
    }
}

/** Rounded campsite photo, or a plain tile when the campsite has none. */
@Composable
internal fun CampsitePhoto(url: String?, size: Dp = 56.dp) {
    val mod = Modifier
        .size(size)
        .clip(RoundedCornerShape(8.dp))
    if (url != null) {
        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = mod)
    } else {
        Box(mod.background(LotteryColors.Line))
    }
}

/** A campsite in the picker: photo, name, subtitle and a round tick (Figma "Select · …"). */
@Composable
internal fun CampsiteSelectRow(
    imageUrl: String?,
    name: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    LotteryCard(
        highlighted = selected,
        onClick = if (enabled) onClick else null,
        padding = 12.dp,
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.5f)
            .testTag(tag)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CampsitePhoto(imageUrl)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name, style = LotteryType.CardTitle)
                Text(subtitle, style = LotteryType.Small, color = LotteryColors.Muted)
            }
            Image(
                painterResource(if (selected) R.drawable.lottery_ic_check_circle else R.drawable.lottery_ic_radio),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/** Review row: small grey label over a bold value, with "Edit" (green) or "Auto" (grey) on the right. */
@Composable
internal fun SummaryRow(label: String, value: String, action: String, onAction: (() -> Unit)?, tag: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = LotteryType.TinyStrong, color = LotteryColors.Muted)
            Text(value, style = LotteryType.BodyStrong)
        }
        Text(
            action,
            style = LotteryType.Action,
            color = if (onAction != null) MaterialTheme.colorScheme.primary else LotteryColors.Muted,
            modifier = if (onAction != null) {
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onAction)
                    .padding(4.dp)
                    .testTag(tag)
            } else {
                Modifier.padding(4.dp)
            }
        )
    }
}

/** Edit-screen row: grey label, bold value on the right and a chevron; the whole row is tappable. */
@Composable
internal fun SettingsRow(label: String, value: String, onClick: () -> Unit, tag: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(label, style = LotteryType.Hint, color = LotteryColors.Muted)
        Text(value, style = LotteryType.BodyStrong, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Icon(painterResource(R.drawable.lottery_ic_chevron_right), contentDescription = null, tint = LotteryColors.Muted, modifier = Modifier.size(16.dp))
    }
}

/** Details-page row: grey label on the left, bold value on the right. */
@Composable
internal fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = LotteryType.Hint, color = LotteryColors.Muted)
        Text(value, style = LotteryType.BodyStrong, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

/** "Lottery name" label and a 44dp text box (Figma "Input / default"). */
@Composable
internal fun LotteryNameField(value: String, onValueChange: (String) -> Unit, showError: Boolean) {
    val shape = RoundedCornerShape(8.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.lottery_name_label), style = LotteryType.Label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = LotteryType.Body.copy(color = Color.White),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(LotteryTags.NAME_FIELD),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, if (showError) LotteryColors.Error else LotteryColors.Line, shape)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(stringResource(R.string.lottery_name_hint), style = LotteryType.Body, color = LotteryColors.Muted)
                    }
                    inner()
                }
            }
        )
        if (showError) ErrorText(stringResource(R.string.lottery_problem_name))
    }
}

/** Red helper text under a field. */
@Composable
internal fun ErrorText(text: String, modifier: Modifier = Modifier) {
    Text(text, style = LotteryType.Small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = LotteryColors.Error, modifier = modifier)
}

/** Switch colours from the mockups: green track when on, muted when off. */
@Composable
internal fun lotterySwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedBorderColor = MaterialTheme.colorScheme.primary,
    uncheckedThumbColor = LotteryColors.Muted,
    uncheckedTrackColor = LotteryColors.Line,
    uncheckedBorderColor = LotteryColors.Line
)

/** "2 days 9 h", "7 days" or "5 h". */
@Composable
internal fun spanText(span: DaysHours): String {
    val days = pluralStringResource(R.plurals.lottery_days, span.days, span.days)
    val hours = stringResource(R.string.lottery_hours, span.hours)
    return when {
        span.days > 0 && span.hours > 0 -> stringResource(R.string.lottery_days_hours, days, hours)
        span.days > 0 -> days
        else -> hours
    }
}

/** The message for a form problem. */
@Composable
internal fun problemText(problem: LotteryProblem): String = when (problem) {
    LotteryProblem.NameMissing -> stringResource(R.string.lottery_problem_name)
    LotteryProblem.NoCampsites -> stringResource(R.string.lottery_problem_campsites)
    LotteryProblem.RegistrationNotSet -> stringResource(R.string.lottery_problem_registration_missing)
    LotteryProblem.RegistrationEndsBeforeStart -> stringResource(R.string.lottery_problem_registration_order)
    LotteryProblem.RegistrationClosesInPast -> stringResource(R.string.lottery_problem_registration_past)
    LotteryProblem.RegistrationStartLocked -> stringResource(R.string.lottery_problem_registration_locked)
    LotteryProblem.BookingNotSet -> stringResource(R.string.lottery_problem_booking_missing)
    is LotteryProblem.BookingBeforeRegistrationCloses ->
        stringResource(R.string.lottery_problem_booking_overlap, formatDateTime(problem.registrationEnd))
    LotteryProblem.BookingEndsBeforeStart -> stringResource(R.string.lottery_problem_booking_order)
    is LotteryProblem.WaitlistBelowWinners -> stringResource(R.string.lottery_problem_waitlist_winners, problem.winners)
    is LotteryProblem.WaitlistBelowEntrants -> stringResource(R.string.lottery_problem_waitlist_entrants, problem.entrants)
}

/** "Up to 100 people" or "No limit". */
@Composable
internal fun waitlistText(capacity: Int?, short: Boolean = false): String = when {
    capacity == null -> stringResource(R.string.lottery_waitlist_no_limit)
    short -> stringResource(R.string.lottery_waitlist_up_to_short, capacity)
    else -> stringResource(R.string.lottery_waitlist_up_to, capacity)
}

/** Bottom sheet in the lottery style: dark green, rounded top, small handle (Figma "Date range sheet"). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LotterySheet(title: String?, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LotteryColors.Sheet,
        scrimColor = Color.Black.copy(alpha = 0.62f),
        // No border modifier here: ModalBottomSheet measures it before the slide-in offset,
        // so it drew a stray box over the top half of the sheet.
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(LotteryColors.Line)
            )
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (title != null) Text(title, style = LotteryType.Header)
            content()
        }
    }
}

/**
 * Small card dialog: title, message and a full-width button, plus a grey Cancel when
 * [dismissLabel] is given (Figma F18 Published, F21 Couldn't save).
 */
@Composable
internal fun LotteryDialog(
    title: String,
    body: String,
    button: String,
    destructive: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(16.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.5.dp, if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, shape)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(title, style = LotteryType.Header)
            Text(body, style = LotteryType.Body.copy(lineHeight = 20.sp))
            LotteryButton(text = button, onClick = onClick, destructive = destructive, modifier = Modifier.testTag(LotteryTags.DIALOG_BUTTON))
            if (dismissLabel != null) {
                Text(
                    dismissLabel,
                    style = LotteryType.Label,
                    color = LotteryColors.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 8.dp)
                        .testTag(LotteryTags.DIALOG_CANCEL)
                )
            }
        }
    }
}

/**
 * Red outlined "Delete lottery", or a grey note saying why it can't be deleted once
 * people have joined. Tapping the button asks for confirmation first.
 *
 * @param entrantCount people on the waiting list; deleting is only offered at 0.
 * @param isDeleting true while the delete runs.
 * @param onConfirm called after the owner confirms.
 */
@Composable
internal fun DeleteLotterySection(entrantCount: Int, isDeleting: Boolean, onConfirm: () -> Unit) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    if (entrantCount > 0) {
        Text(
            pluralStringResource(R.plurals.lottery_delete_blocked, entrantCount, entrantCount),
            style = LotteryType.Small,
            color = LotteryColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag(LotteryTags.DELETE_BLOCKED)
        )
        return
    }
    val red = LotteryColors.Error
    OutlinedButton(
        onClick = { confirming = true },
        enabled = !isDeleting,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isDeleting) LotteryColors.Line else red),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = red, disabledContentColor = LotteryColors.Muted),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag(LotteryTags.DELETE)
    ) {
        Text(stringResource(if (isDeleting) R.string.lottery_deleting else R.string.lottery_delete), style = LotteryType.Button)
    }
    if (confirming) {
        LotteryDialog(
            title = stringResource(R.string.lottery_delete_confirm_title),
            body = stringResource(R.string.lottery_delete_confirm_body),
            button = stringResource(R.string.lottery_delete_confirm),
            destructive = true,
            onClick = { confirming = false; onConfirm() },
            onDismiss = { confirming = false },
            dismissLabel = stringResource(R.string.lottery_cancel)
        )
    }
}
