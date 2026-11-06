// Unit tests for the availability calendar's date logic.
// OWNER: Ibrahim (ibr2309) — Campsite Management
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import com.example.trails.data.fake.dateOf
import com.example.trails.data.model.DateRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvailabilityDaysTest {

    private val may7 = dayKey(2027, 5, 7)

    @Test
    fun dayKey_countsDaysSince1970() {
        assertEquals(0L, dayKey(1970, 1, 1))
        assertEquals(1L, dayKey(1970, 1, 2))
    }

    @Test
    fun monthOfAndDayOfMonth_roundTrip() {
        assertEquals(CalendarMonth(2027, 5), monthOf(may7))
        assertEquals(7, dayOfMonth(may7))
    }

    @Test
    fun calendarMonth_may2027_startsSaturdayWith31Days() {
        val may = CalendarMonth(2027, 5)
        assertEquals(31, may.daysInMonth)
        assertEquals(6, may.firstWeekday)
    }

    @Test
    fun calendarMonth_leapFebruary_29Days() {
        assertEquals(29, CalendarMonth(2028, 2).daysInMonth)
        assertEquals(28, CalendarMonth(2027, 2).daysInMonth)
    }

    @Test
    fun calendarMonth_plus_rollsOverYears() {
        assertEquals(CalendarMonth(2028, 1), CalendarMonth(2027, 12).plus(1))
        assertEquals(CalendarMonth(2026, 12), CalendarMonth(2027, 1).plus(-1))
    }

    @Test
    fun monthCells_blanksBeforeFirstThenEveryDay() {
        val cells = monthCells(CalendarMonth(2027, 5))
        assertEquals(6 + 31, cells.size)
        assertTrue(cells.take(6).all { it == null })
        assertEquals(dayKey(2027, 5, 1), cells[6])
    }

    @Test
    fun toggleDay_addsThenRemoves() {
        val added = toggleDay(emptySet(), may7)
        assertEquals(setOf(may7), added)
        assertEquals(emptySet<Long>(), toggleDay(added, may7))
    }

    @Test
    fun availabilityToDays_includesBothEnds() {
        val weekend = DateRange(dateOf(2027, 5, 7), dateOf(2027, 5, 9))
        assertEquals(setOf(may7, may7 + 1, may7 + 2), availabilityToDays(listOf(weekend)))
    }

    @Test
    fun availabilityToDays_skipsIncompleteRanges() {
        assertEquals(emptySet<Long>(), availabilityToDays(listOf(DateRange(start = dateOf(2027, 5, 7), end = null))))
    }

    @Test
    fun daysToAvailability_mergesConsecutiveDays() {
        val days = setOf(may7, may7 + 1, may7 + 2, may7 + 7)
        val ranges = daysToAvailability(days)
        assertEquals(2, ranges.size)
        assertEquals(days, availabilityToDays(ranges))
    }

    @Test
    fun daysToAvailability_empty_noRanges() {
        assertTrue(daysToAvailability(emptySet()).isEmpty())
    }
}
