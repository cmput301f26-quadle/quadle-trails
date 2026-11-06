// OWNER: Skanda (skandarajeev) — Lottery Setup
// USER STORIES: 02.04.01–02.04.07
// PURPOSE: Routes and nav graph for the lottery screens.
// Outstanding issues: none.
package com.example.trails.ui.lottery

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.trails.ui.navigation.optionalArg
import com.example.trails.ui.navigation.optionalId
import com.example.trails.ui.navigation.requireArg
import com.example.trails.ui.navigation.requiredId

/** Key the edit screen uses to hand the "Changes saved" text back to the details page. */
const val LOTTERY_SAVED_MESSAGE = "lottery_saved_message"

object LotteryRoutes {
    const val LOTTERY_FORM = "lottery_form?lotteryId={lotteryId}&campsiteId={campsiteId}&reuseId={reuseId}"
    const val MY_LOTTERIES = "my_lotteries"
    const val LOTTERY_DETAILS = "lottery_details/{lotteryId}"

    /**
     * New lottery wizard, or Edit lottery when [lotteryId] is set. Only non-null arguments are
     * added to the query string.
     *
     * @param campsiteId campsite to pre-select in a new lottery.
     * @param reuseId closed lottery whose settings (not dates) start the new one.
     */
    fun lotteryForm(lotteryId: String? = null, campsiteId: String? = null, reuseId: String? = null): String {
        val params = listOfNotNull(
            lotteryId?.let { "lotteryId=$it" },
            campsiteId?.let { "campsiteId=$it" },
            reuseId?.let { "reuseId=$it" }
        )
        return if (params.isEmpty()) "lottery_form" else "lottery_form?" + params.joinToString("&")
    }

    /** The owner's page for one lottery. */
    fun lotteryDetails(lotteryId: String) = "lottery_details/$lotteryId"
}

fun NavGraphBuilder.lotteryGraph(navController: NavController) {
    composable(
        LotteryRoutes.LOTTERY_FORM,
        arguments = listOf(optionalId("lotteryId"), optionalId("campsiteId"), optionalId("reuseId"))
    ) { entry ->
        LotteryFormScreen(
            navController,
            lotteryId = entry.optionalArg("lotteryId"),
            campsiteId = entry.optionalArg("campsiteId"),
            reuseLotteryId = entry.optionalArg("reuseId")
        )
    }
    composable(LotteryRoutes.MY_LOTTERIES) { MyLotteriesScreen(navController) }
    composable(LotteryRoutes.LOTTERY_DETAILS, arguments = listOf(requiredId("lotteryId"))) { entry ->
        LotteryDetailsScreen(navController, lotteryId = entry.requireArg("lotteryId"), savedState = entry.savedStateHandle)
    }
}
