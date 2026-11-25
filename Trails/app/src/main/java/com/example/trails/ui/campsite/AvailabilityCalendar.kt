// Month calendar showing a campsite's available days (Figma "Availability").
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Editable in the Register/Edit form, read-only on the campsite page. Booked days are red and
// locked (US 02.06.01).
// Outstanding issues: none.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.trails.R
import com.example.trails.ui.theme.Ember
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import java.util.Locale

/**
 * A month grid with available days filled green, booked days filled red (US 02.06.01), and
 * arrows to change month.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param availableDays the selected day keys (see [dayKey]).
 * @param onToggleDay called with a day key when it's tapped; null makes the calendar read-only.
 *  Past days and booked days can't be tapped.
 * @param hint footer text; null shows "N days open this month". A month with booked days
 *  always shows "N open · M booked (May 15–16)" instead, as in Figma.
 * @param bookedDays day keys taken by reservations, e.g. from [bookedDayKeys].
 */
@Composable
fun AvailabilityCalendar(
    availableDays: Set<Long>,
    modifier: Modifier = Modifier,
    onToggleDay: ((Long) -> Unit)? = null,
    hint: String? = null,
    bookedDays: Set<Long> = emptySet()
) {
    val today = remember { todayKey() }
    val first = remember { (availableDays + bookedDays).minOrNull()?.let(::monthOf) ?: monthOf(today) }
    var year by rememberSaveable { mutableIntStateOf(first.year) }
    var month by rememberSaveable { mutableIntStateOf(first.month) }
    val shown = CalendarMonth(year, month)
    val cells = monthCells(shown)
    val show: (CalendarMonth) -> Unit = { year = it.year; month = it.month }
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonthArrow("‹", stringResource(R.string.campsite_calendar_prev), CalendarTags.PREV) { show(shown.plus(-1)) }
            Text(
                monthLabel(shown),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .testTag(CalendarTags.MONTH)
            )
            MonthArrow("›", stringResource(R.string.campsite_calendar_next), CalendarTags.NEXT) { show(shown.plus(1)) }
        }
        Spacer(Modifier.height(8.dp))
        Row {
            weekdayInitials().forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        cells.chunked(7).forEach { week ->
            Row {
                for (i in 0 until 7) {
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(3.dp)
                    ) {
                        val key = week.getOrNull(i)
                        if (key != null) DayCell(key, key in availableDays, key in bookedDays, today, onToggleDay)
                    }
                }
            }
        }
        val monthDays = cells.filterNotNull()
        val booked = monthDays.filter { it in bookedDays }
        val openCount = monthDays.count { it in availableDays && it !in bookedDays }
        val showHint = hint != null && booked.isEmpty()
        Text(
            when {
                booked.isNotEmpty() -> stringResource(
                    R.string.campsite_calendar_open_booked,
                    openCount,
                    booked.size,
                    formatDayRuns(consecutiveRuns(booked), ::monthDayLabel)
                )
                hint != null -> hint
                else -> pluralStringResource(R.plurals.campsite_calendar_open, openCount, openCount)
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (showHint) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag(CalendarTags.FOOTER)
        )
    }
}

/**
 * One day: red with dark text when booked (locked), green with dark text when available,
 * dimmed when it's in the past and editable.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun DayCell(key: Long, isAvailable: Boolean, isBooked: Boolean, today: Long, onToggleDay: ((Long) -> Unit)?) {
    val canTap = onToggleDay != null && key >= today && !isBooked
    val bookedState = stringResource(R.string.campsite_calendar_booked_state)
    val background = when {
        isBooked -> Ember
        isAvailable -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val textColour = when {
        isBooked || isAvailable -> MaterialTheme.colorScheme.onPrimary
        onToggleDay != null && key < today -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .then(if (canTap) Modifier.clickable { onToggleDay?.invoke(key) } else Modifier)
            .semantics {
                selected = isAvailable
                if (isBooked) stateDescription = bookedState
            }
            .testTag(CalendarTags.day(key)),
        contentAlignment = Alignment.Center
    ) {
        Text(dayOfMonth(key).toString(), style = MaterialTheme.typography.bodySmall, color = textColour)
    }
}

/**
 * A ‹ or › button for changing month.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
@Composable
private fun MonthArrow(glyph: String, description: String, tag: String, onClick: () -> Unit) {
    Text(
        glyph,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .testTag(tag)
    )
}

/**
 * "May 2027" in the phone's language.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
private fun monthLabel(month: CalendarMonth): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(GregorianCalendar(month.year, month.month - 1, 1).time)

/**
 * "May 15" in the phone's language, for the booked footer.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
private fun monthDayLabel(key: Long): String {
    val m = monthOf(key)
    return SimpleDateFormat("MMM d", Locale.getDefault()).format(GregorianCalendar(m.year, m.month - 1, dayOfMonth(key)).time)
}

/**
 * "S M T W T F S" in the phone's language, starting on Sunday.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
private fun weekdayInitials(): List<String> =
    DateFormatSymbols.getInstance(Locale.getDefault()).shortWeekdays.drop(1).map { it.take(1) }

/**
 * Test tags for the calendar.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
object CalendarTags {
    const val MONTH = "calendar_month"
    const val PREV = "calendar_prev"
    const val NEXT = "calendar_next"
    const val FOOTER = "calendar_footer"

    /** Tag for one day cell. */
    fun day(key: Long) = "calendar_day_$key"
}
