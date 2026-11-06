// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.01.01
// PURPOSE: UI (intent) tests for the admin Campsite page and its Remove campsite dialog.
// Outstanding issues: none.
package com.example.trails.ui.admin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.trails.data.model.Campsite
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
}
