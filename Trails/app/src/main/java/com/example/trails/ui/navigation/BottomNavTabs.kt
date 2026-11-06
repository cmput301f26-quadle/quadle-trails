// Which bottom-nav tab is selected and where each tab goes.
// OWNER: Zainab (zsaghir) — App shell
// Outstanding issues: mode switching (US 03.11.01) is Aarav's; until
//   then every user is in ENTRANT mode by default.
package com.example.trails.ui.navigation

import com.example.trails.data.model.Role
import com.example.trails.ui.admin.AdminRoutes
import com.example.trails.ui.campsite.CampsiteRoutes
import com.example.trails.ui.discovery.DiscoveryRoutes
import com.example.trails.ui.profile.ProfileRoutes

/** The four tabs on the bottom bar; the same for every role. */
enum class BottomNavTab {
    /** The home screen for the user's current mode. */
    HOME,
    /** The user's notifications. */
    NOTIFICATIONS,
    /** The user's messages. */
    MESSAGES,
    /** Settings, including profile and app policy. */
    SETTINGS
}

/**
 * The home screen for a mode.
 *
 * @param mode the role the UI is currently acting as ([com.example.trails.data.model.User.activeMode]).
 * @return Browse for entrants, Hosting for owners, Admin home for admins.
 */
fun homeRouteFor(mode: Role): String = when (mode) {
    Role.ENTRANT -> DiscoveryRoutes.BROWSE
    Role.OWNER -> CampsiteRoutes.HOSTING
    Role.ADMIN -> AdminRoutes.ADMIN_HOME
}

/**
 * Where tapping a tab goes. Only [BottomNavTab.HOME] depends on the mode.
 *
 * @param tab the tab tapped.
 * @param mode the role the UI is currently acting as.
 * @return the route to navigate to.
 */
fun routeFor(tab: BottomNavTab, mode: Role): String = when (tab) {
    BottomNavTab.HOME -> homeRouteFor(mode)
    BottomNavTab.NOTIFICATIONS -> ShellRoutes.NOTIFICATIONS
    BottomNavTab.MESSAGES -> ShellRoutes.MESSAGES
    BottomNavTab.SETTINGS -> ShellRoutes.SETTINGS
}

/**
 * Which tab to highlight for the screen being shown. Profile, app policy and
 * camping guidelines count as Settings; every other screen counts as Home.
 *
 * @param route the current route pattern (e.g. `NavDestination.route`), or null if none.
 * @return the selected tab, or null when there is no route.
 */
fun selectedTab(route: String?): BottomNavTab? = when (route) {
    null -> null
    ShellRoutes.NOTIFICATIONS -> BottomNavTab.NOTIFICATIONS
    ShellRoutes.MESSAGES -> BottomNavTab.MESSAGES
    ShellRoutes.SETTINGS,
    ProfileRoutes.PROFILE,
    ProfileRoutes.EDIT_PROFILE,
    AdminRoutes.POLICY,
    AdminRoutes.GUIDELINES -> BottomNavTab.SETTINGS
    else -> BottomNavTab.HOME
}
