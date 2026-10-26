// OWNER: Neaz (NeazMZ06) — Search & Discovery
// USER STORIES: 01.01.01, 01.01.02, 01.01.03, 01.01.04
// PURPOSE: Lets entrants browse and search campsites.
// TODO: List campsites from CampsiteRepository.observeAll().
//       Add search and filters for the browse stories.
//       Open details with DiscoveryRoutes.campsiteDetails(id).
package com.example.trails.ui.discovery

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.trails.ui.common.PlaceholderScreen

@Composable
fun BrowseScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(name = "Browse", modifier = modifier)
}
