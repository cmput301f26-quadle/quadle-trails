// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.07.01
// PURPOSE: Controller for the admin's Policy reports list: every owner with open reports.
// Outstanding issues: Firestore listener errors are not surfaced to the screen yet.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.ReportRepository
import com.example.trails.data.repository.UserRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Controller for [ReportsScreen]: combines every report, user and campsite into the list of
 * reported owners, so the list updates by itself when a report is filed or closed.
 *
 * @param reportRepository where policy reports are stored.
 * @param userRepository where user profiles are stored, used to look up each reported owner.
 * @param campsiteRepository where campsites are stored, used to count each owner's campsites.
 * @param now the current time; the 30-day window is measured back from it.
 */
class ReportsViewModel(
    reportRepository: ReportRepository = AppContainer.reportRepository,
    userRepository: UserRepository = AppContainer.userRepository,
    campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    private val now: () -> Timestamp = { Timestamp.now() }
) : ViewModel() {

    /** The list state; starts as Loading. */
    val state: StateFlow<ReportsUiState> = combine(
        reportRepository.observeAll(),
        userRepository.observeAll(),
        campsiteRepository.observeAll()
    ) { reports, users, campsites ->
        ReportsUiState.Loaded(buildReportedOwners(reports, users, campsites, now()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState.Loading)
}
