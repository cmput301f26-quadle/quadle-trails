// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 01.11.01, 01.13.01, 03.04.01, 03.05.01
// PURPOSE: Routes and nav graph for the admin screens.
// Outstanding issues: ADMIN_PROFILE shows a placeholder until the read-only profile page is built.
package com.example.trails.ui.admin

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.trails.ui.common.PlaceholderScreen
import com.example.trails.ui.navigation.requiredId

object AdminRoutes {
    const val POLICY = "policy"
    const val ADMIN_CAMPSITES = "admin_campsites"
    const val ADMIN_PROFILES = "admin_profiles"
    const val ADMIN_HOME = "admin_home"
    const val ADMIN_PROFILE = "admin_profile/{userId}"
    fun adminProfile(userId: String) = "admin_profile/$userId"
}

fun NavGraphBuilder.adminGraph(navController: NavController) {
    composable(AdminRoutes.POLICY) { PolicyScreen(navController) }
    composable(AdminRoutes.ADMIN_CAMPSITES) { AdminCampsitesScreen(navController) }
    composable(AdminRoutes.ADMIN_PROFILES) { AdminProfilesScreen(navController) }
    composable(AdminRoutes.ADMIN_HOME) { AdminHomeScreen(navController) }
    // Temporary: the read-only profile page replaces this placeholder in the next commit.
    composable(AdminRoutes.ADMIN_PROFILE, arguments = listOf(requiredId("userId"))) {
        PlaceholderScreen(name = "Profile")
    }
}
