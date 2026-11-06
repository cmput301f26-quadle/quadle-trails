// OWNER: Neaz (NeazMZ06) — Search & Discovery
// USER STORIES: 01.01.05
// PURPOSE: Page states for the entrant campsite details screen. Plain Kotlin so it can be unit-tested.
// Outstanding issues: none.
package com.example.trails.ui.discovery

import com.example.trails.data.model.Campsite

/**
 * Every situation the entrant campsite screen can be in.
 */
sealed interface CampsiteDetailsUiState {
    /** Waiting for the campsite to load. */
    data object Loading : CampsiteDetailsUiState

    /** No campsite has that id. */
    data object NotFound : CampsiteDetailsUiState

    /**
     * The campsite finished loading.
     *
     * @property campsite the campsite to display.
     */
    data class Loaded(val campsite: Campsite) : CampsiteDetailsUiState
}

/**
 * Picks the page state from the latest campsite.
 *
 * A missing campsite becomes [CampsiteDetailsUiState.NotFound]. Anything else is loaded.
 *
 * @param campsite the campsite, or null when that id does not exist.
 * @return the state the details screen should show.
 */
fun buildCampsiteDetailsState(campsite: Campsite?): CampsiteDetailsUiState =
    if (campsite == null) CampsiteDetailsUiState.NotFound
    else CampsiteDetailsUiState.Loaded(campsite)
