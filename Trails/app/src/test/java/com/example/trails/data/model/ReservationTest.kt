// Unit tests for Reservation data model and validation.
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
package com.example.trails.data.model

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

class ReservationTest {

    private lateinit var validReservation: Reservation

    @Before
    fun setUp() {
        val now = Timestamp(Date())
        validReservation = Reservation(
            id = "res-123",
            userId = "user-device-01",
            campsiteId = "campsite-two-jack",
            lotteryId = "lottery-rockies-2027",
            partySize = 2,
            startDate = now,
            endDate = now,
            totalPrice = 130.0,
            rulesAccepted = true,
            rulesAcceptedAt = now,
            createdAt = now
        )
    }

    @Test
    fun validate_validReservation_noErrors() {
        assertTrue(validReservation.validate().isEmpty())
    }

    @Test
    fun validate_blankUserId_userIdError() {
        val errors = validReservation.copy(userId = "").validate()
        assertEquals(setOf("userId"), errors.keys)
    }

    @Test
    fun validate_blankCampsiteId_campsiteIdError() {
        val errors = validReservation.copy(campsiteId = "  ").validate()
        assertEquals(setOf("campsiteId"), errors.keys)
    }

    @Test
    fun validate_zeroPartySize_partySizeError() {
        val errors = validReservation.copy(partySize = 0).validate()
        assertEquals(setOf("partySize"), errors.keys)
    }

    @Test
    fun validate_negativeTotalPrice_totalPriceError() {
        val errors = validReservation.copy(totalPrice = -1.0).validate()
        assertEquals(setOf("totalPrice"), errors.keys)
    }

    @Test
    fun validate_rulesNotAccepted_rulesError() {
        val errors = validReservation.copy(rulesAccepted = false, rulesAcceptedAt = null).validate()
        assertEquals(setOf("rules"), errors.keys)
    }
}
