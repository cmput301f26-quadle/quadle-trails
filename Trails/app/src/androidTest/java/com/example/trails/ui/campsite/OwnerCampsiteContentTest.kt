// UI (intent) tests for the owner campsite page, US 02.01.04.
// OWNER: Ibrahim (ibr2309) — Campsite Management
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.trails.data.model.Campsite
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OwnerCampsiteContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val hinton = Campsite(
        id = "campsite-hinton", ownerId = "owner-01", name = "Wildhorse Lake Campground",
        locationName = "Hinton - Grande Cache, AB", latitude = 53.89, longitude = -118.01,
        maxOccupancy = 8, pricePerNight = 65.0, ratingAverage = 4.6, ratingCount = 23,
        description = "Outdoor cookhouse and on-site tennis court.",
        tags = listOf("Lakeside", "Tennis"),
        rules = "Quiet hours 10pm-7am.",
        cancellationPolicy = "Free cancellation up to 7 days before arrival."
    )

    private fun show(
        state: OwnerCampsiteUiState = OwnerCampsiteUiState.Loaded(hinton, null),
        showPublished: Boolean = false,
        onDismiss: () -> Unit = {},
        onEdit: () -> Unit = {},
        onCreateLottery: () -> Unit = {}
    ) = composeRule.setContent {
        TrailsTheme {
            OwnerCampsiteContent(state, showPublished, onDismiss, {}, onEdit, onCreateLottery)
        }
    }

    @Test
    fun loaded_showsNameLocationAndStats() {
        show()
        composeRule.onNodeWithText("Wildhorse Lake Campground").assertIsDisplayed()
        composeRule.onNodeWithText("Hinton - Grande Cache, AB").assertIsDisplayed()
        composeRule.onNodeWithText("8").assertIsDisplayed()
        composeRule.onNodeWithText("$65").assertIsDisplayed()
        composeRule.onNodeWithText("★ 4.6").assertIsDisplayed()
        composeRule.onNodeWithText("23 ratings").assertIsDisplayed()
    }

    @Test
    fun loaded_showsDetailSections() {
        show()
        composeRule.onNodeWithText("53.89° N, 118.01° W").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Tennis").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Quiet hours 10pm-7am.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Free cancellation up to 7 days before arrival.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun inLottery_showsLotteryChip() {
        show(OwnerCampsiteUiState.Loaded(hinton, "May long weekend"))
        composeRule.onNodeWithText("In lottery · May long weekend").assertIsDisplayed()
    }

    @Test
    fun noRatingsOrPin_showsPlaceholders() {
        show(OwnerCampsiteUiState.Loaded(hinton.copy(ratingCount = 0, latitude = 0.0, longitude = 0.0), null))
        composeRule.onNodeWithText("No ratings yet").assertIsDisplayed()
        composeRule.onNodeWithText("No map pin yet").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun availability_showsCalendarWithOpenCount() {
        val days = setOf(dayKey(2027, 5, 7), dayKey(2027, 5, 8), dayKey(2027, 5, 9))
        show(OwnerCampsiteUiState.Loaded(hinton.copy(availability = daysToAvailability(days)), null))
        composeRule.onNodeWithText("May 2027").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("3 days open this month").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun noAvailability_showsPlaceholder() {
        show()
        composeRule.onNodeWithText("No dates added yet").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun tapEdit_callsOnEdit() {
        var edited = false
        show(onEdit = { edited = true })
        composeRule.onNodeWithTag(OwnerCampsiteTags.EDIT).performScrollTo().performClick()
        assertTrue(edited)
    }

    @Test
    fun tapCreateLottery_callsOnCreateLottery() {
        var created = false
        show(onCreateLottery = { created = true })
        composeRule.onNodeWithTag(OwnerCampsiteTags.CREATE_LOTTERY).performScrollTo().performClick()
        assertTrue(created)
    }

    @Test
    fun notFound_showsMessage() {
        show(OwnerCampsiteUiState.NotFound)
        composeRule.onNodeWithText("This campsite no longer exists.").assertIsDisplayed()
    }

    @Test
    fun justPublished_showsDialogAndViewDismisses() {
        var dismissed = false
        show(showPublished = true, onDismiss = { dismissed = true })
        composeRule.onNodeWithText("Campsite published").assertIsDisplayed()
        composeRule.onNodeWithText("Wildhorse Lake Campground is now live for entrants.").assertIsDisplayed()
        composeRule.onNodeWithTag(OwnerCampsiteTags.VIEW_BUTTON).performClick()
        assertTrue(dismissed)
    }
}
