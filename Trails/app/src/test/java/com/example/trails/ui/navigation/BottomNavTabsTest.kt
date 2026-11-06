// OWNER: Zainab (zsaghir) — App shell
// PURPOSE: Unit tests for homeRouteFor(), routeFor() and selectedTab(), the bottom-nav logic.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.ui.navigation

import com.example.trails.data.model.Role
import com.example.trails.ui.admin.AdminRoutes
import com.example.trails.ui.campsite.CampsiteRoutes
import com.example.trails.ui.discovery.DiscoveryRoutes
import com.example.trails.ui.profile.ProfileRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BottomNavTabsTest {

    // This checks that an entrant's Home tab opens the Browse screen.
    @Test
    fun entrantMode_homeGoesToBrowse() {
        assertEquals(DiscoveryRoutes.BROWSE, homeRouteFor(Role.ENTRANT))
    }

    // This checks that an owner's Home tab opens the Hosting screen.
    @Test
    fun ownerMode_homeGoesToHosting() {
        assertEquals(CampsiteRoutes.HOSTING, homeRouteFor(Role.OWNER))
    }

    // This checks that an admin's Home tab opens the Admin home screen.
    @Test
    fun adminMode_homeGoesToAdminHome() {
        assertEquals(AdminRoutes.ADMIN_HOME, homeRouteFor(Role.ADMIN))
    }

    // This checks that tapping Home goes to that mode's home screen, for every mode.
    @Test
    fun homeTab_followsMode() {
        for (mode in Role.entries) {
            assertEquals(homeRouteFor(mode), routeFor(BottomNavTab.HOME, mode))
        }
    }

    // This checks that Notifications, Messages and Settings go to the same screen whatever the mode.
    @Test
    fun nonHomeTabs_sameRouteForEveryMode() {
        for (mode in Role.entries) {
            assertEquals(
                listOf(ShellRoutes.NOTIFICATIONS, ShellRoutes.MESSAGES, ShellRoutes.SETTINGS),
                listOf(BottomNavTab.NOTIFICATIONS, BottomNavTab.MESSAGES, BottomNavTab.SETTINGS)
                    .map { routeFor(it, mode) }
            )
        }
    }

    // This checks that being on any mode's home screen highlights the Home tab.
    @Test
    fun everyMode_homeRouteSelectsHome() {
        for (mode in Role.entries) {
            assertEquals(BottomNavTab.HOME, selectedTab(homeRouteFor(mode)))
        }
    }

    // This checks that the Notifications screen highlights the Notifications tab.
    @Test
    fun notificationsRoute_selectsNotifications() {
        assertEquals(BottomNavTab.NOTIFICATIONS, selectedTab(ShellRoutes.NOTIFICATIONS))
    }

    // This checks that the Messages screen highlights the Messages tab.
    @Test
    fun messagesRoute_selectsMessages() {
        assertEquals(BottomNavTab.MESSAGES, selectedTab(ShellRoutes.MESSAGES))
    }

    // This checks that the Settings screen highlights the Settings tab.
    @Test
    fun settingsRoute_selectsSettings() {
        assertEquals(BottomNavTab.SETTINGS, selectedTab(ShellRoutes.SETTINGS))
    }

    // This checks that profile, app policy and camping guidelines, which are opened from Settings,
    // keep Settings highlighted.
    @Test
    fun profileAndPolicyRoutes_selectSettings() {
        val routes = listOf(
            ProfileRoutes.PROFILE,
            ProfileRoutes.EDIT_PROFILE,
            AdminRoutes.POLICY,
            AdminRoutes.GUIDELINES
        )
        assertEquals(List(routes.size) { BottomNavTab.SETTINGS }, routes.map(::selectedTab))
    }

    // This checks that a screen opened from a home screen (the campsite form) keeps Home highlighted.
    @Test
    fun campsiteFormRoute_selectsHome() {
        assertEquals(BottomNavTab.HOME, selectedTab(CampsiteRoutes.CAMPSITE_FORM))
    }

    // This checks that no tab is highlighted when there is no current screen.
    @Test
    fun nullRoute_selectsNothing() {
        assertNull(selectedTab(null))
    }
}
