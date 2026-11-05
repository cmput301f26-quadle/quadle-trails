// UI (intent) tests for the icon-only bottom navigation bar.
// OWNER: Zainab (zsaghir) — App shell
package com.example.trails.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.trails.ui.theme.TrailsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BottomBarContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val descriptions = listOf("Home", "Notifications", "Messages", "Settings")

    private fun show(
        selected: BottomNavTab?,
        onTabClick: (BottomNavTab) -> Unit = {}
    ) = composeRule.setContent { TrailsTheme { BottomBarContent(selected, onTabClick) } }

    // This checks that all four tabs are on screen and can be found by what a screen reader says.
    @Test
    fun allTabs_shownByContentDescription() {
        show(BottomNavTab.HOME)
        composeRule.onNodeWithTag(BottomBarTags.BAR).assertIsDisplayed()
        descriptions.forEach { composeRule.onNodeWithContentDescription(it).assertIsDisplayed() }
    }

    // This checks that only the tab we pass in is marked as selected.
    @Test
    fun selectedTab_isMarkedSelected_othersNot() {
        show(BottomNavTab.MESSAGES)
        composeRule.onNodeWithContentDescription("Messages").assertIsSelected()
        composeRule.onNodeWithContentDescription("Home").assertIsNotSelected()
        composeRule.onNodeWithContentDescription("Notifications").assertIsNotSelected()
        composeRule.onNodeWithContentDescription("Settings").assertIsNotSelected()
    }

    // This checks that tapping the bell reports Notifications to the caller.
    @Test
    fun tapNotifications_reportsNotifications() {
        var tapped: BottomNavTab? = null
        show(BottomNavTab.HOME, onTabClick = { tapped = it })
        composeRule.onNodeWithContentDescription("Notifications").performClick()
        assertEquals(BottomNavTab.NOTIFICATIONS, tapped)
    }

    // This checks that every tab, when tapped, reports itself and not some other tab.
    @Test
    fun tapEachTab_reportsThatTab() {
        val tapped = mutableListOf<BottomNavTab>()
        show(null, onTabClick = { tapped += it })
        BottomNavTab.entries.forEach { composeRule.onNodeWithTag(BottomBarTags.tab(it)).performClick() }
        assertEquals(BottomNavTab.entries, tapped)
    }

    // This checks that when no tab is selected, none of the tabs are marked selected.
    @Test
    fun nothingSelected_noTabMarkedSelected() {
        show(null)
        BottomNavTab.entries.forEach {
            composeRule.onNodeWithTag(BottomBarTags.tab(it)).assertIsNotSelected()
        }
    }
}
