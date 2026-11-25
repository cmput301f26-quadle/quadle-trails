// Date logic for the availability calendar (US 02.01.02, 02.06.01 / Figma "Availability").
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Days are "day keys": whole days since 1970-01-01, so they're timezone-free and easy to
// compare. Plain Kotlin without java.time, because minSdk is 24.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import com.example.trails.data.model.DateRange
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus
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
fun availabilityToDays(ranges: List<DateRange>): Set<Long> =
    ranges.flatMap { daysBetween(it.start, it.end) }.toSet()

/**
 * Every day from [start] to [end], both included; empty if either is missing or [end] is
 * before [start].
 *
 * Written in assistance with Claude AI by Anthropic.
 */
private fun daysBetween(start: Timestamp?, end: Timestamp?): List<Long> {
    val first = start?.toDayKey()
    val last = end?.toDayKey()
    return if (first == null || last == null || last < first) emptyList() else (first..last).toList()
}

/**
 * The days taken by reservations (US 02.06.01): the red days on the calendar. Check-in and
 * check-out days both count, as in Figma, where a May 15–16 stay shows both days red.
 * Only [ReservationStatus.ACTIVE] reservations count, so a cancelled or rejected stay frees
 * its days. Reservations missing a date, or ending before they start, are skipped.
 *
 * Shared: also usable for a winner picking dates (US 01.04.01) with [openDayKeys].
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param reservations any reservations, e.g. from ReservationRepository.observeForCampsite;
 *   non-active ones are ignored.
 * @param campsiteId if set, only reservations for this campsite count.
 * @return the booked day keys (see [dayKey]).
 */
fun bookedDayKeys(reservations: List<Reservation>, campsiteId: String? = null): Set<Long> =
    reservations
        .filter { it.status == ReservationStatus.ACTIVE }
        .filter { campsiteId == null || it.campsiteId == campsiteId }
        .flatMap { daysBetween(it.startDate, it.endDate) }
        .toSet()

/**
 * Days that can still be booked: available and not already booked.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param available the campsite's available days, e.g. from [availabilityToDays].
 * @param booked the booked days, e.g. from [bookedDayKeys].
 */
fun openDayKeys(available: Set<Long>, booked: Set<Long>): Set<Long> = available - booked

/**
 * Groups days into runs of consecutive days, in order: {15, 16, 20} becomes 15..16 and 20..20.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
fun consecutiveRuns(days: Collection<Long>): List<LongRange> {
    val runs = mutableListOf<LongRange>()
    for (day in days.toSortedSet()) {
        val last = runs.lastOrNull()
        if (last != null && day == last.last + 1) runs[runs.size - 1] = last.first..day
        else runs += day..day
    }
    return runs
}

/**
 * Labels runs of days within one month, e.g. "May 15–16, May 20" (Figma's booked footer).
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param runs from [consecutiveRuns].
 * @param monthDay formats one day, e.g. "May 15".
 */
fun formatDayRuns(runs: List<LongRange>, monthDay: (Long) -> String): String =
    runs.joinToString(", ") { run ->
        if (run.first == run.last) monthDay(run.first) else "${monthDay(run.first)}–${dayOfMonth(run.last)}"
    }

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
