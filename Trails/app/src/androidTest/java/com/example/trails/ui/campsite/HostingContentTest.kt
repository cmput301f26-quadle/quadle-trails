// UI (intent) tests for the My Campsites screen, US 02.03.01.
// OWNER: Ibrahim (ibr2309) — Campsite Management
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Role
import com.example.trails.ui.profile.ModePillChip
import com.example.trails.ui.profile.ModePillTags
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HostingContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val alpine = Campsite(
        id = "alpine-1", ownerId = "owner-01", name = "Alpine Lake · Site 01",
        locationName = "Banff, AB", maxOccupancy = 6
    )
    private val twoJack = Campsite(
        id = "two-jack", ownerId = "owner-01", name = "Two Jack Lakeside",
        locationName = "Banff National Park, AB", maxOccupancy = 4
    )
    private val twoSites = HostingUiState(
        items = listOf(HostingItem(alpine, "May long weekend"), HostingItem(twoJack, null)),
        query = "", totalCount = 2, inLotteryCount = 1
    )

    private fun show(
        state: HostingUiState?,
        onQuery: (String) -> Unit = {},
        onAdd: () -> Unit = {},
        onOpen: (String) -> Unit = {},
        modePill: @Composable () -> Unit = { ModePillChip(Role.OWNER, onClick = {}) }
    ) = composeRule.setContent {
        TrailsTheme { HostingContent(state, onQuery, onAdd, onOpen, modePill = modePill) }
    }

    @Test
    fun campsites_shownWithDetailsAndLotteryStatus() {
        show(twoSites)
        composeRule.onNodeWithText("Alpine Lake · Site 01").assertIsDisplayed()
        composeRule.onNodeWithText("Banff, AB · Up to 6 guests").assertIsDisplayed()
        composeRule.onNodeWithText("In lottery · May long weekend").assertIsDisplayed()
        composeRule.onNodeWithText("Not in a lottery").assertIsDisplayed()
        composeRule.onNodeWithText("2 campsites").assertIsDisplayed()
        composeRule.onNodeWithText("1 in a lottery").assertIsDisplayed()
    }

    @Test
    fun tapCampsite_opensThatCampsite() {
        var opened: String? = null
        show(twoSites, onOpen = { opened = it })
        composeRule.onNodeWithText("Two Jack Lakeside").performClick()
        assertEquals("two-jack", opened)
    }

    @Test
    fun typeInSearch_reportsQuery() {
        var query = ""
        show(twoSites, onQuery = { query = it })
        composeRule.onNodeWithTag(HostingTags.SEARCH).performTextInput("jack")
        assertEquals("jack", query)
    }

    @Test
    fun noCampsites_showsEmptyMessage() {
        show(HostingUiState(emptyList(), "", 0, 0))
        composeRule.onNodeWithTag(HostingTags.EMPTY_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun searchWithNoMatch_showsNoMatchMessage() {
        show(HostingUiState(emptyList(), "jasper", 2, 1))
        composeRule.onNodeWithTag(HostingTags.NO_MATCH).assertIsDisplayed()
    }

    @Test
    fun tapAdd_callsAdd() {
        var added = false
        show(HostingUiState(emptyList(), "", 0, 0), onAdd = { added = true })
        composeRule.onNodeWithTag(HostingTags.ADD_BUTTON).performClick()
        assertTrue(added)
    }

    @Test
    fun header_showsTitleAndOwnerRole() {
        show(twoSites)
        composeRule.onNodeWithText("Hosting").assertIsDisplayed()
        composeRule.onNodeWithTag(HostingTags.ROLE_PILL).assertIsDisplayed()
        composeRule.onNodeWithText("Owner").assertIsDisplayed()
    }

    // This checks that the mode pill slot is shown in the header and its pill can be tapped (US 03.11.01).
    @Test
    fun modePillSlot_isShown() {
        var taps = 0
        show(twoSites, modePill = { ModePillChip(Role.OWNER, onClick = { taps++ }) })
        composeRule.onNodeWithTag(HostingTags.ROLE_PILL).assertIsDisplayed()
        composeRule.onNodeWithTag(ModePillTags.PILL).assertIsDisplayed().performClick()
        assertEquals(1, taps)
    }

    @Test
    fun tapLotteriesTab_reportsTab() {
        var tab = HostingTab.CAMPSITES
        composeRule.setContent {
            TrailsTheme { HostingContent(twoSites, {}, {}, {}, onTabChange = { tab = it }) }
        }
        composeRule.onNodeWithTag(HostingTags.tab(HostingTab.LOTTERIES)).performClick()
        assertEquals(HostingTab.LOTTERIES, tab)
    }

    @Test
    fun lotteriesTab_showsLotteryContentAndHidesAddButton() {
        composeRule.setContent {
            TrailsTheme {
                HostingContent(
                    twoSites, {}, {}, {},
                    selectedTab = HostingTab.LOTTERIES,
                    lotteriesContent = { Text("Lottery list here") }
                )
            }
        }
        composeRule.onNodeWithText("Lottery list here").assertIsDisplayed()
        composeRule.onNodeWithTag(HostingTags.ADD_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithText("Alpine Lake · Site 01").assertDoesNotExist()
    }
}
