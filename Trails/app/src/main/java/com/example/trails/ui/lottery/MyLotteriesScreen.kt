// OWNER: Skanda (skandarajeev) — Lottery Setup
// USER STORIES: supports 02.04.06
// PURPOSE: Lists the lotteries the current owner has created.
// TODO: List lotteries from LotteryRepository.observeByOwner(CurrentUser.id()).
package com.example.trails.ui.lottery

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.trails.ui.common.PlaceholderScreen

@Composable
fun MyLotteriesScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(name = "My Lotteries", modifier = modifier)
}
