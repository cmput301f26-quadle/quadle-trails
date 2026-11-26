package com.example.trails.ui.notification

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.trails.ui.reservation.ReservationScreen
import com.example.trails.ui.shell.NotificationsContent

object NotificationRoutes {
    const val NOTIFICATION_LIST = "notifications"
    const val RESERVATION_LIST = "reservations"
}

/**
 * Builds the notification part of the navigation graph.
 * Includes reservation list as notifications can navigate there.
 */
fun NavGraphBuilder.notificationGraph(navController: NavController) {
    composable(NotificationRoutes.NOTIFICATION_LIST) { NotificationsContent(navController = navController) }
    composable(NotificationRoutes.RESERVATION_LIST) { ReservationScreen(navController) }
}