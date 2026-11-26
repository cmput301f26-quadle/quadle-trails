// AI assistance: used ai to understand and modify lines of codes to tailor to my requirements
// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 03.12.01
// PURPOSE: UI tests for the admin message log and its tile on the admin home.
// Outstanding issues: none.
package com.example.trails.ui.messaging

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestReports
import com.example.trails.data.fake.TestMessages
import com.example.trails.data.fake.TestUsers
import com.example.trails.ui.admin.AdminHomeContent
import com.example.trails.ui.admin.buildAdminHomeState
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AdminMessageLogContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val names = TestUsers.all.associate { it.deviceId to it.name }

    private fun show(state: AdminMessageLogUiState, query: String = "", onQuery: (String) -> Unit = {}) {
        composeRule.setContent {
            TrailsTheme { AdminMessageLogContent(state, query, onQuery, onBack = {}) }
        }
    }

    // US 03.12.01: owner-to-camper messages are listed with who sent them, to whom, and where.
    @Test
    fun log_showsOwnerMessagesOnly() {
        show(AdminMessageLogUiState.Loaded(adminMessageLog(TestMessages.all, names), totalCount = 1))
        composeRule.onAllNodesWithTag(AdminMessageLogTags.ROW).assertCountEquals(1)
        composeRule.onNodeWithText("Daniel Whitehorse → Priya Sharma").assertIsDisplayed()
        composeRule.onNodeWithText("Tunnel Mountain Site 14").assertIsDisplayed()
        composeRule.onNodeWithText("Hi Priya, thanks for booking! Check-in is after 2pm.").assertIsDisplayed()
        composeRule.onNodeWithText("Thanks Daniel! Is there firewood on site?").assertDoesNotExist()
        composeRule.onNodeWithText("1 message").assertIsDisplayed()
    }

    // Read only: the only text field is the search box, and there's no send button.
    @Test
    fun log_isReadOnly() {
        show(AdminMessageLogUiState.Loaded(adminMessageLog(TestMessages.all, names), totalCount = 1))
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(1)
        composeRule.onNodeWithTag(AdminMessageLogTags.SEARCH).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatTags.SEND).assertDoesNotExist()
    }

    @Test
    fun typingInSearch_reportsTheQuery() {
        var query = ""
        show(AdminMessageLogUiState.Loaded(adminMessageLog(TestMessages.all, names), totalCount = 1)) { query = it }
        composeRule.onNodeWithTag(AdminMessageLogTags.SEARCH).performTextInput("firewood")
        assertEquals("firewood", query)
    }

    @Test
    fun noMatch_andNoMessages_showDifferentNotes() {
        show(AdminMessageLogUiState.Loaded(emptyList(), totalCount = 1), query = "xyz")
        composeRule.onNodeWithText("No messages match \"xyz\".").assertIsDisplayed()
    }

    @Test
    fun noMessagesYet_showsEmptyNote() {
        show(AdminMessageLogUiState.Loaded(emptyList(), totalCount = 0))
        composeRule.onNodeWithTag(AdminMessageLogTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("No owner messages yet.").assertIsDisplayed()
    }

    @Test
    fun nonAdmin_seesNotAllowed() {
        show(AdminMessageLogUiState.NotAllowed)
        composeRule.onNodeWithTag(AdminMessageLogTags.NOT_ALLOWED).assertIsDisplayed()
        composeRule.onNodeWithTag(AdminMessageLogTags.ROW).assertDoesNotExist()
    }

    // The admin home's Message log tile opens the log.
    @Test
    fun adminHomeTile_opensTheLog() {
        var opened = false
        composeRule.setContent {
            TrailsTheme {
                AdminHomeContent(
                    state = buildAdminHomeState(
                        user = TestUsers.all.first { it.deviceId == TestUsers.ADMIN_ID },
                        campsites = TestCampsites.all,
                        users = TestUsers.all,
                        hour = 9,
                        reports = TestReports.all
                    ),
                    onCampsitesClick = {},
                    onProfilesClick = {},
                    onMessageLogClick = { opened = true }
                )
            }
        }
        composeRule.onNodeWithText("Message log ›").assertIsDisplayed().performClick()
        assertEquals(true, opened)
    }
}
