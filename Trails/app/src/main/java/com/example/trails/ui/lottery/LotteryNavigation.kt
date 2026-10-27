// OWNER: Skanda (skandarajeev) — Lottery Setup
// USER STORIES: 02.04.01–02.04.07
// PURPOSE: Routes and nav graph for the lottery screens.
// TODO: Register new lottery screens in lotteryGraph().
package com.example.trails.ui.lottery

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.trails.ui.navigation.optionalArg
import com.example.trails.ui.navigation.optionalId

object LotteryRoutes {
    const val LOTTERY_FORM = "lottery_form?lotteryId={lotteryId}&campsiteId={campsiteId}"
    const val MY_LOTTERIES = "my_lotteries"

    // Only non-null arguments are added to the query string.
    fun lotteryForm(lotteryId: String? = null, campsiteId: String? = null): String {
        val params = listOfNotNull(
            lotteryId?.let { "lotteryId=$it" },
            campsiteId?.let { "campsiteId=$it" }
        )
        return if (params.isEmpty()) "lottery_form" else "lottery_form?" + params.joinToString("&")
    }
}

fun NavGraphBuilder.lotteryGraph(navController: NavController) {
    composable(
        LotteryRoutes.LOTTERY_FORM,
        arguments = listOf(optionalId("lotteryId"), optionalId("campsiteId"))
    ) { entry ->
        LotteryFormScreen(
            navController,
            lotteryId = entry.optionalArg("lotteryId"),
            campsiteId = entry.optionalArg("campsiteId")
        )
    }
    composable(LotteryRoutes.MY_LOTTERIES) { MyLotteriesScreen(navController) }
}
