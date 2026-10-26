// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// USER STORIES: 01.07.03, 01.08.01, 03.11.01
// PURPOSE: Shows the current user's profile and roles.
// TODO: Load the user with UserRepository.observe(CurrentUser.id()).
//       Link to ProfileRoutes.EDIT_PROFILE.
package com.example.trails.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.trails.ui.common.PlaceholderScreen

@Composable
fun ProfileScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(name = "Profile", modifier = modifier)
}
