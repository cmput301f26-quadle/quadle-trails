// OWNER: Aarav (aarav-kapoor-cs) — Owner notifications
// USER STORIES: 02.10.01, 02.10.02
// PURPOSE: Routes and nav graph for New notification, Sent notifications and their details.
// Outstanding issues: None currently known.
// AI assistance: Used AI to help set up the owner notification routes.
package com.example.trails.ui.ownernotify

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.trails.ui.navigation.requireArg
import com.example.trails.ui.navigation.requiredId

/** Route names for the owner's notification screens. */
object OwnerNotifyRoutes {
    /** New notification for one lottery's waitlist (US 02.10.01). */
    const val COMPOSE = "notify_waitlist/{lotteryId}"

    /** Everything the owner has sent (US 02.10.02). */
    const val SENT = "sent_notifications"

    /** One sent notification (US 02.10.02). */
    const val SENT_DETAILS = "sent_notifications/{sentId}"

    /**
     * The route that opens New notification.
     *
     * @param lotteryId the lottery whose waitlist is notified.
     */
    fun compose(lotteryId: String) = "notify_waitlist/$lotteryId"

    /**
     * The route that opens one sent notification.
     *
     * @param id the notification's id.
     */
    fun sentDetails(id: String) = "sent_notifications/$id"
}

/**
 * Registers the owner's notification screens in the app's navigation graph.
 *
 * @param navController the app's navigation controller.
 */
fun NavGraphBuilder.ownerNotifyGraph(navController: NavController) {
    composable(OwnerNotifyRoutes.COMPOSE, arguments = listOf(requiredId("lotteryId"))) { entry ->
        ComposeNotificationScreen(navController, lotteryId = entry.requireArg("lotteryId"))
    }
    composable(OwnerNotifyRoutes.SENT) { SentNotificationsScreen(navController) }
    composable(OwnerNotifyRoutes.SENT_DETAILS, arguments = listOf(requiredId("sentId"))) { entry ->
        SentNotificationDetailsScreen(navController, id = entry.requireArg("sentId"))
    }
}
