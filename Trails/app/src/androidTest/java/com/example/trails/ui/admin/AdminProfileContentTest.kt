// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.05.01, 03.02.01
// PURPOSE: UI (intent) tests for the admin's Profile page and its Remove profile dialog.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.ui.admin

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    private fun show(
        state: AdminProfileUiState,
        onBack: () -> Unit = {},
        onRemoveClick: () -> Unit = {},
        onDismiss: () -> Unit = {},
        onConfirmRemove: () -> Unit = {}
    ) = composeRule.setContent {
        TrailsTheme {
            AdminProfileContent(
                state, onBack,
                onRemoveClick = onRemoveClick, onDismiss = onDismiss, onConfirmRemove = onConfirmRemove
            )
        }
    }

    private fun danielWith(removal: ProfileRemovalState) =
        AdminProfileUiState.Loaded(daniel, 2, isSelf = false, lotteryEntryCount = 1, removal = removal)

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

    // This checks that someone else's profile shows the Remove profile button.
    @Test
    fun otherUser_showsRemoveButton() {
        show(danielWith(ProfileRemovalState.Idle))
        composeRule.onNodeWithTag(AdminProfileTags.REMOVE_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithText("Remove profile").assertIsDisplayed()
    }

    // This checks that the admin can't remove their own profile, so the button is hidden.
    @Test
    fun self_hidesRemoveButton() {
        show(AdminProfileUiState.Loaded(daniel, 2, isSelf = true))
        composeRule.onNodeWithTag(AdminProfileTags.REMOVE_BUTTON).assertDoesNotExist()
    }

    // This checks that tapping Remove profile asks to open the dialog, and the open dialog shows
    // the title and what is removed.
    @Test
    fun tapRemove_showsDialogWithMessage() {
        var state by mutableStateOf<AdminProfileUiState>(danielWith(ProfileRemovalState.Idle))
        composeRule.setContent {
            TrailsTheme {
                AdminProfileContent(
                    state, onBack = {},
                    onRemoveClick = { state = danielWith(ProfileRemovalState.Confirming) }
                )
            }
        }
        composeRule.onNodeWithTag(AdminProfileTags.DIALOG).assertDoesNotExist()
        composeRule.onNodeWithTag(AdminProfileTags.REMOVE_BUTTON).performClick()
        composeRule.onNodeWithTag(AdminProfileTags.DIALOG).assertIsDisplayed()
        composeRule.onNodeWithText("Remove profile?").assertIsDisplayed()
        composeRule.onNodeWithTag(AdminProfileTags.DIALOG_MESSAGE).assertTextEquals(
            "Daniel Whitehorse loses access to Quadle. Their 2 campsites and 1 lottery entry are removed. " +
                "This can't be undone."
        )
    }

    // This checks that Cancel in the dialog asks to close it.
    @Test
    fun dialogCancel_reportsDismiss() {
        var dismissed = false
        show(danielWith(ProfileRemovalState.Confirming), onDismiss = { dismissed = true })
        composeRule.onNodeWithTag(AdminProfileTags.DIALOG_CANCEL).performClick()
        assertTrue(dismissed)
    }

    // This checks that Remove in the dialog asks to remove the profile.
    @Test
    fun dialogRemove_reportsConfirm() {
        var confirmed = false
        show(danielWith(ProfileRemovalState.Confirming), onConfirmRemove = { confirmed = true })
        composeRule.onNodeWithTag(AdminProfileTags.DIALOG_REMOVE).performClick()
        assertTrue(confirmed)
    }

    // This checks that while the removal runs the dialog stays open with both buttons disabled.
    @Test
    fun removing_buttonsDisabled() {
        show(danielWith(ProfileRemovalState.Removing))
        composeRule.onNodeWithTag(AdminProfileTags.DIALOG).assertIsDisplayed()
        composeRule.onNodeWithTag(AdminProfileTags.DIALOG_CANCEL).assertIsNotEnabled()
        composeRule.onNodeWithTag(AdminProfileTags.DIALOG_REMOVE).assertIsNotEnabled()
    }

    // This checks that a failed removal closes the dialog and shows an error.
    @Test
    fun error_isShown() {
        show(danielWith(ProfileRemovalState.Error))
        composeRule.onNodeWithTag(AdminProfileTags.DIALOG).assertDoesNotExist()
        composeRule.onNodeWithTag(AdminProfileTags.ERROR).assertIsDisplayed()
        composeRule.onNodeWithText("Couldn't remove this profile. Please try again.").assertIsDisplayed()
    }
}
