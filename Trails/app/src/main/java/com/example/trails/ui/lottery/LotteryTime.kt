// Date and time helpers for the lottery screens (US 02.04.03, 02.04.04).
// OWNER: Skanda (skandarajeev) — Lottery Setup
// Plain Kotlin without java.time (minSdk is 24), so it is unit-tested directly.
// Every function takes the time zone and locale so tests don't depend on the laptop's settings.
package com.example.trails.ui.lottery

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

private const val DAY_MS = 86_400_000L
private const val HOUR_MS = 3_600_000L

/**
 * A date on the calendar, with no time of day.
 *
 * @property year e.g. 2027.
 * @property month 1-based (4 = April).
 * @property day day of the month.
 */
data class CalendarDay(val year: Int, val month: Int, val day: Int) : Comparable<CalendarDay> {

    /** Whole days since 1970-01-01, so days can be compared and subtracted. */
    val epochDay: Long
        get() = utc().apply { set(year, month - 1, day) }.timeInMillis / DAY_MS

    override fun compareTo(other: CalendarDay): Int = epochDay.compareTo(other.epochDay)

    /** Milliseconds for this day at [minuteOfDay] (e.g. 9 * 60 for 09:00) in [zone]. */
    fun atMinute(minuteOfDay: Int, zone: TimeZone = TimeZone.getDefault()): Long =
        GregorianCalendar(zone).apply {
            clear()
            set(year, month - 1, day, minuteOfDay / 60, minuteOfDay % 60)
        }.timeInMillis

    companion object {
        /** The calendar day [millis] falls on in [zone]. */
        fun of(millis: Long, zone: TimeZone = TimeZone.getDefault()): CalendarDay {
            val cal = GregorianCalendar(zone).apply { timeInMillis = millis }
            return CalendarDay(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        }
    }
}

private fun utc(): Calendar = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply { clear() }

/** Minutes since midnight for [millis] in [zone], e.g. 540 for 09:00. */
fun minuteOfDay(millis: Long, zone: TimeZone = TimeZone.getDefault()): Int {
    val cal = GregorianCalendar(zone).apply { timeInMillis = millis }
    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
}

/**
 * One month of the date picker.
 *
 * @property year e.g. 2027.
 * @property month 1-based (4 = April).
 */
data class CalendarMonth(val year: Int, val month: Int) {

    /** Number of days in this month (handles leap years). */
    val daysInMonth: Int
        get() = utc().apply { set(year, month - 1, 1) }.getActualMaximum(Calendar.DAY_OF_MONTH)

    /** Weekday of the 1st: 0 = Sunday … 6 = Saturday. */
    val firstWeekday: Int
        get() = utc().apply { set(year, month - 1, 1) }.get(Calendar.DAY_OF_WEEK) - 1

    /** Grid cells, Sunday first: null for the blanks before the 1st, then each day. */
    val cells: List<CalendarDay?>
        get() = List(firstWeekday) { null } + (1..daysInMonth).map { CalendarDay(year, month, it) }

    fun next(): CalendarMonth = if (month == 12) CalendarMonth(year + 1, 1) else CalendarMonth(year, month + 1)

    fun previous(): CalendarMonth = if (month == 1) CalendarMonth(year - 1, 12) else CalendarMonth(year, month - 1)

    companion object {
        fun of(day: CalendarDay) = CalendarMonth(day.year, day.month)
    }
}

/**
 * What the owner has tapped in the date range picker so far.
 *
 * @property start first day tapped, or null.
 * @property end second day tapped (never before [start]), or null.
 */
data class DayRange(val start: CalendarDay? = null, val end: CalendarDay? = null) {

    /**
     * The selection after tapping [day]: the first tap (or a tap after a full range) starts a new
     * range, a tap before the start moves the start, and any other tap sets the end.
     */
    fun tap(day: CalendarDay): DayRange = when {
        start == null || end != null -> DayRange(day, null)
        day < start -> DayRange(day, null)
        else -> copy(end = day)
    }

    /** True if [day] is on or between the two ends. */
    fun contains(day: CalendarDay): Boolean {
        val s = start ?: return false
        val e = end ?: return day == s
        return day >= s && day <= e
    }

    val isComplete: Boolean get() = start != null && end != null
}

/**
 * A length of time in whole days and hours, e.g. "2 days 9 h".
 *
 * @property days whole days.
 * @property hours leftover hours, 0–23.
 */
data class DaysHours(val days: Int, val hours: Int)

/** The time between [start] and [end], rounded down to the hour; negative spans become zero. */
fun spanBetween(start: Long, end: Long): DaysHours {
    val totalHours = ((end - start).coerceAtLeast(0) / HOUR_MS).toInt()
    return DaysHours(totalHours / 24, totalHours % 24)
}

/** Calendar days from [now] until [target], e.g. 12 for "opens in 12 days"; 0 means today. */
fun calendarDaysUntil(target: Long, now: Long, zone: TimeZone = TimeZone.getDefault()): Long =
    CalendarDay.of(target, zone).epochDay - CalendarDay.of(now, zone).epochDay

/** Milliseconds for a Firestore timestamp, or null when it isn't set. */
fun Timestamp?.millis(): Long? = this?.toDate()?.time

/** A Firestore timestamp for [millis]. */
fun timestampOf(millis: Long): Timestamp = Timestamp(Date(millis))

private fun format(pattern: String, millis: Long, zone: TimeZone, locale: Locale): String =
    SimpleDateFormat(pattern, locale).apply { timeZone = zone }.format(Date(millis))

/** "Apr 1, 09:00" — used on every summary row. */
fun formatDateTime(millis: Long, zone: TimeZone = TimeZone.getDefault(), locale: Locale = Locale.getDefault()) =
    format("MMM d, HH:mm", millis, zone, locale)

/** "Apr 1 · 09:00" — the Opens / Closes boxes in the date picker. */
fun formatPickerDateTime(millis: Long, zone: TimeZone = TimeZone.getDefault(), locale: Locale = Locale.getDefault()) =
    format("MMM d · HH:mm", millis, zone, locale)

/** "Apr 1". */
fun formatDay(millis: Long, zone: TimeZone = TimeZone.getDefault(), locale: Locale = Locale.getDefault()) =
    format("MMM d", millis, zone, locale)

/** "09:00". */
fun formatTime(millis: Long, zone: TimeZone = TimeZone.getDefault(), locale: Locale = Locale.getDefault()) =
    format("HH:mm", millis, zone, locale)

/** "Mar 2027". */
fun formatMonthYear(millis: Long, zone: TimeZone = TimeZone.getDefault(), locale: Locale = Locale.getDefault()) =
    format("MMM yyyy", millis, zone, locale)

/** "April 2027" — the date picker's month title. */
fun formatMonthTitle(month: CalendarMonth, locale: Locale = Locale.getDefault()): String {
    val zone = TimeZone.getTimeZone("UTC")
    return format("LLLL yyyy", CalendarDay(month.year, month.month, 1).atMinute(0, zone), zone, locale)
}

/** "Apr 1 – 8" within a month, "Apr 28 – May 2" across months — the timeline's short ranges. */
fun formatDayRange(start: Long, end: Long, zone: TimeZone = TimeZone.getDefault(), locale: Locale = Locale.getDefault()): String {
    val sameMonth = CalendarDay.of(start, zone).let { s -> CalendarDay.of(end, zone).let { e -> s.year == e.year && s.month == e.month } }
    val endText = if (sameMonth) format("d", end, zone, locale) else formatDay(end, zone, locale)
    return "${formatDay(start, zone, locale)} – $endText"
}
