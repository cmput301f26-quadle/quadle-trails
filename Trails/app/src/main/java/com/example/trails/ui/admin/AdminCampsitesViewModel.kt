// Controller for the admin's Campsites screen (US 03.04.01).
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// Outstanding issues: Firestore listener errors are not surfaced to the screen yet.
package com.example.trails.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Controller for [AdminCampsitesScreen]: combines every campsite and every user with the search text.
 *
 * @param campsiteRepository where campsites are stored.
 * @param userRepository where user profiles are stored, used for each campsite's owner name.
 */
class AdminCampsitesViewModel(
    campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    userRepository: UserRepository = AppContainer.userRepository
) : ViewModel() {

    private val query = MutableStateFlow("")

    /** The screen state; null while the first load is in progress. */
    val state: StateFlow<AdminCampsitesUiState?> = combine(
        campsiteRepository.observeAll(),
        userRepository.observeAll(),
        query,
        ::buildAdminCampsitesState
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Updates the search text; the list re-filters immediately. */
    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }
}
