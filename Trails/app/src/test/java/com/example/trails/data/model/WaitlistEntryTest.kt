// Unit test suite for WaitlistEntry domain model validation and defaults (Model in MVVM).
// implements US 01.02.01, US 01.02.02
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
package com.example.trails.data.model

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Unit test suite for [WaitlistEntry] data model validation rules, default values, and status transitions.
 */
class WaitlistEntryTest {

    private lateinit var validEntry: WaitlistEntry

    @Before
    fun setUp() {
        val now = Timestamp(Date())
        validEntry = WaitlistEntry(
            userId = "user-device-01",
            userName = "Chitkarsh Rathi",
            lotteryId = "lottery-rockies-2027",
            partySize = 4,
            joinedAt = now,
            status = WaitlistStatus.WAITING,
            rulesAcceptedAt = now,
            rulesAccepted = true
        )
    }

    @Test
    fun validate_validEntry_noErrors() {
        assertTrue(validEntry.validate().isEmpty())
    }

    @Test
    fun validate_blankUserId_userIdError() {
        val errors = validEntry.copy(userId = "").validate()
        assertEquals(setOf("userId"), errors.keys)
    }

    @Test
    fun validate_blankLotteryId_lotteryIdError() {
        val errors = validEntry.copy(lotteryId = "   ").validate()
        assertEquals(setOf("lotteryId"), errors.keys)
    }

    @Test
    fun validate_zeroPartySize_partySizeError() {
        val errors = validEntry.copy(partySize = 0).validate()
        assertEquals(setOf("partySize"), errors.keys)
    }

    @Test
    fun validate_negativePartySize_partySizeError() {
        val errors = validEntry.copy(partySize = -2).validate()
        assertEquals(setOf("partySize"), errors.keys)
    }

    @Test
    fun validate_rulesNotAccepted_rulesError() {
        val errors = validEntry.copy(rulesAccepted = false, rulesAcceptedAt = null).validate()
        assertEquals(setOf("rules"), errors.keys)
    }

    @Test
    fun validate_rulesAcceptedAtNotNull_defaultsRulesAcceptedToTrue() {
        val entry = WaitlistEntry(
            userId = "u1",
            lotteryId = "l1",
            partySize = 2,
            rulesAcceptedAt = Timestamp(Date())
        )
        assertTrue(entry.rulesAccepted)
        assertTrue(entry.validate().isEmpty())
    }

    @Test
    fun validate_multipleErrors_allReported() {
        val errors = validEntry.copy(userId = "", lotteryId = "", partySize = 0).validate()
        assertEquals(setOf("userId", "lotteryId", "partySize"), errors.keys)
    }
}
