// OWNER: Zainab (zsaghir) — App shell; list states by Aarav (aarav-kapoor-cs) — Notifications
// PURPOSE: UI (intent) tests for the Notifications tab: empty, loading, error and list states
//  (US 01.03.01, 01.03.02).
// Outstanding issues: none.
// AI assistance: Used AI to help write Compose UI tests for the Notifications tab's states.
package com.example.trails.ui.shell

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.trails.data.model.Notification
import com.example.trails.data.model.NotificationType
import com.example.trails.ui.notification.NotificationsUiState
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** UI tests for [NotificationsContent], driven with fixed states. */
class NotificationsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val won = Notification(
        recipient = "device-entrant-001",
        title = "You've won the lottery for Tunnel Mountain Site 14",
        content = "May long weekend",
        type = NotificationType.LOTTERY_SINGLE
    )
    private val lost = Notification(
        recipient = "device-entrant-001",
        title = "Lottery not picked for Whistlers Riverside Site 7 this time",
        type = NotificationType.LOTTERY_LOST
    )

    // This checks that the top bar title, the empty message and its hint are on screen.
    @Test
    fun showsEmptyMessage() {
        composeRule.setContent { TrailsTheme { NotificationsContent() } }
        composeRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeRule.onNodeWithTag(NotificationsTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("No notifications yet").assertIsDisplayed()
        composeRule.onNodeWithText("Lottery results and booking updates will appear here.").assertIsDisplayed()
    }

    @Test
    fun loading_showsSpinner() {
        composeRule.setContent { TrailsTheme { NotificationsContent(state = NotificationsUiState.Loading) } }
        composeRule.onNodeWithTag(NotificationsTags.LOADING).assertIsDisplayed()
    }

    @Test
    fun error_showsMessageAndRetry() {
        var retries = 0
        composeRule.setContent {
            TrailsTheme { NotificationsContent(state = NotificationsUiState.Error("offline"), onRetry = { retries++ }) }
        }
        composeRule.onNodeWithTag(NotificationsTags.ERROR).assertIsDisplayed()
        composeRule.onNodeWithTag(NotificationsTags.RETRY).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun loaded_handsEveryNotificationToTheList() {
        composeRule.setContent {
            TrailsTheme {
                NotificationsContent(
                    state = NotificationsUiState.Loaded(listOf(won, lost)),
                    listContent = { items -> items.forEach { Text(it.title) } }
                )
            }
        }
        composeRule.onNodeWithTag(NotificationsTags.LIST).assertIsDisplayed()
        composeRule.onNodeWithText("You've won the lottery for Tunnel Mountain Site 14").assertIsDisplayed()
        composeRule.onNodeWithText("Lottery not picked for Whistlers Riverside Site 7 this time").assertIsDisplayed()
        composeRule.onNodeWithTag(NotificationsTags.EMPTY).assertDoesNotExist()
    }
}
