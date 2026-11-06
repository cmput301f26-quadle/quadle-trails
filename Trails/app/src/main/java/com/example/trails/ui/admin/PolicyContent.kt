// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 01.11.01
// PURPOSE: The fixed text cards shown on the App policy screen (Figma Scenario 1 · App policy).
// Outstanding issues: policy text is fixed in the app, not loaded from Firestore.
package com.example.trails.ui.admin

import androidx.annotation.StringRes
import com.example.trails.R

/**
 * One titled block of policy text on the App policy screen.
 *
 * @param title string resource for the card's heading.
 * @param body string resource for the card's paragraph.
 */
data class PolicyCard(
    @StringRes val title: Int,
    @StringRes val body: Int
)

/** The four App policy cards, in the order they appear on screen. */
val appPolicyCards: List<PolicyCard> = listOf(
    PolicyCard(R.string.policy_card_lottery_title, R.string.policy_card_lottery_body),
    PolicyCard(R.string.policy_card_selected_title, R.string.policy_card_selected_body),
    PolicyCard(R.string.policy_card_device_title, R.string.policy_card_device_body),
    PolicyCard(R.string.policy_card_rules_title, R.string.policy_card_rules_body)
)
