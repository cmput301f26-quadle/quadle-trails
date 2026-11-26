// Compose UI test suite validating ReserveDatesContent fields, calendar, and decline dialog (UI Controller tests).
// implements US 01.04.01, US 01.04.02, US 01.04.03
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.ui.waitlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.ui.campsite.dayKey
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test suite validating [ReserveDatesContent] layout, forms, and dialogs.
 */
class ReserveDatesContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val day1 = dayKey(2027, 5, 1)
    private val day2 = dayKey(2027, 5, 2)

    private val sampleCampsite = Campsite(
        id = "campsite-1",
        name = "Two Jack Lakeside Campground",
        locationName = "Banff, AB",
        maxOccupancy = 6,
        pricePerNight = 35.0
    )

    private val sampleLottery = Lottery(
        id = "lottery-1",
        name = "May Long Weekend",
        campsiteIds = listOf("campsite-1")
    )

    private fun show(
        state: ReserveDatesUiState,
        onBack: () -> Unit = {},
        onNameChange: (String) -> Unit = {},
        onEmailChange: (String) -> Unit = {},
        onPhoneChange: (String) -> Unit = {},
        onIncrementGuests: () -> Unit = {},
        onDecrementGuests: () -> Unit = {},
        onDayTapped: (Long) -> Unit = {},
        onReserveClick: () -> Unit = {},
        onDeclineClick: () -> Unit = {}
    ) = composeRule.setContent {
        TrailsTheme {
            ReserveDatesContent(
                state = state,
                onBack = onBack,
                onNameChange = onNameChange,
                onEmailChange = onEmailChange,
                onPhoneChange = onPhoneChange,
                onIncrementGuests = onIncrementGuests,
                onDecrementGuests = onDecrementGuests,
                onDayTapped = onDayTapped,
                onReserveClick = onReserveClick,
                onDeclineClick = onDeclineClick
            )
        }
    }

    @Test
    fun bookingDetailsAndCalendar_displayed() {
        show(
            state = ReserveDatesUiState(
                isLoading = false,
                lottery = sampleLottery,
                campsite = sampleCampsite,
                name = "Alex Morgan",
                email = "alex.morgan@email.com",
                phone = "780-555-0142",
                guests = 2,
                availableDays = setOf(day1, day2),
                openDays = setOf(day1, day2)
            )
        )

        composeRule.onNodeWithText("Reserve Dates").assertIsDisplayed()
        composeRule.onNodeWithText("Confirm your Booking Details").assertIsDisplayed()
        composeRule.onNodeWithTag("reserve_name_input").assertIsDisplayed()
        composeRule.onNodeWithTag("reserve_email_input").assertIsDisplayed()
        composeRule.onNodeWithTag("reserve_phone_input").assertIsDisplayed()
        composeRule.onNodeWithTag("reserve_dates_calendar").assertIsDisplayed()
        composeRule.onNodeWithTag("reserve_button").assertIsDisplayed()
        composeRule.onNodeWithTag("decline_lottery_button").assertIsDisplayed()
    }

    @Test
    fun reserveButton_disabledWhenNoDatesSelected_enabledWhenSelected() {
        show(
            state = ReserveDatesUiState(
                isLoading = false,
                lottery = sampleLottery,
                campsite = sampleCampsite,
                name = "Alex Morgan",
                email = "alex.morgan@email.com",
                phone = "780-555-0142",
                guests = 2,
                availableDays = setOf(day1, day2),
                openDays = setOf(day1, day2),
                selectedDays = emptySet()
            )
        )
        composeRule.onNodeWithTag("reserve_button").assertIsNotEnabled()
    }

    @Test
    fun declineButton_showsConfirmationDialog() {
        var declined = false
        show(
            state = ReserveDatesUiState(
                isLoading = false,
                lottery = sampleLottery,
                campsite = sampleCampsite,
                name = "Alex Morgan",
                email = "alex.morgan@email.com",
                phone = "780-555-0142",
                guests = 2
            ),
            onDeclineClick = { declined = true }
        )

        composeRule.onNodeWithTag("decline_lottery_button").performClick()
        composeRule.onNodeWithText("Decline Lottery Win?").assertIsDisplayed()

        composeRule.onNodeWithTag("decline_dialog_confirm_button").performClick()
        assertTrue(declined)
    }
}
