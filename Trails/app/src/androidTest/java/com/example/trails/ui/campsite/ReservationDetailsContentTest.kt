// UI (intent) tests for Reservation details and rejecting, US 02.08.01.
// OWNER: Ibrahim (ibr2309) — Campsite Management
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.User
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReservationDetailsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val stay = Reservation(
        id = "qr1048", userId = "priya", campsiteId = "lake", partySize = 2, totalPrice = 110.0,
        startDate = dayKeyToTimestamp(dayKey(2027, 6, 5)), endDate = dayKeyToTimestamp(dayKey(2027, 6, 7))
    )
    private val lake = Campsite(id = "lake", name = "Two Jack Lakeside", locationName = "Banff National Park, AB")
    private val priya = User(deviceId = "priya", name = "Priya Shah")

    private var rejects = 0
    private var confirms = 0
    private var dismissed = 0
    private var messaged: Reservation? = null

    private fun show(filter: ReservationFilter = ReservationFilter.ACTIVE, reject: RejectState = RejectState.Idle) =
        composeRule.setContent {
            TrailsTheme {
                ReservationDetailsContent(
                    state = ReservationDetailsUiState.Loaded(stay, lake, priya, filter),
                    rejectState = reject,
                    onBack = {},
                    onReject = { rejects++ },
                    onConfirmReject = { confirms++ },
                    onDismissDialog = { dismissed++ },
                    onMessage = { messaged = it }
                )
            }
        }

    // The details match Figma: status, campsite, dates and nights, guests, total, entrant.
    @Test
    fun showsFigmaDetails() {
        show()
        composeRule.onNodeWithTag(DetailsTags.STATUS).assertTextEquals("CONFIRMED")
        composeRule.onNodeWithText("Two Jack Lakeside").assertIsDisplayed()
        composeRule.onNodeWithText("Banff National Park, AB").assertIsDisplayed()
        composeRule.onNodeWithText("Jun 5 – 7, 2027 · 2 nights").assertIsDisplayed()
        composeRule.onNodeWithText("2 guests").assertIsDisplayed()
        composeRule.onNodeWithText("$110").assertIsDisplayed()
        composeRule.onNodeWithText("Priya Shah").performScrollTo().assertIsDisplayed()
    }

    // "Reject reservation" asks first.
    @Test
    fun rejectButton_reportsClick() {
        show()
        composeRule.onNodeWithTag(DetailsTags.REJECT).performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, rejects)
    }

    // The confirm dialog names the guest, campsite and dates, and Reject / Cancel report back.
    @Test
    fun confirmDialog_rejectAndCancel() {
        show(reject = RejectState.Confirming)
        composeRule.onNodeWithText("Reject reservation?").assertIsDisplayed()
        composeRule.onNodeWithText("Reject Priya Shah's reservation at Two Jack Lakeside for Jun 5 – 7, 2027? This can't be undone.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(DetailsTags.CONFIRM).performClick()
        composeRule.onNodeWithTag(DetailsTags.CANCEL).performClick()
        assertEquals(1, confirms)
        assertEquals(1, dismissed)
    }

    // A failure offers Try again.
    @Test
    fun failedDialog_tryAgain() {
        show(reject = RejectState.Failed)
        composeRule.onNodeWithText("Couldn't reject reservation").assertIsDisplayed()
        composeRule.onNodeWithTag(DetailsTags.RETRY).performClick()
        assertEquals(1, confirms)
    }

    // Completed stays: Reject is disabled with Figma's note.
    @Test
    fun completedStay_rejectDisabled() {
        show(filter = ReservationFilter.PAST)
        composeRule.onNodeWithTag(DetailsTags.STATUS).assertTextEquals("COMPLETED")
        composeRule.onNodeWithTag(DetailsTags.REJECT).performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag(DetailsTags.CANT_REJECT).assertTextEquals("Completed stays can't be rejected.")
    }

    // "Message entrant" passes the reservation on (Neaz's chat, US 02.12.01).
    @Test
    fun messageEntrant_reportsReservation() {
        show()
        composeRule.onNodeWithTag(DetailsTags.MESSAGE).performScrollTo().performClick()
        assertTrue(messaged == stay)
    }

    // After a rejection, My Reservations shows Figma's banner.
    @Test
    fun myReservations_showsRejectedBanner() {
        composeRule.setContent {
            TrailsTheme {
                OwnerReservationsContent(
                    state = OwnerReservationsUiState(isLoading = false, today = dayKey(2027, 5, 1)),
                    onFilterChange = {}, onCampsiteChange = {}, onViewDetails = {},
                    rejectedBanner = "Priya Shah · Two Jack Lakeside · Jun 5 – 7, 2027"
                )
            }
        }
        composeRule.onNodeWithTag(ReservationsTags.REJECTED_BANNER).assertIsDisplayed()
        composeRule.onNodeWithText("Reservation rejected").assertIsDisplayed()
        composeRule.onNodeWithText("Moved from Active to Cancelled.").assertIsDisplayed()
    }
}
