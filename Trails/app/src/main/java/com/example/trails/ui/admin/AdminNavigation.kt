// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 01.11.01, 01.13.01, 03.01.01, 03.04.01, 03.05.01
// PURPOSE: Routes and nav graph for the admin screens.
// Outstanding issues: none.
package com.example.trails.ui.admin

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.trails.ui.navigation.requireArg
import com.example.trails.ui.navigation.requiredId

/**
 * Route strings for the admin, policy and guidelines screens.
 *
 * @property POLICY the app policy screen.
 * @property GUIDELINES the camping guidelines screen.
 * @property ADMIN_CAMPSITES the admin's list of every campsite.
 * @property ADMIN_PROFILES the admin's list of every user profile.
 * @property ADMIN_HOME the admin home with the Campsites and Profiles tiles.
 * @property ADMIN_PROFILE pattern for one user's admin page; build it with [adminProfile].
 * @property ADMIN_CAMPSITE pattern for one campsite's admin page; build it with [adminCampsite].
 */
object AdminRoutes {
    const val POLICY = "policy"
    const val GUIDELINES = "guidelines"
    const val ADMIN_CAMPSITES = "admin_campsites"
    const val ADMIN_PROFILES = "admin_profiles"
    const val ADMIN_HOME = "admin_home"
    const val ADMIN_PROFILE = "admin_profile/{userId}"

    /**
     * Route to one user's admin page.
     *
     * @param userId device id of the user to show.
     * @return the route to navigate to.
     */
    fun adminProfile(userId: String) = "admin_profile/$userId"

    const val ADMIN_CAMPSITE = "admin_campsite/{campsiteId}"

    /**
     * Route to one campsite's admin page.
     *
     * @param campsiteId id of the campsite to show.
     * @return the route to navigate to.
     */
    fun adminCampsite(campsiteId: String) = "admin_campsite/$campsiteId"
}

/**
 * Adds the admin, policy and guidelines screens to the app's nav graph.
 *
 * @param navController used by each screen to navigate onward and back.
 */
fun NavGraphBuilder.adminGraph(navController: NavController) {
    composable(AdminRoutes.POLICY) { PolicyScreen(navController) }
    composable(AdminRoutes.GUIDELINES) { GuidelinesScreen(navController) }
    composable(AdminRoutes.ADMIN_CAMPSITES) { AdminCampsitesScreen(navController) }
    composable(AdminRoutes.ADMIN_PROFILES) { AdminProfilesScreen(navController) }
    composable(AdminRoutes.ADMIN_HOME) { AdminHomeScreen(navController) }
    composable(AdminRoutes.ADMIN_PROFILE, arguments = listOf(requiredId("userId"))) { entry ->
        AdminProfileScreen(navController, userId = entry.requireArg("userId"))
    }
    composable(AdminRoutes.ADMIN_CAMPSITE, arguments = listOf(requiredId("campsiteId"))) { entry ->
        AdminCampsiteScreen(navController, campsiteId = entry.requireArg("campsiteId"))
    }
}
