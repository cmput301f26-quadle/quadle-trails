// AI assistance.
// OWNER: Neaz (NeazMZ06) — Search & Discovery
// USER STORIES: 01.01.05
// PURPOSE: UI tests for the entrant campsite details screen.
// Outstanding issues: none.
package com.example.trails.ui.discovery

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.rememberNavController
import com.example.trails.data.fake.TestCampsites
import com.example.trails.ui.campsite.formatDateRange
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Checks that [CampsiteDetailsContent] shows a loaded campsite, a missing one, a loading spinner, and the back arrow.
 */
class CampsiteDetailsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val banff = TestCampsites.all.first { it.id == TestCampsites.BANFF_ID }

    private fun show(
        state: CampsiteDetailsUiState = CampsiteDetailsUiState.Loaded(banff),
        onBack: () -> Unit = {}
    ) = composeRule.setContent {
        TrailsTheme {
            CampsiteDetailsContent(
                state = state,
                campsiteId = banff.id,
                onBack = onBack,
                navController = rememberNavController()
            )
        }
    }

    @Test
    fun loaded_showsSummaryAndSections() {
        show()
        composeRule.onNodeWithText("Tunnel Mountain Site 14").assertIsDisplayed()
        composeRule.onNodeWithText("Banff, AB").assertIsDisplayed()
        composeRule.onNodeWithText("6").assertIsDisplayed()
        composeRule.onNodeWithText("$42").assertIsDisplayed()
        composeRule.onNodeWithText("★ 4.5").assertIsDisplayed()
        composeRule.onNodeWithText("2 ratings").assertIsDisplayed()
        composeRule.onNodeWithText("Fire pit").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(banff.rules).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(banff.cancellationPolicy).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(formatDateRange(banff.availability.first())!!)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("51.19° N, 115.55° W").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Join Lottery").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun noRatingsOrPin_showsPlaceholders() {
        show(
            CampsiteDetailsUiState.Loaded(
                banff.copy(ratingCount = 0, latitude = 0.0, longitude = 0.0)
            )
        )
        composeRule.onNodeWithText("No ratings yet").assertIsDisplayed()
        composeRule.onNodeWithText("No map pin yet").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun loading_showsSpinnerWithoutDetails() {
        show(CampsiteDetailsUiState.Loading)
        composeRule.onNodeWithTag(CampsiteDetailsTags.LOADING).assertIsDisplayed()
        composeRule.onNodeWithText("Tunnel Mountain Site 14").assertDoesNotExist()
        composeRule.onNodeWithText("Join Lottery").assertDoesNotExist()
    }

    @Test
    fun notFound_showsMessageWithoutJoinOrRating() {
        show(CampsiteDetailsUiState.NotFound)
        composeRule.onNodeWithText("This campsite no longer exists.").assertIsDisplayed()
        composeRule.onNodeWithText("Join Lottery").assertDoesNotExist()
        composeRule.onNodeWithText("Rate this campsite").assertDoesNotExist()
    }

    @Test
    fun tapBack_callsOnBack() {
        var wentBack = false
        show(onBack = { wentBack = true })
        composeRule.onNodeWithTag(CampsiteDetailsTags.BACK).performClick()
        assertTrue(wentBack)
    }
}
