// UI (intent) tests for the Register / Edit campsite form, US 02.01.01, 02.01.02 (tags), 02.01.03.
// OWNER: Ibrahim (ibr2309) — Campsite Management
package com.example.trails.ui.campsite

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CampsiteFormContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** Shows the form with real state so typing updates what is on screen; returns the latest fields. */
    private fun showInteractive(
        initial: CampsiteFormUiState = CampsiteFormUiState(),
        onSave: () -> Unit = {}
    ): () -> CampsiteFormFields {
        var latest = initial.fields
        composeRule.setContent {
            var state by remember { mutableStateOf(initial) }
            TrailsTheme {
                CampsiteFormContent(
                    state = state,
                    onFieldsChange = { state = state.copy(fields = it); latest = it },
                    onSave = onSave,
                    onBack = {}
                )
            }
        }
        return { latest }
    }

    @Test
    fun registerMode_showsTitleAndPublish() {
        showInteractive()
        composeRule.onNodeWithText("Register Campsite").assertIsDisplayed()
        composeRule.onNodeWithText("Publish Campsite").assertIsDisplayed()
    }

    @Test
    fun editMode_showsEditTitleAndSaveChanges() {
        showInteractive(CampsiteFormUiState(isEditing = true, fields = CampsiteFormFields(name = "Two Jack Lakeside")))
        composeRule.onNodeWithText("Edit Campsite").assertIsDisplayed()
        composeRule.onNodeWithText("Save changes").assertIsDisplayed()
        composeRule.onNodeWithText("Two Jack Lakeside").assertIsDisplayed()
    }

    @Test
    fun typingName_updatesFields() {
        val fields = showInteractive()
        composeRule.onNodeWithTag(FormTags.NAME).performTextInput("Grande Cache Ridge")
        assertEquals("Grande Cache Ridge", fields().name)
    }

    @Test
    fun typingOccupancyAndPrice_updatesFields() {
        val fields = showInteractive()
        composeRule.onNodeWithTag(FormTags.OCCUPANCY).performTextInput("8")
        composeRule.onNodeWithTag(FormTags.PRICE).performTextInput("65")
        assertEquals("8", fields().maxOccupancy)
        assertEquals("65", fields().pricePerNight)
    }

    @Test
    fun errors_shownUnderFields() {
        showInteractive(CampsiteFormUiState(errors = mapOf("name" to "Name is required")))
        composeRule.onNodeWithText("Name is required").assertIsDisplayed()
    }

    @Test
    fun tapPublish_callsSave() {
        var saved = false
        showInteractive(onSave = { saved = true })
        composeRule.onNodeWithTag(FormTags.SAVE).performClick()
        assertTrue(saved)
    }

    @Test
    fun addTag_appearsAsChip() {
        val fields = showInteractive()
        composeRule.onNodeWithTag(FormTags.ADD_TAG).performClick()
        composeRule.onNodeWithTag(FormTags.TAG_INPUT).performTextInput("Lakeside")
        composeRule.onNodeWithTag(FormTags.TAG_CONFIRM).performClick()
        assertEquals(listOf("Lakeside"), fields().tags)
        composeRule.onNodeWithText("Lakeside").assertIsDisplayed()
    }

    @Test
    fun removeTag_removesChip() {
        val fields = showInteractive(CampsiteFormUiState(fields = CampsiteFormFields(tags = listOf("Lakeside", "Tennis"))))
        composeRule.onNodeWithTag(FormTags.removeTag("Tennis")).performClick()
        assertEquals(listOf("Lakeside"), fields().tags)
    }

    @Test
    fun noPhoto_showsAddPhotoBox() {
        showInteractive()
        composeRule.onNodeWithText("Add promo photo (optional)").assertIsDisplayed()
    }

    @Test
    fun removePhoto_clearsImage() {
        val fields = showInteractive(CampsiteFormUiState(fields = CampsiteFormFields(imageUrl = "https://example.com/ridge.jpg")))
        composeRule.onNodeWithTag(FormTags.REMOVE_PHOTO).performClick()
        assertEquals(null, fields().imageUrl)
        composeRule.onNodeWithText("Add promo photo (optional)").assertIsDisplayed()
    }

    @Test
    fun notFound_showsMessageAndDisablesSave() {
        var saved = false
        showInteractive(CampsiteFormUiState(isEditing = true, notFound = true), onSave = { saved = true })
        composeRule.onNodeWithText("This campsite no longer exists.").assertIsDisplayed()
        composeRule.onNodeWithTag(FormTags.SAVE).performClick()
        assertTrue(!saved)
    }
}
