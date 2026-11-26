// AI assistance: used ai to understand and modify lines of codes to tailor to my requirements
// OWNER: Neaz (NeazMZ06) — Search & Discovery
// USER STORIES: 01.01.01, 01.01.02, 01.01.03, 01.12.01, 03.11.01
// PURPOSE: UI tests for the browse list, keyword search, filters, and the QR scan button and link.
// Outstanding issues: none.
package com.example.trails.ui.discovery

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Role
import com.example.trails.ui.navigation.requireArg
import com.example.trails.ui.navigation.requiredId
import com.example.trails.ui.profile.ModePillChip
import com.example.trails.ui.profile.ModePillTags
import com.example.trails.ui.qr.CAMPSITE_QR_URI_PATTERN
import com.example.trails.ui.qr.campsiteQrText
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * Checks that [BrowseScreen] lists campsites, searches by keyword, and applies the filters.
 */
class BrowseScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(campsites: List<Campsite> = TestCampsites.all): BrowseViewModel {
        val viewModel = BrowseViewModel(FakeCampsiteRepository(campsites))
        composeRule.setContent {
            TrailsTheme {
                BrowseScreen(navController = rememberNavController(), viewModel = viewModel)
            }
        }
        return viewModel
    }

    @Test
    fun list_showsEveryCampsiteAndScrollsToTheLast() {
        show()
        composeRule.onNodeWithText("Browse").assertIsDisplayed()
        composeRule.onNodeWithText("Tunnel Mountain Site 14").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(BrowseTags.LIST)
            .performScrollToNode(hasText("Wildhorse Lake Site 3"))
        composeRule.onNodeWithText("Wildhorse Lake Site 3").performScrollTo().assertIsDisplayed()
    }

    // This checks that the mode pill slot is shown in the Browse header and its pill can be tapped (US 03.11.01).
    @Test
    fun modePillSlot_isShown() {
        var taps = 0
        composeRule.setContent {
            TrailsTheme {
                BrowseScreen(
                    navController = rememberNavController(),
                    viewModel = BrowseViewModel(FakeCampsiteRepository(TestCampsites.all)),
                    modePill = { ModePillChip(Role.ENTRANT, onClick = { taps++ }) }
                )
            }
        }
        composeRule.onNodeWithText("Browse").assertIsDisplayed()
        composeRule.onNodeWithTag(BrowseTags.MODE_PILL).assertIsDisplayed()
        composeRule.onNodeWithTag(ModePillTags.PILL).assertIsDisplayed().performClick()
        assertEquals(1, taps)
    }

    @Test
    fun noCampsites_showsEmptyMessage() {
        show(emptyList())
        composeRule.onNodeWithText("No campsites to show.").assertIsDisplayed()
        composeRule.onNodeWithTag(BrowseTags.LIST).assertDoesNotExist()
    }

    @Test
    fun keyword_keepsTheMatchAndDropsTheOthers() {
        show()
        composeRule.onNodeWithTag(BrowseTags.SEARCH).performTextInput("tunnel")
        composeRule.onNodeWithText("Tunnel Mountain Site 14").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Whistlers Riverside Site 7").assertDoesNotExist()
        composeRule.onNodeWithText("Wildhorse Lake Site 3").assertDoesNotExist()
    }

    @Test
    fun keywordWithNoMatch_showsNoMatchMessage() {
        show()
        composeRule.onNodeWithTag(BrowseTags.SEARCH).performTextInput("zzzz")
        composeRule.onNodeWithText("No campsites match.").assertIsDisplayed()
        composeRule.onNodeWithTag(BrowseTags.LIST).assertDoesNotExist()
    }

    @Test
    fun location_keepsCampsitesInThatPlace() {
        show()
        composeRule.onNodeWithText("Filters").performClick()
        composeRule.onNodeWithTag(BrowseTags.LOCATION).performTextInput("jasper")
        composeRule.onNodeWithText("Apply filters").performScrollTo().performClick()
        composeRule.onNodeWithText("Whistlers Riverside Site 7").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Tunnel Mountain Site 14").assertDoesNotExist()
    }

    @Test
    fun guests_keepsCampsitesThatCanSleepThatMany() {
        show()
        composeRule.onNodeWithText("Filters").performClick()
        composeRule.onNodeWithTag(BrowseTags.GUESTS).performScrollTo().performTextInput("7")
        composeRule.onNodeWithText("Apply filters").performScrollTo().performClick()
        composeRule.onNodeWithText("Wildhorse Lake Site 3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Whistlers Riverside Site 7").assertDoesNotExist()
        composeRule.onNodeWithText("Tunnel Mountain Site 14").assertDoesNotExist()
    }

    @Test
    fun dates_filterTheListAndClearDatesBringsThemBack() {
        val viewModel = show()
        val day = utcMidnight(2027, 5, 8)
        viewModel.onArriveChange(day)
        viewModel.onLeaveChange(day)
        composeRule.onNodeWithText("Wildhorse Lake Site 3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Tunnel Mountain Site 14").assertDoesNotExist()
        composeRule.onNodeWithText("Edit filters").performClick()
        composeRule.onNodeWithText("Reset filters").performScrollTo().performClick()
        composeRule.onNodeWithText("Apply filters").performScrollTo().performClick()
        composeRule.onNodeWithText("Tunnel Mountain Site 14").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun tapCampsite_opensItsDetailsRoute() {
        val viewModel = BrowseViewModel(FakeCampsiteRepository(TestCampsites.all))
        composeRule.setContent {
            TrailsTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = DiscoveryRoutes.BROWSE) {
                    composable(DiscoveryRoutes.BROWSE) {
                        BrowseScreen(navController, viewModel = viewModel)
                    }
                    composable(
                        DiscoveryRoutes.CAMPSITE_DETAILS,
                        arguments = listOf(requiredId("campsiteId"))
                    ) { entry ->
                        Text("opened ${entry.requireArg("campsiteId")}")
                    }
                }
            }
        }
        composeRule.onNodeWithText("Tunnel Mountain Site 14").performScrollTo().performClick()
        composeRule.onNodeWithText("View details").performClick()
        composeRule.onNodeWithText("opened ${TestCampsites.BANFF_ID}").assertIsDisplayed()
    }

    // US 01.12.01: the scan button sits next to the search field.
    @Test
    fun scanButton_isShownNextToSearch() {
        show()
        composeRule.onNodeWithTag(BrowseTags.SCAN_QR).assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithContentDescription("Scan a campsite QR code").assertIsDisplayed()
    }

    // US 01.12.01: the link inside a campsite QR code opens that campsite's details.
    @Test
    fun campsiteQrLink_opensItsDetailsRoute() {
        lateinit var navController: NavHostController
        composeRule.setContent {
            TrailsTheme {
                navController = rememberNavController()
                NavHost(navController, startDestination = DiscoveryRoutes.BROWSE) {
                    composable(DiscoveryRoutes.BROWSE) { Text("browse") }
                    composable(
                        DiscoveryRoutes.CAMPSITE_DETAILS,
                        arguments = listOf(requiredId("campsiteId")),
                        deepLinks = listOf(navDeepLink { uriPattern = CAMPSITE_QR_URI_PATTERN })
                    ) { entry ->
                        Text("opened ${entry.requireArg("campsiteId")}")
                    }
                }
            }
        }
        composeRule.runOnIdle {
            navController.navigate(Uri.parse(campsiteQrText(TestCampsites.HINTON_ID)))
        }
        composeRule.onNodeWithText("opened ${TestCampsites.HINTON_ID}").assertIsDisplayed()
    }

    private fun utcMidnight(year: Int, month: Int, day: Int): Long =
        GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, day)
        }.timeInMillis
}
