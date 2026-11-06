// OWNER: Neaz (NeazMZ06) — Search & Discovery
// USER STORIES: 01.01.05
// PURPOSE: Unit tests for the entrant campsite page state.
// Outstanding issues: none.
package com.example.trails.ui.discovery

import com.example.trails.data.fake.TestCampsites
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Checks that [buildCampsiteDetailsState] reports a missing campsite and a loaded one.
 */
class CampsiteDetailsUiStateTest {

    @Test
    fun nullCampsite_isNotFound() {
        assertEquals(CampsiteDetailsUiState.NotFound, buildCampsiteDetailsState(null))
    }

    @Test
    fun campsite_isLoaded() {
        val banff = TestCampsites.all.first { it.id == TestCampsites.BANFF_ID }
        assertEquals(CampsiteDetailsUiState.Loaded(banff), buildCampsiteDetailsState(banff))
    }
}
