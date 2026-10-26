// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.05.01
// PURPOSE: Admin view of all user profiles.
// TODO: List users from UserRepository.observeAll() with the admin actions.
package com.example.trails.ui.admin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.trails.ui.common.PlaceholderScreen

@Composable
fun AdminProfilesScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(name = "Admin Profiles", modifier = modifier)
}
