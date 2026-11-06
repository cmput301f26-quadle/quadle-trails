// OWNER: Zainab (zsaghir) — App shell
// PURPOSE: UI (intent) tests for the empty Messages tab.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.ui.shell

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Rule
import org.junit.Test

class MessagesContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    // This checks that the top bar title, the empty message and its hint are on screen.
    @Test
    fun showsEmptyMessage() {
        composeRule.setContent { TrailsTheme { MessagesContent() } }
        composeRule.onNodeWithText("Messages").assertIsDisplayed()
        composeRule.onNodeWithTag(MessagesTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("No messages yet").assertIsDisplayed()
        composeRule.onNodeWithText("Messages from campsite owners will appear here.").assertIsDisplayed()
    }
}
