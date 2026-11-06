// Compose UI test suite validating shared TrailsTopBar component navigation and slots (UI Component pattern).
// implements US 01.01.06, US 01.02.01
// Outstanding issues: None
// OWNER: shared — created by Chitkarsh (chitkarshrathi)
package com.example.trails.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test suite validating [TrailsTopBar] title rendering, subtitles,
 * back button clicks, chevron icons, and custom action slots.
 */
class TrailsTopBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    // verifies title is rendered on screen
    @Test
    fun title_isDisplayed() {
        composeRule.setContent {
            TrailsTheme {
                TrailsTopBar(title = "Join Lottery")
            }
        }

        composeRule.onNodeWithText("Join Lottery").assertIsDisplayed()
    }

    // verifies subtitle is rendered when provided
    @Test
    fun subtitle_whenProvided_isDisplayed() {
        composeRule.setContent {
            TrailsTheme {
                TrailsTopBar(
                    title = "Grande Cache Ridge",
                    subtitle = "Hinton – Grande Cache, AB"
                )
            }
        }

        composeRule.onNodeWithText("Grande Cache Ridge").assertIsDisplayed()
        composeRule.onNodeWithText("Hinton – Grande Cache, AB").assertIsDisplayed()
    }

    // verifies back button is absent when onback is null
    @Test
    fun onBack_whenNull_hidesBackButton() {
        composeRule.setContent {
            TrailsTheme {
                TrailsTopBar(
                    title = "Discover",
                    onBack = null
                )
            }
        }

        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
    }

    // verifies back button appears and triggers callback on click
    @Test
    fun onBack_whenProvided_showsBackButtonAndInvokesCallback() {
        var backClicked = false
        composeRule.setContent {
            TrailsTheme {
                TrailsTopBar(
                    title = "Join Lottery",
                    onBack = { backClicked = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(backClicked)
    }

    // verifies custom back test tag works properly for test compatibility
    @Test
    fun backTestTag_customTag_isAttachedAndClickable() {
        var clicked = false
        composeRule.setContent {
            TrailsTheme {
                TrailsTopBar(
                    title = "Waitlist",
                    onBack = { clicked = true },
                    backTestTag = "waitlist_custom_back"
                )
            }
        }

        composeRule.onNodeWithTag("waitlist_custom_back").assertIsDisplayed()
        composeRule.onNodeWithTag("waitlist_custom_back").performClick()
        assertTrue(clicked)
    }

    // verifies chevron navigation icon is displayed and clickable
    @Test
    fun useChevron_true_showsChevronNavigationAndClicks() {
        var clicked = false
        composeRule.setContent {
            TrailsTheme {
                TrailsTopBar(
                    title = "Campsite Details",
                    onBack = { clicked = true },
                    useChevron = true,
                    backTestTag = "chevron_back_button"
                )
            }
        }

        composeRule.onNodeWithTag("chevron_back_button").assertIsDisplayed()
        composeRule.onNodeWithTag("chevron_back_button").performClick()
        assertTrue(clicked)
    }

    // verifies action text button is displayed and triggers callback
    @Test
    fun actionLabel_whenProvided_triggersOnActionCallback() {
        var actionClicked = false
        composeRule.setContent {
            TrailsTheme {
                TrailsTopBar(
                    title = "Hosting Lotteries",
                    actionLabel = "Create",
                    onAction = { actionClicked = true },
                    actionTestTag = "header_action_create"
                )
            }
        }

        composeRule.onNodeWithText("Create").assertIsDisplayed()
        composeRule.onNodeWithTag("header_action_create").performClick()
        assertTrue(actionClicked)
    }

    // verifies trailing actions slot renders custom action composables
    @Test
    fun actionsSlot_rendersCustomActionIcon() {
        var searchClicked = false
        composeRule.setContent {
            TrailsTheme {
                TrailsTopBar(
                    title = "All Campsites",
                    actions = {
                        IconButton(
                            onClick = { searchClicked = true },
                            modifier = Modifier.testTag("search_action_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                    }
                )
            }
        }

        composeRule.onNodeWithTag("search_action_button").assertIsDisplayed()
        composeRule.onNodeWithTag("search_action_button").performClick()
        assertTrue(searchClicked)
    }

    // verifies custom composable title overload displays properly
    @Test
    fun composableTitleOverload_rendersCustomTitleContent() {
        composeRule.setContent {
            TrailsTheme {
                TrailsTopBar(
                    titleContent = {
                        Text(
                            text = "Custom Composable Title",
                            modifier = Modifier.testTag("custom_title_text")
                        )
                    }
                )
            }
        }

        composeRule.onNodeWithTag("custom_title_text").assertIsDisplayed()
        composeRule.onNodeWithText("Custom Composable Title").assertIsDisplayed()
    }
}
