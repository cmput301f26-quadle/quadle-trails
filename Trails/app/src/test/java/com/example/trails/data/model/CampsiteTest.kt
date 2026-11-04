// Unit tests for Campsite.validate().
// OWNER: Ibrahim (ibr2309) — Campsite Management
package com.example.trails.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CampsiteTest {

    private lateinit var campsite: Campsite

    @Before
    fun setUp() {
        campsite = Campsite(
            ownerId = "owner-device-01",
            name = "Wildhorse Lake Campground",
            description = "Outdoor cookhouse and on-site tennis court",
            locationName = "Hinton-Grande Cache, AB",
            latitude = 53.4,
            longitude = -117.6,
            maxOccupancy = 8,
            pricePerNight = 65.0,
            rules = "Quiet hours 10pm-7am. No generators.",
            cancellationPolicy = "Free cancellation up to 7 days before arrival."
        )
    }

    @Test
    fun validate_validCampsite_noErrors() {
        assertTrue(campsite.validate().isEmpty())
    }

    @Test
    fun validate_blankName_nameError() {
        assertEquals(setOf("name"), campsite.copy(name = "   ").validate().keys)
    }

    @Test
    fun validate_blankLocation_locationNameError() {
        assertEquals(setOf("locationName"), campsite.copy(locationName = "").validate().keys)
    }

    @Test
    fun validate_blankDescription_descriptionError() {
        assertEquals(setOf("description"), campsite.copy(description = "").validate().keys)
    }

    @Test
    fun validate_blankRules_rulesError() {
        assertEquals(setOf("rules"), campsite.copy(rules = " ").validate().keys)
    }

    @Test
    fun validate_blankCancellationPolicy_cancellationPolicyError() {
        assertEquals(setOf("cancellationPolicy"), campsite.copy(cancellationPolicy = "").validate().keys)
    }

    @Test
    fun validate_zeroOccupancy_maxOccupancyError() {
        assertEquals(setOf("maxOccupancy"), campsite.copy(maxOccupancy = 0).validate().keys)
    }

    @Test
    fun validate_oneOccupant_noErrors() {
        assertTrue(campsite.copy(maxOccupancy = 1).validate().isEmpty())
    }

    @Test
    fun validate_negativePrice_priceError() {
        assertEquals(setOf("pricePerNight"), campsite.copy(pricePerNight = -0.01).validate().keys)
    }

    @Test
    fun validate_freeSite_noErrors() {
        assertTrue(campsite.copy(pricePerNight = 0.0).validate().isEmpty())
    }

    @Test
    fun validate_latitudeOutOfRange_locationError() {
        assertEquals(setOf("location"), campsite.copy(latitude = 90.5).validate().keys)
    }

    @Test
    fun validate_multipleProblems_allReported() {
        val errors = campsite.copy(name = "", maxOccupancy = 0).validate()
        assertEquals(setOf("name", "maxOccupancy"), errors.keys)
    }
}
