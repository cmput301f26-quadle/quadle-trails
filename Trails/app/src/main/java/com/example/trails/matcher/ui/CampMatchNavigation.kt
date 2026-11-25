// Navigation route and NavGraph builder for the CampMatch discovery screen.
// implements Bonus CampMatch Recommendation Engine
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Matching
package com.example.trails.matcher.ui

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * Route constants for the CampMatch recommendation screen.
 *
 * NOTE FOR TEAMMATES:
 * - App Shell / Discovery: To navigate to CampMatch from any button, banner, or drawer:
 *   ```kotlin
 *   navController.navigate(CampMatchNavigation.CAMP_MATCH_ROUTE)
 *   ```
 */
object CampMatchNavigation {
    const val CAMP_MATCH_ROUTE = "camp_match"

    /**
     * Navigates to the CampMatch screen.
     */
    fun navigateToCampMatch(navController: NavController) {
        navController.navigate(CAMP_MATCH_ROUTE)
    }
}

/**
 * Adds the [CampMatchScreen] destination to the navigation graph.
 *
 * @param onBack callback on back navigation.
 * @param onCampsiteClick callback when an entrant taps to view a campsite.
 * @param onJoinLotteryClick callback when an entrant taps to view or join a lottery.
 */
fun NavGraphBuilder.campMatchDestination(
    onBack: () -> Unit,
    onCampsiteClick: (campsiteId: String) -> Unit,
    onJoinLotteryClick: (lotteryId: String) -> Unit
) {
    composable(CampMatchNavigation.CAMP_MATCH_ROUTE) {
        CampMatchScreen(
            onBack = onBack,
            onCampsiteClick = onCampsiteClick,
            onJoinLotteryClick = onJoinLotteryClick
        )
    }
}
