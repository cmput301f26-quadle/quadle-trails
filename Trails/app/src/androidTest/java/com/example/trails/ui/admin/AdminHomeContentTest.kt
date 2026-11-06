// UI (intent) tests for the admin home, US 03.04.01 and 03.05.01.
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
package com.example.trails.ui.admin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AdminHomeContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val state = AdminHomeUiState(greeting = "Good morning, Maya", campsiteCount = 12, profileCount = 48)

    private fun show(
        onCampsites: () -> Unit = {},
        onProfiles: () -> Unit = {}
    ) = composeRule.setContent { TrailsTheme { AdminHomeContent(state, onCampsites, onProfiles) } }

    // This checks that the greeting, the muted line and both tiles with their counts are on screen.
    @Test
    fun showsGreetingAndCounts() {
        show()
        composeRule.onNodeWithText("Good morning, Maya").assertIsDisplayed()
        composeRule.onNodeWithText("Here's what needs your attention today.").assertIsDisplayed()
        composeRule.onNodeWithText("12").assertIsDisplayed()
        composeRule.onNodeWithText("Campsites ›").assertIsDisplayed()
        composeRule.onNodeWithText("48").assertIsDisplayed()
        composeRule.onNodeWithText("Profiles ›").assertIsDisplayed()
    }

    // This checks that tapping the Campsites tile reports a campsites click and nothing else.
    @Test
    fun tapCampsites_reportsClick() {
        var campsites = 0
        var profiles = 0
        show(onCampsites = { campsites++ }, onProfiles = { profiles++ })
        composeRule.onNodeWithTag(AdminHomeTags.CAMPSITES).performClick()
        assertEquals(1, campsites)
        assertEquals(0, profiles)
    }

    // This checks that tapping the Profiles tile reports a profiles click and nothing else.
    @Test
    fun tapProfiles_reportsClick() {
        var campsites = 0
        var profiles = 0
        show(onCampsites = { campsites++ }, onProfiles = { profiles++ })
        composeRule.onNodeWithTag(AdminHomeTags.PROFILES).performClick()
        assertEquals(0, campsites)
        assertEquals(1, profiles)
    }
}
