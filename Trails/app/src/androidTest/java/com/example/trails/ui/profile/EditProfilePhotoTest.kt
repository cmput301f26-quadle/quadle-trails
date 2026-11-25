// Intent tests for the profile photo controls on Edit profile (US 01.08.02).
// OWNER: Ibrahim (ibr2309) — shared image upload, on Aarav's Edit profile screen
// Outstanding issues: none. The system photo picker itself isn't driven; the callbacks are.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.trails.data.model.User
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditProfilePhotoTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val noPhoto = User(deviceId = "d1", name = "Priya Sharma", email = "priya@example.com")
    private val withPhoto = noPhoto.copy(profileImageUrl = "https://firebasestorage.googleapis.com/v0/b/x/o/a.jpg")

    private var picks = 0
    private var removes = 0

    private fun show(user: User, photoState: PhotoState = PhotoState.Idle) {
        composeRule.setContent {
            TrailsTheme {
                EditProfileContent(
                    user = user, form = user.toProfileForm(), errors = ProfileFormErrors(),
                    saveState = SaveState.Idle, deleteState = DeleteState.Idle,
                    onBack = {}, onNameChange = {}, onEmailChange = {}, onPhoneChange = {},
                    onSave = {}, onDeleteConfirmed = {}, onOpenDeviceProfile = {},
                    photoState = photoState,
                    onPickPhoto = { picks++ },
                    onRemovePhoto = { removes++ }
                )
            }
        }
    }

    // With no photo, the button says "Add photo", opens the picker, and there's no Remove.
    @Test
    fun noPhoto_addOpensPicker_noRemove() {
        show(noPhoto)
        composeRule.onNodeWithTag(EditProfileTags.CHANGE_PHOTO).assertTextContains("Add photo").performClick()
        assertEquals(1, picks)
        composeRule.onNodeWithTag(EditProfileTags.REMOVE_PHOTO).assertDoesNotExist()
    }

    // Tapping the photo itself also opens the picker.
    @Test
    fun tappingAvatar_opensPicker() {
        show(noPhoto)
        composeRule.onNodeWithTag(EditProfileTags.PHOTO).performClick()
        assertEquals(1, picks)
    }

    // With a photo, "Change photo" and "Remove photo" are both offered.
    @Test
    fun withPhoto_changeAndRemove() {
        show(withPhoto)
        composeRule.onNodeWithTag(EditProfileTags.CHANGE_PHOTO).assertTextContains("Change photo")
        composeRule.onNodeWithTag(EditProfileTags.REMOVE_PHOTO).assertIsDisplayed().performClick()
        assertEquals(1, removes)
    }

    // While uploading, the buttons are disabled and progress is shown.
    @Test
    fun uploading_disablesButtons() {
        show(withPhoto, PhotoState.Uploading)
        composeRule.onNodeWithTag(EditProfileTags.CHANGE_PHOTO).assertIsNotEnabled()
        composeRule.onNodeWithTag(EditProfileTags.REMOVE_PHOTO).assertIsNotEnabled()
        composeRule.onNodeWithTag(EditProfileTags.PHOTO_STATUS).assertIsDisplayed()
    }

    // A failed change shows an error.
    @Test
    fun failed_showsError() {
        show(withPhoto, PhotoState.Failed("offline"))
        composeRule.onNodeWithTag(EditProfileTags.PHOTO_ERROR).assertIsDisplayed()
    }
}
