// Compose UI test suite validating SelectCampsiteContent rendering, campsite cards, and OR divider (UI Controller tests).
// implements US 01.04.02, US 02.04.02
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.ui.waitlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test suite validating [SelectCampsiteContent] layout and interactions.
 */
class SelectCampsiteContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val site1 = Campsite(
        id = "site-1",
        name = "Grande Cache Ridge",
        locationName = "Hinton, AB",
        pricePerNight = 45.0
    )

    private val site2 = Campsite(
        id = "site-2",
        name = "Two Jack Lakeside Campground",
        locationName = "Banff, AB",
        pricePerNight = 35.0
    )

    private val sampleLottery = Lottery(
        id = "lottery-01",
        name = "Rockies Multi-Draw",
        campsiteIds = listOf("site-1", "site-2")
    )

    private fun show(
        state: SelectCampsiteUiState,
        onBack: () -> Unit = {},
        onSelectCampsite: (String) -> Unit = {}
    ) = composeRule.setContent {
        TrailsTheme {
            SelectCampsiteContent(
                state = state,
                onBack = onBack,
                onSelectCampsite = onSelectCampsite
            )
        }
    }

    @Test
    fun multiCampsite_rendersBothCampsitesAndOrDivider() {
        show(
            state = SelectCampsiteUiState.Success(
                lottery = sampleLottery,
                campsites = listOf(site1, site2)
            )
        )

        composeRule.onNodeWithText("Select Campsite").assertIsDisplayed()
        composeRule.onNodeWithText("Grande Cache Ridge").assertIsDisplayed()
        composeRule.onNodeWithText("Two Jack Lakeside Campground").assertIsDisplayed()
        composeRule.onNodeWithTag("select_campsite_or_divider").assertIsDisplayed()
        composeRule.onNodeWithTag("reserve_campsite_button_site-1").assertIsDisplayed()
        composeRule.onNodeWithTag("reserve_campsite_button_site-2").assertIsDisplayed()
    }

    @Test
    fun clickReserveDates_invokesCallbackWithCampsiteId() {
        var selectedId = ""
        show(
            state = SelectCampsiteUiState.Success(
                lottery = sampleLottery,
                campsites = listOf(site1, site2)
            ),
            onSelectCampsite = { selectedId = it }
        )

        composeRule.onNodeWithTag("reserve_campsite_button_site-2").performClick()
        assertEquals("site-2", selectedId)
    }

    @Test
    fun loadingState_showsLoadingIndicator() {
        show(state = SelectCampsiteUiState.Loading)
        composeRule.onNodeWithTag("select_campsite_loading").assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorMessage() {
        show(state = SelectCampsiteUiState.Error("Network failure"))
        composeRule.onNodeWithText("Network failure").assertIsDisplayed()
    }
}
