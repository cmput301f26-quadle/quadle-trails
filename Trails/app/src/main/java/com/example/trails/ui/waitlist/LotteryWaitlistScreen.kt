// shows waitlist entries for one lottery
// owner: chitkarsh (chitkarshrathi) - waitlist
package com.example.trails.ui.waitlist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

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
