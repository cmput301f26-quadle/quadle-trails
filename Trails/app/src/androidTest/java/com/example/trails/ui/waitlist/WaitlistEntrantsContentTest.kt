// compose ui tests for waitlist entrants screen matching user story 02.09.01
// owner: chitkarsh (chitkarshrathi) - waitlist
// used antigravity to generate entrant list ui tests
package com.example.trails.ui.waitlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.ui.theme.TrailsTheme
import com.google.firebase.Timestamp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Date

class WaitlistEntrantsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleCampsite = Campsite(
        id = "campsite-01",
        ownerId = "owner-01",
        name = "Grande Cache Ridge",
        locationName = "Hinton – Grande Cache, AB",
        pricePerNight = 65.0
    )

    private val sampleLottery = Lottery(
        id = "lottery-01",
        ownerId = "owner-01",
        name = "Grande Cache Ridge · June 2027",
        campsiteIds = listOf("campsite-01"),
        numWinners = 5,
        waitlistCapacity = 10
    )

    private fun show(
        entrants: List<WaitlistEntry>,
        lottery: Lottery? = sampleLottery,
        campsite: Campsite? = sampleCampsite,
        isLoading: Boolean = false,
        errorMessage: String? = null,
        onBack: () -> Unit = {},
        onNotifyClick: () -> Unit = {}
    ) = composeRule.setContent {
        TrailsTheme {
            WaitlistEntrantsContent(
                entrants = entrants,
                lottery = lottery,
                campsite = campsite,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onBack = onBack,
                onNotifyClick = onNotifyClick
            )
        }
    }

    // verifies entrants are rendered in the exact order sorted by joined at
    // adapted from official compose testing docs for assertisdisplayed and getboundsinroot
    @Test
    fun lazyColumn_rendersEntrantsInCorrectOrder() {
        val t1 = Timestamp(Date(1000000000L))
        val t2 = Timestamp(Date(2000000000L))
        val t3 = Timestamp(Date(3000000000L))

        val entrant1 = WaitlistEntry(
            userId = "user-1",
            userName = "Maya Singh",
            lotteryId = "lottery-01",
            partySize = 2,
            joinedAt = t1,
            rulesAccepted = true
        )
        val entrant2 = WaitlistEntry(
            userId = "user-2",
            userName = "Daniel Kim",
            lotteryId = "lottery-01",
            partySize = 4,
            joinedAt = t2,
            rulesAccepted = true
        )
        val entrant3 = WaitlistEntry(
            userId = "user-3",
            userName = "Lena Fischer",
            lotteryId = "lottery-01",
            partySize = 3,
            joinedAt = t3,
            rulesAccepted = true
        )

        show(entrants = listOf(entrant1, entrant2, entrant3))

        // assert list is displayed
        composeRule.onNodeWithTag("waitlist_entrants_list").assertIsDisplayed()

        // assert each entrant card is displayed
        composeRule.onNodeWithTag("waitlist_entrant_item_user-1").assertIsDisplayed()
        composeRule.onNodeWithTag("waitlist_entrant_item_user-2").assertIsDisplayed()
        composeRule.onNodeWithTag("waitlist_entrant_item_user-3").assertIsDisplayed()

        // verify text details
        composeRule.onNodeWithTag("entrant_name_user-1").assertTextEquals("Maya Singh")
        composeRule.onNodeWithTag("entrant_name_user-2").assertTextEquals("Daniel Kim")
        composeRule.onNodeWithTag("entrant_name_user-3").assertTextEquals("Lena Fischer")

        composeRule.onNodeWithTag("entrant_details_user-1").assertTextContains("Party of 2")
        composeRule.onNodeWithTag("entrant_details_user-2").assertTextContains("Party of 4")
        composeRule.onNodeWithTag("entrant_details_user-3").assertTextContains("Party of 3")

        // assert vertical ordering from top to bottom
        val top1 = composeRule.onNodeWithTag("waitlist_entrant_item_user-1").getBoundsInRoot().top
        val top2 = composeRule.onNodeWithTag("waitlist_entrant_item_user-2").getBoundsInRoot().top
        val top3 = composeRule.onNodeWithTag("waitlist_entrant_item_user-3").getBoundsInRoot().top

        assertTrue("item 1 must be above item 2", top1 < top2)
        assertTrue("item 2 must be above item 3", top2 < top3)
    }

    // verifies empty state when no entrants on waitlist
    @Test
    fun emptyWaitlist_showsEmptyEntrantsView() {
        show(entrants = emptyList())
        composeRule.onNodeWithTag("waitlist_empty_entrants").assertIsDisplayed()
        composeRule.onNodeWithText("No entrants on this waitlist yet.").assertIsDisplayed()
    }

    // verifies progress spinner when in loading state
    @Test
    fun loadingState_showsProgressIndicator() {
        show(entrants = emptyList(), isLoading = true)
        composeRule.onNodeWithTag("waitlist_loading_spinner").assertIsDisplayed()
    }

    // verifies notify button triggers click callback
    @Test
    fun notifyEntrantsButton_triggersCallback() {
        var clicked = false
        show(
            entrants = emptyList(),
            onNotifyClick = { clicked = true }
        )

        composeRule.onNodeWithTag("notify_entrants_button").performClick()
        assertTrue(clicked)
    }
}
