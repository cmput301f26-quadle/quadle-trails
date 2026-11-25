// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.07.01
// PURPOSE: UI (intent) tests for the admin's Policy reports list.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.ui.admin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.trails.data.model.Report
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.example.trails.ui.theme.TrailsTheme
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

class ReportsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun dateOf(month: Int, day: Int) =
        Timestamp(Calendar.getInstance().apply { set(2026, month, day, 12, 0, 0) }.time)

    private val maya = User(deviceId = "device-maya", name = "Maya Singh", roles = listOf(Role.OWNER))
    private val omar = User(deviceId = "device-omar", name = "Omar Haddad", roles = listOf(Role.OWNER))

    private val owners = listOf(
        ReportedOwner(
            maya,
            listOf(Report(id = "r1", ownerId = maya.deviceId), Report(id = "r2", ownerId = maya.deviceId),
                Report(id = "r3", ownerId = maya.deviceId)),
            campsiteCount = 3,
            oldestReportAt = dateOf(Calendar.NOVEMBER, 1)
        ),
        ReportedOwner(
            omar,
            listOf(Report(id = "r4", ownerId = omar.deviceId)),
            campsiteCount = 1,
            oldestReportAt = dateOf(Calendar.NOVEMBER, 20)
        )
    )

    private fun show(state: ReportsUiState, onOpenOwner: (String) -> Unit = {}) =
        composeRule.setContent {
            TrailsTheme { ReportsContent(state, onBack = {}, onOpenOwner = onOpenOwner) }
        }

    // This checks that each reported owner is listed with their report count and oldest date.
    @Test
    fun loaded_showsOwnersWithCountAndOldest() {
        show(ReportsUiState.Loaded(owners))
        composeRule.onNodeWithText("Policy reports").assertIsDisplayed()
        composeRule.onNodeWithText("Maya Singh").assertIsDisplayed()
        composeRule.onNodeWithText("3 reports · oldest Nov 1").assertIsDisplayed()
        composeRule.onNodeWithText("Omar Haddad").assertIsDisplayed()
        composeRule.onNodeWithText("1 report · oldest Nov 20").assertIsDisplayed()
        composeRule.onNodeWithText("MS").assertIsDisplayed()
    }

    // This checks that tapping an owner's row asks to open their review page with their id.
    @Test
    fun tapOwner_reportsId() {
        var opened: String? = null
        show(ReportsUiState.Loaded(owners), onOpenOwner = { opened = it })
        composeRule.onNodeWithTag(ReportsTags.row("device-omar")).performClick()
        assertEquals("device-omar", opened)
    }

    // This checks that with no open reports the empty message is shown instead of a list.
    @Test
    fun empty_showsNoOpenReports() {
        show(ReportsUiState.Loaded(emptyList()))
        composeRule.onNodeWithTag(ReportsTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("No open reports").assertIsDisplayed()
        composeRule.onNodeWithTag(ReportsTags.LIST).assertDoesNotExist()
    }

    // This checks that while loading, a spinner is shown.
    @Test
    fun loading_showsSpinner() {
        show(ReportsUiState.Loading)
        composeRule.onNodeWithTag(ReportsTags.LOADING).assertIsDisplayed()
    }
}
