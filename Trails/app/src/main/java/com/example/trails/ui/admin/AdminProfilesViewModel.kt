// Controller for the admin's Profiles screen (US 03.05.01).
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
 * Controller for [AdminProfilesScreen]: combines every user and every campsite with the search
 * text and the selected filter chip.
 *
 * @param userRepository where user profiles are stored.
 * @param campsiteRepository where campsites are stored, used to count each owner's campsites.
 */
class AdminProfilesViewModel(
    userRepository: UserRepository = AppContainer.userRepository,
    campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(ProfileFilter.ALL)

    /** The screen state; null while the first load is in progress. */
    val state: StateFlow<AdminProfilesUiState?> = combine(
        userRepository.observeAll(),
        campsiteRepository.observeAll(),
        query,
        filter,
        ::buildAdminProfilesState
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Updates the search text; the list re-filters immediately. */
    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    /** Selects a filter chip; the list re-filters immediately. */
    fun onFilterChange(newFilter: ProfileFilter) {
        filter.value = newFilter
    }
}
