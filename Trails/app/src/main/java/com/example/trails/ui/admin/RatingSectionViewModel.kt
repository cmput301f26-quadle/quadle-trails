// Controller for the rating section on a campsite's details page (US 01.13.01).
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// Outstanding issues: the error shown on the sheet is a general "try again" message;
//   the specific reason is kept in the state but not shown.
package com.example.trails.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.Rating
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.RatingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Everything the rating section and its bottom sheet need to draw themselves.
 *
 * @property canRate false while loading, for the campsite's owner, or if the campsite is gone.
 * @property myRating the current user's saved rating of this campsite, or null if they haven't rated it.
 * @property isSheetOpen whether the "Rate campsite" bottom sheet is showing.
 * @property draftScore stars picked in the sheet, 0 until one is tapped.
 * @property draftReview review text typed in the sheet.
 * @property isSubmitting true while a rating is being saved.
 * @property justSubmitted true after a successful save, until the sheet is opened again.
 * @property error why the last submit failed, or null if it didn't.
 */
data class RatingSectionUiState(
    val canRate: Boolean = false,
    val myRating: Rating? = null,
    val isSheetOpen: Boolean = false,
    val draftScore: Int = 0,
    val draftReview: String = "",
    val isSubmitting: Boolean = false,
    val justSubmitted: Boolean = false,
    val error: String? = null
)

/**
 * Controller for [RatingSection]. Watches the campsite and its ratings, so the section
 * updates by itself when the user's rating is saved or changed elsewhere, and holds the
 * draft being edited in the bottom sheet.
 *
 * @param campsiteId id of the campsite being rated.
 * @param userId device id of the person using the app.
 * @param ratingRepository where ratings are stored.
 * @param campsiteRepository where campsites are stored, used to check who owns this one.
 * @param submitter saves the rating and updates the campsite's average and count.
 */
class RatingSectionViewModel(
    private val campsiteId: String,
    private val userId: String = CurrentUser.id(),
    ratingRepository: RatingRepository = AppContainer.ratingRepository,
    campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    private val submitter: RatingSubmitter = RatingSubmitter(ratingRepository, campsiteRepository)
) : ViewModel() {

    /** The sheet and draft fields; canRate and myRating are filled in from the repositories. */
    private val local = MutableStateFlow(RatingSectionUiState())

    /** The section state; canRate stays false until the campsite has loaded. */
    val state: StateFlow<RatingSectionUiState> = combine(
        campsiteRepository.observeById(campsiteId),
        ratingRepository.observeForCampsite(campsiteId),
        local
    ) { campsite, ratings, ui ->
        ui.copy(
            canRate = campsite != null && canRate(userId, campsite),
            myRating = ratings.find { it.userId == userId }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RatingSectionUiState())

    /**
     * Opens the sheet. When editing, the draft starts from the user's saved rating.
     */
    fun onOpenSheet() {
        val mine = state.value.myRating
        local.update {
            it.copy(
                isSheetOpen = true,
                draftScore = mine?.score ?: 0,
                draftReview = mine?.review ?: "",
                justSubmitted = false,
                error = null
            )
        }
    }

    /** Closes the sheet without saving; the draft is thrown away. */
    fun onDismissSheet() {
        local.update { it.copy(isSheetOpen = false, error = null) }
    }

    /**
     * Picks a star score in the sheet.
     *
     * @param score 1 to 5; anything else is ignored.
     */
    fun onStarClick(score: Int) {
        if (isValidScore(score)) local.update { it.copy(draftScore = score, error = null) }
    }

    /**
     * Updates the review text in the sheet.
     *
     * @param review the typed text; anything past [MAX_REVIEW_LENGTH] characters is dropped.
     */
    fun onReviewChange(review: String) {
        local.update { it.copy(draftReview = review.take(MAX_REVIEW_LENGTH)) }
    }

    /**
     * Saves the draft. On success the sheet closes and the thank-you message shows;
     * on failure the sheet stays open with an error. Does nothing without a star score
     * or while a save is already running.
     */
    fun onSubmit() {
        val current = local.value
        if (!isValidScore(current.draftScore) || current.isSubmitting) return
        local.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = runCatching {
                submitter.submit(campsiteId, userId, current.draftScore, current.draftReview).getOrThrow()
            }
            local.update {
                result.fold(
                    onSuccess = { _ -> it.copy(isSubmitting = false, isSheetOpen = false, justSubmitted = true) },
                    onFailure = { e -> it.copy(isSubmitting = false, error = e.message ?: "Couldn't save rating") }
                )
            }
        }
    }
}
