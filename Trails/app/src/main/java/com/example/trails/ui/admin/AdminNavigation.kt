// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 01.11.01, 03.04.01, 03.05.01
// PURPOSE: Routes and nav graph for the admin screens.
// TODO: Register new admin screens in adminGraph().
package com.example.trails.ui.admin

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

object AdminRoutes {
    const val POLICY = "policy"
    const val ADMIN_CAMPSITES = "admin_campsites"
    const val ADMIN_PROFILES = "admin_profiles"
}

fun NavGraphBuilder.adminGraph(navController: NavController) {
    composable(AdminRoutes.POLICY) { PolicyScreen(navController) }
    composable(AdminRoutes.ADMIN_CAMPSITES) { AdminCampsitesScreen(navController) }
    composable(AdminRoutes.ADMIN_PROFILES) { AdminProfilesScreen(navController) }
}
