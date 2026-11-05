// Unit tests for the entrant campsite page state (US 01.01.05).
// OWNER: Neaz (NeazMZ06) — Search & Discovery
package com.example.trails.ui.discovery

import com.example.trails.data.fake.TestCampsites
import org.junit.Assert.assertEquals
import org.junit.Test

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
