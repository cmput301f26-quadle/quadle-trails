// UI (intent) tests for the empty Notifications tab.
// OWNER: Zainab (zsaghir) — App shell
package com.example.trails.ui.shell

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Rule
import org.junit.Test

class NotificationsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    // This checks that the top bar title, the empty message and its hint are on screen.
    @Test
    fun showsEmptyMessage() {
        composeRule.setContent { TrailsTheme { NotificationsContent() } }
        composeRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeRule.onNodeWithTag(NotificationsTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("No notifications yet").assertIsDisplayed()
        composeRule.onNodeWithText("Lottery results and booking updates will appear here.").assertIsDisplayed()
    }
}
