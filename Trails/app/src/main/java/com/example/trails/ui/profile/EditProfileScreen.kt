// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// USER STORIES: 01.07.03, 01.08.01, 03.11.01
// PURPOSE: Edit the current user's profile details.
// TODO: Build the form for the User fields and save via UserRepository.
package com.example.trails.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.trails.ui.common.PlaceholderScreen

@Composable
fun EditProfileScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(name = "Edit Profile", modifier = modifier)
}
