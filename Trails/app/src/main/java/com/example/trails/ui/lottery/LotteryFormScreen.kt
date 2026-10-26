// OWNER: Skanda (skandarajeev) — Lottery Setup
// USER STORIES: 02.04.01–02.04.07
// PURPOSE: Create a lottery, or edit one when lotteryId is set.
// TODO: Build the form for the Lottery fields and save via LotteryRepository.
//       Pre-select the campsite when campsiteId is passed in.
package com.example.trails.ui.lottery

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.trails.ui.common.PlaceholderScreen

@Composable
fun LotteryFormScreen(
    navController: NavController,
    lotteryId: String?,
    campsiteId: String?,
    modifier: Modifier = Modifier
) {
    PlaceholderScreen(name = "Lottery Form", modifier = modifier)
}
