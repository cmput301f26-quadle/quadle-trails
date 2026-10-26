// OWNER: Ibrahim (ibr2309) — Campsite Management
// USER STORIES: 02.03.01
// PURPOSE: Owner's home screen listing the campsites they host.
// TODO: List campsites from CampsiteRepository.observeByOwner(CurrentUser.id()).
//       Link to CampsiteRoutes.campsiteForm() and ownerCampsite(id).
package com.example.trails.ui.campsite

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.trails.ui.common.PlaceholderScreen

@Composable
fun HostingScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(name = "Hosting", modifier = modifier)
}
