// Unit tests for the map tile URL (US 02.01.02).
// OWNER: Ibrahim (ibr2309) — Campsite Management
package com.example.trails.ui.campsite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampsiteMapTest {

    @Test
    fun mapTilerTileUrl_buildsZoomXYAndKey() {
        assertEquals(
            "https://api.maptiler.com/maps/streets-v2-dark/256/6/11/21.png?key=abc123",
            mapTilerTileUrl("abc123", zoom = 6, x = 11, y = 21)
        )
    }

    @Test
    fun mapTilerTileUrl_worldTile() {
        assertEquals(
            "https://api.maptiler.com/maps/streets-v2-dark/256/0/0/0.png?key=abc123",
            mapTilerTileUrl("abc123", zoom = 0, x = 0, y = 0)
        )
    }

    @Test
    fun mapTilerTileUrl_usesHttps() {
        assertTrue(mapTilerTileUrl("abc123", 13, 1458, 2600).startsWith("https://"))
    }
}
