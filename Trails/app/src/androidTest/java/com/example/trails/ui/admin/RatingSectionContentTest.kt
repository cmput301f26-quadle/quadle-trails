// UI (intent) tests for the rating section on a campsite's details page, US 01.13.01.
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
package com.example.trails.ui.admin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.trails.data.model.Rating
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RatingSectionContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val priyasRating = Rating(
        userId = "device-priya",
        campsiteId = "campsite-banff-tunnel",
        score = 4,
        review = "Quiet site with a great view of Rundle. Washrooms were a short walk."
    )

    private fun show(state: RatingSectionUiState, onRateClick: () -> Unit = {}) =
        composeRule.setContent { TrailsTheme { RatingSectionContent(state, onRateClick) } }

    // This checks that someone who hasn't rated yet sees the Rate this campsite button and no Edit button.
    @Test
    fun notRated_showsRateButton() {
        show(RatingSectionUiState(canRate = true))
        composeRule.onNodeWithTag(RatingTags.RATE_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithText("Rate this campsite").assertIsDisplayed()
        composeRule.onNodeWithTag(RatingTags.EDIT_BUTTON).assertDoesNotExist()
        composeRule.onNodeWithTag(RatingTags.YOUR_RATING).assertDoesNotExist()
    }

    // This checks that someone who already rated sees "Your rating", their review and an Edit rating button.
    @Test
    fun rated_showsYourRatingAndEdit() {
        show(RatingSectionUiState(canRate = true, myRating = priyasRating))
        composeRule.onNodeWithTag(RatingTags.YOUR_RATING).assertIsDisplayed()
        composeRule.onNodeWithText("Your rating").assertIsDisplayed()
        composeRule.onNodeWithText(priyasRating.review).assertIsDisplayed()
        composeRule.onNodeWithText("Edit rating").assertIsDisplayed()
        composeRule.onNodeWithTag(RatingTags.RATE_BUTTON).assertDoesNotExist()
    }

    // This checks that right after a successful submit, the thank-you message is shown.
    @Test
    fun justSubmitted_showsThanks() {
        show(RatingSectionUiState(canRate = true, myRating = priyasRating, justSubmitted = true))
        composeRule.onNodeWithTag(RatingTags.THANKS).assertIsDisplayed()
        composeRule.onNodeWithText("Thanks for your rating").assertIsDisplayed()
    }

    // This checks that tapping Rate this campsite asks to open the rating sheet.
    @Test
    fun tapRate_reportsClick() {
        var clicked = false
        show(RatingSectionUiState(canRate = true), onRateClick = { clicked = true })
        composeRule.onNodeWithTag(RatingTags.RATE_BUTTON).performClick()
        assertTrue(clicked)
    }
}
