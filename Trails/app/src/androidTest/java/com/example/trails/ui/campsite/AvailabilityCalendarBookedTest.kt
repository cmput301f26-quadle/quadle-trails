// UI (intent) tests for booked days on the availability calendar, US 02.06.01.
// OWNER: Ibrahim (ibr2309) — Campsite Management
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AvailabilityCalendarBookedTest {

    @get:Rule
    val composeRule = createComposeRule()

    // Same data as the Figma owner calendar: May 2027 weekends open, May 15–16 booked.
    private val weekends = listOf(1, 2, 8, 9, 15, 16, 22, 23, 29, 30).map { dayKey(2027, 5, it) }.toSet()
    private val booked = setOf(dayKey(2027, 5, 15), dayKey(2027, 5, 16))
    private val toggled = mutableListOf<Long>()

    private fun show(editable: Boolean, hint: String? = null, bookedDays: Set<Long> = booked) =
        composeRule.setContent {
            TrailsTheme {
                AvailabilityCalendar(
                    availableDays = weekends,
                    onToggleDay = if (editable) ({ toggled += it }) else null,
                    hint = hint,
                    bookedDays = bookedDays
                )
            }
        }

    private val isBooked = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Booked")

    // Booked days are marked booked; open days aren't.
    @Test
    fun bookedDays_areMarkedBooked() {
        show(editable = false)
        composeRule.onNodeWithTag(CalendarTags.day(dayKey(2027, 5, 15))).assert(isBooked)
        composeRule.onNodeWithTag(CalendarTags.day(dayKey(2027, 5, 16))).assert(isBooked)
        composeRule.onNodeWithTag(CalendarTags.day(dayKey(2027, 5, 22))).assert(!isBooked)
    }

    // The footer matches Figma: open days exclude booked ones, then the booked dates.
    @Test
    fun footer_showsOpenAndBooked() {
        show(editable = false)
        composeRule.onNodeWithTag(CalendarTags.FOOTER).assertTextEquals("8 open · 2 booked (May 15–16)")
    }

    // In the edit form, booked days are locked but open days can still be tapped.
    @Test
    fun editable_bookedDaysLocked_otherDaysToggle() {
        show(editable = true, hint = "Tap dates to mark them available")
        composeRule.onNodeWithTag(CalendarTags.day(dayKey(2027, 5, 15))).performClick()
        assertTrue(toggled.isEmpty())
        composeRule.onNodeWithTag(CalendarTags.day(dayKey(2027, 5, 22))).performClick()
        assertEquals(listOf(dayKey(2027, 5, 22)), toggled)
    }

    // The edit form shows the booked summary instead of the hint when the month has bookings.
    @Test
    fun editable_withBookings_footerShowsBookedSummary() {
        show(editable = true, hint = "Tap dates to mark them available")
        composeRule.onNodeWithTag(CalendarTags.FOOTER).assertTextEquals("8 open · 2 booked (May 15–16)")
    }

    // With no bookings the footer is unchanged.
    @Test
    fun noBookings_footerUnchanged() {
        show(editable = false, bookedDays = emptySet())
        composeRule.onNodeWithTag(CalendarTags.FOOTER).assertTextEquals("10 days open this month")
    }
}
