// Navigation routing and graph builder for waitlist destinations (Navigation Graph pattern).
// implements US 01.01.06, US 01.02.01, US 02.09.01, US 02.11.01
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
package com.example.trails.ui.waitlist

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.trails.ui.navigation.requireArg
import com.example.trails.ui.navigation.requiredId

/**
 * Navigation route constants and route builder helpers for the waitlist flow.
 */
object WaitlistRoutes {
    const val LOTTERY_WAITLIST = "lottery_waitlist/{lotteryId}"
    const val CONFIRM_RESERVATION = "confirm_reservation/{campsiteId}"
    const val WAITLIST_SCREEN = "waitlist_screen/{campsiteId}"
    const val WAITLIST_ENTRANTS = "waitlist_entrants/{lotteryId}"

    fun lotteryWaitlist(lotteryId: String) = "lottery_waitlist/$lotteryId"
    fun confirmReservation(campsiteId: String) = "confirm_reservation/$campsiteId"
    fun waitlistScreen(campsiteId: String) = "waitlist_screen/$campsiteId"
    fun waitlistEntrants(lotteryId: String) = "waitlist_entrants/$lotteryId"
}

// based on android developer guide for compose navigation graph
/**
 * Registers waitlist screen destinations into the app's navigation graph.
 *
 * @param navController controller for navigating across destinations.
 */
fun NavGraphBuilder.waitlistGraph(navController: NavController) {
    composable(WaitlistRoutes.LOTTERY_WAITLIST, arguments = listOf(requiredId("lotteryId"))) { entry ->
        LotteryWaitlistScreen(navController, lotteryId = entry.requireArg("lotteryId"))
    }

    composable(WaitlistRoutes.WAITLIST_ENTRANTS, arguments = listOf(requiredId("lotteryId"))) { entry ->
        WaitlistEntrantsScreen(
            lotteryId = entry.requireArg("lotteryId"),
            onBack = { navController.popBackStack() }
        )
    }

    composable(WaitlistRoutes.CONFIRM_RESERVATION, arguments = listOf(requiredId("campsiteId"))) { entry ->
        ConfirmReservationScreen(navController, campsiteId = entry.requireArg("campsiteId"))
    }

    composable(WaitlistRoutes.WAITLIST_SCREEN, arguments = listOf(requiredId("campsiteId"))) { entry ->
        WaitlistScreen(navController, campsiteId = entry.requireArg("campsiteId"))
    }
}
