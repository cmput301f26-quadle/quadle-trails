// routes and nav graph for the waitlist screens
// owner: chitkarsh (chitkarshrathi) - waitlist
package com.example.trails.ui.waitlist

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.trails.ui.navigation.requireArg
import com.example.trails.ui.navigation.requiredId

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
