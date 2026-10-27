// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 01.13.01
// PURPOSE: Shows and collects ratings for a campsite.
// TODO: List ratings from RatingRepository.observeForCampsite(campsiteId).
//       Let the current user add or update their rating.
package com.example.trails.ui.admin

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun RatingSection(campsiteId: String, modifier: Modifier = Modifier) {
    Text(text = "Rating Section", modifier = modifier)
}
