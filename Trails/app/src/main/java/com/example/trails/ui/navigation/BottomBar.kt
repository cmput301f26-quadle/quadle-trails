// Icon-only bottom navigation bar shown on every screen.
// OWNER: Zainab (zsaghir) — App shell
// Outstanding issues: none; shown on every screen by TrailsApp.
package com.example.trails.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.trails.R
import com.example.trails.ui.theme.NightSurface
import com.example.trails.ui.theme.TrailGreen
import com.example.trails.ui.theme.TrailsTheme

/**
 * Stateless bottom bar: draws one icon per [BottomNavTab] and reports taps.
 * It knows nothing about navigation; the caller decides what a tap does.
 *
 * @param selected the tab to show as selected, or null to select none.
 * @param onTabClick called with the tab the user tapped.
 * @param modifier modifier for the bar.
 */
@Composable
fun BottomBarContent(
    selected: BottomNavTab?,
    onTabClick: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = NightSurface,
        modifier = modifier.testTag(BottomBarTags.BAR)
    ) {
        BottomNavTab.entries.forEach { tab ->
            val (icon, description) = tab.iconAndDescription()
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onTabClick(tab) },
                icon = { Icon(painterResource(icon), contentDescription = stringResource(description)) },
                label = null,
                alwaysShowLabel = false,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TrailGreen,
                    unselectedIconColor = TrailGreen.copy(alpha = 0.6f),
                    indicatorColor = Color.Transparent
                ),
                modifier = Modifier.testTag(BottomBarTags.tab(tab))
            )
        }
    }
}

/** A tab's Figma icon and its content description (the bar has no text labels). */
private data class TabIcon(@DrawableRes val icon: Int, @StringRes val description: Int)

/** Which icon and content description each tab uses. */
private fun BottomNavTab.iconAndDescription(): TabIcon = when (this) {
    BottomNavTab.HOME -> TabIcon(R.drawable.ic_nav_home, R.string.nav_home)
    BottomNavTab.NOTIFICATIONS -> TabIcon(R.drawable.ic_nav_notifications, R.string.nav_notifications)
    BottomNavTab.MESSAGES -> TabIcon(R.drawable.ic_nav_messages, R.string.nav_messages)
    BottomNavTab.SETTINGS -> TabIcon(R.drawable.ic_nav_settings, R.string.nav_settings)
}

/**
 * Test tags used by UI tests to find the bottom bar and its tabs.
 *
 * @property BAR tag on the whole bar.
 */
object BottomBarTags {
    const val BAR = "bottom_bar"

    /**
     * Tag for one tab's item.
     *
     * @param tab the tab.
     * @return the tag on that tab's item.
     */
    fun tab(tab: BottomNavTab) = "bottom_bar_tab_${tab.name.lowercase()}"
}

/** Design-time preview with Home selected, for comparing against the Figma bottom bar. */
@Preview
@Composable
private fun BottomBarHomePreview() {
    TrailsTheme { BottomBarContent(selected = BottomNavTab.HOME, onTabClick = {}) }
}

/** Design-time preview with Settings selected. */
@Preview
@Composable
private fun BottomBarSettingsPreview() {
    TrailsTheme { BottomBarContent(selected = BottomNavTab.SETTINGS, onTabClick = {}) }
}
