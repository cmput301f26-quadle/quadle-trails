// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.01.01, 03.09.01
// PURPOSE: Controller for the admin's Campsite page: loads one campsite with its
//   ratings, and runs the removal of the campsite or of a single rating.
// Outstanding issues:
//   - The entry count refreshes when the campsite, users or lotteries change,
//     not when someone joins or leaves a waitlist.
//   - Firestore listener errors are not surfaced to the screen yet.
//   - Ratings come from FakeRatingRepository until AppContainer is switched to
//     FirestoreRatingRepository (once ratings are seeded).
package com.example.trails.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.LotteryRepository
import com.example.trails.data.repository.RatingRepository
import com.example.trails.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Controller for [AdminCampsiteScreen]: watches one campsite by id, every user (for the owner's
 * and reviewers' names), every lottery (for the removal counts) and the campsite's ratings, and
 * removes the campsite or one rating on request.
 *
 * @param campsiteId id of the campsite to show.
 * @param campsiteRepository where campsites are stored.
 * @param userRepository where user profiles are stored.
 * @param lotteryRepository where lotteries are stored; only watched so counts stay current.
 * @param ratingRepository where the campsite's ratings are stored.
 * @param removal does the counting and the removal itself.
 */
class AdminCampsiteViewModel(
    private val campsiteId: String,
    campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    userRepository: UserRepository = AppContainer.userRepository,
    lotteryRepository: LotteryRepository = AppContainer.lotteryRepository,
    ratingRepository: RatingRepository = AppContainer.ratingRepository,
    private val removal: AdminRemoval = AdminRemoval()
) : ViewModel() {

    /** The page state; starts as Loading. */
    val state: StateFlow<AdminCampsiteUiState> = combine(
        campsiteRepository.observeById(campsiteId),
        userRepository.observeAll(),
        lotteryRepository.observeAll(),
        ratingRepository.observeForCampsite(campsiteId)
    ) { campsite, users, _, ratings ->
        buildAdminCampsiteState(
            campsite,
            users,
            campsite?.let { removal.campsiteImpact(it.id) },
            buildAdminRatingItems(ratings, users)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminCampsiteUiState.Loading)

    private val _removeState = MutableStateFlow(AdminCampsiteRemoveState())

    /** Progress of a confirmed removal. */
    val removeState: StateFlow<AdminCampsiteRemoveState> = _removeState.asStateFlow()

    /** Removes the campsite after the admin confirmed; ignored while a removal is running. */
    fun onConfirmRemove() {
        if (_removeState.value.removing) return
        _removeState.value = AdminCampsiteRemoveState(removing = true)
        viewModelScope.launch {
            val result = removal.removeCampsite(campsiteId)
            _removeState.update {
                AdminCampsiteRemoveState(failed = result.isFailure, removed = result.isSuccess)
            }
        }
    }

    private val _ratingRemoveState = MutableStateFlow(AdminRatingRemoveState())

    /** Which rating the "Remove rating?" dialog is about, and the progress of removing it. */
    val ratingRemoveState: StateFlow<AdminRatingRemoveState> = _ratingRemoveState.asStateFlow()

    /**
     * Opens the "Remove rating?" dialog for one reviewer's rating; ignored while a removal runs.
     *
     * @param userId device id of the reviewer whose rating the admin wants to remove.
     */
    fun onRemoveRatingClick(userId: String) {
        if (_ratingRemoveState.value.removing) return
        _ratingRemoveState.value = AdminRatingRemoveState(confirmingUserId = userId)
    }

    /** Closes the "Remove rating?" dialog without removing anything; ignored while a removal runs. */
    fun onDismiss() {
        _ratingRemoveState.update { if (it.removing) it else it.copy(confirmingUserId = null) }
    }

    /**
     * Removes the rating the dialog is about after the admin confirmed. The dialog closes when
     * it finishes; on failure an error message is shown. Ignored if no rating is being confirmed
     * or a removal is already running.
     */
    fun onConfirmRemoveRating() {
        val current = _ratingRemoveState.value
        val userId = current.confirmingUserId ?: return
        if (current.removing) return
        _ratingRemoveState.value = current.copy(removing = true, failed = false)
        viewModelScope.launch {
            val result = removal.removeRating(campsiteId, userId)
            _ratingRemoveState.value = AdminRatingRemoveState(failed = result.isFailure)
        }
    }
}
