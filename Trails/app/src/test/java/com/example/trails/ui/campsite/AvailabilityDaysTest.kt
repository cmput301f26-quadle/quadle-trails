// Unit tests for the availability calendar's date logic (US 02.01.02, 02.06.01).
// OWNER: Ibrahim (ibr2309) — Campsite Management
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import com.example.trails.data.fake.dateOf
import com.example.trails.data.model.DateRange
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus
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

    // US 02.06.01: booked days

    private fun stay(campsiteId: String, fromDay: Int, toDay: Int, month: Int = 5) = Reservation(
        campsiteId = campsiteId,
        startDate = dateOf(2027, month, fromDay),
        endDate = dateOf(2027, month, toDay)
    )

    // A May 15–16 stay books both days, as in Figma.
    @Test
    fun bookedDayKeys_includesCheckInAndCheckOut() {
        assertEquals(
            setOf(dayKey(2027, 5, 15), dayKey(2027, 5, 16)),
            bookedDayKeys(listOf(stay("c1", 15, 16)))
        )
    }

    // A one-night stay on a single day books just that day.
    @Test
    fun bookedDayKeys_sameDay_oneDay() {
        assertEquals(setOf(dayKey(2027, 5, 15)), bookedDayKeys(listOf(stay("c1", 15, 15))))
    }

    // Only the given campsite's reservations count.
    @Test
    fun bookedDayKeys_filtersByCampsite() {
        val booked = bookedDayKeys(listOf(stay("c1", 15, 16), stay("c2", 20, 21)), campsiteId = "c1")
        assertEquals(setOf(dayKey(2027, 5, 15), dayKey(2027, 5, 16)), booked)
    }

    // A stay across a month end books days in both months.
    @Test
    fun bookedDayKeys_acrossMonthEnd() {
        val r = Reservation(campsiteId = "c1", startDate = dateOf(2027, 5, 31), endDate = dateOf(2027, 6, 1))
        assertEquals(setOf(dayKey(2027, 5, 31), dayKey(2027, 6, 1)), bookedDayKeys(listOf(r)))
    }

    // Reservations missing a date, or ending before they start, are skipped.
    @Test
    fun bookedDayKeys_skipsIncompleteOrBackwards() {
        val noEnd = Reservation(campsiteId = "c1", startDate = dateOf(2027, 5, 15))
        val backwards = stay("c1", 16, 15)
        assertTrue(bookedDayKeys(listOf(noEnd, backwards)).isEmpty())
    }

    // A cancelled or rejected stay frees its days; only active ones are booked.
    @Test
    fun bookedDayKeys_ignoresCancelledAndRejected() {
        val active = stay("c1", 15, 16)
        val cancelled = stay("c1", 20, 21).copy(status = ReservationStatus.CANCELLED)
        val rejected = stay("c1", 25, 25).copy(status = ReservationStatus.REJECTED)
        assertEquals(
            setOf(dayKey(2027, 5, 15), dayKey(2027, 5, 16)),
            bookedDayKeys(listOf(active, cancelled, rejected))
        )
    }

    // Open days are available days that aren't booked.
    @Test
    fun openDayKeys_removesBooked() {
        val available = setOf(dayKey(2027, 5, 15), dayKey(2027, 5, 16), dayKey(2027, 5, 22))
        assertEquals(setOf(dayKey(2027, 5, 22)), openDayKeys(available, setOf(dayKey(2027, 5, 15), dayKey(2027, 5, 16))))
    }

    // Days are grouped into consecutive runs, in order.
    @Test
    fun consecutiveRuns_groupsAdjacentDays() {
        val runs = consecutiveRuns(listOf(dayKey(2027, 5, 20), dayKey(2027, 5, 15), dayKey(2027, 5, 16)))
        assertEquals(listOf(dayKey(2027, 5, 15)..dayKey(2027, 5, 16), dayKey(2027, 5, 20)..dayKey(2027, 5, 20)), runs)
    }

    @Test
    fun consecutiveRuns_empty_noRuns() {
        assertTrue(consecutiveRuns(emptyList()).isEmpty())
    }

    // Runs are labelled like Figma's footer: "May 15–16", and single days on their own.
    @Test
    fun formatDayRuns_matchesFigmaLabel() {
        val label: (Long) -> String = { "May ${dayOfMonth(it)}" }
        val runs = consecutiveRuns(listOf(dayKey(2027, 5, 15), dayKey(2027, 5, 16), dayKey(2027, 5, 20)))
        assertEquals("May 15–16, May 20", formatDayRuns(runs, label))
    }
}
