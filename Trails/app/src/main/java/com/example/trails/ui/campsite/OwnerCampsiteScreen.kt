// OWNER: Ibrahim (ibr2309) — Campsite Management
// USER STORIES: 02.01.04
// PURPOSE: Owner's view of a single campsite they host.
// TODO: Load the campsite by id and show the owner actions for 02.01.04.
package com.example.trails.ui.campsite

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.trails.ui.common.PlaceholderScreen

@Composable
fun OwnerCampsiteScreen(
    navController: NavController,
    campsiteId: String,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(name = "Owner Campsite", modifier = modifier)
}
