// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 01.11.01
// PURPOSE: UI (intent) tests for the entrant's Camping guidelines screen.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
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

class GuidelinesContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        onBack: () -> Unit = {},
        onBackToPolicy: () -> Unit = {}
    ) = composeRule.setContent {
        TrailsTheme {
            GuidelinesContent(campingGuidelineCards, onBack, onBackToPolicy)
        }
    }

    // This checks that the screen shows the "Leave it better" title and all three guideline cards.
    @Test
    fun showsTitleAndAllThreeCards() {
        show()
        composeRule.onNodeWithTag(GuidelinesTags.TITLE).assertTextEquals("Leave it better")
        repeat(3) { index ->
            composeRule.onNodeWithTag(GuidelinesTags.card(index)).performScrollTo().assertIsDisplayed()
        }
        composeRule.onNodeWithText("Respect fellow campers").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Leave no trace").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Camp safely").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(GuidelinesTags.card(3)).assertDoesNotExist()
    }

    // This checks that the emergency services note is shown below the cards.
    @Test
    fun showsEmergencyNote() {
        show()
        composeRule.onNodeWithTag(GuidelinesTags.NOTE)
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals(
                "For urgent danger, contact local emergency services. " +
                    "Follow posted notices and current local restrictions."
            )
    }

    // This checks that tapping "Back to app policy" asks to return to App policy.
    @Test
    fun tapBackToPolicy_reportsClick() {
        var clicked = false
        show(onBackToPolicy = { clicked = true })
        composeRule.onNodeWithTag(GuidelinesTags.BACK_TO_POLICY).performScrollTo().performClick()
        assertTrue(clicked)
    }

    // This checks that tapping the back arrow asks to leave the screen.
    @Test
    fun tapBack_reportsClick() {
        var clicked = false
        show(onBack = { clicked = true })
        composeRule.onNodeWithTag(GuidelinesTags.BACK).performClick()
        assertTrue(clicked)
    }
}
