// UI (intent) tests for the admin's read-only Profile page, US 03.05.01.
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
package com.example.trails.ui.admin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdminProfileContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val daniel = User(
        deviceId = "device-daniel", name = "Daniel Whitehorse", email = "daniel.whitehorse@example.com",
        phone = "403-555-0187", roles = listOf(Role.ENTRANT, Role.OWNER)
    )
    private val priya = User(
        deviceId = "device-priya", name = "Priya Sharma", email = "priya.sharma@example.com",
        phone = "780-555-0142"
    )

    private fun show(state: AdminProfileUiState, onBack: () -> Unit = {}) =
        composeRule.setContent { TrailsTheme { AdminProfileContent(state, onBack) } }

    // This checks that a loaded profile shows the name, email and a chip for each role held.
    @Test
    fun loaded_showsNameEmailAndRoles() {
        show(AdminProfileUiState.Loaded(daniel, 2))
        composeRule.onNodeWithTag(AdminProfileTags.NAME).assertTextEquals("Daniel Whitehorse")
        composeRule.onNodeWithText("daniel.whitehorse@example.com").assertIsDisplayed()
        composeRule.onNodeWithTag(AdminProfileTags.ROLES).assertIsDisplayed()
        composeRule.onNodeWithText("Owner").assertIsDisplayed()
        composeRule.onNodeWithText("Entrant").assertIsDisplayed()
        composeRule.onNodeWithText("Admin").assertDoesNotExist()
        composeRule.onNodeWithText("DW").assertIsDisplayed()
    }

    // This checks that an owner's profile shows how many campsites they own.
    @Test
    fun owner_showsCampsitesOwned() {
        show(AdminProfileUiState.Loaded(daniel, 2))
        composeRule.onNodeWithText("Campsites owned").assertIsDisplayed()
        composeRule.onNodeWithTag(AdminProfileTags.CAMPSITES).assertTextEquals("2")
    }

    // This checks that a plain entrant's profile has no Campsites owned row.
    @Test
    fun entrant_hidesCampsitesOwned() {
        show(AdminProfileUiState.Loaded(priya, 0))
        composeRule.onNodeWithTag(AdminProfileTags.PHONE).assertTextEquals("780-555-0142")
        composeRule.onNodeWithText("Campsites owned").assertDoesNotExist()
        composeRule.onNodeWithTag(AdminProfileTags.CAMPSITES).assertDoesNotExist()
    }

    // This checks that a user with no phone number shows "Not provided" on the Phone row.
    @Test
    fun blankPhone_showsNotProvided() {
        show(AdminProfileUiState.Loaded(priya.copy(phone = ""), 0))
        composeRule.onNodeWithTag(AdminProfileTags.PHONE).assertTextEquals("Not provided")
    }

    // This checks that a removed profile shows a message instead of the details.
    @Test
    fun notFound_showsMessage() {
        show(AdminProfileUiState.NotFound)
        composeRule.onNodeWithTag(AdminProfileTags.NOT_FOUND).assertIsDisplayed()
        composeRule.onNodeWithText("This profile no longer exists").assertIsDisplayed()
        composeRule.onNodeWithTag(AdminProfileTags.NAME).assertDoesNotExist()
    }

    // This checks that while the profile is loading, a spinner is shown and no details yet.
    @Test
    fun loading_showsSpinner() {
        show(AdminProfileUiState.Loading)
        composeRule.onNodeWithTag(AdminProfileTags.LOADING).assertIsDisplayed()
        composeRule.onNodeWithTag(AdminProfileTags.NAME).assertDoesNotExist()
    }

    // This checks that tapping the back arrow asks to leave the page.
    @Test
    fun tapBack_reportsBack() {
        var wentBack = false
        show(AdminProfileUiState.Loaded(priya, 0), onBack = { wentBack = true })
        composeRule.onNodeWithTag(AdminProfileTags.BACK).performClick()
        assertTrue(wentBack)
    }
}
