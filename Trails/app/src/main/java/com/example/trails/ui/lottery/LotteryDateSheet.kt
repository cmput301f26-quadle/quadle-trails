// OWNER: Skanda (skandarajeev) — Lottery Setup
// USER STORIES: 02.04.03 (registration period), 02.04.04 (booking period)
// PURPOSE: Bottom sheet for picking a period: month calendar, Opens / Closes times and a
//  summary line (Figma F11 · Registration period, F12 · Booking period).
// Outstanding issues: none.
package com.example.trails.ui.lottery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trails.R
import com.example.trails.ui.theme.TrailsTheme
import java.text.DateFormatSymbols
import java.util.Locale

/**
 * Period picker in a bottom sheet.
 *
 * @param title e.g. "Registration period".
 * @param confirmLabel e.g. "Set registration period".
 * @param initialStart the current start, or a suggested one (booking suggests registration close).
 * @param initialEnd the current end, or null.
 * @param defaultStartMinute opening time used until the owner changes it, e.g. 9 * 60 for 09:00.
 * @param defaultEndMinute closing time used until the owner changes it.
 * @param minDay earliest day that can be tapped.
 * @param lockedStart set when registration already opened: the start is fixed and only the end moves.
 * @param summary the grey line under the times for a valid start and end.
 * @param onConfirm called with the chosen start and end in milliseconds.
 * @param onDismiss called on Cancel or when the sheet is swiped away.
 */
@Composable
internal fun DateRangeSheet(
    title: String,
    confirmLabel: String,
    initialStart: Long?,
    initialEnd: Long?,
    defaultStartMinute: Int,
    defaultEndMinute: Int,
    minDay: CalendarDay,
    lockedStart: Long?,
    summary: @Composable (start: Long, end: Long) -> String,
    onConfirm: (start: Long, end: Long) -> Unit,
    onDismiss: () -> Unit
) {
    LotterySheet(title = null, onDismiss = onDismiss) {
        DateRangePickerContent(
            title, confirmLabel, initialStart, initialEnd, defaultStartMinute, defaultEndMinute,
            minDay, lockedStart, summary, onConfirm, onDismiss
        )
    }
}

private enum class TimeTarget { START, END }

/** Sheet body without the sheet itself, so previews and UI tests can show it directly. */
@Composable
internal fun DateRangePickerContent(
    title: String,
    confirmLabel: String,
    initialStart: Long?,
    initialEnd: Long?,
    defaultStartMinute: Int,
    defaultEndMinute: Int,
    minDay: CalendarDay,
    lockedStart: Long?,
    summary: @Composable (start: Long, end: Long) -> String,
    onConfirm: (start: Long, end: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val lockedDay = lockedStart?.let { CalendarDay.of(it) }
    var range by remember { mutableStateOf(DayRange(initialStart?.let { CalendarDay.of(it) }, initialEnd?.let { CalendarDay.of(it) })) }
    var startMinute by remember { mutableIntStateOf(initialStart?.let { minuteOfDay(it) } ?: defaultStartMinute) }
    var endMinute by remember { mutableIntStateOf(initialEnd?.let { minuteOfDay(it) } ?: defaultEndMinute) }
    var month by remember { mutableStateOf(CalendarMonth.of(range.end ?: range.start ?: minDay)) }
    var picking by remember { mutableStateOf<TimeTarget?>(null) }

    val start = range.start?.atMinute(startMinute)
    val end = range.end?.atMinute(endMinute)
    val earliest = lockedDay ?: minDay

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = LotteryType.Header, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.lottery_cancel),
                style = LotteryType.Label,
                color = LotteryColors.Muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onDismiss)
                    .padding(4.dp)
            )
        }
        MonthSwitcher(
            month = month,
            canGoBack = month.year * 12 + month.month > earliest.year * 12 + earliest.month,
            onPrevious = { month = month.previous() },
            onNext = { month = month.next() }
        )
        MonthGrid(month, range, earliest) { day ->
            range = if (lockedDay != null) DayRange(lockedDay, day) else range.tap(day)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TimeBox(
                label = stringResource(R.string.lottery_sheet_opens),
                value = start?.let { formatPickerDateTime(it) } ?: stringResource(R.string.lottery_dash),
                enabled = start != null && lockedStart == null,
                onClick = { picking = TimeTarget.START },
                tag = LotteryTags.SHEET_OPENS,
                modifier = Modifier.weight(1f)
            )
            TimeBox(
                label = stringResource(R.string.lottery_sheet_closes),
                value = end?.let { formatPickerDateTime(it) } ?: stringResource(R.string.lottery_dash),
                enabled = end != null,
                onClick = { picking = TimeTarget.END },
                tag = LotteryTags.SHEET_CLOSES,
                modifier = Modifier.weight(1f)
            )
        }
        val ordered = start != null && end != null && end > start
        Text(
            when {
                start == null || end == null -> stringResource(R.string.lottery_sheet_pick_days)
                !ordered -> stringResource(R.string.lottery_sheet_end_before_start)
                else -> summary(start, end)
            },
            style = LotteryType.Hint,
            color = if (start != null && end != null && !ordered) LotteryColors.Error else LotteryColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (lockedStart != null) {
            Text(stringResource(R.string.lottery_sheet_opens_locked), style = LotteryType.Small, color = LotteryColors.Muted, textAlign = TextAlign.Center)
        }
        LotteryButton(
            text = confirmLabel,
            enabled = ordered,
            onClick = { if (start != null && end != null) onConfirm(start, end) },
            modifier = Modifier.testTag(LotteryTags.SHEET_CONFIRM)
        )
    }

    when (picking) {
        TimeTarget.START -> TimeDialog(stringResource(R.string.lottery_pick_opening_time), startMinute,
            onConfirm = { startMinute = it; picking = null }, onDismiss = { picking = null })
        TimeTarget.END -> TimeDialog(stringResource(R.string.lottery_pick_closing_time), endMinute,
            onConfirm = { endMinute = it; picking = null }, onDismiss = { picking = null })
        null -> Unit
    }
}

/** "‹  April 2027  ›". */
@Composable
private fun MonthSwitcher(month: CalendarMonth, canGoBack: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val previous = stringResource(R.string.lottery_previous_month)
        val next = stringResource(R.string.lottery_next_month)
        Text(
            "‹",
            fontSize = 20.sp,
            color = if (canGoBack) LotteryColors.Muted else LotteryColors.Line,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(enabled = canGoBack, onClick = onPrevious)
                .semantics { contentDescription = previous }
                .padding(horizontal = 10.dp)
        )
        Text(formatMonthTitle(month), style = LotteryType.CardTitle, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Text(
            "›",
            fontSize = 20.sp,
            color = LotteryColors.Muted,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onNext)
                .semantics { contentDescription = next }
                .padding(horizontal = 10.dp)
        )
    }
}

/** Weekday letters and the day grid, with the picked range highlighted. */
@Composable
private fun MonthGrid(month: CalendarMonth, range: DayRange, earliest: CalendarDay, onTap: (CalendarDay) -> Unit) {
    val weekdays = remember {
        DateFormatSymbols(Locale.getDefault()).shortWeekdays.drop(1).map { it.take(1).uppercase() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            weekdays.forEach {
                Box(Modifier.weight(1f).height(24.dp), contentAlignment = Alignment.Center) {
                    Text(it, style = LotteryType.TinyStrong, color = LotteryColors.Muted)
                }
            }
        }
        month.cells.chunked(7).forEach { week ->
            Row {
                (week + List(7 - week.size) { null }).forEach { day ->
                    DayCell(day, range, enabled = day != null && day >= earliest, onTap = onTap, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: CalendarDay?, range: DayRange, enabled: Boolean, onTap: (CalendarDay) -> Unit, modifier: Modifier) {
    val cell = modifier.height(38.dp)
    if (day == null) {
        Box(cell)
        return
    }
    val isStart = day == range.start
    val isEnd = day == range.end
    val inBand = range.isComplete && range.contains(day) && range.start != range.end
    val bandShape = when {
        isStart -> RoundedCornerShape(topStart = 19.dp, bottomStart = 19.dp)
        isEnd -> RoundedCornerShape(topEnd = 19.dp, bottomEnd = 19.dp)
        else -> RectangleShape
    }
    Box(
        cell
            .then(if (inBand) Modifier.clip(bandShape).background(LotteryColors.RangeFill) else Modifier)
            .clickable(enabled = enabled) { onTap(day) }
            .testTag(LotteryTags.day(day)),
        contentAlignment = Alignment.Center
    ) {
        if (isStart || isEnd) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(day.day.toString(), style = LotteryType.BodyStrong.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimary)
            }
        } else {
            Text(
                day.day.toString(),
                style = if (inBand) LotteryType.BodyStrong else LotteryType.Body,
                color = if (enabled) Color.White else LotteryColors.Muted.copy(alpha = 0.45f)
            )
        }
    }
}

/** "Opens / Apr 1 · 09:00" box; tapping it changes the time. */
@Composable
private fun TimeBox(label: String, value: String, enabled: Boolean, onClick: () -> Unit, tag: String, modifier: Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, LotteryColors.Line, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = LotteryType.Tiny, color = LotteryColors.Muted)
        Text(value, style = LotteryType.CardTitle)
    }
}

/** 24-hour clock dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(title: String, initialMinute: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initialMinute / 60, initialMinute = initialMinute % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LotteryColors.Sheet,
        title = { Text(title, style = LotteryType.Header) },
        text = { TimePicker(state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text(stringResource(R.string.lottery_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.lottery_cancel), color = LotteryColors.Muted) }
        }
    )
}

/** Design-time preview matching F11: Apr 1 09:00 → Apr 8 09:00. */
@Preview(showBackground = true, backgroundColor = 0xFF111A15, heightDp = 640)
@Composable
private fun RegistrationSheetPreview() {
    val apr1 = CalendarDay(2027, 4, 1)
    TrailsTheme {
        Box(Modifier.padding(24.dp)) {
            DateRangePickerContent(
                title = stringResource(R.string.lottery_registration_period),
                confirmLabel = stringResource(R.string.lottery_set_registration),
                initialStart = apr1.atMinute(9 * 60),
                initialEnd = CalendarDay(2027, 4, 8).atMinute(9 * 60),
                defaultStartMinute = 9 * 60,
                defaultEndMinute = 9 * 60,
                minDay = apr1,
                lockedStart = null,
                summary = { s, e -> stringResource(R.string.lottery_sheet_registration_summary, spanText(spanBetween(s, e))) },
                onConfirm = { _, _ -> },
                onDismiss = {}
            )
        }
    }
}
