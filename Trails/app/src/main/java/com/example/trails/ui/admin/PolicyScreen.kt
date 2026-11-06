// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 01.11.01
// PURPOSE: Entrant's App policy screen explaining lottery entry, selection, the device-linked
//   profile and campsite rules (Figma Scenario 1 · App policy).
// Outstanding issues: policy text is fixed in the app, not loaded from Firestore.
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
 * Connects navigation to [PolicyContent] with the fixed [appPolicyCards].
 * Back and "Back to settings" both return to the previous screen;
 * "Read camping guidelines" opens [GuidelinesScreen].
 *
 * @param navController used to go back to Settings and to open the guidelines.
 * @param modifier modifier for the screen's root layout.
 */
@Composable
fun PolicyScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    PolicyContent(
        cards = appPolicyCards,
        onBack = { navController.popBackStack() },
        onGuidelinesClick = { navController.navigate(AdminRoutes.GUIDELINES) },
        onBackToSettings = { navController.popBackStack() },
        modifier = modifier
    )
}

/**
 * Stateless App policy UI, split out so UI tests can drive it directly.
 *
 * @param cards policy cards to show, top to bottom.
 * @param onBack called when the back arrow is tapped.
 * @param onGuidelinesClick called when "Read camping guidelines" is tapped.
 * @param onBackToSettings called when "Back to settings" is tapped.
 * @param modifier modifier for the screen's root layout.
 */
@Composable
fun PolicyContent(
    cards: List<PolicyCard>,
    onBack: () -> Unit,
    onGuidelinesClick: () -> Unit,
    onBackToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TrailsTopBar(
                title = stringResource(R.string.policy_title),
                onBack = onBack,
                backTestTag = PolicyTags.BACK,
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
                stringResource(R.string.policy_breadcrumb),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.policy_heading),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.testTag(PolicyTags.TITLE)
            )
            Text(
                stringResource(R.string.policy_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            cards.forEachIndexed { index, card ->
                PolicyCardItem(card, Modifier.testTag(PolicyTags.card(index)))
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onGuidelinesClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(PolicyTags.GUIDELINES_BUTTON)
            ) {
                Text(stringResource(R.string.policy_guidelines_button))
            }
            OutlinedButton(
                onClick = onBackToSettings,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(PolicyTags.BACK_TO_SETTINGS)
            ) {
                Text(stringResource(R.string.policy_back_to_settings))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * One outlined green card with a heading and its paragraph; shared by the
 * App policy and Camping guidelines screens.
 *
 * @param card the heading and paragraph to show.
 * @param modifier modifier for the card, e.g. its test tag.
 */
@Composable
internal fun PolicyCardItem(card: PolicyCard, modifier: Modifier = Modifier) {
    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(stringResource(card.title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(card.body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Test tags used by UI tests to find elements on this screen. */
object PolicyTags {
    const val BACK = "policy_back"
    const val TITLE = "policy_title"
    const val GUIDELINES_BUTTON = "policy_guidelines_button"
    const val BACK_TO_SETTINGS = "policy_back_to_settings"

    /** Tag for the policy card at [index] (0-based). */
    fun card(index: Int) = "policy_card_$index"
}

/** Design-time preview of the App policy screen, for comparing against Scenario 1. */
@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun PolicyContentPreview() {
    TrailsTheme {
        PolicyContent(
            cards = appPolicyCards,
            onBack = {},
            onGuidelinesClick = {},
            onBackToSettings = {}
        )
    }
}
