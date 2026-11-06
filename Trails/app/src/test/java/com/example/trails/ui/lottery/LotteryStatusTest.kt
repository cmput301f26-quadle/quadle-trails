// Unit tests for lottery status and the list filter chips.
// OWNER: Skanda (skandarajeev) — Lottery Setup
package com.example.trails.ui.lottery

import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Lottery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LotteryStatusTest {

    private val day = 86_400_000L
    private val now = 1_800_000_000_000L
    private val mayLongWeekend = Lottery(
        id = "lottery-may",
        ownerId = TestUsers.OWNER_ID,
        name = "May long weekend",
        campsiteIds = listOf(TestCampsites.HINTON_ID),
        registrationStart = timestampOf(now + 12 * day),
        registrationEnd = timestampOf(now + 19 * day),
        numWinners = 5
    )

    @Test
    fun beforeRegistrationOpens_isScheduled() {
        assertEquals(LotteryStatus.SCHEDULED, lotteryStatus(mayLongWeekend, 0, now))
    }

    @Test
    fun duringRegistration_isOpen() {
        assertEquals(LotteryStatus.OPEN, lotteryStatus(mayLongWeekend, 24, now + 15 * day))
    }

    // Registration closing at exactly "now" counts as closed: the draw has run.
    @Test
    fun afterRegistrationWithEntrants_isDrawn() {
        assertEquals(LotteryStatus.DRAWN, lotteryStatus(mayLongWeekend, 24, now + 19 * day))
    }

    @Test
    fun afterRegistrationWithNobody_isNoEntrants() {
        val status = lotteryStatus(mayLongWeekend, 0, now + 20 * day)
        assertEquals(LotteryStatus.NO_ENTRANTS, status)
        assertTrue(status.isClosed)
    }

    @Test
    fun missingDates_countAsOpen() {
        assertEquals(LotteryStatus.OPEN, lotteryStatus(Lottery(name = "Draft"), 0, now))
    }

    @Test
    fun doneFilter_coversBothClosedStatuses() {
        assertTrue(LotteryFilter.DONE.matches(LotteryStatus.DRAWN))
        assertTrue(LotteryFilter.DONE.matches(LotteryStatus.NO_ENTRANTS))
        assertFalse(LotteryFilter.DONE.matches(LotteryStatus.OPEN))
    }

    @Test
    fun allFilter_matchesEverything() {
        assertTrue(LotteryStatus.entries.all { LotteryFilter.ALL.matches(it) })
    }
}
