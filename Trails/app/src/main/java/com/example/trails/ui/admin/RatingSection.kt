// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 01.13.01
// PURPOSE: Lets a user rate a campsite from its details page, using a bottom sheet (Figma Scenario 4 · Rate campsite / Rating submitted).
// Outstanding issues: anyone except the owner can rate (no completed-stay check until reservations, Part 4); other users' reviews aren't listed.
package com.example.trails.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trails.R
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Rating
import com.example.trails.ui.theme.TrailGreen
import com.example.trails.ui.theme.TrailsTheme

/**
 * The rating section on a campsite's details page. Neaz's CampsiteDetailsScreen calls this
 * with just the campsite id; everything else has a default.
 *
 * @param campsiteId id of the campsite being rated.
 * @param viewModel one per campsite, kept by the key so each details page has its own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingSection(
    campsiteId: String,
    modifier: Modifier = Modifier,
    viewModel: RatingSectionViewModel = viewModel(key = "rating-$campsiteId") { RatingSectionViewModel(campsiteId) }
) {
    val state by viewModel.state.collectAsState()
    RatingSectionContent(state = state, onRateClick = viewModel::onOpenSheet, modifier = modifier)

    if (state.isSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = viewModel::onDismissSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            RatingSheetContent(
                score = state.draftScore,
                review = state.draftReview,
                isSubmitting = state.isSubmitting,
                error = state.error,
                onStarClick = viewModel::onStarClick,
                onReviewChange = viewModel::onReviewChange,
                onSubmit = viewModel::onSubmit,
                onCancel = viewModel::onDismissSheet
            )
        }
    }
}

/**
 * Stateless section shown on the details page, split out so UI tests can drive it directly.
 * Shows nothing when the user can't rate (e.g. they own the campsite).
 *
 * @param state what to show: the Rate button, or the user's rating with an Edit button.
 * @param onRateClick called when Rate this campsite or Edit rating is tapped.
 */
@Composable
fun RatingSectionContent(
    state: RatingSectionUiState,
    onRateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.canRate) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.justSubmitted) ThanksBanner()
        val mine = state.myRating
        if (mine == null) {
            Button(
                onClick = onRateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(RatingTags.RATE_BUTTON)
            ) {
                Text(stringResource(R.string.rating_rate_button))
            }
        } else {
            YourRatingCard(mine)
            OutlinedButton(
                onClick = onRateClick,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(RatingTags.EDIT_BUTTON)
            ) {
                Text(stringResource(R.string.rating_edit_button))
            }
        }
    }
}

/** Green-on-forest "Thanks for your rating" message shown after a successful submit. */
@Composable
private fun ThanksBanner() {
    Text(
        stringResource(R.string.rating_thanks),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
            .padding(12.dp)
            .testTag(RatingTags.THANKS)
    )
}

/** Card with "Your rating", the user's stars and, if they wrote one, their review. */
@Composable
private fun YourRatingCard(rating: Rating) {
    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.rating_your_rating),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag(RatingTags.YOUR_RATING)
            )
            val starsDesc = stringResource(R.string.rating_stars_desc, rating.score)
            Row(Modifier.clearAndSetSemantics { contentDescription = starsDesc }) {
                for (n in 1..5) StarIcon(filled = n <= rating.score, size = 20.dp)
            }
            if (rating.review.isNotBlank()) {
                Text(
                    rating.review,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Stateless contents of the "Rate campsite" bottom sheet, split out so UI tests can drive it
 * without opening a real sheet.
 *
 * @param score stars picked so far, 0 for none.
 * @param review review text typed so far.
 * @param isSubmitting true while saving; the form is locked and Submit shows a spinner.
 * @param error non-null if the last submit failed; a general "try again" message is shown.
 * @param onStarClick called with 1 to 5 when a star is tapped.
 * @param onReviewChange called with the new text as the review is typed.
 * @param onSubmit called when Submit rating is tapped.
 * @param onCancel called when Cancel is tapped.
 */
@Composable
fun RatingSheetContent(
    score: Int,
    review: String,
    isSubmitting: Boolean,
    error: String?,
    onStarClick: (Int) -> Unit,
    onReviewChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.rating_sheet_title), style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (n in 1..5) {
                val desc = stringResource(R.string.rating_star_button_desc, n)
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !isSubmitting, role = Role.Button) { onStarClick(n) }
                        .semantics {
                            contentDescription = desc
                            selected = n <= score
                        }
                        .testTag(RatingTags.star(n)),
                    contentAlignment = Alignment.Center
                ) {
                    StarIcon(filled = n <= score, size = 40.dp)
                }
            }
        }

        if (isValidScore(score)) {
            Text(
                scoreSummary(score),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag(RatingTags.SUMMARY)
            )
        } else {
            Text(
                stringResource(R.string.rating_tap_star),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = review,
            onValueChange = onReviewChange,
            label = { Text(stringResource(R.string.rating_review_label)) },
            supportingText = {
                Text(
                    stringResource(R.string.rating_review_counter, review.length, MAX_REVIEW_LENGTH),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            },
            enabled = !isSubmitting,
            minLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(RatingTags.REVIEW)
        )

        Text(
            stringResource(R.string.rating_review_helper),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (error != null) {
            Text(
                stringResource(R.string.rating_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(RatingTags.ERROR)
            )
        }

        Button(
            onClick = onSubmit,
            enabled = isValidScore(score) && !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(RatingTags.SUBMIT)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(stringResource(R.string.rating_submit))
            }
        }
        TextButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(RatingTags.CANCEL)
        ) {
            Text(stringResource(R.string.rating_cancel))
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** One star: TrailGreen when filled, a faint grey when not. Decorative; callers describe it. */
@Composable
private fun StarIcon(filled: Boolean, size: Dp) {
    Icon(
        Icons.Filled.Star,
        contentDescription = null,
        tint = if (filled) TrailGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.size(size)
    )
}

/** Test tags used by UI tests to find elements in the rating section and sheet. */
object RatingTags {
    const val RATE_BUTTON = "rating_rate_button"
    const val EDIT_BUTTON = "rating_edit_button"
    const val YOUR_RATING = "rating_your_rating"
    const val THANKS = "rating_thanks"
    const val REVIEW = "rating_review"
    const val SUBMIT = "rating_submit"
    const val CANCEL = "rating_cancel"
    const val ERROR = "rating_error"
    const val SUMMARY = "rating_summary"

    /** Tag for the [n]th star (1 to 5) in the sheet. */
    fun star(n: Int) = "rating_star_$n"
}

/** Design-time preview of the section for someone who hasn't rated yet. */
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun RatingSectionNotRatedPreview() {
    TrailsTheme {
        RatingSectionContent(state = RatingSectionUiState(canRate = true), onRateClick = {})
    }
}

/** Design-time preview of the section right after rating, with the saved rating shown. */
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun RatingSectionRatedPreview() {
    TrailsTheme {
        RatingSectionContent(
            state = RatingSectionUiState(
                canRate = true,
                justSubmitted = true,
                myRating = Rating(
                    userId = TestUsers.ENTRANT_ID,
                    campsiteId = TestCampsites.BANFF_ID,
                    score = 4,
                    review = "Quiet site with a great view of Rundle. Washrooms were a short walk."
                )
            ),
            onRateClick = {}
        )
    }
}

/** Design-time preview of the bottom sheet with four stars picked, for comparing against Scenario 4. */
@Preview(showBackground = true, heightDp = 640)
@Composable
private fun RatingSheetPreview() {
    TrailsTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            RatingSheetContent(
                score = 4,
                review = "Lovely and quiet.",
                isSubmitting = false,
                error = null,
                onStarClick = {},
                onReviewChange = {},
                onSubmit = {},
                onCancel = {},
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}
