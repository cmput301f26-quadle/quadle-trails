// OWNER: Zainab (zsaghir) — App shell
// PURPOSE: UI (intent) tests for the app shell: the bottom bar driving real navigation.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.trails.ui.shell.SettingsTags
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// The default user is the sample entrant, so the app starts on Explore (Browse).
class TrailsAppTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun showApp() = composeRule.setContent { TrailsTheme { TrailsApp() } }

    private fun tapTab(tab: BottomNavTab) =
        composeRule.onNodeWithTag(BottomBarTags.tab(tab)).performClick()

    private fun assertTabSelected(tab: BottomNavTab) =
        composeRule.onNodeWithTag(BottomBarTags.tab(tab)).assertIsSelected()

    // This checks that an entrant opens the app on Explore with the bar showing and Home highlighted.
    @Test
    fun appStart_entrant_homeSelected() {
        composeRule.onNodeWithTag(BottomBarTags.BAR).assertIsDisplayed()
        composeRule.onNodeWithText("Browse").assertIsDisplayed()
        assertTabSelected(BottomNavTab.HOME)
    }

    // This checks that tapping the gear opens Settings and highlights the Settings tab.
    @Test
    fun tapSettings_opensSettings_settingsSelected() {
        tapTab(BottomNavTab.SETTINGS)
        composeRule.onNodeWithTag(SettingsTags.SCREEN).assertIsDisplayed()
        assertTabSelected(BottomNavTab.SETTINGS)
    }

    // This checks that going Settings -> Profile still highlights Settings, since Profile lives under it.
    @Test
    fun settingsThenProfile_settingsStaysSelected() {
        tapTab(BottomNavTab.SETTINGS)
        composeRule.onNodeWithTag(SettingsTags.PROFILE_ROW).performClick()
        composeRule.onNodeWithTag(SettingsTags.SCREEN).assertDoesNotExist()
        composeRule.onNodeWithText("Profile").assertIsDisplayed()
        assertTabSelected(BottomNavTab.SETTINGS)
    }

    // This checks that the bell opens the Notifications placeholder saying it comes in Part 4.
    @Test
    fun tapNotifications_showsComingInPart4() {
        tapTab(BottomNavTab.NOTIFICATIONS)
        composeRule.onNodeWithText("Coming in Part 4", substring = true).assertIsDisplayed()
        assertTabSelected(BottomNavTab.NOTIFICATIONS)
    }

    // This checks that Home brings an entrant back to Explore after visiting Settings.
    @Test
    fun settingsThenHome_backToExplore_homeSelected() {
        tapTab(BottomNavTab.SETTINGS)
        tapTab(BottomNavTab.HOME)
        composeRule.onNodeWithTag(SettingsTags.SCREEN).assertDoesNotExist()
        composeRule.onNodeWithText("Browse").assertIsDisplayed()
        assertTabSelected(BottomNavTab.HOME)
    }
}
