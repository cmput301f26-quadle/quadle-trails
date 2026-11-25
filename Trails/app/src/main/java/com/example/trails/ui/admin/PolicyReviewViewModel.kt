// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.07.01
// PURPOSE: Controller for the admin's Policy review page: loads one reported owner and runs
//   Remove owner and Dismiss reports.
// Outstanding issues: Firestore listener errors are not surfaced to the screen yet.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.ReportRepository
import com.example.trails.data.repository.UserRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Controller for [PolicyReviewScreen]: watches one owner by id, their reports and every
 * campsite, and runs the Remove owner / Dismiss reports flow.
 *
 * @param userId device id of the reported owner.
 * @param userRepository where user profiles are stored.
 * @param reportRepository where policy reports are stored.
 * @param campsiteRepository where campsites are stored, used to count and name the owner's campsites.
 * @param currentUserId device id of the signed-in admin; they can't remove themselves.
 * @param removal removes the owner or dismisses their reports.
 * @param now the current time; the 30-day window is measured back from it.
 */
class PolicyReviewViewModel(
    private val userId: String,
    userRepository: UserRepository = AppContainer.userRepository,
    reportRepository: ReportRepository = AppContainer.reportRepository,
    campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    private val currentUserId: String = CurrentUser.id(),
    private val removal: AdminRemoval = AdminRemoval(),
    private val now: () -> Timestamp = { Timestamp.now() }
) : ViewModel() {

    private val action = MutableStateFlow<PolicyActionState>(PolicyActionState.Idle)
    private val doneEvents = Channel<Unit>(Channel.BUFFERED)

    /** What removing the owner takes with them; null until counted. */
    private val impact: Flow<ProfileImpact?> = flow {
        emit(null)
        emit(removal.profileImpact(userId))
    }

    /** The page state; starts as Loading. */
    val state: StateFlow<PolicyReviewUiState> = combine(
        userRepository.observe(userId),
        reportRepository.observeForOwner(userId),
        campsiteRepository.observeAll(),
        impact,
        action
    ) { user, reports, campsites, impact, action ->
        buildPolicyReviewState(user, reports, campsites, impact, currentUserId, now(), action)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PolicyReviewUiState.Loading)

    /** Emits once each time the owner is removed or their reports dismissed, so the screen can go back. */
    val done: Flow<Unit> = doneEvents.receiveAsFlow()

    /** Opens the "Remove owner?" dialog; ignored while an action is running. */
    fun onRemoveClick() {
        if (action.value != PolicyActionState.Working) action.value = PolicyActionState.Confirming
    }

    /** Closes the dialog after Cancel or a tap outside; ignored while an action is running. */
    fun onDismissDialog() {
        if (action.value != PolicyActionState.Working) action.value = PolicyActionState.Idle
    }

    /** Removes the owner after the admin confirmed in the dialog. */
    fun onConfirmRemove() = perform { removal.removeOwner(userId, currentUserId) }

    /** Dismisses every open report against the owner. */
    fun onDismissReports() = perform { removal.dismissReports(userId) }

    /** Runs [block] unless an action is already running, then goes back on success or shows the error. */
    private fun perform(block: suspend () -> Result<Unit>) {
        if (action.value == PolicyActionState.Working) return
        action.value = PolicyActionState.Working
        viewModelScope.launch {
            if (block().isSuccess) {
                action.value = PolicyActionState.Idle
                doneEvents.send(Unit)
            } else {
                action.value = PolicyActionState.Error
            }
        }
    }
}
