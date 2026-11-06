// Date logic for the availability calendar (US 02.01.02 / Figma "Availability").
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Days are "day keys": whole days since 1970-01-01, so they're timezone-free and easy to
// compare. Plain Kotlin without java.time, because minSdk is 24.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import com.example.trails.data.model.DateRange
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

private const val DAY_MS = 86_400_000L

private fun utcCalendar(): Calendar = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply { clear() }

/**
 * Day key for a calendar date; [month] is 1-based (5 = May).
 *
 * Written in assistance with Claude AI by Anthropic.
 */
fun dayKey(year: Int, month: Int, day: Int): Long =
    utcCalendar().apply { set(year, month - 1, day) }.timeInMillis / DAY_MS

/**
 * Today's day key, in the phone's timezone.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
fun todayKey(): Long = Calendar.getInstance().let {
    dayKey(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH))
}

/**
 * One month of the calendar.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @property year e.g. 2027.
 * @property month 1-based (5 = May).
 */
data class CalendarMonth(val year: Int, val month: Int) {

    /** Number of days in this month (handles leap years). */
    val daysInMonth: Int
        get() = utcCalendar().apply { set(year, month - 1, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)

    /** Weekday of the 1st: 0 = Sunday … 6 = Saturday. */
    val firstWeekday: Int
        get() = utcCalendar().apply { set(year, month - 1, 1) }.get(Calendar.DAY_OF_WEEK) - 1

    /** The month [months] later (negative for earlier), rolling over years. */
    fun plus(months: Int): CalendarMonth {
        val index = year * 12 + (month - 1) + months
        return CalendarMonth(index.floorDiv(12), index.mod(12) + 1)
    }
}

/**
 * The month a day key falls in.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
fun monthOf(key: Long): CalendarMonth = utcCalendar().apply { timeInMillis = key * DAY_MS }
    .let { CalendarMonth(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1) }

/**
 * Day of the month (1–31) for a day key.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
fun dayOfMonth(key: Long): Int = utcCalendar().apply { timeInMillis = key * DAY_MS }.get(Calendar.DAY_OF_MONTH)

/**
 * Cells for a month grid starting on Sunday: null for blanks before the 1st, then each day.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @return firstWeekday nulls followed by one day key per day.
 */
fun monthCells(month: CalendarMonth): List<Long?> =
    List(month.firstWeekday) { null } + (1..month.daysInMonth).map { dayKey(month.year, month.month, it) }

/**
 * Adds [key] if it isn't selected, removes it if it is.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
fun toggleDay(days: Set<Long>, key: Long): Set<Long> = if (key in days) days - key else days + key

/**
 * The day key of a stored timestamp, read in the phone's timezone.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
fun Timestamp.toDayKey(): Long = Calendar.getInstance().apply { time = toDate() }.let {
    dayKey(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH))
}

/**
 * Midnight (phone's timezone) on the day of [key], for storing in a [DateRange].
 *
 * Written in assistance with Claude AI by Anthropic.
 */
fun dayKeyToTimestamp(key: Long): Timestamp {
    val m = monthOf(key)
    return Timestamp(GregorianCalendar(m.year, m.month - 1, dayOfMonth(key)).time)
}

/**
 * Expands stored ranges into individual days. Both ends are included; ranges missing an
 * end, or ending before they start, are skipped.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
fun availabilityToDays(ranges: List<DateRange>): Set<Long> = ranges.flatMap { range ->
    val start = range.start?.toDayKey()
    val end = range.end?.toDayKey()
    if (start == null || end == null || end < start) emptyList() else (start..end).toList()
}.toSet()

/**
 * Merges selected days into as few ranges as possible, e.g. May 7, 8, 9 and 14 become
 * May 7–9 and May 14–14.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
fun daysToAvailability(days: Set<Long>): List<DateRange> {
    val ranges = mutableListOf<DateRange>()
    var start: Long? = null
    var previous: Long? = null
    for (day in days.sorted()) {
        if (start == null) {
            start = day
        } else if (day != previous!! + 1) {
            ranges += DateRange(dayKeyToTimestamp(start), dayKeyToTimestamp(previous))
            start = day
        }
        previous = day
    }
    if (start != null) ranges += DateRange(dayKeyToTimestamp(start), dayKeyToTimestamp(previous!!))
    return ranges
}
