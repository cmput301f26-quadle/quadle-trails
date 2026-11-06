// UI (intent) tests for the Lottery details page and the Lotteries tab, US 02.04.06.
// OWNER: Skanda (skandarajeev) — Lottery Setup
package com.example.trails.ui.lottery

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LotteryDetailsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val day = 86_400_000L
    private val now = System.currentTimeMillis()

    private fun lottery(id: String, name: String, opensIn: Int) = Lottery(
        id = id,
        ownerId = TestUsers.OWNER_ID,
        name = name,
        campsiteIds = listOf(TestCampsites.BANFF_ID, TestCampsites.JASPER_ID),
        registrationStart = timestampOf(now + opensIn * day),
        registrationEnd = timestampOf(now + (opensIn + 7) * day),
        bookingStart = timestampOf(now + (opensIn + 7) * day),
        bookingEnd = timestampOf(now + (opensIn + 9) * day),
        numWinners = 5,
        waitlistCapacity = 100
    )

    private val scheduled = lottery("may", "May long weekend", opensIn = 12)
    private val open = lottery("summer", "Summer weekends", opensIn = -5)
    private val closed = lottery("spring", "Spring opening", opensIn = -60)

    private fun showDetails(
        lottery: Lottery,
        entrants: Int = 0,
        savedMessage: String? = null,
        onEdit: () -> Unit = {},
        onReuse: () -> Unit = {},
        onDelete: () -> Unit = {}
    ) {
        composeRule.setContent {
            TrailsTheme {
                LotteryDetailsContent(
                    state = buildLotteryDetailsState(lottery, TestCampsites.all, entrants, now),
                    savedMessage = savedMessage,
                    onBack = {}, onEdit = onEdit, onReuse = onReuse, onDelete = onDelete
                )
            }
        }
    }

    // Figma F19: scheduled lottery counts down to registration and can be edited.
    @Test
    fun scheduled_showsCountdownAndEdit() {
        var edited = false
        showDetails(scheduled, onEdit = { edited = true })
        composeRule.onNodeWithText("May long weekend").assertIsDisplayed()
        composeRule.onNodeWithText("Scheduled").assertIsDisplayed()
        composeRule.onNodeWithText("Registration opens in 12 days").assertIsDisplayed()
        composeRule.onNodeWithText("Up to 100 people").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(LotteryTags.HEADER_ACTION).performClick()
        assertTrue(edited)
    }

    // Figma F23: entrants and winners tiles while registration is open.
    @Test
    fun open_showsEntrantsOfCapacityAndDrawCountdown() {
        showDetails(open, entrants = 24)
        composeRule.onNodeWithText("Registration open").assertIsDisplayed()
        composeRule.onNodeWithText("24").assertIsDisplayed()
        composeRule.onNodeWithText("entrants · of 100").assertIsDisplayed()
        composeRule.onNodeWithText("winners to draw").assertIsDisplayed()
        composeRule.onNodeWithText("Draw runs automatically in 2 days").assertIsDisplayed()
    }

    @Test
    fun drawn_showsWinnersAndHidesEdit() {
        showDetails(closed, entrants = 31)
        composeRule.onNodeWithText("Draw complete").assertIsDisplayed()
        composeRule.onNodeWithText("5 winners drawn").assertIsDisplayed()
        composeRule.onNodeWithTag(LotteryTags.HEADER_ACTION).assertDoesNotExist()
    }

    // Figma F25: nobody entered, so it can be reused or deleted.
    @Test
    fun noEntrants_offersReuseAndDelete() {
        var reused = false
        var deleted = false
        showDetails(closed, onReuse = { reused = true }, onDelete = { deleted = true })
        composeRule.onNodeWithText("Closed · no entrants").assertIsDisplayed()
        composeRule.onNodeWithText("No one entered").assertIsDisplayed()
        composeRule.onNodeWithText("Reuse settings in a new lottery").performClick()
        assertTrue(reused)
        composeRule.onNodeWithTag(LotteryTags.DELETE).performScrollTo().performClick()
        composeRule.onNodeWithTag(LotteryTags.DIALOG_BUTTON).performClick()
        assertTrue(deleted)
    }

    // Figma F22: banner after saving an edit.
    @Test
    fun savedMessage_showsBanner() {
        showDetails(scheduled, savedMessage = "Changes saved. Winners updated to 6.")
        composeRule.onNodeWithTag(LotteryTags.SAVED_BANNER).assertIsDisplayed()
        composeRule.onNodeWithText("Changes saved. Winners updated to 6.").assertIsDisplayed()
    }

    @Test
    fun notFound_saysSo() {
        composeRule.setContent {
            TrailsTheme { LotteryDetailsContent(LotteryDetailsUiState.NotFound, null, {}, {}, {}) }
        }
        composeRule.onNodeWithText("This lottery no longer exists.").assertIsDisplayed()
    }

    // Lotteries tab (Figma F06): one card per lottery with its badge and next step.
    @Test
    fun lotteriesTab_showsCardsAndOpensOne() {
        var opened: String? = null
        composeRule.setContent {
            TrailsTheme {
                MyLotteriesContent(
                    state = buildMyLotteriesState(listOf(scheduled, open, closed), mapOf("summer" to 24, "spring" to 31), LotteryFilter.ALL, now),
                    onFilterChange = {}, onOpenLottery = { opened = it }, onCreate = {}
                )
            }
        }
        composeRule.onNodeWithText("Registration opens ${formatDateTime(scheduled.registrationStart.millis()!!)}").assertIsDisplayed()
        composeRule.onNodeWithText("24 entrants · 5 winners").assertIsDisplayed()
        composeRule.onNodeWithText("5 winners drawn from 31 entrants").assertIsDisplayed()
        composeRule.onNodeWithText("Summer weekends").performClick()
        assertEquals("summer", opened)
    }

    @Test
    fun lotteriesTab_filterAndCreate() {
        var chip: LotteryFilter? = null
        var created = false
        composeRule.setContent {
            TrailsTheme {
                MyLotteriesContent(
                    state = buildMyLotteriesState(listOf(scheduled), emptyMap(), LotteryFilter.ALL, now),
                    onFilterChange = { chip = it }, onOpenLottery = {}, onCreate = { created = true }
                )
            }
        }
        composeRule.onNodeWithTag(LotteryTags.filter(LotteryFilter.DONE)).performClick()
        assertEquals(LotteryFilter.DONE, chip)
        composeRule.onNodeWithTag(LotteryTags.CREATE).performClick()
        assertTrue(created)
    }

    @Test
    fun lotteriesTab_noLotteries_showsEmptyMessage() {
        composeRule.setContent {
            TrailsTheme {
                MyLotteriesContent(buildMyLotteriesState(emptyList(), emptyMap(), LotteryFilter.ALL, now), {}, {}, {})
            }
        }
        composeRule.onNodeWithText("No lotteries yet").assertIsDisplayed()
    }
}
