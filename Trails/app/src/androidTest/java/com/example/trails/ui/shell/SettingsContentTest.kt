// OWNER: Zainab (zsaghir) — App shell
// PURPOSE: UI (intent) tests for the Settings tab.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.ui.shell

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        onProfile: () -> Unit = {},
        onPolicy: () -> Unit = {}
    ) = composeRule.setContent { TrailsTheme { SettingsContent(onProfile, onPolicy) } }

    // This checks that the Settings title and both rows are on screen with the right words.
    @Test
    fun settings_showsBothRows() {
        show()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTags.PROFILE_ROW).assertIsDisplayed()
        composeRule.onNodeWithText("Profile").assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTags.POLICY_ROW).assertIsDisplayed()
        composeRule.onNodeWithText("App policy & guidelines").assertIsDisplayed()
    }

    // This checks that tapping "Profile" reports a profile click and nothing else.
    @Test
    fun tapProfile_reportsProfileClick() {
        var profile = 0
        var policy = 0
        show(onProfile = { profile++ }, onPolicy = { policy++ })
        composeRule.onNodeWithTag(SettingsTags.PROFILE_ROW).performClick()
        assertEquals(1, profile)
        assertEquals(0, policy)
    }

    // This checks that tapping "App policy & guidelines" reports a policy click and nothing else.
    @Test
    fun tapPolicy_reportsPolicyClick() {
        var profile = 0
        var policy = 0
        show(onProfile = { profile++ }, onPolicy = { policy++ })
        composeRule.onNodeWithTag(SettingsTags.POLICY_ROW).performClick()
        assertEquals(0, profile)
        assertEquals(1, policy)
    }
}
