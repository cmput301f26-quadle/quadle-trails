// Unit tests for Reservation model, validation rules, and status lifecycle (Model in MVVM).
// implements US 01.04.01, US 01.04.02, US 01.04.03
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.data.model

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Unit tests verifying [Reservation] fields, validation logic, and status lifecycle.
 */
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
            createdAt = now,
            status = ReservationStatus.ACTIVE
        )
    }

    @Test
    fun validate_validReservation_noErrors() {
        assertTrue(validReservation.validate().isEmpty())
    }

    @Test
    fun defaultStatus_isActive() {
        val res = Reservation()
        assertEquals(ReservationStatus.ACTIVE, res.status)
    }

    @Test
    fun statusTransition_cancelledAndRejected() {
        val cancelled = validReservation.copy(status = ReservationStatus.CANCELLED)
        assertEquals(ReservationStatus.CANCELLED, cancelled.status)

        val rejected = validReservation.copy(status = ReservationStatus.REJECTED)
        assertEquals(ReservationStatus.REJECTED, rejected.status)
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
