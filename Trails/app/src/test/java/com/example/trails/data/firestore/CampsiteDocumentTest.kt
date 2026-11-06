// Unit tests for converting campsites to and from Firestore documents.
// OWNER: Ibrahim (ibr2309) — Campsite Management
package com.example.trails.data.firestore

import com.example.trails.data.fake.dateOf
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.DateRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CampsiteDocumentTest {

    private lateinit var hinton: Campsite

    @Before
    fun setUp() {
        hinton = Campsite(
            id = "campsite-hinton",
            ownerId = "device-owner-001",
            name = "Grande Cache Ridge",
            description = "Ridge-top site with an outdoor cookhouse and tennis court.",
            locationName = "Hinton - Grande Cache, AB",
            latitude = 53.89,
            longitude = -118.01,
            maxOccupancy = 8,
            pricePerNight = 65.0,
            tags = listOf("Cookhouse", "Tennis"),
            rules = "Quiet hours 10pm-7am.",
            cancellationPolicy = "Free cancellation up to 7 days before arrival.",
            imageUrl = "https://example.com/ridge.jpg",
            availability = listOf(DateRange(dateOf(2027, 5, 1), dateOf(2027, 5, 2))),
            ratingAverage = 4.6,
            ratingCount = 23,
            activeLotteryId = "lottery-may"
        )
    }

    @Test
    fun roundTrip_keepsEveryField() {
        val back = campsiteFromDocument(hinton.id, campsiteToDocument(hinton))
        assertEquals(hinton, back)
    }

    @Test
    fun toDocument_doesNotStoreId() {
        assertFalse(campsiteToDocument(hinton).containsKey("id"))
    }

    @Test
    fun fromDocument_idComesFromDocumentId() {
        assertEquals("abc123", campsiteFromDocument("abc123", campsiteToDocument(hinton)).id)
    }

    @Test
    fun fromDocument_wholeNumbersAsLong_converted() {
        val data = campsiteToDocument(hinton) + mapOf("maxOccupancy" to 8L, "pricePerNight" to 65L, "ratingCount" to 23L)
        val c = campsiteFromDocument(hinton.id, data)
        assertEquals(8, c.maxOccupancy)
        assertEquals(65.0, c.pricePerNight, 0.0)
        assertEquals(23, c.ratingCount)
    }

    @Test
    fun fromDocument_emptyDocument_defaults() {
        val c = campsiteFromDocument("empty", emptyMap())
        assertEquals(Campsite(id = "empty"), c)
    }

    @Test
    fun fromDocument_wrongTypes_fallBackToDefaults() {
        val c = campsiteFromDocument("bad", mapOf("name" to 42, "maxOccupancy" to "eight", "tags" to "Cookhouse"))
        assertEquals("", c.name)
        assertEquals(1, c.maxOccupancy)
        assertEquals(emptyList<String>(), c.tags)
    }

    @Test
    fun fromDocument_nullOptionalFields_staysNull() {
        val c = campsiteFromDocument(hinton.id, campsiteToDocument(hinton.copy(imageUrl = null, activeLotteryId = null)))
        assertNull(c.imageUrl)
        assertNull(c.activeLotteryId)
    }
}
