// OWNER: Neaz (NeazMZ06) — Search & Discovery
// USER STORIES: 01.01.01–01.01.05
// PURPOSE: Routes and nav graph for the discovery screens.
// Outstanding issues: none.
package com.example.trails.ui.discovery

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.trails.ui.navigation.requireArg
import com.example.trails.ui.navigation.requiredId

/**
 * Route names for the discovery screens.
 *
 * @property BROWSE the entrant home screen.
 * @property CAMPSITE_DETAILS the route pattern for one campsite, with a campsite id argument.
 */
object DiscoveryRoutes {
    const val BROWSE = "browse"
    const val CAMPSITE_DETAILS = "campsite/{campsiteId}"

    /**
     * Builds the route that opens one campsite.
     *
     * @param id the campsite id.
     * @return the route to that campsite's details screen.
     */
    fun campsiteDetails(id: String) = "campsite/$id"
}

/**
 * Registers the browse and campsite details screens.
 *
 * @param navController passed into each screen so it can open the next one.
 */
fun NavGraphBuilder.discoveryGraph(navController: NavController) {
    composable(DiscoveryRoutes.BROWSE) { BrowseScreen(navController) }
    composable(DiscoveryRoutes.CAMPSITE_DETAILS, arguments = listOf(requiredId("campsiteId"))) { entry ->
        CampsiteDetailsScreen(navController, campsiteId = entry.requireArg("campsiteId"))
    }
}
