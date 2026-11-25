// Controller for the admin's home screen (US 03.04.01, 03.05.01, 03.07.01).
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// Outstanding issues: the greeting's hour is read when the data changes, so it
//   doesn't switch from "morning" to "afternoon" while the screen sits open.
package com.example.trails.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.ReportRepository
import com.example.trails.data.repository.UserRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

/**
 * Controller for [AdminHomeScreen]: combines the signed-in admin, every campsite, every user and
 * every policy report.
 *
 * @param campsiteRepository where campsites are stored, counted for the Campsites tile.
 * @param userRepository where user profiles are stored, for the admin's name and the Profiles tile.
 * @param reportRepository where policy reports are stored, counted for the Needs review card.
 * @param currentUserId device id of the signed-in admin.
 * @param currentHour returns the current hour of the day, 0–23; replaceable for tests.
 * @param now returns the current time, for the report window and oldest report's age; replaceable for tests.
 */
class AdminHomeViewModel(
    campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    userRepository: UserRepository = AppContainer.userRepository,
    reportRepository: ReportRepository = AppContainer.reportRepository,
    currentUserId: String = CurrentUser.id(),
    currentHour: () -> Int = { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) },
    now: () -> Timestamp = { Timestamp.now() }
) : ViewModel() {

    /** The screen state; null while the first load is in progress. */
    val state: StateFlow<AdminHomeUiState?> = combine(
        userRepository.observe(currentUserId),
        campsiteRepository.observeAll(),
        userRepository.observeAll(),
        reportRepository.observeAll()
    ) { user, campsites, users, reports ->
        buildAdminHomeState(user, campsites, users, currentHour(), reports, now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
