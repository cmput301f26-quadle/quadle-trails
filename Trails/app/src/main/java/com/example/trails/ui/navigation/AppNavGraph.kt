// Shared file, should not need editing. Add new screens in your own <Package>Navigation.kt.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help add the outstanding issues line to this header.
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
import com.example.trails.ui.shell.shellGraph
import com.example.trails.ui.waitlist.waitlistGraph

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = DiscoveryRoutes.BROWSE
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        discoveryGraph(navController)
        campsiteGraph(navController)
        lotteryGraph(navController)
        waitlistGraph(navController)
        profileGraph(navController)
        adminGraph(navController)
        shellGraph(navController)
    }
}
