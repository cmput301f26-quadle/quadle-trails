// OWNER: Zainab (zsaghir) — App shell
// PURPOSE: Nav graph for the screens behind the Notifications, Messages and Settings tabs.
// Outstanding issues: none.
package com.example.trails.ui.shell

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.trails.ui.navigation.ShellRoutes

/**
 * Registers the app-shell screens for each [ShellRoutes] route.
 *
 * @param navController passed to each screen so it can navigate.
 */
fun NavGraphBuilder.shellGraph(navController: NavController) {
    composable(ShellRoutes.NOTIFICATIONS) { NotificationsScreen(navController) }
    composable(ShellRoutes.MESSAGES) { MessagesScreen(navController) }
    composable(ShellRoutes.SETTINGS) { SettingsScreen(navController) }
}
