// OWNER: Ibrahim (ibr2309) — Campsite Management
// USER STORIES: 02.01.01, 02.01.02, 02.01.03
// PURPOSE: Create a campsite, or edit one when campsiteId is set.
// TODO: Build the form for the Campsite fields and save via CampsiteRepository.
package com.example.trails.ui.campsite

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.trails.ui.common.PlaceholderScreen

@Composable
fun CampsiteFormScreen(
    navController: NavController,
    campsiteId: String?,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(name = "Campsite Form", modifier = modifier)
}
