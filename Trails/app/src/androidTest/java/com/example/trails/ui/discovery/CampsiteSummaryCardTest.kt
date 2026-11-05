// UI tests for the campsite summary card (US 01.01.04).
// OWNER: Neaz (NeazMZ06) — Search & Discovery
package com.example.trails.ui.discovery

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.trails.data.fake.TestCampsites
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CampsiteSummaryCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun ratedCampsite_showsNameLocationCapacityPriceAndRating() {
        val banff = TestCampsites.all.first { it.id == TestCampsites.BANFF_ID }
        composeRule.setContent {
            TrailsTheme { CampsiteSummaryCard(banff, onClick = {}) }
        }
        composeRule.onNodeWithText("Tunnel Mountain Site 14").assertIsDisplayed()
        composeRule.onNodeWithText("Banff, AB · Up to 6 guests").assertIsDisplayed()
        composeRule.onNodeWithText("$42 / night").assertIsDisplayed()
        composeRule.onNodeWithText("★ 4.5").assertIsDisplayed()
        composeRule.onNodeWithText("2 ratings").assertIsDisplayed()
        composeRule.onNodeWithText("Fire pit · Power hookup · Showers").assertIsDisplayed()
    }

    @Test
    fun unratedCampsite_showsNoRatingsYet() {
        val hinton = TestCampsites.all.first { it.id == TestCampsites.HINTON_ID }
        composeRule.setContent {
            TrailsTheme { CampsiteSummaryCard(hinton, onClick = {}) }
        }
        composeRule.onNodeWithText("Wildhorse Lake Site 3").assertIsDisplayed()
        composeRule.onNodeWithText("Hinton-Grande Cache, AB · Up to 8 guests").assertIsDisplayed()
        composeRule.onNodeWithText("$65 / night").assertIsDisplayed()
        composeRule.onNodeWithText("No ratings yet").assertIsDisplayed()
    }

    @Test
    fun tapCard_callsOnClick() {
        val banff = TestCampsites.all.first { it.id == TestCampsites.BANFF_ID }
        var clicked = false
        composeRule.setContent {
            TrailsTheme { CampsiteSummaryCard(banff, onClick = { clicked = true }) }
        }
        composeRule.onNodeWithText("Tunnel Mountain Site 14").performClick()
        assertTrue(clicked)
    }
}
