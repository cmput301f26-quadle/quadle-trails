// compose ui tests for waitlist content matching stories 01.01.06 01.02.01 and 01.02.02
// owner: chitkarsh (chitkarshrathi) - waitlist
// used antigravity to generate compose ui test suite
package com.example.trails.ui.waitlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.model.WaitlistStatus
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WaitlistContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleCampsite = Campsite(
        id = "campsite-01",
        ownerId = "owner-01",
        name = "Grande Cache Ridge",
        locationName = "Hinton – Grande Cache, AB",
        pricePerNight = 65.0,
        rules = "Quiet hours 10pm-7am. No generators."
    )

    private val sampleLottery = Lottery(
        id = "lottery-01",
        ownerId = "owner-01",
        name = "All May weekends · 2027",
        campsiteIds = listOf("campsite-01"),
        waitlistCapacity = 10
    )

    // adapted from official compose testing docs for setcontent
    private fun show(
        state: WaitlistUiState,
        onRulesCheckedChange: (Boolean) -> Unit = {},
        onJoinClick: () -> Unit = {},
        onLeaveClick: () -> Unit = {},
        onBack: () -> Unit = {},
        onClearError: () -> Unit = {}
    ) = composeRule.setContent {
        TrailsTheme {
            WaitlistContent(
                state = state,
                onRulesCheckedChange = onRulesCheckedChange,
                onJoinClick = onJoinClick,
                onLeaveClick = onLeaveClick,
                onBack = onBack,
                onClearError = onClearError
            )
        }
    }

    // loading state renders spinner
    @Test
    fun loading_showsProgressIndicator() {
        show(WaitlistUiState.Loading)
        composeRule.onNodeWithTag("waitlist_loading").assertIsDisplayed()
    }

    // not waitlisted state renders campsite info and rules
    @Test
    fun notWaitlisted_showsCampsiteDetailsAndRules() {
        show(
            WaitlistUiState.NotWaitlisted(
                lottery = sampleLottery,
                campsite = sampleCampsite,
                isFull = false,
                isRulesAccepted = false
            )
        )
        composeRule.onNodeWithText("Grande Cache Ridge").assertIsDisplayed()
        composeRule.onNodeWithText("Hinton – Grande Cache, AB").assertIsDisplayed()
        composeRule.onNodeWithText("$65 / night").assertIsDisplayed()
        composeRule.onNodeWithText("All May weekends · 2027").assertIsDisplayed()
        composeRule.onNodeWithText("Campsite Rules & Policies").performScrollTo().assertIsDisplayed()
    }

    // join button is disabled when rules are not accepted
    @Test
    fun notWaitlisted_rulesNotAccepted_joinButtonDisabled() {
        show(
            WaitlistUiState.NotWaitlisted(
                lottery = sampleLottery,
                campsite = sampleCampsite,
                isFull = false,
                isRulesAccepted = false
            )
        )
        composeRule.onNodeWithTag("join_lottery_button").performScrollTo().assertIsNotEnabled()
    }

    // join button is enabled when rules are accepted and triggers callback
    @Test
    fun notWaitlisted_rulesAccepted_joinButtonEnabledAndClicks() {
        var joined = false
        show(
            state = WaitlistUiState.NotWaitlisted(
                lottery = sampleLottery,
                campsite = sampleCampsite,
                isFull = false,
                isRulesAccepted = true
            ),
            onJoinClick = { joined = true }
        )
        composeRule.onNodeWithTag("join_lottery_button").performScrollTo().assertIsEnabled()
        composeRule.onNodeWithTag("join_lottery_button").performClick()
        assertTrue(joined)
    }

    // checking rules box triggers callback
    @Test
    fun toggleRules_triggersOnRulesCheckedChange() {
        var checked = false
        show(
            state = WaitlistUiState.NotWaitlisted(
                lottery = sampleLottery,
                campsite = sampleCampsite,
                isRulesAccepted = false
            ),
            onRulesCheckedChange = { checked = it }
        )
        composeRule.onNodeWithTag("rules_checkbox").performScrollTo().performClick()
        assertTrue(checked)
    }

    // waitlisted state renders badge and leave button
    @Test
    fun waitlisted_showsBadgeAndLeaveButton() {
        val entry = WaitlistEntry(
            userId = "user-1",
            lotteryId = "lottery-01",
            partySize = 3,
            status = WaitlistStatus.WAITING,
            rulesAccepted = true
        )
        var left = false
        show(
            state = WaitlistUiState.Waitlisted(
                entry = entry,
                lottery = sampleLottery,
                campsite = sampleCampsite,
                entrantCount = 1
            ),
            onLeaveClick = { left = true }
        )
        composeRule.onNodeWithText("ON WAITING LIST").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("3 Guests").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("leave_lottery_button").performScrollTo().performClick()
        assertTrue(left)
    }

    // full waitlist state renders capacity warning and disables join
    @Test
    fun fullWaitlist_showsWarningAndDisabledButton() {
        show(
            WaitlistUiState.NotWaitlisted(
                lottery = sampleLottery,
                campsite = sampleCampsite,
                isFull = true,
                entrantCount = 10,
                isRulesAccepted = true
            )
        )
        composeRule.onNodeWithText("Waitlist Is Full").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("join_lottery_button").performScrollTo().assertIsNotEnabled()
    }

    // error state shows error banner and text
    @Test
    fun errorState_showsBanner() {
        show(WaitlistUiState.Error("Network error occurred"))
        composeRule.onNodeWithTag("waitlist_error_banner").assertIsDisplayed()
        composeRule.onNodeWithText("Network error occurred").assertIsDisplayed()
    }

    // cancel and back button triggers callback
    @Test
    fun tapBack_callsOnBack() {
        var backed = false
        show(
            state = WaitlistUiState.NotWaitlisted(lottery = sampleLottery, campsite = sampleCampsite),
            onBack = { backed = true }
        )
        composeRule.onNodeWithTag("waitlist_back_button").performClick()
        assertTrue(backed)
    }
}
