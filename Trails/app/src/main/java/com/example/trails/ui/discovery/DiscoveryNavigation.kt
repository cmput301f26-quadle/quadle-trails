// OWNER: Neaz (NeazMZ06) — Search & Discovery
// USER STORIES: 01.01.01–01.01.05
// PURPOSE: Routes and nav graph for the discovery screens.
// TODO: Register new discovery screens in discoveryGraph().
package com.example.trails.ui.discovery

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.trails.ui.navigation.requireArg
import com.example.trails.ui.navigation.requiredId

object DiscoveryRoutes {
    const val BROWSE = "browse"
    const val CAMPSITE_DETAILS = "campsite/{campsiteId}"
    fun campsiteDetails(id: String) = "campsite/$id"
}

fun NavGraphBuilder.discoveryGraph(navController: NavController) {
    composable(DiscoveryRoutes.BROWSE) { BrowseScreen(navController) }
    composable(DiscoveryRoutes.CAMPSITE_DETAILS, arguments = listOf(requiredId("campsiteId"))) { entry ->
        CampsiteDetailsScreen(navController, campsiteId = entry.requireArg("campsiteId"))
    }
}
