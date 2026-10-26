// OWNER: Ibrahim (ibr2309) — Campsite Management
// USER STORIES: 02.01.01–02.01.04, 02.03.01
// PURPOSE: Routes and nav graph for the owner's campsite screens.
// TODO: Register new campsite screens in campsiteGraph().
package com.example.trails.ui.campsite

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.trails.ui.navigation.optionalArg
import com.example.trails.ui.navigation.optionalId
import com.example.trails.ui.navigation.requireArg
import com.example.trails.ui.navigation.requiredId

object CampsiteRoutes {
    const val HOSTING = "hosting"
    const val CAMPSITE_FORM = "campsite_form?campsiteId={campsiteId}"
    const val OWNER_CAMPSITE = "owner_campsite/{campsiteId}"

    // Pass null (or nothing) to create a new campsite instead of editing one.
    fun campsiteForm(campsiteId: String? = null) =
        if (campsiteId == null) "campsite_form" else "campsite_form?campsiteId=$campsiteId"

    fun ownerCampsite(id: String) = "owner_campsite/$id"
}

fun NavGraphBuilder.campsiteGraph(navController: NavController) {
    composable(CampsiteRoutes.HOSTING) { HostingScreen(navController) }
    composable(CampsiteRoutes.CAMPSITE_FORM, arguments = listOf(optionalId("campsiteId"))) { entry ->
        CampsiteFormScreen(navController, campsiteId = entry.optionalArg("campsiteId"))
    }
    composable(CampsiteRoutes.OWNER_CAMPSITE, arguments = listOf(requiredId("campsiteId"))) { entry ->
        OwnerCampsiteScreen(navController, campsiteId = entry.requireArg("campsiteId"))
    }
}
