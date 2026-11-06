// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 01.11.01
// PURPOSE: Entrant's Camping guidelines screen, opened from App policy, with general
//   etiquette, leave-no-trace and safety guidance (Figma Scenario 1 · Guidelines).
// Outstanding issues: campsite-specific rules are on the campsite rules screen
//   (US 02.11.01), not here; guideline text is fixed in the app, not loaded from Firestore.
package com.example.trails.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.ui.theme.TrailsTheme

/**
 * Connects navigation to [GuidelinesContent] with the fixed [campingGuidelineCards].
 * Back and "Back to app policy" both return to the App policy screen.
 *
 * @param navController used to go back to App policy.
 * @param modifier modifier for the screen's root layout.
 */
@Composable
fun GuidelinesScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    GuidelinesContent(
        cards = campingGuidelineCards,
        onBack = { navController.popBackStack() },
        onBackToPolicy = { navController.popBackStack() },
        modifier = modifier
    )
}

/**
 * Stateless Camping guidelines UI, split out so UI tests can drive it directly.
 *
 * @param cards guideline cards to show, top to bottom.
 * @param onBack called when the back arrow is tapped.
 * @param onBackToPolicy called when "Back to app policy" is tapped.
 * @param modifier modifier for the screen's root layout.
 */
@Composable
fun GuidelinesContent(
    cards: List<PolicyCard>,
    onBack: () -> Unit,
    onBackToPolicy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TrailsTopBar(
                title = stringResource(R.string.guidelines_title),
                onBack = onBack,
                backTestTag = GuidelinesTags.BACK,
                backContentDescription = stringResource(R.string.policy_back)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.guidelines_breadcrumb),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.guidelines_heading),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.testTag(GuidelinesTags.TITLE)
            )
            Text(
                stringResource(R.string.guidelines_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            cards.forEachIndexed { index, card ->
                PolicyCardItem(card, Modifier.testTag(GuidelinesTags.card(index)))
            }
            Text(
                stringResource(R.string.guidelines_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(GuidelinesTags.NOTE)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onBackToPolicy,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(GuidelinesTags.BACK_TO_POLICY)
            ) {
                Text(stringResource(R.string.guidelines_back_to_policy))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Test tags used by UI tests to find elements on this screen. */
object GuidelinesTags {
    const val BACK = "guidelines_back"
    const val TITLE = "guidelines_title"
    const val NOTE = "guidelines_note"
    const val BACK_TO_POLICY = "guidelines_back_to_policy"

    /**
     * Tag for the guideline card at [index].
     *
     * @param index 0-based position of the card.
     * @return the test tag for that card.
     */
    fun card(index: Int) = "guidelines_card_$index"
}

/** Design-time preview of the Camping guidelines screen, for comparing against Scenario 1. */
@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun GuidelinesContentPreview() {
    TrailsTheme {
        GuidelinesContent(
            cards = campingGuidelineCards,
            onBack = {},
            onBackToPolicy = {}
        )
    }
}
