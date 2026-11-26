// OWNER: Neaz (NeazMZ06) — Messaging
// USER STORIES: 02.12.01, 01.14.01, 03.12.01
// PURPOSE: Routes and nav graph for the chat screen and the admin message log. The Messages tab
//  itself is ShellRoutes.MESSAGES.
// Outstanding issues: none.
package com.example.trails.ui.messaging

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.trails.ui.navigation.requireArg
import com.example.trails.ui.navigation.requiredId

/** Routes for messaging screens. */
object MessagingRoutes {
    /** One owner–camper conversation about a campsite. */
    const val CHAT = "chat/{campsiteId}/{camperId}"

    /**
     * The chat route for one campsite and camper.
     *
     * @param campsiteId the campsite.
     * @param camperId the camper.
     * @return the route to navigate to.
     */
    fun chat(campsiteId: String, camperId: String) = "chat/$campsiteId/$camperId"

    /** The admin's read-only log of owner-to-camper messages (US 03.12.01). */
    const val ADMIN_LOG = "admin_message_log"
}

/**
 * Registers the messaging screens.
 *
 * @param navController passed to each screen so it can navigate.
 */
fun NavGraphBuilder.messagingGraph(navController: NavController) {
    composable(MessagingRoutes.CHAT, arguments = listOf(requiredId("campsiteId"), requiredId("camperId"))) { entry ->
        ChatScreen(
            navController,
            campsiteId = entry.requireArg("campsiteId"),
            camperId = entry.requireArg("camperId")
        )
    }
    composable(MessagingRoutes.ADMIN_LOG) { AdminMessageLogScreen(navController) }
}
