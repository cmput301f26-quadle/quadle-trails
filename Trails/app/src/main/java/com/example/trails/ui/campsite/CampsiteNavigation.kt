// OWNER: Ibrahim (ibr2309) — Campsite Management
// USER STORIES: 02.01.01–02.01.04, 02.03.01
// PURPOSE: Routes and nav graph for the owner's campsite screens.
// Outstanding issues: none.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.trails.ui.navigation.optionalArg
import com.example.trails.ui.navigation.optionalId
import com.example.trails.ui.navigation.requireArg
import com.example.trails.ui.navigation.requiredId

object CampsiteRoutes {
    const val HOSTING = "hosting"
    const val CAMPSITE_FORM = "campsite_form?campsiteId={campsiteId}"
    const val OWNER_CAMPSITE = "owner_campsite/{campsiteId}?published={published}"

    // Pass null (or nothing) to create a new campsite instead of editing one.
    fun campsiteForm(campsiteId: String? = null) =
        if (campsiteId == null) "campsite_form" else "campsite_form?campsiteId=$campsiteId"

    // published = true shows the "Campsite published" dialog; only used right after registering.
    fun ownerCampsite(id: String, published: Boolean = false) =
        if (published) "owner_campsite/$id?published=true" else "owner_campsite/$id"
}

fun NavGraphBuilder.campsiteGraph(navController: NavController) {
    composable(CampsiteRoutes.HOSTING) { HostingScreen(navController) }
    composable(CampsiteRoutes.CAMPSITE_FORM, arguments = listOf(optionalId("campsiteId"))) { entry ->
        CampsiteFormScreen(navController, campsiteId = entry.optionalArg("campsiteId"))
    }
    composable(
        CampsiteRoutes.OWNER_CAMPSITE,
        arguments = listOf(
            requiredId("campsiteId"),
            navArgument("published") { type = NavType.BoolType; defaultValue = false }
        )
    ) { entry ->
        OwnerCampsiteScreen(
            navController,
            campsiteId = entry.requireArg("campsiteId"),
            justPublished = entry.arguments?.getBoolean("published") ?: false
        )
    }
}
