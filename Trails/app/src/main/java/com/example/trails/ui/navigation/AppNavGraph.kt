// Shared file, should not need editing. Add new screens in your own <Package>Navigation.kt.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.trails.ui.admin.adminGraph
import com.example.trails.ui.campsite.campsiteGraph
import com.example.trails.ui.discovery.DiscoveryRoutes
import com.example.trails.ui.discovery.discoveryGraph
import com.example.trails.ui.lottery.lotteryGraph
import com.example.trails.ui.profile.profileGraph
import com.example.trails.ui.waitlist.waitlistGraph

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = DiscoveryRoutes.BROWSE,
        modifier = modifier
    ) {
        discoveryGraph(navController)
        campsiteGraph(navController)
        lotteryGraph(navController)
        waitlistGraph(navController)
        profileGraph(navController)
        adminGraph(navController)
    }
}
