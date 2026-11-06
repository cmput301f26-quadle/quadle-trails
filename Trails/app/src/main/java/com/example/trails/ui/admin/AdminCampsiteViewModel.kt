// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.01.01
// PURPOSE: Controller for the admin's Campsite page: loads one campsite and runs its removal.
// Outstanding issues:
//   - The entry count refreshes when the campsite, users or lotteries change,
//     not when someone joins or leaves a waitlist.
//   - Firestore listener errors are not surfaced to the screen yet.
package com.example.trails.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.LotteryRepository
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
 * name) and every lottery (for the removal counts), and removes the campsite on request.
 *
 * @param campsiteId id of the campsite to show.
 * @param campsiteRepository where campsites are stored.
 * @param userRepository where user profiles are stored.
 * @param lotteryRepository where lotteries are stored; only watched so counts stay current.
 * @param removal does the counting and the removal itself.
 */
class AdminCampsiteViewModel(
    private val campsiteId: String,
    campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    userRepository: UserRepository = AppContainer.userRepository,
    lotteryRepository: LotteryRepository = AppContainer.lotteryRepository,
    private val removal: AdminRemoval = AdminRemoval()
) : ViewModel() {

    /** The page state; starts as Loading. */
    val state: StateFlow<AdminCampsiteUiState> = combine(
        campsiteRepository.observeById(campsiteId),
        userRepository.observeAll(),
        lotteryRepository.observeAll()
    ) { campsite, users, _ ->
        buildAdminCampsiteState(campsite, users, campsite?.let { removal.campsiteImpact(it.id) })
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
}
