// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.05.01, 03.02.01
// PURPOSE: Controller for the admin's Profile page: loads one profile and runs its removal.
// Outstanding issues: Firestore listener errors are not surfaced to the screen yet.
package com.example.trails.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.LotteryRepository
import com.example.trails.data.repository.UserRepository
import com.example.trails.data.repository.WaitlistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Controller for [AdminProfileScreen]: watches one user by id, every campsite (to count the
 * ones they own) and their entry in every lottery's waitlist, so the page updates by itself if
 * any of these change. Also runs the Remove profile flow.
 *
 * @param userId device id of the user to show.
 * @param userRepository where user profiles are stored.
 * @param campsiteRepository where campsites are stored, used to count the user's campsites.
 * @param lotteryRepository where lotteries are stored, used to find the user's waitlist entries.
 * @param waitlistRepository where each lottery's waitlist entries are stored.
 * @param currentUserId device id of the signed-in admin; their own profile can't be removed.
 * @param removal does the removal itself.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminProfileViewModel(
    private val userId: String,
    userRepository: UserRepository = AppContainer.userRepository,
    campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    lotteryRepository: LotteryRepository = AppContainer.lotteryRepository,
    waitlistRepository: WaitlistRepository = AppContainer.waitlistRepository,
    private val currentUserId: String = CurrentUser.id(),
    private val removal: AdminRemoval = AdminRemoval()
) : ViewModel() {

    private val removalState = MutableStateFlow<ProfileRemovalState>(ProfileRemovalState.Idle)
    private val removedEvents = Channel<Unit>(Channel.BUFFERED)

    /** The user's entry in each lottery, without the lotteries they haven't joined. */
    private val entries: Flow<List<WaitlistEntry>> = lotteryRepository.observeAll().flatMapLatest { lotteries ->
        if (lotteries.isEmpty()) flowOf(emptyList())
        else combine(lotteries.map { waitlistRepository.observeEntry(it.id, userId) }) { it.filterNotNull() }
    }

    /** The page state; starts as Loading. */
    val state: StateFlow<AdminProfileUiState> = combine(
        userRepository.observe(userId),
        campsiteRepository.observeAll(),
        entries,
        removalState
    ) { user, campsites, entries, removal ->
        buildAdminProfileState(user, campsites, entries, currentUserId, removal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminProfileUiState.Loading)

    /** Emits once each time the profile is removed, so the screen can go back to the list. */
    val removed: Flow<Unit> = removedEvents.receiveAsFlow()

    /** Opens the "Remove profile?" dialog; ignored while a removal is running. */
    fun onRemoveClick() {
        if (removalState.value != ProfileRemovalState.Removing) removalState.value = ProfileRemovalState.Confirming
    }

    /** Closes the dialog after Cancel or a tap outside; ignored while a removal is running. */
    fun onDismiss() {
        if (removalState.value != ProfileRemovalState.Removing) removalState.value = ProfileRemovalState.Idle
    }

    /** Removes the profile after the admin confirmed; ignored while a removal is running. */
    fun onConfirmRemove() {
        if (removalState.value == ProfileRemovalState.Removing) return
        removalState.value = ProfileRemovalState.Removing
        viewModelScope.launch {
            val result = removal.removeProfile(userId, currentUserId)
            if (result.isSuccess) {
                removalState.value = ProfileRemovalState.Idle
                removedEvents.send(Unit)
            } else {
                removalState.value = ProfileRemovalState.Error
            }
        }
    }
}
