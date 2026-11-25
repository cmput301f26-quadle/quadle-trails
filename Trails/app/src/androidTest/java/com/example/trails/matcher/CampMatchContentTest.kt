// Compose UI test suite validating CampMatch screen amenity selection, search, and recommendation cards.
// implements Bonus CampMatch Recommendation Engine
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Matching
package com.example.trails.matcher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.trails.data.model.Campsite
import com.example.trails.matcher.engine.CampsiteMatcher
import com.example.trails.matcher.model.CamperPreferences
import com.example.trails.matcher.model.CamperTagRegistry
import com.example.trails.matcher.ui.CampMatchContent
import com.example.trails.matcher.ui.CampMatchItem
import com.example.trails.matcher.ui.CampMatchUiState
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test suite validating [CampMatchContent] interaction.
 */
class CampMatchContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleCampsite = Campsite(
        id = "site-test-1",
        name = "Two Jack Lakeside",
        locationName = "Banff, AB",
        tags = listOf("Fire pit", "Lakeside"),
        pricePerNight = 45.0,
        maxOccupancy = 4,
        activeLotteryId = "lottery-draw-1"
    )

    private val matchResult = CampsiteMatcher.calculateMatch(
        campsite = sampleCampsite,
        preferences = CamperPreferences(preferredTags = setOf("Fire pit", "Lakeside"))
    )

    @Test
    fun screenRenders_searchBar_tagChips_andRankedCards() {
        val state = CampMatchUiState(
            isLoading = false,
            selectedTags = setOf("Fire pit"),
            rankedMatches = listOf(CampMatchItem(sampleCampsite, matchResult)),
            availableTags = CamperTagRegistry.options
        )

        composeRule.setContent {
            TrailsTheme {
                CampMatchContent(
                    state = state,
                    onBack = {},
                    onSearchQueryChange = {},
                    onClearSearch = {},
                    onTagToggled = {},
                    onClearAllTags = {},
                    onMaxPriceChanged = {},
                    onTogglePrioritizeOdds = {},
                    onCampsiteClick = {},
                    onJoinLotteryClick = {}
                )
            }
        }

        composeRule.onNodeWithText("CampMatch").assertIsDisplayed()
        composeRule.onNodeWithTag("camp_match_search_input").assertIsDisplayed()
        composeRule.onNodeWithTag("camp_match_selected_container").assertIsDisplayed()
        composeRule.onNodeWithTag("camp_match_card_site-test-1").assertIsDisplayed()
        composeRule.onNodeWithText("Two Jack Lakeside").assertIsDisplayed()
        composeRule.onNodeWithTag("match_score_badge").assertIsDisplayed()
    }

    @Test
    fun tagChipClick_invokesOnTagToggled() {
        var toggledTag: String? = null
        val state = CampMatchUiState(
            isLoading = false,
            availableTags = CamperTagRegistry.options
        )

        composeRule.setContent {
            TrailsTheme {
                CampMatchContent(
                    state = state,
                    onBack = {},
                    onSearchQueryChange = {},
                    onClearSearch = {},
                    onTagToggled = { toggledTag = it },
                    onClearAllTags = {},
                    onMaxPriceChanged = {},
                    onTogglePrioritizeOdds = {},
                    onCampsiteClick = {},
                    onJoinLotteryClick = {}
                )
            }
        }

        // Click on Fire pit chip
        composeRule.onNodeWithTag("tag_chip_Fire_pit").performClick()
        assertEquals("Fire pit", toggledTag)
    }

    @Test
    fun searchBar_triggersQueryChange() {
        var typedQuery: String? = null
        val state = CampMatchUiState(isLoading = false)

        composeRule.setContent {
            TrailsTheme {
                CampMatchContent(
                    state = state,
                    onBack = {},
                    onSearchQueryChange = { typedQuery = it },
                    onClearSearch = {},
                    onTagToggled = {},
                    onClearAllTags = {},
                    onMaxPriceChanged = {},
                    onTogglePrioritizeOdds = {},
                    onCampsiteClick = {},
                    onJoinLotteryClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("camp_match_search_input").performTextInput("Lake")
        assertEquals("Lake", typedQuery)
    }

    @Test
    fun actionButtons_invokeCorrectCallbacks() {
        var clickedCampsiteId: String? = null
        var clickedLotteryId: String? = null

        val state = CampMatchUiState(
            isLoading = false,
            rankedMatches = listOf(CampMatchItem(sampleCampsite, matchResult))
        )

        composeRule.setContent {
            TrailsTheme {
                CampMatchContent(
                    state = state,
                    onBack = {},
                    onSearchQueryChange = {},
                    onClearSearch = {},
                    onTagToggled = {},
                    onClearAllTags = {},
                    onMaxPriceChanged = {},
                    onTogglePrioritizeOdds = {},
                    onCampsiteClick = { clickedCampsiteId = it },
                    onJoinLotteryClick = { clickedLotteryId = it }
                )
            }
        }

        composeRule.onNodeWithTag("match_view_campsite_button").performClick()
        assertEquals("site-test-1", clickedCampsiteId)

        composeRule.onNodeWithTag("match_join_lottery_button").performClick()
        assertEquals("lottery-draw-1", clickedLotteryId)
    }
}
