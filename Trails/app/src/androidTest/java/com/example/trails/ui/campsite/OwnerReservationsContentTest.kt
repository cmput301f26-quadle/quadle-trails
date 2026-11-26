// UI (intent) tests for the owner's reservations list, US 02.07.01, 02.07.02, 02.07.03.
// OWNER: Ibrahim (ibr2309) — Campsite Management
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OwnerReservationsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val ridge = Campsite(id = "ridge", name = "Grande Cache Ridge")
    private val lake = Campsite(id = "lake", name = "Two Jack Lakeside")
    private val alexStay = Reservation(
        id = "qr1042", userId = "alex", campsiteId = "ridge", partySize = 4, totalPrice = 130.0,
        startDate = dayKeyToTimestamp(dayKey(2027, 5, 15)), endDate = dayKeyToTimestamp(dayKey(2027, 5, 17))
    )

    private var chip: ReservationFilter? = null
    private var picked: String? = "unset"
    private var opened: String? = null

    private fun show(
        items: List<OwnerReservationItem>,
        filter: ReservationFilter = ReservationFilter.ACTIVE
    ) = composeRule.setContent {
        TrailsTheme {
            OwnerReservationsContent(
                state = OwnerReservationsUiState(
                    isLoading = false, campsites = listOf(ridge, lake), filter = filter,
                    items = items, today = dayKey(2027, 5, 1)
                ),
                onFilterChange = { chip = it },
                onCampsiteChange = { picked = it },
                onViewDetails = { opened = it }
            )
        }
    }

    // An active card shows what Figma shows: guest, campsite, dates and nights, guests and total, CONFIRMED.
    @Test
    fun activeCard_showsFigmaDetails() {
        show(listOf(OwnerReservationItem(alexStay, "Alex Morgan", ridge, ReservationFilter.ACTIVE)))
        composeRule.onNodeWithText("Alex Morgan").assertIsDisplayed()
        composeRule.onNodeWithText("Grande Cache Ridge").assertIsDisplayed()
        composeRule.onNodeWithText("May 15 – 17, 2027 · 2 nights").assertIsDisplayed()
        composeRule.onNodeWithText("4 guests · $130 total").assertIsDisplayed()
        composeRule.onNodeWithTag(ReservationsTags.status("qr1042")).assertTextEquals("CONFIRMED")
        composeRule.onNodeWithText("Reservation #QR1042").assertIsDisplayed()
    }

    // The count line matches Figma.
    @Test
    fun summary_countsAndDate() {
        show(listOf(OwnerReservationItem(alexStay, "Alex Morgan", ridge, ReservationFilter.ACTIVE)))
        composeRule.onNodeWithTag(ReservationsTags.SUMMARY).assertTextEquals("1 upcoming reservation • As of May 1, 2027")
    }

    // The selected chip is selected, and tapping another reports it.
    @Test
    fun chips_selectAndReport() {
        show(emptyList())
        composeRule.onNodeWithTag(ReservationsTags.filter(ReservationFilter.ACTIVE)).assertIsSelected()
        composeRule.onNodeWithTag(ReservationsTags.filter(ReservationFilter.CANCELLED)).performClick()
        assertEquals(ReservationFilter.CANCELLED, chip)
    }

    // Choosing a campsite in the dropdown reports its id.
    @Test
    fun campsiteDropdown_reportsChoice() {
        show(emptyList())
        composeRule.onNodeWithTag(ReservationsTags.CAMPSITE_PICKER).assertTextContains("All campsites").performClick()
        composeRule.onNodeWithTag(ReservationsTags.campsiteOption("lake")).performClick()
        assertEquals("lake", picked)
    }

    // "View details" reports the reservation id.
    @Test
    fun viewDetails_reportsId() {
        show(listOf(OwnerReservationItem(alexStay, "Alex Morgan", ridge, ReservationFilter.ACTIVE)))
        composeRule.onNodeWithTag(ReservationsTags.details("qr1042")).performClick()
        assertEquals("qr1042", opened)
    }

    // Past cards say COMPLETED; cancelled say CANCELLED; rejected say REJECTED.
    @Test
    fun statusChips_matchFilter() {
        val rejected = alexStay.copy(id = "rej", status = ReservationStatus.REJECTED)
        show(
            listOf(
                OwnerReservationItem(alexStay.copy(id = "done"), "Alex Morgan", ridge, ReservationFilter.PAST),
                OwnerReservationItem(alexStay.copy(id = "gone", status = ReservationStatus.CANCELLED), "Alex Morgan", ridge, ReservationFilter.CANCELLED),
                OwnerReservationItem(rejected, "Alex Morgan", ridge, ReservationFilter.CANCELLED)
            )
        )
        listOf("done" to "COMPLETED", "gone" to "CANCELLED", "rej" to "REJECTED").forEach { (id, label) ->
            composeRule.onNodeWithTag(ReservationsTags.LIST).performScrollToNode(hasTestTag(ReservationsTags.status(id)))
            composeRule.onNodeWithTag(ReservationsTags.status(id)).assertTextEquals(label)
        }
    }

    // An empty chip says so.
    @Test
    fun empty_showsMessage() {
        show(emptyList())
        composeRule.onNodeWithTag(ReservationsTags.EMPTY).assertTextEquals("No upcoming reservations.")
    }
}
