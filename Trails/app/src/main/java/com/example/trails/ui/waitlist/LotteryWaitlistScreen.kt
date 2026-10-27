// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
// USER STORIES: 02.09.01
// PURPOSE: Shows the waitlist entries for one lottery.
// TODO: List entries from WaitlistRepository.observeForLottery(lotteryId).
package com.example.trails.ui.waitlist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.trails.ui.common.PlaceholderScreen

@Composable
fun LotteryWaitlistScreen(
    navController: NavController,
    lotteryId: String,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(name = "Lottery Waitlist", modifier = modifier)
}
