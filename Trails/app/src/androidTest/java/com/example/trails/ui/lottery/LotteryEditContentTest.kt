// UI (intent) tests for Edit lottery and Delete lottery, US 02.04.06.
// OWNER: Skanda (skandarajeev) — Lottery Setup
package com.example.trails.ui.lottery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Lottery
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LotteryEditContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val day = 86_400_000L
    private val now = System.currentTimeMillis()
    private val mayLongWeekend = Lottery(
        id = "lottery-may",
        ownerId = TestUsers.OWNER_ID,
        name = "May long weekend",
        campsiteIds = listOf(TestCampsites.HINTON_ID),
        registrationStart = timestampOf(now + 12 * day),
        registrationEnd = timestampOf(now + 19 * day),
        bookingStart = timestampOf(now + 19 * day),
        bookingEnd = timestampOf(now + 21 * day),
        numWinners = 5,
        waitlistCapacity = 100
    )

    private fun editState(lottery: Lottery = mayLongWeekend, entrants: Int = 0) = LotteryFormUiState(
        draft = LotteryDraft.from(lottery),
        original = lottery,
        isEditing = true,
        campsites = TestCampsites.all.map { CampsiteOption(it) },
        entrantCount = entrants,
        now = now
    )

    private fun show(state: LotteryFormUiState, actions: LotteryFormActions = LotteryFormActions()) {
        composeRule.setContent { TrailsTheme { LotteryFormContent(state, actions) } }
    }

    // Everything on one screen with the saved values (Figma F20).
    @Test
    fun edit_showsSavedSettings() {
        show(editState())
        composeRule.onNodeWithText("Edit lottery").assertIsDisplayed()
        composeRule.onNodeWithText("Registration hasn’t opened yet, so everything can still change.").assertIsDisplayed()
        composeRule.onNodeWithText("May long weekend").assertIsDisplayed()
        composeRule.onNodeWithText("Wildhorse Lake Site 3").assertIsDisplayed()
        composeRule.onNodeWithText("Up to 100").assertIsDisplayed()
        composeRule.onNodeWithText("Save changes").assertIsDisplayed()
    }

    // Changing winners shows what it was before (Figma F20 "Changed from 5").
    @Test
    fun edit_changingWinners_showsChangedFrom() {
        composeRule.setContent {
            var state by remember { mutableStateOf(editState()) }
            TrailsTheme {
                LotteryFormContent(state, LotteryFormActions(onWinnersChange = { state = state.copy(draft = state.draft.withWinners(it)) }))
            }
        }
        composeRule.onNodeWithTag(LotteryTags.WINNERS_INCREASE).performScrollTo().performClick()
        composeRule.onNodeWithText("Changed from 5").assertIsDisplayed()
    }

    @Test
    fun edit_saveChanges_callsOnSave() {
        var saved = false
        show(editState(), LotteryFormActions(onSave = { saved = true }))
        composeRule.onNodeWithTag(LotteryTags.PRIMARY_BUTTON).performClick()
        assertTrue(saved)
    }

    // Figma F21: a failed save keeps the edits and offers Try again.
    @Test
    fun edit_saveFailed_offersTryAgain() {
        var retried = false
        show(editState().copy(saveFailed = true), LotteryFormActions(onSave = { retried = true }))
        composeRule.onNodeWithText("Couldn’t save changes").assertIsDisplayed()
        composeRule.onNodeWithText("Your edits are still here", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Try again").performClick()
        assertTrue(retried)
    }

    // Delete asks first, then deletes.
    @Test
    fun delete_nobodyJoined_confirmsThenDeletes() {
        var deleted = false
        show(editState(), LotteryFormActions(onDelete = { deleted = true }))
        composeRule.onNodeWithTag(LotteryTags.DELETE).performScrollTo().performClick()
        composeRule.onNodeWithText("Delete this lottery?").assertIsDisplayed()
        assertFalse(deleted)
        composeRule.onNodeWithTag(LotteryTags.DIALOG_BUTTON).performClick()
        assertTrue(deleted)
    }

    @Test
    fun delete_cancel_keepsTheLottery() {
        var deleted = false
        show(editState(), LotteryFormActions(onDelete = { deleted = true }))
        composeRule.onNodeWithTag(LotteryTags.DELETE).performScrollTo().performClick()
        composeRule.onNodeWithTag(LotteryTags.DIALOG_CANCEL).performClick()
        composeRule.onNodeWithText("Delete this lottery?").assertDoesNotExist()
        assertFalse(deleted)
    }

    // Once people have joined, Delete is replaced by a note saying why.
    @Test
    fun delete_peopleJoined_explainsInstead() {
        show(editState(entrants = 2))
        composeRule.onNodeWithTag(LotteryTags.DELETE_BLOCKED).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2 people have joined the waiting list, so this lottery can’t be deleted.").assertIsDisplayed()
        composeRule.onNodeWithTag(LotteryTags.DELETE).assertDoesNotExist()
    }

    // Registration already open: the note says the opening time is fixed.
    @Test
    fun edit_registrationOpen_saysOpeningTimeIsFixed() {
        val open = mayLongWeekend.copy(registrationStart = timestampOf(now - day))
        show(editState(open))
        composeRule.onNodeWithText("the opening time is fixed", substring = true).assertIsDisplayed()
    }

    // After the draw a lottery can't be edited.
    @Test
    fun edit_closedLottery_isLocked() {
        val closed = mayLongWeekend.copy(registrationStart = timestampOf(now - 10 * day), registrationEnd = timestampOf(now - day))
        show(editState(closed))
        composeRule.onNodeWithText("Registration has closed, so this lottery can’t be edited.").assertIsDisplayed()
        composeRule.onNodeWithText("Save changes").assertDoesNotExist()
    }
}
