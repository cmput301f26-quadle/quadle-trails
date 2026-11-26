// Navigation routing and graph builder for waitlist destinations (Navigation Graph pattern).
// implements US 01.01.06, US 01.02.01, US 01.04.01, US 01.04.02, US 01.04.03, US 02.04.02, US 02.09.01, US 02.11.01
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.ui.waitlist

import com.example.trails.ui.ownernotify.OwnerNotifyRoutes
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
    const val SELECT_CAMPSITE = "select_campsite/{lotteryId}"
    const val RESERVE_DATES = "reserve_dates/{lotteryId}/{campsiteId}"
    const val CONFIRM_RESERVATION_DETAILS = "confirm_reservation_details/{campsiteId}/{lotteryId}/{startDay}/{endDay}/{guests}"

    fun lotteryWaitlist(lotteryId: String) = "lottery_waitlist/$lotteryId"
    fun confirmReservation(campsiteId: String) = "confirm_reservation/$campsiteId"
    fun waitlistScreen(campsiteId: String) = "waitlist_screen/$campsiteId"
    fun waitlistEntrants(lotteryId: String) = "waitlist_entrants/$lotteryId"
    fun selectCampsite(lotteryId: String) = "select_campsite/$lotteryId"
    fun reserveDates(lotteryId: String, campsiteId: String) = "reserve_dates/$lotteryId/$campsiteId"
    fun confirmReservationWithDetails(
        campsiteId: String,
        lotteryId: String,
        startDay: Long,
        endDay: Long,
        guests: Int,
        contactName: String = "",
        contactEmail: String = "",
        contactPhone: String = ""
    ) = "confirm_reservation_details/$campsiteId/$lotteryId/$startDay/$endDay/$guests"
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
        val lotteryId = entry.requireArg("lotteryId")
        WaitlistEntrantsScreen(
            lotteryId = lotteryId,
            onBack = { navController.popBackStack() },
            onNotifyClick = { navController.navigate(OwnerNotifyRoutes.compose(lotteryId)) }
        )
    }

    composable(WaitlistRoutes.CONFIRM_RESERVATION, arguments = listOf(requiredId("campsiteId"))) { entry ->
        ConfirmReservationScreen(navController, campsiteId = entry.requireArg("campsiteId"))
    }

    composable(WaitlistRoutes.WAITLIST_SCREEN, arguments = listOf(requiredId("campsiteId"))) { entry ->
        WaitlistScreen(navController, campsiteId = entry.requireArg("campsiteId"))
    }

    composable(WaitlistRoutes.SELECT_CAMPSITE, arguments = listOf(requiredId("lotteryId"))) { entry ->
        SelectCampsiteScreen(navController, lotteryId = entry.requireArg("lotteryId"))
    }

    composable(
        WaitlistRoutes.RESERVE_DATES,
        arguments = listOf(requiredId("lotteryId"), requiredId("campsiteId"))
    ) { entry ->
        ReserveDatesScreen(
            navController = navController,
            lotteryId = entry.requireArg("lotteryId"),
            campsiteId = entry.requireArg("campsiteId")
        )
    }

    composable(
        WaitlistRoutes.CONFIRM_RESERVATION_DETAILS,
        arguments = listOf(
            requiredId("campsiteId"),
            requiredId("lotteryId"),
            requiredId("startDay"),
            requiredId("endDay"),
            requiredId("guests")
        )
    ) { entry ->
        ConfirmReservationScreen(
            navController = navController,
            campsiteId = entry.requireArg("campsiteId"),
            lotteryId = entry.requireArg("lotteryId"),
            startDayKey = entry.requireArg("startDay").toLongOrNull(),
            endDayKey = entry.requireArg("endDay").toLongOrNull(),
            guests = entry.requireArg("guests").toIntOrNull() ?: 1
        )
    }
}
