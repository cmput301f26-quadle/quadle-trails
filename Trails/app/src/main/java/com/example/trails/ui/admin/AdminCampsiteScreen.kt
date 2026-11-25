// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.01.01, 03.09.01, 03.10.01
// PURPOSE: Admin's view of one campsite, opened from the Campsites list, with a
//   Remove campsite button and confirmation dialog (Figma Scenario 8 · Remove campsite?),
//   and a Ratings & reviews section where each rating can be removed (US 03.09.01), or
//   only its review text, keeping the stars (US 03.10.01). No Figma mockup for either,
//   so they follow this page's card style.
// Outstanding issues:
//   - Guests with reservations aren't notified, since reservations and
//     notifications don't exist yet (Part 4).
//   - The campsite's ratings stay in storage after removal.
//   - Ratings come from FakeRatingRepository until AppContainer is switched to
//     FirestoreRatingRepository (once ratings are seeded).
//   - Every rating is listed; there is no way for campers to flag one yet.
package com.example.trails.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.trails.R
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestRatings
import com.example.trails.data.fake.TestUsers
import com.example.trails.ui.theme.Ember
import com.example.trails.ui.discovery.DiscoveryRoutes
import com.example.trails.ui.theme.TrailsTheme

/**
 * Connects [AdminCampsiteViewModel] to navigation and renders [AdminCampsiteContent].
 * Only the campsite's id arrives through navigation; the ViewModel loads it itself.
 *
 * @param navController used to go back to the Campsites list and open the public page.
 * @param campsiteId id of the campsite to show.
 */
@Composable
fun AdminCampsiteScreen(
    navController: NavController,
    campsiteId: String,
    modifier: Modifier = Modifier,
    viewModel: AdminCampsiteViewModel = viewModel(key = campsiteId) { AdminCampsiteViewModel(campsiteId) }
) {
    val state by viewModel.state.collectAsState()
    val removeState by viewModel.removeState.collectAsState()
    val ratingRemoveState by viewModel.ratingRemoveState.collectAsState()
    LaunchedEffect(removeState.removed) {
        if (removeState.removed) navController.popBackStack()
    }
    AdminCampsiteContent(
        state = state,
        removeState = removeState,
        onBack = { navController.popBackStack() },
        onOpenPublicPage = { id -> navController.navigate(DiscoveryRoutes.campsiteDetails(id)) },
        onConfirmRemove = viewModel::onConfirmRemove,
        ratingRemoveState = ratingRemoveState,
        onRemoveRatingClick = viewModel::onRemoveRatingClick,
        onDismissRemoveRating = viewModel::onDismiss,
        onConfirmRemoveRating = viewModel::onConfirmRemoveRating,
        onRemoveReviewClick = viewModel::onRemoveReviewClick,
        onConfirmRemoveReview = viewModel::onConfirmRemoveReview,
        modifier = modifier
    )
}

/**
 * Stateless admin Campsite UI, split out so UI tests can drive it directly. Only whether the
 * confirmation dialog is open is kept here; it also stays open while a removal is running.
 * Which rating the "Remove rating?" dialog is about comes from [ratingRemoveState], so the
 * ViewModel knows which one to remove.
 *
 * @param state what to show: a spinner, a not-found message, or the campsite.
 * @param removeState progress of a confirmed removal.
 * @param onBack called when the back arrow is tapped.
 * @param onOpenPublicPage called with the campsite's id when "Open public page" is tapped.
 * @param onConfirmRemove called when the admin taps Remove in the dialog.
 * @param ratingRemoveState which rating the "Remove rating?" dialog is about and its progress.
 * @param onRemoveRatingClick called with the reviewer's device id when "Remove rating" is tapped.
 * @param onDismissRemoveRating called when Cancel is tapped in the "Remove rating?" or
 *   "Remove review?" dialog.
 * @param onConfirmRemoveRating called when Remove is tapped in the "Remove rating?" dialog.
 * @param onRemoveReviewClick called with the reviewer's device id when "Remove review" is tapped.
 * @param onConfirmRemoveReview called when Remove is tapped in the "Remove review?" dialog.
 */
@Composable
fun AdminCampsiteContent(
    state: AdminCampsiteUiState,
    removeState: AdminCampsiteRemoveState,
    onBack: () -> Unit,
    onOpenPublicPage: (String) -> Unit,
    onConfirmRemove: () -> Unit,
    modifier: Modifier = Modifier,
    ratingRemoveState: AdminRatingRemoveState = AdminRatingRemoveState(),
    onRemoveRatingClick: (String) -> Unit = {},
    onDismissRemoveRating: () -> Unit = {},
    onConfirmRemoveRating: () -> Unit = {},
    onRemoveReviewClick: (String) -> Unit = {},
    onConfirmRemoveReview: () -> Unit = {}
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TrailsTopBar(
                title = stringResource(R.string.admin_campsite_title),
                onBack = onBack,
                backTestTag = AdminCampsiteTags.BACK,
                backContentDescription = stringResource(R.string.admin_campsites_back)
            )
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (state) {
                AdminCampsiteUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.testTag(AdminCampsiteTags.LOADING))
                }
                AdminCampsiteUiState.NotFound -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.admin_campsite_not_found),
                        modifier = Modifier.testTag(AdminCampsiteTags.NOT_FOUND)
                    )
                }
                is AdminCampsiteUiState.Loaded -> {
                    CampsiteDetails(
                        state = state,
                        failed = removeState.failed,
                        ratingRemoveState = ratingRemoveState,
                        onOpenPublicPage = { onOpenPublicPage(state.campsite.id) },
                        onRemove = { showDialog = true },
                        onRemoveRating = onRemoveRatingClick,
                        onRemoveReview = onRemoveReviewClick
                    )
                    if (showDialog || removeState.removing) {
                        AdminRemoveDialog(
                            title = stringResource(R.string.admin_campsite_remove_title),
                            message = removeCampsiteMessage(
                                state.campsite.name, state.lotteryCount, state.entryCount
                            ),
                            removing = removeState.removing,
                            tags = AdminCampsiteTags.dialogTags,
                            onCancel = { showDialog = false },
                            onRemove = {
                                showDialog = false
                                onConfirmRemove()
                            }
                        )
                    }
                    val confirming = state.ratings.find { it.rating.userId == ratingRemoveState.confirmingUserId }
                    if (confirming != null && ratingRemoveState.reviewOnly) {
                        AdminRemoveDialog(
                            title = stringResource(R.string.admin_campsite_remove_review_title),
                            message = removeReviewMessage(confirming.reviewerName),
                            removing = ratingRemoveState.removing,
                            tags = AdminCampsiteTags.reviewDialogTags,
                            onCancel = onDismissRemoveRating,
                            onRemove = onConfirmRemoveReview
                        )
                    } else if (confirming != null) {
                        AdminRemoveDialog(
                            title = stringResource(R.string.admin_campsite_remove_rating_title),
                            message = removeRatingMessage(confirming.reviewerName, confirming.rating.score),
                            removing = ratingRemoveState.removing,
                            tags = AdminCampsiteTags.ratingDialogTags,
                            onCancel = onDismissRemoveRating,
                            onRemove = onConfirmRemoveRating
                        )
                    }
                }
            }
        }
    }
}

/**
 * Photo, name, location, owner, info card, ratings, public page link, and the Remove campsite
 * button.
 */
@Composable
private fun CampsiteDetails(
    state: AdminCampsiteUiState.Loaded,
    failed: Boolean,
    ratingRemoveState: AdminRatingRemoveState,
    onOpenPublicPage: () -> Unit,
    onRemove: () -> Unit,
    onRemoveRating: (String) -> Unit,
    onRemoveReview: (String) -> Unit
) {
    val campsite = state.campsite
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Photo(campsite.imageUrl)
            Spacer(Modifier.height(16.dp))
            Text(
                campsite.name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.testTag(AdminCampsiteTags.NAME)
            )
            if (campsite.locationName.isNotBlank()) {
                Text(
                    campsite.locationName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                stringResource(R.string.admin_campsite_owner, state.ownerName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            InfoCard(state)
            RatingsSection(state.ratings, ratingRemoveState, onRemoveRating, onRemoveReview)
            TextButton(
                onClick = onOpenPublicPage,
                modifier = Modifier.testTag(AdminCampsiteTags.OPEN_PUBLIC)
            ) {
                Text(stringResource(R.string.admin_campsite_open_public))
            }
        }
        if (failed) {
            Text(
                stringResource(R.string.admin_campsite_remove_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag(AdminCampsiteTags.ERROR)
            )
        }
        AdminRemoveButton(
            label = stringResource(R.string.admin_campsite_remove),
            tag = AdminCampsiteTags.REMOVE_BUTTON,
            onClick = onRemove
        )
    }
}

/** Full-width rounded promo photo, or a plain dark tile when the campsite has none. */
@Composable
private fun Photo(url: String?) {
    val mod = Modifier
        .fillMaxWidth()
        .height(180.dp)
        .clip(RoundedCornerShape(12.dp))
    if (url != null) {
        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = mod)
    } else {
        Box(mod.background(MaterialTheme.colorScheme.surfaceContainer))
    }
}

/** Card with the Lotteries, Lottery entries and Rating rows. */
@Composable
private fun InfoCard(state: AdminCampsiteUiState.Loaded) {
    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        val divider = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        InfoRow(stringResource(R.string.admin_campsite_lotteries), state.lotteryCount.toString(), AdminCampsiteTags.LOTTERIES)
        HorizontalDivider(color = divider)
        InfoRow(stringResource(R.string.admin_campsite_entries), state.entryCount.toString(), AdminCampsiteTags.ENTRIES)
        HorizontalDivider(color = divider)
        InfoRow(stringResource(R.string.admin_campsite_rating), ratingLabel(state.campsite), AdminCampsiteTags.RATING)
    }
}

/**
 * "Ratings & reviews" heading with one card per rating, or "No ratings yet" (US 03.09.01).
 *
 * @param ratings the campsite's ratings, newest first.
 * @param removeState used to disable the Remove rating buttons and show the error.
 * @param onRemoveRating called with the reviewer's device id when Remove rating is tapped.
 * @param onRemoveReview called with the reviewer's device id when Remove review is tapped.
 */
@Composable
private fun RatingsSection(
    ratings: List<AdminRatingItem>,
    removeState: AdminRatingRemoveState,
    onRemoveRating: (String) -> Unit,
    onRemoveReview: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .testTag(AdminCampsiteTags.RATINGS),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.admin_campsite_ratings_heading), style = MaterialTheme.typography.titleMedium)
        if (removeState.failed) {
            Text(
                stringResource(
                    if (removeState.reviewOnly) R.string.admin_campsite_remove_review_error
                    else R.string.admin_campsite_remove_rating_error
                ),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag(AdminCampsiteTags.RATING_ERROR)
            )
        }
        if (ratings.isEmpty()) {
            Text(
                stringResource(R.string.admin_campsite_ratings_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(AdminCampsiteTags.RATINGS_EMPTY)
            )
        }
        ratings.forEach { item ->
            RatingCard(
                item,
                enabled = !removeState.removing,
                onRemove = { onRemoveRating(item.rating.userId) },
                onRemoveReview = { onRemoveReview(item.rating.userId) }
            )
        }
    }
}

/**
 * One rating: reviewer and date, stars, the review if there is one, and small muted red
 * "Remove review" (only when there is review text, US 03.10.01) and "Remove rating" buttons.
 * A review an admin already removed shows a muted "Review removed ..." line instead of the text.
 * Same card style as the info card.
 *
 * @param item the rating to show.
 * @param enabled false to grey out the Remove buttons while a removal runs.
 * @param onRemove called when Remove rating is tapped.
 * @param onRemoveReview called when Remove review is tapped.
 */
@Composable
private fun RatingCard(
    item: AdminRatingItem,
    enabled: Boolean,
    onRemove: () -> Unit,
    onRemoveReview: () -> Unit
) {
    val userId = item.rating.userId
    OutlinedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AdminCampsiteTags.rating(userId))
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.reviewerName, style = MaterialTheme.typography.titleSmall)
                Text(
                    item.dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            val starsDesc = stringResource(R.string.rating_stars_desc, item.rating.score)
            Row(Modifier.clearAndSetSemantics { contentDescription = starsDesc }) {
                for (n in 1..5) StarIcon(filled = n <= item.rating.score, size = 16.dp)
            }
            val hasReview = !item.rating.reviewRemoved && item.rating.review.isNotBlank()
            if (item.rating.reviewRemoved) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.admin_campsite_review_removed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag(AdminCampsiteTags.reviewRemoved(userId))
                )
            } else if (hasReview) {
                Spacer(Modifier.height(8.dp))
                Text(
                    item.rating.review,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Row(Modifier.align(Alignment.End)) {
                if (hasReview) {
                    TextButton(
                        onClick = onRemoveReview,
                        enabled = enabled,
                        colors = ButtonDefaults.textButtonColors(contentColor = Ember),
                        modifier = Modifier.testTag(AdminCampsiteTags.removeReview(userId))
                    ) {
                        Text(stringResource(R.string.admin_campsite_remove_review), style = MaterialTheme.typography.labelMedium)
                    }
                }
                TextButton(
                    onClick = onRemove,
                    enabled = enabled,
                    colors = ButtonDefaults.textButtonColors(contentColor = Ember),
                    modifier = Modifier.testTag(AdminCampsiteTags.removeRating(userId))
                ) {
                    Text(stringResource(R.string.admin_campsite_remove_rating), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/** One label/value row inside the info card; [tag] is on the value. */
@Composable
private fun InfoRow(label: String, value: String, tag: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag(tag))
    }
}

/** Test tags used by UI tests to find elements on this screen. */
object AdminCampsiteTags {
    const val BACK = "admin_campsite_back"
    const val LOADING = "admin_campsite_loading"
    const val NOT_FOUND = "admin_campsite_not_found"
    const val NAME = "admin_campsite_name"
    const val LOTTERIES = "admin_campsite_lotteries"
    const val ENTRIES = "admin_campsite_entries"
    const val RATING = "admin_campsite_rating"
    const val OPEN_PUBLIC = "admin_campsite_open_public"
    const val REMOVE_BUTTON = "admin_campsite_remove"
    const val ERROR = "admin_campsite_error"
    const val DIALOG = "admin_campsite_dialog"
    const val DIALOG_MESSAGE = "admin_campsite_dialog_message"
    const val DIALOG_CANCEL = "admin_campsite_dialog_cancel"
    const val DIALOG_REMOVE = "admin_campsite_dialog_remove"
    const val RATINGS = "admin_campsite_ratings"
    const val RATINGS_EMPTY = "admin_campsite_ratings_empty"
    const val RATING_ERROR = "admin_campsite_rating_error"
    const val RATING_DIALOG = "admin_campsite_rating_dialog"
    const val RATING_DIALOG_MESSAGE = "admin_campsite_rating_dialog_message"
    const val RATING_DIALOG_CANCEL = "admin_campsite_rating_dialog_cancel"
    const val RATING_DIALOG_REMOVE = "admin_campsite_rating_dialog_remove"
    const val REVIEW_DIALOG = "admin_campsite_review_dialog"
    const val REVIEW_DIALOG_MESSAGE = "admin_campsite_review_dialog_message"
    const val REVIEW_DIALOG_CANCEL = "admin_campsite_review_dialog_cancel"
    const val REVIEW_DIALOG_REMOVE = "admin_campsite_review_dialog_remove"

    /** Tag for the card of the rating written by [userId]. */
    fun rating(userId: String) = "admin_campsite_rating_card_$userId"

    /** Tag for the Remove rating button on the card of the rating written by [userId]. */
    fun removeRating(userId: String) = "admin_campsite_remove_rating_$userId"

    /** Tag for the Remove review button on the card of the rating written by [userId]. */
    fun removeReview(userId: String) = "admin_campsite_remove_review_$userId"

    /** Tag for the "Review removed ..." line on the card of the rating written by [userId]. */
    fun reviewRemoved(userId: String) = "admin_campsite_review_removed_$userId"

    /** The Remove campsite dialog's tags, bundled for [AdminRemoveDialog]. */
    internal val dialogTags = AdminRemoveDialogTags(DIALOG, DIALOG_MESSAGE, DIALOG_CANCEL, DIALOG_REMOVE)

    /** The Remove rating dialog's tags, bundled for [AdminRemoveDialog]. */
    internal val ratingDialogTags = AdminRemoveDialogTags(
        RATING_DIALOG, RATING_DIALOG_MESSAGE, RATING_DIALOG_CANCEL, RATING_DIALOG_REMOVE
    )

    /** The Remove review dialog's tags, bundled for [AdminRemoveDialog]. */
    internal val reviewDialogTags = AdminRemoveDialogTags(
        REVIEW_DIALOG, REVIEW_DIALOG_MESSAGE, REVIEW_DIALOG_CANCEL, REVIEW_DIALOG_REMOVE
    )
}

/** Design-time preview of the sample Banff campsite, for comparing against Scenario 8. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AdminCampsitePreview() {
    TrailsTheme {
        AdminCampsiteContent(
            state = buildAdminCampsiteState(
                TestCampsites.all.first { it.id == TestCampsites.BANFF_ID },
                TestUsers.all,
                CampsiteImpact("Tunnel Mountain Site 14", 1, 2),
                buildAdminRatingItems(
                    TestRatings.all.filter { it.campsiteId == TestCampsites.BANFF_ID },
                    TestUsers.all
                )
            ),
            removeState = AdminCampsiteRemoveState(),
            onBack = {}, onOpenPublicPage = {}, onConfirmRemove = {}
        )
    }
}

/** Design-time preview of a removal that failed. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AdminCampsiteFailedPreview() {
    TrailsTheme {
        AdminCampsiteContent(
            state = buildAdminCampsiteState(
                TestCampsites.all.first { it.id == TestCampsites.HINTON_ID },
                TestUsers.all,
                null
            ),
            removeState = AdminCampsiteRemoveState(failed = true),
            onBack = {}, onOpenPublicPage = {}, onConfirmRemove = {}
        )
    }
}

/** Design-time preview of the "Remove rating?" dialog open over the Banff campsite's ratings. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AdminCampsiteRemoveRatingPreview() {
    TrailsTheme {
        AdminCampsiteContent(
            state = buildAdminCampsiteState(
                TestCampsites.all.first { it.id == TestCampsites.BANFF_ID },
                TestUsers.all,
                CampsiteImpact("Tunnel Mountain Site 14", 1, 2),
                buildAdminRatingItems(
                    TestRatings.all.filter { it.campsiteId == TestCampsites.BANFF_ID },
                    TestUsers.all
                )
            ),
            removeState = AdminCampsiteRemoveState(),
            onBack = {}, onOpenPublicPage = {}, onConfirmRemove = {},
            ratingRemoveState = AdminRatingRemoveState(confirmingUserId = TestUsers.ENTRANT_ID)
        )
    }
}

/** Design-time preview of the "Remove review?" dialog open over the Banff campsite's ratings. */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AdminCampsiteRemoveReviewPreview() {
    TrailsTheme {
        AdminCampsiteContent(
            state = buildAdminCampsiteState(
                TestCampsites.all.first { it.id == TestCampsites.BANFF_ID },
                TestUsers.all,
                CampsiteImpact("Tunnel Mountain Site 14", 1, 2),
                buildAdminRatingItems(
                    TestRatings.all.filter { it.campsiteId == TestCampsites.BANFF_ID },
                    TestUsers.all
                )
            ),
            removeState = AdminCampsiteRemoveState(),
            onBack = {}, onOpenPublicPage = {}, onConfirmRemove = {},
            ratingRemoveState = AdminRatingRemoveState(confirmingUserId = TestUsers.ENTRANT_ID, reviewOnly = true)
        )
    }
}
