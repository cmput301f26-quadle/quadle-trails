// Reusable match percentage score badge composable for embedding on campsite cards (UI Controller component).
// implements Bonus CampMatch Recommendation Engine
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Matching
package com.example.trails.matcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trails.ui.theme.Ember
import com.example.trails.ui.theme.Night
import com.example.trails.ui.theme.TrailGreen
import com.example.trails.ui.theme.TrailsTheme

/**
 * Reusable compatibility badge rendering the calculated match percentage.
 *
 * NOTE FOR TEAMMATES:
 * - Ibrahim (Campsite Discovery & Cards): You can embed this badge directly on any campsite card
 *   with one line of code:
 *   ```kotlin
 *   MatchScoreBadge(score = matchResult.matchPercentage)
 *   ```
 *
 * Color Coded:
 * - 70% to 100%: High match (TrailGreen container, high contrast)
 * - 40% to 69%: Moderate match (Warm Amber outline)
 * - Below 40%: Subtle / low match (Muted grey)
 *
 * @param score match percentage (0 to 100).
 * @param modifier optional layout modifier.
 */
@Composable
fun MatchScoreBadge(
    score: Int,
    modifier: Modifier = Modifier
) {
    val clamped = score.coerceIn(0, 100)

    val (containerColor, contentColor, borderColor) = when {
        clamped >= 70 -> Triple(
            TrailGreen.copy(alpha = 0.20f),
            TrailGreen,
            TrailGreen.copy(alpha = 0.60f)
        )
        clamped >= 40 -> Triple(
            Ember.copy(alpha = 0.15f),
            Ember,
            Ember.copy(alpha = 0.50f)
        )
        else -> Triple(
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.60f),
            Color.White.copy(alpha = 0.20f)
        )
    }

    Box(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("match_score_badge"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$clamped% Match",
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.testTag("match_score_text")
        )
    }
}

@Preview(name = "High Match", showBackground = true, backgroundColor = 0xFF141915)
@Composable
private fun MatchScoreBadgeHighPreview() {
    TrailsTheme {
        MatchScoreBadge(score = 92)
    }
}

@Preview(name = "Medium Match", showBackground = true, backgroundColor = 0xFF141915)
@Composable
private fun MatchScoreBadgeMediumPreview() {
    TrailsTheme {
        MatchScoreBadge(score = 58)
    }
}
