// Controller for the admin's read-only Profile page (US 03.05.01).
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// Outstanding issues: Firestore listener errors are not surfaced to the screen yet.
package com.example.trails.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Controller for [AdminProfileScreen]: watches one user by id and every campsite, so the page
 * updates by itself if the profile changes or is removed.
 *
 * @param userId device id of the user to show.
 * @param userRepository where user profiles are stored.
 * @param campsiteRepository where campsites are stored, used to count the user's campsites.
 */
class AdminProfileViewModel(
    userId: String,
    userRepository: UserRepository = AppContainer.userRepository,
    campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository
) : ViewModel() {

    /** The page state; starts as Loading. */
    val state: StateFlow<AdminProfileUiState> = combine(
        userRepository.observe(userId),
        campsiteRepository.observeAll(),
        ::buildAdminProfileState
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminProfileUiState.Loading)
}
