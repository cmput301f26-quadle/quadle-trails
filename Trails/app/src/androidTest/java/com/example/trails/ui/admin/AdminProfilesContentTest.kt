// UI (intent) tests for the admin's Profiles screen, US 03.05.01.
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
package com.example.trails.ui.admin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdminProfilesContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val priya = User(
        deviceId = "device-priya", name = "Priya Sharma", email = "priya.sharma@example.com"
    )
    private val daniel = User(
        deviceId = "device-daniel", name = "Daniel Whitehorse", email = "daniel.whitehorse@example.com",
        roles = listOf(Role.ENTRANT, Role.OWNER)
    )
    private val morgan = User(
        deviceId = "device-morgan", name = "Morgan Lee", email = "morgan.lee@example.com",
        roles = listOf(Role.ENTRANT, Role.ADMIN)
    )
    private val threeProfiles = AdminProfilesUiState(
        items = listOf(
            AdminProfileItem(daniel, 2),
            AdminProfileItem(morgan, 0),
            AdminProfileItem(priya, 0)
        ),
        query = "", filter = ProfileFilter.ALL, totalCount = 3
    )

    private fun show(
        state: AdminProfilesUiState?,
        onQuery: (String) -> Unit = {},
        onFilter: (ProfileFilter) -> Unit = {},
        onBack: () -> Unit = {},
        onOpen: (String) -> Unit = {}
    ) = composeRule.setContent {
        TrailsTheme { AdminProfilesContent(state, onQuery, onFilter, onBack, onOpen) }
    }

    // This checks that every profile shows its name and the grey role line under it.
    @Test
    fun profiles_shownWithSubtitle() {
        show(threeProfiles)
        composeRule.onNodeWithText("Daniel Whitehorse").assertIsDisplayed()
        composeRule.onNodeWithText("Owner · 2 campsites").assertIsDisplayed()
        composeRule.onNodeWithText("Morgan Lee").assertIsDisplayed()
        composeRule.onNodeWithText("Admin").assertIsDisplayed()
        composeRule.onNodeWithText("Priya Sharma").assertIsDisplayed()
        composeRule.onNodeWithText("Entrant").assertIsDisplayed()
    }

    // This checks that the line above the list counts every profile and shows the A–Z sort.
    @Test
    fun countLine_showsTotal() {
        show(threeProfiles)
        composeRule.onNodeWithText("3 profiles").assertIsDisplayed()
        composeRule.onNodeWithText("A–Z").assertIsDisplayed()
    }

    // This checks that the All chip is the selected one when the screen opens.
    @Test
    fun allChipSelectedByDefault() {
        show(threeProfiles)
        composeRule.onNodeWithTag(AdminProfilesTags.chip(ProfileFilter.ALL)).assertIsSelected()
        composeRule.onNodeWithTag(AdminProfilesTags.chip(ProfileFilter.OWNERS)).assertIsNotSelected()
        composeRule.onNodeWithTag(AdminProfilesTags.chip(ProfileFilter.ENTRANTS)).assertIsNotSelected()
        composeRule.onNodeWithTag(AdminProfilesTags.chip(ProfileFilter.ADMINS)).assertIsNotSelected()
    }

    // This checks that tapping the Owners chip passes the Owners filter up to the ViewModel.
    @Test
    fun tapOwnersChip_reportsOwnersFilter() {
        var filter: ProfileFilter? = null
        show(threeProfiles, onFilter = { filter = it })
        composeRule.onNodeWithTag(AdminProfilesTags.chip(ProfileFilter.OWNERS)).performClick()
        assertEquals(ProfileFilter.OWNERS, filter)
    }

    // This checks that typing in the search box passes the text up to the ViewModel.
    @Test
    fun typingInSearch_reportsQuery() {
        var query = ""
        show(threeProfiles, onQuery = { query = it })
        composeRule.onNodeWithTag(AdminProfilesTags.SEARCH).performTextInput("morgan")
        assertEquals("morgan", query)
    }

    // This checks that tapping a row reports only that user's id so their profile can open.
    @Test
    fun tapProfile_reportsItsId() {
        var opened: String? = null
        show(threeProfiles, onOpen = { opened = it })
        composeRule.onNodeWithTag(AdminProfilesTags.row(morgan.deviceId)).performClick()
        assertEquals("device-morgan", opened)
    }

    // This checks that tapping the back arrow asks to leave the screen.
    @Test
    fun tapBack_reportsBack() {
        var wentBack = false
        show(threeProfiles, onBack = { wentBack = true })
        composeRule.onNodeWithTag(AdminProfilesTags.BACK).performClick()
        assertTrue(wentBack)
    }

    // This checks that with no profiles in the app, "No profiles yet" is shown instead of a list.
    @Test
    fun noProfiles_showsEmptyMessage() {
        show(AdminProfilesUiState(emptyList(), "", ProfileFilter.ALL, 0))
        composeRule.onNodeWithTag(AdminProfilesTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("No profiles yet").assertIsDisplayed()
        composeRule.onNodeWithTag(AdminProfilesTags.LIST).assertDoesNotExist()
    }

    // This checks that when profiles exist but none match the search, "No profiles match" is shown.
    @Test
    fun searchWithNoMatch_showsNoMatchMessage() {
        show(AdminProfilesUiState(emptyList(), "zoe", ProfileFilter.ALL, 3))
        composeRule.onNodeWithTag(AdminProfilesTags.NO_MATCH).assertIsDisplayed()
        composeRule.onNodeWithText("No profiles match").assertIsDisplayed()
        composeRule.onNodeWithTag(AdminProfilesTags.LIST).assertDoesNotExist()
    }

    // This checks that before the first load finishes, a spinner is shown and no search box yet.
    @Test
    fun nullState_showsLoading() {
        show(null)
        composeRule.onNodeWithTag(AdminProfilesTags.LOADING).assertIsDisplayed()
        composeRule.onNodeWithTag(AdminProfilesTags.SEARCH).assertDoesNotExist()
    }

    // This checks that a user without a photo gets a circle with their initials instead.
    @Test
    fun noPhoto_showsInitials() {
        show(threeProfiles)
        composeRule.onNodeWithText("PS").assertIsDisplayed()
        composeRule.onNodeWithText("DW").assertIsDisplayed()
        composeRule.onNodeWithText("ML").assertIsDisplayed()
    }
}
