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
import androidx.compose.ui.test.performScrollTo
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
        composeRule.onNodeWithTag(FormTags.ADD_TAG).performScrollTo().performClick()
        composeRule.onNodeWithTag(FormTags.TAG_INPUT).performScrollTo().performTextInput("Lakeside")
        composeRule.onNodeWithTag(FormTags.TAG_CONFIRM).performScrollTo().performClick()
        assertEquals(listOf("Lakeside"), fields().tags)
        composeRule.onNodeWithText("Lakeside").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun removeTag_removesChip() {
        val fields = showInteractive(CampsiteFormUiState(fields = CampsiteFormFields(tags = listOf("Lakeside", "Tennis"))))
        composeRule.onNodeWithTag(FormTags.removeTag("Tennis")).performScrollTo().performClick()
        assertEquals(listOf("Lakeside"), fields().tags)
    }

    @Test
    fun calendar_opensOnSelectedMonth() {
        showInteractive(CampsiteFormUiState(fields = CampsiteFormFields(availableDays = setOf(dayKey(2027, 5, 7)))))
        composeRule.onNodeWithTag(CalendarTags.MONTH).performScrollTo()
        composeRule.onNodeWithText("May 2027").assertIsDisplayed()
    }

    @Test
    fun tapDay_marksItAvailable() {
        val fields = showInteractive(CampsiteFormUiState(fields = CampsiteFormFields(availableDays = setOf(dayKey(2027, 5, 7)))))
        composeRule.onNodeWithTag(CalendarTags.day(dayKey(2027, 5, 10))).performScrollTo().performClick()
        assertEquals(setOf(dayKey(2027, 5, 7), dayKey(2027, 5, 10)), fields().availableDays)
    }

    @Test
    fun tapSelectedDay_unmarksIt() {
        val fields = showInteractive(CampsiteFormUiState(fields = CampsiteFormFields(availableDays = setOf(dayKey(2027, 5, 7)))))
        composeRule.onNodeWithTag(CalendarTags.day(dayKey(2027, 5, 7))).performScrollTo().performClick()
        assertEquals(emptySet<Long>(), fields().availableDays)
    }

    @Test
    fun nextArrow_showsNextMonth() {
        showInteractive(CampsiteFormUiState(fields = CampsiteFormFields(availableDays = setOf(dayKey(2027, 5, 7)))))
        composeRule.onNodeWithTag(CalendarTags.NEXT).performScrollTo().performClick()
        composeRule.onNodeWithText("June 2027").assertIsDisplayed()
    }

    @Test
    fun noDays_errorShown() {
        showInteractive(CampsiteFormUiState(errors = mapOf("availability" to "Pick at least one available date")))
        composeRule.onNodeWithText("Pick at least one available date").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun noPin_showsDropPinPrompt() {
        showInteractive()
        composeRule.onNodeWithTag(FormTags.MAP_PIN).performScrollTo()
        composeRule.onNodeWithText("Tap to drop a pin on the map").assertIsDisplayed()
    }

    @Test
    fun withPin_showsCoordinates() {
        showInteractive(CampsiteFormUiState(fields = CampsiteFormFields(latitude = 53.89, longitude = -118.01)))
        composeRule.onNodeWithTag(FormTags.MAP_PIN).performScrollTo()
        composeRule.onNodeWithText("53.89° N, 118.01° W").assertIsDisplayed()
    }

    @Test
    fun pinError_shownUnderMapBox() {
        showInteractive(CampsiteFormUiState(errors = mapOf("location" to "Drop a pin on the map")))
        composeRule.onNodeWithText("Drop a pin on the map").performScrollTo().assertIsDisplayed()
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
