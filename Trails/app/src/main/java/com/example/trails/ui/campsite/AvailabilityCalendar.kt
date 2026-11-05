// Month calendar showing a campsite's available days (Figma "Availability").
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Editable in the Register/Edit form, read-only on the campsite page.
// Outstanding issues: booked days (red in Figma) need reservations, which aren't modelled yet.
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.trails.R
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import java.util.Locale

/**
 * A month grid with available days filled green and arrows to change month.
 *
 * @param availableDays the selected day keys (see [dayKey]).
 * @param onToggleDay called with a day key when it's tapped; null makes the calendar read-only.
 *  Past days can't be tapped.
 * @param hint footer text; null shows "N days open this month".
 */
@Composable
fun AvailabilityCalendar(
    availableDays: Set<Long>,
    modifier: Modifier = Modifier,
    onToggleDay: ((Long) -> Unit)? = null,
    hint: String? = null
) {
    val today = remember { todayKey() }
    val first = remember { availableDays.minOrNull()?.let(::monthOf) ?: monthOf(today) }
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
                        if (key != null) DayCell(key, key in availableDays, today, onToggleDay)
                    }
                }
            }
        }
        val openCount = cells.count { it != null && it in availableDays }
        Text(
            hint ?: pluralStringResource(R.plurals.campsite_calendar_open, openCount, openCount),
            style = MaterialTheme.typography.labelSmall,
            color = if (hint != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/** One day: green with dark text when available, dimmed when it's in the past and editable. */
@Composable
private fun DayCell(key: Long, isAvailable: Boolean, today: Long, onToggleDay: ((Long) -> Unit)?) {
    val canTap = onToggleDay != null && key >= today
    val textColour = when {
        isAvailable -> MaterialTheme.colorScheme.onPrimary
        onToggleDay != null && key < today -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isAvailable) MaterialTheme.colorScheme.primary else Color.Transparent)
            .then(if (canTap) Modifier.clickable { onToggleDay?.invoke(key) } else Modifier)
            .semantics { selected = isAvailable }
            .testTag(CalendarTags.day(key)),
        contentAlignment = Alignment.Center
    ) {
        Text(dayOfMonth(key).toString(), style = MaterialTheme.typography.bodySmall, color = textColour)
    }
}

/** A ‹ or › button for changing month. */
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

/** "May 2027" in the phone's language. */
private fun monthLabel(month: CalendarMonth): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(GregorianCalendar(month.year, month.month - 1, 1).time)

/** "S M T W T F S" in the phone's language, starting on Sunday. */
private fun weekdayInitials(): List<String> =
    DateFormatSymbols.getInstance(Locale.getDefault()).shortWeekdays.drop(1).map { it.take(1) }

/** Test tags for the calendar. */
object CalendarTags {
    const val MONTH = "calendar_month"
    const val PREV = "calendar_prev"
    const val NEXT = "calendar_next"

    /** Tag for one day cell. */
    fun day(key: Long) = "calendar_day_$key"
}
