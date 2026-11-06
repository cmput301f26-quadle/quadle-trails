// UI (intent) tests for the entrant's App policy screen, US 01.11.01.
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
package com.example.trails.ui.admin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PolicyScreenContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        onBack: () -> Unit = {},
        onGuidelinesClick: () -> Unit = {},
        onBackToSettings: () -> Unit = {}
    ) = composeRule.setContent {
        TrailsTheme {
            PolicyContent(appPolicyCards, onBack, onGuidelinesClick, onBackToSettings)
        }
    }

    // This checks that the screen shows the "Using Quadle" title and all four policy cards.
    @Test
    fun showsTitleAndAllFourCards() {
        show()
        composeRule.onNodeWithTag(PolicyTags.TITLE).assertTextEquals("Using Quadle")
        repeat(4) { index ->
            composeRule.onNodeWithTag(PolicyTags.card(index)).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithText("Lottery entry ≠ reservation").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Campsite rules & cancellation").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(PolicyTags.card(4)).assertDoesNotExist()
    }

    // This checks that tapping "Read camping guidelines" asks to open the guidelines.
    @Test
    fun tapGuidelines_reportsClick() {
        var clicked = false
        show(onGuidelinesClick = { clicked = true })
        composeRule.onNodeWithTag(PolicyTags.GUIDELINES_BUTTON).performScrollTo().performClick()
        assertTrue(clicked)
    }

    // This checks that tapping "Back to settings" asks to return to Settings.
    @Test
    fun tapBackToSettings_reportsClick() {
        var clicked = false
        show(onBackToSettings = { clicked = true })
        composeRule.onNodeWithTag(PolicyTags.BACK_TO_SETTINGS).performScrollTo().performClick()
        assertTrue(clicked)
    }

    // This checks that tapping the back arrow asks to leave the screen.
    @Test
    fun tapBack_reportsClick() {
        var clicked = false
        show(onBack = { clicked = true })
        composeRule.onNodeWithTag(PolicyTags.BACK).performClick()
        assertTrue(clicked)
    }
}
