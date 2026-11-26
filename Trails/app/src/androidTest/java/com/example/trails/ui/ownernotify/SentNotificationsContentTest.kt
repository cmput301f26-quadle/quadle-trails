// Intent tests for Sent notifications and a sent notification's details (US 02.10.02).
// OWNER: Aarav (aarav-kapoor-cs) — Owner notifications
// Outstanding issues: None currently known.
// AI assistance: Used AI to help write Compose UI tests for the sent-notification history.
package com.example.trails.ui.ownernotify

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.trails.data.model.SentNotification
import com.example.trails.data.model.SentRecipient
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI tests for [SentNotificationsContent] and [SentNotificationDetailsContent]. */
@RunWith(AndroidJUnit4::class)
class SentNotificationsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val drawUpdate = SentNotification(
        id = "draw", subject = "Lottery draw date update", campsiteName = "Grande Cache Ridge", lotteryName = "June 2027",
        message = "Hi! The June lottery draw for Grande Cache Ridge is now on May 24, 2027.",
        recipients = listOf(SentRecipient("maya", "Maya Singh", 2), SentRecipient("daniel", "Daniel Kim", 4), SentRecipient("lena", "Lena Fischer", 3))
    )

    private var opened: String? = null
    private var chip: SentFilter? = null

    private fun showList(state: SentListUiState) {
        composeRule.setContent {
            TrailsTheme {
                SentNotificationsContent(state, onBack = {}, onFilterChange = { chip = it }, onOpen = { opened = it })
            }
        }
    }

    @Test
    fun list_showsCountAndCard() {
        showList(SentListUiState(isLoading = false, items = listOf(drawUpdate)))
        composeRule.onNodeWithText("Sent notifications").assertIsDisplayed()
        composeRule.onNodeWithTag(SentTags.COUNT).assertTextContains("1 sent", substring = true)
        composeRule.onNodeWithTag(SentTags.card("draw")).assertTextContains("To: 3 entrants · Grande Cache Ridge", substring = true)
    }

    @Test
    fun tappingCard_opensDetails() {
        showList(SentListUiState(isLoading = false, items = listOf(drawUpdate)))
        composeRule.onNodeWithTag(SentTags.card("draw")).performClick()
        assertEquals("draw", opened)
    }

    @Test
    fun chips_reportTheFilter() {
        showList(SentListUiState(isLoading = false))
        composeRule.onNodeWithTag(SentTags.chip(SentFilter.WEEK)).performClick()
        assertEquals(SentFilter.WEEK, chip)
    }

    @Test
    fun empty_showsMessage() {
        showList(SentListUiState(isLoading = false))
        composeRule.onNodeWithTag(SentTags.EMPTY).assertIsDisplayed()
    }

    @Test
    fun details_showMessageAndRecipients() {
        composeRule.setContent {
            TrailsTheme { SentNotificationDetailsContent(SentDetailsUiState.Loaded(drawUpdate), onBack = {}, onRetry = {}) }
        }
        composeRule.onNodeWithTag(SentTags.SUBJECT).assertTextContains("Lottery draw date update")
        composeRule.onNodeWithTag(SentTags.MESSAGE).assertTextContains("May 24, 2027", substring = true)
        composeRule.onNodeWithTag(SentTags.RECIPIENTS).performScrollTo().assertTextContains("Recipients (3)")
        composeRule.onNodeWithText("Waitlist entrant · Party of 4").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun details_notFound() {
        composeRule.setContent {
            TrailsTheme { SentNotificationDetailsContent(SentDetailsUiState.NotFound, onBack = {}, onRetry = {}) }
        }
        composeRule.onNodeWithText("This notification no longer exists.").assertIsDisplayed()
    }
}
