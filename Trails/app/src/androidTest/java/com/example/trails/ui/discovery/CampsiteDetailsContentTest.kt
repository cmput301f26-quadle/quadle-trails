// UI tests for the entrant campsite details screen (US 01.01.05).
// OWNER: Neaz (NeazMZ06) — Search & Discovery
package com.example.trails.ui.discovery

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.trails.data.fake.TestCampsites
import com.example.trails.ui.campsite.formatDateRange
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CampsiteDetailsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val banff = TestCampsites.all.first { it.id == TestCampsites.BANFF_ID }

    private fun show(
        state: CampsiteDetailsUiState = CampsiteDetailsUiState.Loaded(banff),
        onBack: () -> Unit = {}
    ) = composeRule.setContent {
        TrailsTheme {
            CampsiteDetailsContent(state, banff.id, onBack)
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
        composeRule.onNodeWithText("Join Lottery Section").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Rating Section").performScrollTo().assertIsDisplayed()
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
    fun notFound_showsMessageWithoutJoinOrRating() {
        show(CampsiteDetailsUiState.NotFound)
        composeRule.onNodeWithText("This campsite no longer exists.").assertIsDisplayed()
        composeRule.onNodeWithText("Join Lottery Section").assertDoesNotExist()
        composeRule.onNodeWithText("Rating Section").assertDoesNotExist()
    }

    @Test
    fun tapBack_callsOnBack() {
        var wentBack = false
        show(onBack = { wentBack = true })
        composeRule.onNodeWithTag(CampsiteDetailsTags.BACK).performClick()
        assertTrue(wentBack)
    }
}
