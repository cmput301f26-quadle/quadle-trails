// OWNER: Zainab (zsaghir) — App shell
// PURPOSE: Root of the UI: picks the start screen from the user's mode, hosts the
//  nav graph and shows the bottom bar on every screen.
// Outstanding issues: the mode switcher (US 03.11.01) is Aarav's; it should call
//  goToHomeFor(newMode) after saving the new activeMode.
package com.example.trails.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.Role
import kotlinx.coroutines.flow.map

/**
 * The whole app UI. Waits for the current user's mode, then shows the nav graph
 * starting at that mode's home with the bottom bar underneath.
 */
@Composable
fun TrailsApp() {
    val modeFlow = remember {
        AppContainer.userRepository.observe(CurrentUser.id()).map { user -> user?.activeMode ?: Role.ENTRANT }
    }
    val mode by modeFlow.collectAsState(initial = null)
    val current = mode
    if (current == null) {
        // Don't build the NavHost yet: its start destination depends on the mode.
        Box(Modifier.fillMaxSize())
    } else {
        TrailsShell(current)
    }
}

/**
 * Scaffold with the bottom bar around the nav graph.
 *
 * @param mode the user's current mode; decides where the Home tab goes.
 */
@Composable
private fun TrailsShell(mode: Role) {
    // Fixed after the first mode arrives, so later mode changes don't rebuild the graph.
    val startDestination = rememberSaveable { homeRouteFor(mode) }
    val navController = rememberNavController()
    val route = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        bottomBar = {
            BottomBarContent(
                selected = selectedTab(route),
                onTabClick = { tab ->
                    navController.navigate(routeFor(tab, mode)) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { padding ->
        AppNavGraph(
            navController = navController,
            startDestination = startDestination,
            // consumeWindowInsets stops screens with their own Scaffold from adding
            // the status-bar / bottom-bar space a second time.
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
        )
    }
}

/**
 * Goes to the home screen for [mode] and clears the back stack, so Back from there
 * leaves the app. For the mode pill (US 03.11.01): call it after switching modes.
 *
 * It also makes that home the graph's start destination, so the bottom bar's
 * `popUpTo(findStartDestination())` keeps working after the switch.
 *
 * @param mode the mode the user just switched to.
 */
fun NavController.goToHomeFor(mode: Role) {
    val home = homeRouteFor(mode)
    graph.setStartDestination(home)
    navigate(home) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
