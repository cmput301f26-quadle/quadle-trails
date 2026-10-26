// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.04.01
// PURPOSE: Admin view of all campsites.
// TODO: List campsites from CampsiteRepository.observeAll() with the admin actions.
package com.example.trails.ui.admin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.trails.ui.common.PlaceholderScreen

@Composable
fun AdminCampsitesScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(name = "Admin Campsites", modifier = modifier)
}
