// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
// USER STORIES: 01.01.06, 01.02.01, 01.02.02
// PURPOSE: Lets an entrant join or leave the lottery for a campsite.
// TODO: Look up the campsite's activeLotteryId and the user's WaitlistEntry.
//       Show join/leave and save via WaitlistRepository.
package com.example.trails.ui.waitlist

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun JoinLotterySection(campsiteId: String, modifier: Modifier = Modifier) {
    Text(text = "Join Lottery Section", modifier = modifier)
}
