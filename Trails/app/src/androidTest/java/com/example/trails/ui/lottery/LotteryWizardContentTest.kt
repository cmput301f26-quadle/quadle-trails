// UI (intent) tests for the New lottery wizard and review, US 02.04.01–02.04.05, 02.04.07.
// OWNER: Skanda (skandarajeev) — Lottery Setup
package com.example.trails.ui.lottery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.trails.data.fake.TestCampsites
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LotteryWizardContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val hour = 3_600_000L
    private val day = 24 * hour
    private val now = System.currentTimeMillis()
    private val opens = now + 12 * day
    private val closes = opens + 7 * day
    private val banff = TestCampsites.BANFF_ID
    private val options = TestCampsites.all.map { CampsiteOption(it) }

    // A complete "May long weekend" draft, as on the Figma review screen.
    private val complete = LotteryDraft(
        name = "May long weekend",
        campsiteIds = listOf(banff),
        registrationStart = opens,
        registrationEnd = closes,
        bookingStart = closes,
        bookingEnd = closes + 2 * day + 9 * hour,
        numWinners = 5,
        limitWaitlist = true,
        waitlistCapacity = 100
    )

    private fun state(step: LotteryStep, draft: LotteryDraft = LotteryDraft(), campsites: List<CampsiteOption> = options) =
        LotteryFormUiState(draft = draft, step = step, campsites = campsites, now = now)

    private fun show(state: LotteryFormUiState, actions: LotteryFormActions = LotteryFormActions()) {
        composeRule.setContent { TrailsTheme { LotteryFormContent(state, actions) } }
    }

    /** Shows the form with real state, so the steppers and switch update what is on screen. */
    private fun showInteractive(initial: LotteryFormUiState) {
        composeRule.setContent {
            var state by remember { mutableStateOf(initial) }
            TrailsTheme {
                LotteryFormContent(
                    state,
                    LotteryFormActions(
                        onToggleCampsite = { state = state.copy(draft = state.draft.toggleCampsite(it)) },
                        onWinnersChange = { state = state.copy(draft = state.draft.withWinners(it)) },
                        onWaitlistLimitChange = { state = state.copy(draft = state.draft.withWaitlistLimit(it)) },
                        onWaitlistCapacityChange = { state = state.copy(draft = state.draft.withWaitlistCapacity(it)) }
                    )
                )
            }
        }
    }

    // US 02.04.01: Next stays disabled until a campsite is picked (Figma F07).
    @Test
    fun campsites_noneSelected_nextDisabledWithHint() {
        show(state(LotteryStep.CAMPSITES))
        composeRule.onNodeWithText("New lottery").assertIsDisplayed()
        composeRule.onNodeWithText("Step 1 of 3 · Campsites").assertIsDisplayed()
        composeRule.onNodeWithText("Select at least one campsite to continue.").assertIsDisplayed()
        composeRule.onNodeWithTag(LotteryTags.PRIMARY_BUTTON).assertIsNotEnabled()
    }

    // US 02.04.02: picking more campsites updates the count on the button (Figma F09).
    @Test
    fun campsites_tappingTwo_buttonCountsThem() {
        showInteractive(state(LotteryStep.CAMPSITES))
        composeRule.onNodeWithText("Tunnel Mountain Site 14").performClick()
        composeRule.onNodeWithText("Next · 1 campsite").assertIsDisplayed()
        composeRule.onNodeWithText("Whistlers Riverside Site 7").performClick()
        composeRule.onNodeWithText("Next · 2 campsites").assertIsDisplayed()
        composeRule.onNodeWithTag(LotteryTags.PRIMARY_BUTTON).assertIsEnabled()
    }

    @Test
    fun campsites_inAnotherLottery_saysWhereAndCantBePicked() {
        var toggled = false
        val taken = options.map { if (it.campsite.id == banff) it.copy(takenBy = "Summer weekends") else it }
        show(state(LotteryStep.CAMPSITES, campsites = taken), LotteryFormActions(onToggleCampsite = { toggled = true }))
        composeRule.onNodeWithText("Already in Summer weekends").assertIsDisplayed()
        composeRule.onNodeWithText("Tunnel Mountain Site 14").performClick()
        assertFalse(toggled)
    }

    @Test
    fun campsites_next_callsOnNext() {
        var next = false
        show(state(LotteryStep.CAMPSITES, LotteryDraft(campsiteIds = listOf(banff))), LotteryFormActions(onNext = { next = true }))
        composeRule.onNodeWithTag(LotteryTags.PRIMARY_BUTTON).performClick()
        assertTrue(next)
    }

    // US 02.04.03: nothing set yet, so booking waits for registration (Figma F10).
    @Test
    fun dates_nothingSet_promptsAndDisablesNext() {
        show(state(LotteryStep.DATES, LotteryDraft(campsiteIds = listOf(banff))))
        composeRule.onNodeWithText("Step 2 of 3 · Dates").assertIsDisplayed()
        composeRule.onNodeWithText("Choose when entries open and close").assertIsDisplayed()
        composeRule.onNodeWithText("Set registration first.").assertIsDisplayed()
        composeRule.onNodeWithTag(LotteryTags.PRIMARY_BUTTON).assertIsNotEnabled()
    }

    // US 02.04.03, 02.04.04: both periods set shows the draw time and booking window (Figma F14).
    @Test
    fun dates_bothSet_showsDrawTimeAndBookingWindow() {
        show(state(LotteryStep.DATES, complete))
        composeRule.onNodeWithText("Winners are drawn automatically at close · ${formatDateTime(closes)}").assertIsDisplayed()
        composeRule.onNodeWithText("Winners have 2 days 9 h to book.").assertIsDisplayed()
        composeRule.onNodeWithTag(LotteryTags.PRIMARY_BUTTON).assertIsEnabled()
    }

    // US 02.04.04: booking that starts before registration closes is caught on the card (Figma F13).
    @Test
    fun dates_bookingOverlapsRegistration_showsInlineError() {
        show(state(LotteryStep.DATES, complete.copy(bookingStart = closes - 3 * day)))
        composeRule.onNodeWithText("Booking must open after registration closes (${formatDateTime(closes)}).").assertIsDisplayed()
        composeRule.onNodeWithText("Check dates").assertIsDisplayed()
        composeRule.onNodeWithTag(LotteryTags.PRIMARY_BUTTON).assertIsNotEnabled()
    }

    // US 02.04.05: the stepper changes the number of winners (Figma F15).
    @Test
    fun winners_plusAndMinus_changeTheCount() {
        showInteractive(state(LotteryStep.WINNERS, complete.copy(numWinners = 5, limitWaitlist = false)))
        composeRule.onNodeWithTag(LotteryTags.WINNERS_VALUE).assertTextEquals("5")
        composeRule.onNodeWithTag(LotteryTags.WINNERS_INCREASE).performClick()
        composeRule.onNodeWithTag(LotteryTags.WINNERS_VALUE).assertTextEquals("6")
        composeRule.onNodeWithText("If fewer than 6 people enter, everyone who entered wins.").assertIsDisplayed()
        composeRule.onNodeWithTag(LotteryTags.WINNERS_DECREASE).performClick()
        composeRule.onNodeWithTag(LotteryTags.WINNERS_VALUE).assertTextEquals("5")
    }

    // US 02.04.07: the waiting list limit is off by default; turning it on shows the maximum (Figma F16).
    @Test
    fun winners_waitlistSwitch_revealsMaximum() {
        showInteractive(state(LotteryStep.WINNERS, complete.copy(limitWaitlist = false)))
        composeRule.onNodeWithText("Optional. Off means anyone can join until registration closes.").assertIsDisplayed()
        composeRule.onNodeWithTag(LotteryTags.WAITLIST_SWITCH).performClick()
        composeRule.onNodeWithText("Maximum people").assertIsDisplayed()
        composeRule.onNodeWithTag(LotteryTags.WAITLIST_VALUE).assertTextEquals("100")
        composeRule.onNodeWithTag(LotteryTags.WAITLIST_INCREASE).performClick()
        composeRule.onNodeWithTag(LotteryTags.WAITLIST_VALUE).assertTextEquals("110")
    }

    // Review lists every setting with Edit, and Draw is automatic (Figma F17).
    @Test
    fun review_showsEverySetting() {
        show(state(LotteryStep.REVIEW, complete))
        composeRule.onNodeWithText("Ready to publish?").assertIsDisplayed()
        composeRule.onNodeWithText("Tunnel Mountain Site 14").assertIsDisplayed()
        composeRule.onNodeWithText("${formatDateTime(opens)} → ${formatDateTime(closes)}").assertIsDisplayed()
        composeRule.onNodeWithText("${formatDateTime(closes)} · automatic").assertIsDisplayed()
        composeRule.onNodeWithText("Up to 100 people").assertIsDisplayed()
        composeRule.onNodeWithText("Publish lottery").assertIsDisplayed()
    }

    @Test
    fun review_editWinners_opensThatStep() {
        var step: LotteryStep? = null
        show(state(LotteryStep.REVIEW, complete), LotteryFormActions(onEditStep = { step = it }))
        composeRule.onNodeWithTag(LotteryTags.editRow("winners")).performScrollTo().performClick()
        assertEquals(LotteryStep.WINNERS, step)
    }

    // Publishing without a name points at the field instead of saving.
    @Test
    fun review_publishWithoutName_showsErrorAndDoesNotSave() {
        var saved = false
        show(state(LotteryStep.REVIEW, complete.copy(name = "")), LotteryFormActions(onSave = { saved = true }))
        composeRule.onNodeWithTag(LotteryTags.PRIMARY_BUTTON).performClick()
        composeRule.onNodeWithText("Give the lottery a name.").assertIsDisplayed()
        assertFalse(saved)
    }

    @Test
    fun review_publish_callsOnSave() {
        var saved = false
        show(state(LotteryStep.REVIEW, complete), LotteryFormActions(onSave = { saved = true }))
        composeRule.onNodeWithTag(LotteryTags.PRIMARY_BUTTON).performClick()
        assertTrue(saved)
    }

    // After publishing, the dialog explains when it opens and Done moves on (Figma F18).
    @Test
    fun published_dialogDone_opensTheLottery() {
        var opened: String? = null
        show(state(LotteryStep.REVIEW, complete).copy(savedId = "lottery-new"), LotteryFormActions(onPublishedDone = { opened = it }))
        composeRule.onNodeWithText("Lottery published").assertIsDisplayed()
        composeRule.onNodeWithText("5 winners are drawn automatically", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag(LotteryTags.DIALOG_BUTTON).performClick()
        assertEquals("lottery-new", opened)
    }

    @Test
    fun publishFailed_offersTryAgain() {
        var retried = false
        show(state(LotteryStep.REVIEW, complete).copy(saveFailed = true), LotteryFormActions(onSave = { retried = true }))
        composeRule.onNodeWithText("Couldn’t publish").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").performClick()
        assertTrue(retried)
    }
}
