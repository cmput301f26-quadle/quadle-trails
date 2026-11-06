// Unit tests for the lottery date helpers: picker grid, range taps, spans and formatting.
// OWNER: Skanda (skandarajeev) — Lottery Setup
package com.example.trails.ui.lottery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class LotteryTimeTest {

    private val edmonton = TimeZone.getTimeZone("America/Edmonton")
    private val apr1 = CalendarDay(2027, 4, 1)
    private val apr8 = CalendarDay(2027, 4, 8)

    // Apr 1, 2027 is a Thursday, so the picker has four blanks before it (Figma F11).
    @Test
    fun april2027_startsOnThursdayWith30Days() {
        val april = CalendarMonth(2027, 4)
        assertEquals(4, april.firstWeekday)
        assertEquals(30, april.daysInMonth)
        assertEquals(34, april.cells.size)
        assertNull(april.cells[3])
        assertEquals(apr1, april.cells[4])
    }

    @Test
    fun february_handlesLeapYears() {
        assertEquals(29, CalendarMonth(2028, 2).daysInMonth)
        assertEquals(28, CalendarMonth(2027, 2).daysInMonth)
    }

    @Test
    fun nextAndPrevious_wrapAroundTheYear() {
        assertEquals(CalendarMonth(2028, 1), CalendarMonth(2027, 12).next())
        assertEquals(CalendarMonth(2026, 12), CalendarMonth(2027, 1).previous())
    }

    // First tap starts the range, second sets the end, a third starts over.
    @Test
    fun dayRange_tapsStartThenEndThenRestart() {
        val first = DayRange().tap(apr1)
        assertEquals(DayRange(apr1, null), first)
        val second = first.tap(apr8)
        assertEquals(DayRange(apr1, apr8), second)
        assertTrue(second.isComplete)
        assertEquals(DayRange(CalendarDay(2027, 4, 20), null), second.tap(CalendarDay(2027, 4, 20)))
    }

    @Test
    fun dayRange_tapBeforeStart_movesTheStart() {
        val moved = DayRange(apr8, null).tap(apr1)
        assertEquals(DayRange(apr1, null), moved)
    }

    @Test
    fun dayRange_containsDaysBetweenTheEnds() {
        val range = DayRange(apr1, apr8)
        assertTrue(range.contains(CalendarDay(2027, 4, 5)))
        assertTrue(range.contains(apr8))
        assertFalse(range.contains(CalendarDay(2027, 4, 9)))
    }

    // Apr 1 09:00 picked in Edmonton comes back as the same day and time.
    @Test
    fun atMinute_roundTripsDayAndTime() {
        val millis = apr1.atMinute(9 * 60, edmonton)
        assertEquals(apr1, CalendarDay.of(millis, edmonton))
        assertEquals(540, minuteOfDay(millis, edmonton))
    }

    // Figma F14: Apr 8 09:00 to Apr 10 18:00 gives winners 2 days 9 hours to book.
    @Test
    fun spanBetween_daysAndLeftoverHours() {
        val start = apr8.atMinute(9 * 60, edmonton)
        val end = CalendarDay(2027, 4, 10).atMinute(18 * 60, edmonton)
        assertEquals(DaysHours(2, 9), spanBetween(start, end))
        assertEquals(DaysHours(0, 0), spanBetween(end, start))
    }

    @Test
    fun calendarDaysUntil_countsCalendarDaysNotHours() {
        val now = CalendarDay(2027, 3, 20).atMinute(23 * 60, edmonton)
        val opens = apr1.atMinute(9 * 60, edmonton)
        assertEquals(12, calendarDaysUntil(opens, now, edmonton))
    }

    @Test
    fun formatters_matchTheMockups() {
        val opens = apr1.atMinute(9 * 60, edmonton)
        val closes = apr8.atMinute(9 * 60, edmonton)
        assertEquals("Apr 1, 09:00", formatDateTime(opens, edmonton, Locale.US))
        assertEquals("Apr 1 · 09:00", formatPickerDateTime(opens, edmonton, Locale.US))
        assertEquals("Apr 1 – 8", formatDayRange(opens, closes, edmonton, Locale.US))
        assertEquals("Apr 2027", formatMonthYear(opens, edmonton, Locale.US))
        assertEquals("April 2027", formatMonthTitle(CalendarMonth(2027, 4), Locale.US))
    }

    @Test
    fun formatDayRange_acrossMonths_namesBothMonths() {
        val start = CalendarDay(2027, 4, 28).atMinute(9 * 60, edmonton)
        val end = CalendarDay(2027, 5, 2).atMinute(9 * 60, edmonton)
        assertEquals("Apr 28 – May 2", formatDayRange(start, end, edmonton, Locale.US))
    }
}
