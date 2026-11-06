// Intent tests for profile editing and the delete confirmation (US 01.07.03, 01.08.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: None currently known.
// AI assistance: Used AI to help write Compose UI tests for profile editing and deletion confirmation.
package com.example.trails.ui.profile

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.trails.data.model.User
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditProfileContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val priya = User(
        deviceId = "d1", name = "Priya Sharma", email = "priya.sharma@example.com", phone = "780-555-0142"
    )

    private var typedName = ""
    private var saves = 0
    private var deletes = 0
    private var deviceOpened = 0

    private fun show(
        errors: ProfileFormErrors = ProfileFormErrors(),
        saveState: SaveState = SaveState.Idle,
        deleteState: DeleteState = DeleteState.Idle
    ) {
        composeRule.setContent {
            TrailsTheme {
                EditProfileContent(
                    user = priya,
                    form = priya.toProfileForm(),
                    errors = errors,
                    saveState = saveState,
                    deleteState = deleteState,
                    onBack = {},
                    onNameChange = { typedName = it },
                    onEmailChange = {},
                    onPhoneChange = {},
                    onSave = { saves++ },
                    onDeleteConfirmed = { deletes++ },
                    onOpenDeviceProfile = { deviceOpened++ }
                )
            }
        }
    }

    @Test
    fun fields_showTheStoredValues() {
        show()
        composeRule.onNodeWithTag(EditProfileTags.NAME).assertTextContains("Priya Sharma")
        composeRule.onNodeWithTag(EditProfileTags.EMAIL).assertTextContains("priya.sharma@example.com")
        composeRule.onNodeWithTag(EditProfileTags.PHONE).assertTextContains("780-555-0142")
    }

    @Test
    fun avatarAndChangePhoto_areShown() {
        show()
        composeRule.onNodeWithTag(EditProfileTags.CHANGE_PHOTO).assertIsDisplayed()
    }

    @Test
    fun deviceCard_opensTheDeviceProfile() {
        show()
        composeRule.onNodeWithTag(EditProfileTags.DEVICE_CARD).performScrollTo().performClick()
        assertEquals(1, deviceOpened)
    }

    @Test
    fun typing_reportsTheNewValue() {
        show()
        composeRule.onNodeWithTag(EditProfileTags.NAME).performTextReplacement("Priya S.")
        assertEquals("Priya S.", typedName)
    }

    @Test
    fun validationError_marksTheFieldAndShowsTheMessage() {
        show(errors = ProfileFormErrors(name = ProfileFieldError.NAME_REQUIRED))
        composeRule.onNodeWithTag(EditProfileTags.NAME)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        composeRule.onNodeWithTag("${EditProfileTags.NAME}_error", useUnmergedTree = true).assertExists()
    }

    @Test
    fun noValidationError_fieldIsNotMarked() {
        show()
        composeRule.onNodeWithTag(EditProfileTags.NAME)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
    }

    @Test
    fun saveChanges_callsSave() {
        show()
        composeRule.onNodeWithTag(EditProfileTags.SAVE).performScrollTo().performClick()
        assertEquals(1, saves)
    }

    @Test
    fun saved_showsConfirmation() {
        show(saveState = SaveState.Saved)
        composeRule.onNodeWithTag(EditProfileTags.SAVED).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun saving_disablesTheButton() {
        show(saveState = SaveState.Saving)
        composeRule.onNodeWithTag(EditProfileTags.SAVE).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun deleteProfile_asksForConfirmationFirst() {
        show()
        composeRule.onNodeWithTag(EditProfileTags.DELETE).performScrollTo().performClick()
        composeRule.onNodeWithTag(EditProfileTags.DIALOG).assertIsDisplayed()
        assertEquals(0, deletes)
    }

    @Test
    fun cancel_closesTheDialogWithoutDeleting() {
        show()
        composeRule.onNodeWithTag(EditProfileTags.DELETE).performScrollTo().performClick()
        composeRule.onNodeWithTag(EditProfileTags.DIALOG_CANCEL).performClick()
        composeRule.onNodeWithTag(EditProfileTags.DIALOG).assertDoesNotExist()
        assertEquals(0, deletes)
    }

    @Test
    fun confirm_deletes() {
        show()
        composeRule.onNodeWithTag(EditProfileTags.DELETE).performScrollTo().performClick()
        composeRule.onNodeWithTag(EditProfileTags.DIALOG_CONFIRM).performClick()
        assertEquals(1, deletes)
    }

    @Test
    fun deleteFailure_isShown() {
        show(deleteState = DeleteState.Failed("offline"))
        composeRule.onNodeWithTag(EditProfileTags.DELETE_ERROR).performScrollTo().assertIsDisplayed()
    }
}
