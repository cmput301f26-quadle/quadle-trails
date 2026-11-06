// UI (intent) tests for the admin's Campsites screen, US 03.04.01.
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
package com.example.trails.ui.admin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.trails.data.model.Campsite
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdminCampsitesContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val tunnel = Campsite(
        id = "campsite-banff-tunnel", ownerId = "owner-daniel", name = "Tunnel Mountain Site 14",
        locationName = "Banff, AB", maxOccupancy = 6
    )
    private val whistlers = Campsite(
        id = "campsite-jasper-whistlers", ownerId = "owner-maya", name = "Whistlers Loop Site 22",
        locationName = "Jasper, AB", maxOccupancy = 4
    )
    private val twoSites = AdminCampsitesUiState(
        items = listOf(
            AdminCampsiteItem(tunnel, "Daniel Whitehorse"),
            AdminCampsiteItem(whistlers, "Maya Singh")
        ),
        query = "", totalCount = 2
    )

    private fun show(
        state: AdminCampsitesUiState?,
        onQuery: (String) -> Unit = {},
        onBack: () -> Unit = {},
        onOpen: (String) -> Unit = {}
    ) = composeRule.setContent { TrailsTheme { AdminCampsitesContent(state, onQuery, onBack, onOpen) } }

    // Every campsite shows its name and a "location · Owner name" line.
    @Test
    fun campsites_shownWithSubtitle() {
        show(twoSites)
        composeRule.onNodeWithText("Tunnel Mountain Site 14").assertIsDisplayed()
        composeRule.onNodeWithText("Banff, AB · Owner Daniel Whitehorse").assertIsDisplayed()
        composeRule.onNodeWithText("Whistlers Loop Site 22").assertIsDisplayed()
        composeRule.onNodeWithText("Jasper, AB · Owner Maya Singh").assertIsDisplayed()
    }

    // The line above the list counts every campsite in the app.
    @Test
    fun countLine_showsTotal() {
        show(twoSites)
        composeRule.onNodeWithText("2 campsites · all owners").assertIsDisplayed()
    }

    // Typing in the search box passes the text up to the ViewModel.
    @Test
    fun typingInSearch_reportsQuery() {
        var query = ""
        show(twoSites, onQuery = { query = it })
        composeRule.onNodeWithTag(AdminCampsitesTags.SEARCH).performTextInput("maya")
        assertEquals("maya", query)
    }

    // Tapping a card reports that campsite's id so the details screen can open it.
    @Test
    fun tapCampsite_reportsItsId() {
        var opened: String? = null
        show(twoSites, onOpen = { opened = it })
        composeRule.onNodeWithTag(AdminCampsitesTags.card(whistlers.id)).performClick()
        assertEquals("campsite-jasper-whistlers", opened)
    }

    // Tapping the back arrow asks to leave the screen.
    @Test
    fun tapBack_reportsBack() {
        var wentBack = false
        show(twoSites, onBack = { wentBack = true })
        composeRule.onNodeWithTag(AdminCampsitesTags.BACK).performClick()
        assertTrue(wentBack)
    }

    // With no campsites in the app, a "No campsites yet" message is shown instead of a list.
    @Test
    fun noCampsites_showsEmptyMessage() {
        show(AdminCampsitesUiState(emptyList(), "", 0))
        composeRule.onNodeWithTag(AdminCampsitesTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("No campsites yet").assertIsDisplayed()
        composeRule.onNodeWithTag(AdminCampsitesTags.LIST).assertDoesNotExist()
    }

    // When campsites exist but none match the search, a no-match message is shown.
    @Test
    fun searchWithNoMatch_showsNoMatchMessage() {
        show(AdminCampsitesUiState(emptyList(), "canmore", 2))
        composeRule.onNodeWithTag(AdminCampsitesTags.NO_MATCH).assertIsDisplayed()
        composeRule.onNodeWithText("No campsites match your search").assertIsDisplayed()
    }

    // Before the first load finishes, a spinner is shown and no search box yet.
    @Test
    fun nullState_showsLoading() {
        show(null)
        composeRule.onNodeWithTag(AdminCampsitesTags.LOADING).assertIsDisplayed()
        composeRule.onNodeWithTag(AdminCampsitesTags.SEARCH).assertDoesNotExist()
    }
}
