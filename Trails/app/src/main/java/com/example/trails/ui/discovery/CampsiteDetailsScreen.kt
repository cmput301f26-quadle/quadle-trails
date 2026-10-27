// OWNER: Neaz (NeazMZ06) — Search & Discovery
// USER STORIES: 01.01.05
// PURPOSE: Shows one campsite's details to an entrant.
// TODO: Load the campsite with CampsiteRepository.getById(campsiteId).
//       Show its details around the join and rating sections owned by others.
package com.example.trails.ui.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.trails.ui.admin.RatingSection
import com.example.trails.ui.waitlist.JoinLotterySection

@Composable
fun CampsiteDetailsScreen(
    navController: NavController,
    campsiteId: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Campsite Details", style = MaterialTheme.typography.headlineSmall)

        // Owned by Chitkarsh. Neaz: leave this call here, don't edit inside it.
        JoinLotterySection(campsiteId)

        // Owned by Zainab. Neaz: leave this call here, don't edit inside it.
        RatingSection(campsiteId)
    }
}
