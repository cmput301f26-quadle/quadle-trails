// OWNER: Neaz (NeazMZ06) — Search & Discovery
// USER STORIES: 01.01.05
// Plain Kotlin so it can be unit-tested.
package com.example.trails.ui.discovery

import com.example.trails.data.model.Campsite

/** Every situation the entrant campsite screen can be in. */
sealed interface CampsiteDetailsUiState {
    /** Waiting for the campsite to load. */
    data object Loading : CampsiteDetailsUiState

    /** No campsite has that id. */
    data object NotFound : CampsiteDetailsUiState

    /** The campsite is loaded. */
    data class Loaded(val campsite: Campsite) : CampsiteDetailsUiState
}

/**
 * Picks the page state from the latest campsite.
 *
 * @param campsite the campsite, or null if it does not exist.
 */
fun buildCampsiteDetailsState(campsite: Campsite?): CampsiteDetailsUiState =
    if (campsite == null) CampsiteDetailsUiState.NotFound
    else CampsiteDetailsUiState.Loaded(campsite)
