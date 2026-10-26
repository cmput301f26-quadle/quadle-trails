// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// USER STORIES: 01.07.03, 01.08.01, 03.11.01
// PURPOSE: Routes and nav graph for the profile screens.
// TODO: Register new profile screens in profileGraph().
package com.example.trails.ui.profile

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

object ProfileRoutes {
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
}

fun NavGraphBuilder.profileGraph(navController: NavController) {
    composable(ProfileRoutes.PROFILE) { ProfileScreen(navController) }
    composable(ProfileRoutes.EDIT_PROFILE) { EditProfileScreen(navController) }
}
