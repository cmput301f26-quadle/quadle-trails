// UI (intent) tests for the "Rate campsite" bottom sheet, US 01.13.01.
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
package com.example.trails.ui.admin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RatingSheetContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        score: Int = 0,
        review: String = "",
        isSubmitting: Boolean = false,
        error: String? = null,
        onStarClick: (Int) -> Unit = {},
        onReviewChange: (String) -> Unit = {},
        onSubmit: () -> Unit = {},
        onCancel: () -> Unit = {}
    ) = composeRule.setContent {
        TrailsTheme {
            RatingSheetContent(
                score, review, isSubmitting, error,
                onStarClick, onReviewChange, onSubmit, onCancel
            )
        }
    }

    // This checks that Submit rating can't be tapped until a star is chosen.
    @Test
    fun noStar_submitDisabled() {
        show(score = 0)
        composeRule.onNodeWithTag(RatingTags.SUBMIT).performScrollTo().assertIsNotEnabled()
    }

    // This checks that tapping the fourth star picks a score of 4.
    @Test
    fun tapFourthStar_reportsFour() {
        var picked = 0
        show(onStarClick = { picked = it })
        composeRule.onNodeWithTag(RatingTags.star(4)).performClick()
        assertEquals(4, picked)
    }

    // This checks that four stars show the "4 out of 5 · Great" summary.
    @Test
    fun fourStars_showsGreatSummary() {
        show(score = 4)
        composeRule.onNodeWithTag(RatingTags.SUMMARY).assertTextEquals("4 out of 5 · Great")
    }

    // This checks that typing a review sends the typed text back.
    @Test
    fun typingReview_reportsText() {
        var typed = ""
        show(score = 5, onReviewChange = { typed = it })
        composeRule.onNodeWithTag(RatingTags.REVIEW).performTextInput("Woke up to elk outside the tent.")
        assertEquals("Woke up to elk outside the tent.", typed)
    }

    // This checks that tapping Submit rating with a star chosen asks to save the rating.
    @Test
    fun tapSubmit_reportsSubmit() {
        var submitted = false
        show(score = 4, review = "Clean washrooms, a bit close to the road.", onSubmit = { submitted = true })
        composeRule.onNodeWithTag(RatingTags.SUBMIT).performScrollTo().performClick()
        assertTrue(submitted)
    }

    // This checks that tapping Cancel asks to close the sheet.
    @Test
    fun tapCancel_reportsCancel() {
        var cancelled = false
        show(score = 3, onCancel = { cancelled = true })
        composeRule.onNodeWithTag(RatingTags.CANCEL).performScrollTo().performClick()
        assertTrue(cancelled)
    }

    // This checks that when saving fails, an error message is shown on the sheet.
    @Test
    fun error_isShown() {
        show(score = 4, error = "Campsite not found")
        composeRule.onNodeWithTag(RatingTags.ERROR).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Couldn't save your rating. Please try again.").assertIsDisplayed()
    }
}
