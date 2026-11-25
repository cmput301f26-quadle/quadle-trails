// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.01.01, 03.09.01
// PURPOSE: UI (intent) tests for the admin Campsite page, its Remove campsite dialog,
//   and its Ratings & reviews section with the Remove rating dialog.
// Outstanding issues: none.
package com.example.trails.ui.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Rating
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdminCampsiteContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val tunnel = Campsite(
        id = "campsite-banff-tunnel", ownerId = "owner-daniel", name = "Tunnel Mountain Site 14",
        locationName = "Banff, AB", maxOccupancy = 6, ratingAverage = 4.5, ratingCount = 12
    )
    private val loaded = AdminCampsiteUiState.Loaded(tunnel, "Daniel Whitehorse", lotteryCount = 2, entryCount = 7)

    private fun show(
        state: AdminCampsiteUiState,
        removeState: AdminCampsiteRemoveState = AdminCampsiteRemoveState(),
        onConfirm: () -> Unit = {}
    ) = composeRule.setContent {
        TrailsTheme {
            AdminCampsiteContent(state, removeState, onBack = {}, onOpenPublicPage = {}, onConfirmRemove = onConfirm)
        }
    }

    // This checks that a loaded campsite shows its name, owner, counts, rating and the Remove campsite button.
    @Test
    fun loaded_showsNameAndRemoveButton() {
        show(loaded)
        composeRule.onNodeWithTag(AdminCampsiteTags.NAME).assertTextEquals("Tunnel Mountain Site 14")
        composeRule.onNodeWithText("Owner: Daniel Whitehorse").assertIsDisplayed()
        composeRule.onNodeWithTag(AdminCampsiteTags.LOTTERIES).assertTextEquals("2")
        composeRule.onNodeWithTag(AdminCampsiteTags.ENTRIES).assertTextEquals("7")
        composeRule.onNodeWithTag(AdminCampsiteTags.RATING).assertTextEquals("★ 4.5 (12)")
        composeRule.onNodeWithTag(AdminCampsiteTags.REMOVE_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(AdminCampsiteTags.DIALOG).assertDoesNotExist()
    }

    // This checks that tapping Remove campsite opens the confirmation dialog with the counts in it.
    @Test
    fun tapRemove_showsDialog() {
        show(loaded)
        composeRule.onNodeWithTag(AdminCampsiteTags.REMOVE_BUTTON).performClick()
        composeRule.onNodeWithText("Remove campsite?").assertIsDisplayed()
        composeRule.onNodeWithTag(AdminCampsiteTags.DIALOG_MESSAGE).assertTextEquals(
            "Tunnel Mountain Site 14 is removed for everyone. Its 2 lotteries and 7 lottery entries " +
                "are removed too. This can't be undone."
        )
    }

    // This checks that Cancel closes the dialog without asking to remove anything.
    @Test
    fun dialogCancel_closesDialog_reportsNothing() {
        var confirmed = false
        show(loaded, onConfirm = { confirmed = true })
        composeRule.onNodeWithTag(AdminCampsiteTags.REMOVE_BUTTON).performClick()
        composeRule.onNodeWithTag(AdminCampsiteTags.DIALOG_CANCEL).performClick()
        composeRule.onNodeWithTag(AdminCampsiteTags.DIALOG).assertDoesNotExist()
        assertTrue(!confirmed)
    }

    // This checks that tapping Remove in the dialog reports the confirmation exactly once.
    @Test
    fun dialogRemove_reportsConfirm() {
        var confirms = 0
        show(loaded, onConfirm = { confirms++ })
        composeRule.onNodeWithTag(AdminCampsiteTags.REMOVE_BUTTON).performClick()
        composeRule.onNodeWithTag(AdminCampsiteTags.DIALOG_REMOVE).performClick()
        assertEquals(1, confirms)
    }

    // This checks that a failed removal shows an error message above the button.
    @Test
    fun error_isShown() {
        show(loaded, removeState = AdminCampsiteRemoveState(failed = true))
        composeRule.onNodeWithTag(AdminCampsiteTags.ERROR).assertIsDisplayed()
        composeRule.onNodeWithText("Couldn't remove this campsite. Please try again.").assertIsDisplayed()
    }

    // This checks that a campsite that no longer exists shows a message instead of details.
    @Test
    fun notFound_showsMessage() {
        show(AdminCampsiteUiState.NotFound)
        composeRule.onNodeWithTag(AdminCampsiteTags.NOT_FOUND).assertIsDisplayed()
        composeRule.onNodeWithText("This campsite no longer exists").assertIsDisplayed()
        composeRule.onNodeWithTag(AdminCampsiteTags.REMOVE_BUTTON).assertDoesNotExist()
    }

    // ---- Ratings & reviews (US 03.09.01) ----

    private val priyaRating = AdminRatingItem(
        Rating(
            userId = "device-priya", campsiteId = "campsite-banff-tunnel", score = 1,
            review = "Owner is a crook, his number is 403-555-0199, call him at night."
        ),
        reviewerName = "Priya Sharma",
        dateLabel = "Nov 2, 2026"
    )
    private val avaRating = AdminRatingItem(
        Rating(userId = "device-ava", campsiteId = "campsite-banff-tunnel", score = 4),
        reviewerName = "Ava Chen",
        dateLabel = "Sep 14, 2026"
    )
    private val withRatings = loaded.copy(ratings = listOf(priyaRating, avaRating))

    /** Matches [matcher] only inside the card of the rating by [userId]. */
    private fun inCard(userId: String, matcher: SemanticsMatcher) =
        composeRule.onNode(matcher and hasAnyAncestor(hasTestTag(AdminCampsiteTags.rating(userId))), useUnmergedTree = true)

    /**
     * Shows the page with a small stand-in for the ViewModel, so tapping Remove rating really opens
     * the dialog. Returns the reviewer ids confirmed and the number of dismissals.
     */
    private fun showRatingsInteractive(): Pair<MutableList<String>, IntArray> {
        val confirmed = mutableListOf<String>()
        val dismissed = intArrayOf(0)
        composeRule.setContent {
            var removeState by remember { mutableStateOf(AdminRatingRemoveState()) }
            TrailsTheme {
                AdminCampsiteContent(
                    withRatings, AdminCampsiteRemoveState(), onBack = {}, onOpenPublicPage = {}, onConfirmRemove = {},
                    ratingRemoveState = removeState,
                    onRemoveRatingClick = { removeState = AdminRatingRemoveState(confirmingUserId = it) },
                    onDismissRemoveRating = {
                        dismissed[0]++
                        removeState = AdminRatingRemoveState()
                    },
                    onConfirmRemoveRating = { removeState.confirmingUserId?.let(confirmed::add) }
                )
            }
        }
        return confirmed to dismissed
    }

    private fun showRatings(removeState: AdminRatingRemoveState, state: AdminCampsiteUiState = withRatings) =
        composeRule.setContent {
            TrailsTheme {
                AdminCampsiteContent(
                    state, AdminCampsiteRemoveState(), onBack = {}, onOpenPublicPage = {}, onConfirmRemove = {},
                    ratingRemoveState = removeState
                )
            }
        }

    // This checks that each rating card shows the reviewer's name, date, stars, review and a Remove rating button.
    @Test
    fun ratings_shownWithReviewerStarsAndReview() {
        showRatings(AdminRatingRemoveState())
        composeRule.onNodeWithTag(AdminCampsiteTags.RATINGS).assertExists()
        composeRule.onNodeWithText("Ratings & reviews", useUnmergedTree = true).assertExists()
        inCard("device-priya", hasText("Priya Sharma")).assertExists()
        inCard("device-priya", hasText("Nov 2, 2026")).assertExists()
        inCard("device-priya", hasContentDescription("1 out of 5 stars")).assertExists()
        inCard("device-priya", hasText("Owner is a crook, his number is 403-555-0199, call him at night.")).assertExists()
        inCard("device-ava", hasText("Ava Chen")).assertExists()
        inCard("device-ava", hasContentDescription("4 out of 5 stars")).assertExists()
        composeRule.onNodeWithTag(AdminCampsiteTags.removeRating("device-priya"), useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AdminCampsiteTags.removeRating("device-ava"), useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AdminCampsiteTags.RATINGS_EMPTY).assertDoesNotExist()
        composeRule.onNodeWithTag(AdminCampsiteTags.RATING_DIALOG).assertDoesNotExist()
    }

    // This checks that a campsite with no ratings shows "No ratings yet" and no rating cards.
    @Test
    fun noRatings_showsEmptyText() {
        showRatings(AdminRatingRemoveState(), state = loaded)
        composeRule.onNodeWithTag(AdminCampsiteTags.RATINGS_EMPTY, useUnmergedTree = true).assertTextEquals("No ratings yet")
        composeRule.onNodeWithTag(AdminCampsiteTags.rating("device-priya")).assertDoesNotExist()
    }

    // This checks that tapping Remove rating opens the "Remove rating?" dialog naming that reviewer and score.
    @Test
    fun tapRemoveRating_showsDialogWithMessage() {
        showRatingsInteractive()
        composeRule.onNodeWithTag(AdminCampsiteTags.removeRating("device-ava"), useUnmergedTree = true).performScrollTo().performClick()
        composeRule.onNodeWithText("Remove rating?").assertIsDisplayed()
        composeRule.onNodeWithTag(AdminCampsiteTags.RATING_DIALOG_MESSAGE).assertTextEquals(
            "Ava Chen's 4-star rating is removed and the campsite's average is updated. This can't be undone."
        )
        composeRule.onNodeWithTag(AdminCampsiteTags.DIALOG).assertDoesNotExist()
    }

    // This checks that tapping Remove in the dialog confirms the removal of the rating that was tapped, once.
    @Test
    fun dialogRemove_reportsConfirmForThatRating() {
        val (confirmed, _) = showRatingsInteractive()
        composeRule.onNodeWithTag(AdminCampsiteTags.removeRating("device-priya"), useUnmergedTree = true).performScrollTo().performClick()
        composeRule.onNodeWithTag(AdminCampsiteTags.RATING_DIALOG_REMOVE).performClick()
        assertEquals(listOf("device-priya"), confirmed)
    }

    // This checks that Cancel reports a dismissal, closes the dialog and confirms nothing.
    @Test
    fun dialogCancel_reportsDismiss() {
        val (confirmed, dismissed) = showRatingsInteractive()
        composeRule.onNodeWithTag(AdminCampsiteTags.removeRating("device-priya"), useUnmergedTree = true).performScrollTo().performClick()
        composeRule.onNodeWithTag(AdminCampsiteTags.RATING_DIALOG_CANCEL).performClick()
        assertEquals(1, dismissed[0])
        assertTrue(confirmed.isEmpty())
        composeRule.onNodeWithTag(AdminCampsiteTags.RATING_DIALOG).assertDoesNotExist()
    }

    // This checks that while a rating is being removed, both dialog buttons and the Remove rating buttons are disabled.
    @Test
    fun removing_buttonsDisabled() {
        showRatings(AdminRatingRemoveState(confirmingUserId = "device-priya", removing = true))
        composeRule.onNodeWithTag(AdminCampsiteTags.RATING_DIALOG_REMOVE).assertIsNotEnabled()
        composeRule.onNodeWithTag(AdminCampsiteTags.RATING_DIALOG_CANCEL).assertIsNotEnabled()
        composeRule.onNodeWithTag(AdminCampsiteTags.removeRating("device-ava"), useUnmergedTree = true).assertIsNotEnabled()
    }

    // This checks that a failed rating removal shows an error message in the Ratings & reviews section.
    @Test
    fun ratingError_isShown() {
        showRatings(AdminRatingRemoveState(failed = true))
        composeRule.onNodeWithTag(AdminCampsiteTags.RATING_ERROR, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Couldn't remove this rating. Please try again.", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AdminCampsiteTags.RATING_DIALOG).assertDoesNotExist()
    }
}
