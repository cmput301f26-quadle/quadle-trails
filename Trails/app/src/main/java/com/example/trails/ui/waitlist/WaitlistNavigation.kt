// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
// USER STORIES: 02.09.01
// PURPOSE: Routes and nav graph for the waitlist screens.
// TODO: Register new waitlist screens in waitlistGraph().
package com.example.trails.ui.waitlist

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.trails.ui.navigation.requireArg
import com.example.trails.ui.navigation.requiredId

object WaitlistRoutes {
    const val LOTTERY_WAITLIST = "lottery_waitlist/{lotteryId}"
    const val CONFIRM_RESERVATION = "confirm_reservation/{campsiteId}"

    fun lotteryWaitlist(lotteryId: String) = "lottery_waitlist/$lotteryId"
    fun confirmReservation(campsiteId: String) = "confirm_reservation/$campsiteId"
}

fun NavGraphBuilder.waitlistGraph(navController: NavController) {
    composable(WaitlistRoutes.LOTTERY_WAITLIST, arguments = listOf(requiredId("lotteryId"))) { entry ->
        LotteryWaitlistScreen(navController, lotteryId = entry.requireArg("lotteryId"))
    }

    composable(WaitlistRoutes.CONFIRM_RESERVATION, arguments = listOf(requiredId("campsiteId"))) { entry ->
        ConfirmReservationScreen(navController, campsiteId = entry.requireArg("campsiteId"))
    }
}
