// UI Controller wrapper displaying waitlist entrants for a specific lottery (UI Controller pattern).
// implements US 02.09.01
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
package com.example.trails.ui.waitlist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

/**
 * Screen wrapper displaying the waitlist entrants for a given lottery ID.
 *
 * Observes [lotteryId] and delegates rendering to [WaitlistEntrantsScreen].
 * Triggers back navigation via [navController].
 *
 * @param navController controller for navigating across destinations.
 * @param lotteryId identifier of the lottery whose entrants to display.
 * @param modifier optional [Modifier] for screen styling.
 */
@Composable
fun LotteryWaitlistScreen(
    navController: NavController,
    lotteryId: String,
    modifier: Modifier = Modifier
) {
    WaitlistEntrantsScreen(
        lotteryId = lotteryId,
        onBack = { navController.popBackStack() },
        modifier = modifier
    )
}
