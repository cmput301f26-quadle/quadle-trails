// Routes and navigation graph for the profile screens (US 01.09.01, 01.07.03, 01.08.01, 03.11.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: The bottom bar does not highlight Settings on Device profile and Choose a mode.
package com.example.trails.ui.profile

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** Route names for the profile screens. */
object ProfileRoutes {
    /** The profile (edit name, email, phone; delete). Settings → Profile opens it. */
    const val PROFILE = "profile"

    /** Same screen as [PROFILE], kept so existing links keep working. */
    const val EDIT_PROFILE = "edit_profile"

    /** How the profile is tied to this device. */
    const val DEVICE_PROFILE = "device_profile"

    /** Choose a mode as a full screen; the mode pill's sheet is the usual way in. */
    const val CHOOSE_MODE = "choose_mode"

    private var pendingRoute: String? = null

    /**
     * Asks the app to open [route] as soon as its navigation is ready. Used by the greeting
     * (Welcome back / Device profile), which is shown before the app's screens exist.
     *
     * @param route the screen to open, e.g. [PROFILE].
     */
    fun requestRoute(route: String) {
        pendingRoute = route
    }

    /**
     * Takes the screen requested with [requestRoute], so it is opened only once.
     *
     * @return the route, or null if nothing was requested.
     */
    fun consumePendingRoute(): String? = pendingRoute.also { pendingRoute = null }
}

/**
 * Registers the profile screens with the app's nav graph.
 *
 * @param navController passed to each screen for navigation.
 */
fun NavGraphBuilder.profileGraph(navController: NavController) {
    composable(ProfileRoutes.PROFILE) { EditProfileScreen(navController) }
    composable(ProfileRoutes.EDIT_PROFILE) { EditProfileScreen(navController) }
    composable(ProfileRoutes.DEVICE_PROFILE) { DeviceProfileScreen(navController) }
    composable(ProfileRoutes.CHOOSE_MODE) { ChooseModeScreen(navController) }
}
