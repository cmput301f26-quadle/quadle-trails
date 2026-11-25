// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.04.01, 03.05.01, 03.07.01
// PURPOSE: UI (intent) tests for the admin home screen.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.ui.admin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
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

    private val withReports = state.copy(
        openReportCount = 3, reportCountLine = "3 policy reports", oldestReportLine = "Oldest report: 2 days ago"
    )

    private fun show(
        onCampsites: () -> Unit = {},
        onProfiles: () -> Unit = {},
        onNeedsReview: () -> Unit = {},
        shown: AdminHomeUiState = state
    ) = composeRule.setContent { TrailsTheme { AdminHomeContent(shown, onCampsites, onProfiles, onNeedsReview) } }

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

    // This checks that with open reports the Needs review card shows its chip, count and oldest report.
    @Test
    fun openReports_showsNeedsReviewCard() {
        show(shown = withReports)
        composeRule.onNodeWithTag(AdminHomeTags.NEEDS_REVIEW).assertIsDisplayed()
        composeRule.onNodeWithText("Needs review").assertIsDisplayed()
        composeRule.onNodeWithTag(AdminHomeTags.NEEDS_REVIEW_COUNT, useUnmergedTree = true).assertTextEquals("3 policy reports")
        composeRule.onNodeWithTag(AdminHomeTags.NEEDS_REVIEW_OLDEST, useUnmergedTree = true).assertTextEquals("Oldest report: 2 days ago")
    }

    // This checks that tapping the Needs review card reports a needs-review click and nothing else.
    @Test
    fun tapNeedsReview_reportsClick() {
        var campsites = 0
        var profiles = 0
        var review = 0
        show(onCampsites = { campsites++ }, onProfiles = { profiles++ }, onNeedsReview = { review++ }, shown = withReports)
        composeRule.onNodeWithTag(AdminHomeTags.NEEDS_REVIEW).performClick()
        assertEquals(1, review)
        assertEquals(0, campsites)
        assertEquals(0, profiles)
    }

    // This checks that with no open reports the Needs review card is hidden.
    @Test
    fun noOpenReports_hidesNeedsReviewCard() {
        show()
        composeRule.onNodeWithTag(AdminHomeTags.NEEDS_REVIEW).assertDoesNotExist()
        composeRule.onNodeWithText("Needs review").assertDoesNotExist()
    }
}
