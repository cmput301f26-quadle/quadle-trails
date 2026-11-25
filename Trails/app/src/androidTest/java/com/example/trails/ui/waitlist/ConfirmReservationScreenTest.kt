// Compose UI test suite validating ConfirmReservationScreen booking summary, rules check, and reservation confirmation (UI Controller tests).
// implements US 01.04.01, US 01.04.02, US 02.11.01
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
import androidx.navigation.compose.rememberNavController
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeReservationRepository
import com.example.trails.data.fake.FakeWaitlistRepository
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.ReservationStatus
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.model.WaitlistStatus
import com.example.trails.ui.campsite.dayKey
import com.example.trails.ui.theme.TrailsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test suite validating [ConfirmReservationScreen] rules confirmation and booking creation.
 */
class ConfirmReservationScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val campsiteId = "campsite-confirm-1"
    private val lotteryId = "lottery-confirm-1"
    private val startDay = dayKey(2027, 5, 22)
    private val endDay = dayKey(2027, 5, 24)

    private val sampleCampsite = Campsite(
        id = campsiteId,
        name = "Grande Cache Ridge",
        locationName = "Hinton – Grande Cache, AB",
        pricePerNight = 65.0,
        rules = "Quiet hours 10pm-7am. No generators.",
        cancellationPolicy = "Free cancellation up to 7 days before arrival."
    )

    @Test
    fun bookingSummaryAndRules_displayed() {
        val campsiteRepo = FakeCampsiteRepository(initial = listOf(sampleCampsite))
        val reservationRepo = FakeReservationRepository(initial = emptyList())
        val waitlistRepo = FakeWaitlistRepository(initial = emptyList())

        composeRule.setContent {
            TrailsTheme {
                ConfirmReservationScreen(
                    navController = rememberNavController(),
                    campsiteId = campsiteId,
                    lotteryId = lotteryId,
                    startDayKey = startDay,
                    endDayKey = endDay,
                    guests = 2,
                    campsiteRepository = campsiteRepo,
                    reservationRepository = reservationRepo,
                    waitlistRepository = waitlistRepo
                )
            }
        }

        composeRule.onNodeWithText("Confirm Reservation").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_booking_summary_card").assertIsDisplayed()
        composeRule.onNodeWithText("Grande Cache Ridge").assertIsDisplayed()
        composeRule.onNodeWithText("2 guests").assertIsDisplayed()
        composeRule.onNodeWithText("$195").assertIsDisplayed() // 65 * 3 nights (22, 23, 24)
        composeRule.onNodeWithTag("rules_checkbox").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_reservation_button").assertIsNotEnabled()
    }

    @Test
    fun checkboxAgreement_enablesButton_andSubmitsReservation() = runBlocking {
        val campsiteRepo = FakeCampsiteRepository(initial = listOf(sampleCampsite))
        val reservationRepo = FakeReservationRepository(initial = emptyList())
        val currentUserId = CurrentUser.id()
        val waitlistRepo = FakeWaitlistRepository(
            initial = listOf(
                WaitlistEntry(
                    userId = currentUserId,
                    lotteryId = lotteryId,
                    status = WaitlistStatus.WON,
                    rulesAccepted = true
                )
            )
        )

        composeRule.setContent {
            TrailsTheme {
                ConfirmReservationScreen(
                    navController = rememberNavController(),
                    campsiteId = campsiteId,
                    lotteryId = lotteryId,
                    startDayKey = startDay,
                    endDayKey = endDay,
                    guests = 2,
                    campsiteRepository = campsiteRepo,
                    reservationRepository = reservationRepo,
                    waitlistRepository = waitlistRepo
                )
            }
        }

        // initially disabled
        composeRule.onNodeWithTag("confirm_reservation_button").assertIsNotEnabled()

        // check rules agreement
        composeRule.onNodeWithTag("rules_checkbox").performClick()
        composeRule.onNodeWithTag("confirm_reservation_button").assertIsEnabled()

        // submit confirmation
        composeRule.onNodeWithTag("confirm_reservation_button").performClick()
        composeRule.waitForIdle()

        // verify reservation is created and active
        val reservations = reservationRepo.observeForCampsite(campsiteId).first()
        assertEquals(1, reservations.size)
        val res = reservations.first()
        assertEquals(campsiteId, res.campsiteId)
        assertEquals(currentUserId, res.userId)
        assertEquals(ReservationStatus.ACTIVE, res.status)
        assertEquals(2, res.partySize)
        assertEquals(195.0, res.totalPrice, 0.01)

        // verify waitlist entry status was updated to RESERVED
        val updatedEntry = waitlistRepo.getEntry(lotteryId, currentUserId)
        assertNotNull(updatedEntry)
        assertEquals(WaitlistStatus.RESERVED, updatedEntry?.status)
    }
}
