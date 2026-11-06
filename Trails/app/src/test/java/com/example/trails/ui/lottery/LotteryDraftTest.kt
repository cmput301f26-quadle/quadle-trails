// Unit tests for the lottery form rules, one group per user story (US 02.04.01–02.04.07).
// OWNER: Skanda (skandarajeev) — Lottery Setup
package com.example.trails.ui.lottery

import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Lottery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LotteryDraftTest {

    private val hour = 3_600_000L
    private val day = 24 * hour
    private val now = 1_800_000_000_000L
    private val opens = now + 12 * day
    private val closes = opens + 7 * day

    // A complete, valid "May long weekend" lottery, as in the Figma review screen.
    private val valid = LotteryDraft(
        name = "May long weekend",
        campsiteIds = listOf(TestCampsites.BANFF_ID, TestCampsites.JASPER_ID),
        registrationStart = opens,
        registrationEnd = closes,
        bookingStart = closes,
        bookingEnd = closes + 2 * day + 9 * hour,
        numWinners = 5,
        limitWaitlist = true,
        waitlistCapacity = 100
    )

    @Test
    fun completeDraft_hasNoProblems() {
        assertTrue(valid.problems(now).isEmpty())
    }

    @Test
    fun blankName_isAProblem() {
        assertEquals(LotteryProblem.NameMissing, valid.copy(name = "  ").problems(now)[LotteryField.NAME])
    }

    // US 02.04.01: a lottery needs at least one campsite.
    @Test
    fun noCampsites_isAProblem() {
        assertEquals(LotteryProblem.NoCampsites, valid.copy(campsiteIds = emptyList()).problems(now)[LotteryField.CAMPSITES])
    }

    // US 02.04.02: tapping campsites adds them in order; tapping again removes.
    @Test
    fun toggleCampsite_addsAndRemoves() {
        val draft = LotteryDraft().toggleCampsite("a").toggleCampsite("b").toggleCampsite("c").toggleCampsite("b")
        assertEquals(listOf("a", "c"), draft.campsiteIds)
    }

    // US 02.04.03: registration needs both ends, in order, closing in the future.
    @Test
    fun registrationNotSet_isAProblem() {
        val problem = valid.copy(registrationEnd = null).problems(now)[LotteryField.REGISTRATION]
        assertEquals(LotteryProblem.RegistrationNotSet, problem)
    }

    @Test
    fun registrationClosingBeforeItOpens_isAProblem() {
        val problem = valid.copy(registrationEnd = opens).problems(now)[LotteryField.REGISTRATION]
        assertEquals(LotteryProblem.RegistrationEndsBeforeStart, problem)
    }

    @Test
    fun registrationAlreadyClosed_isAProblem() {
        val problem = valid.copy(registrationStart = now - 3 * day, registrationEnd = now - day,
            bookingStart = now, bookingEnd = now + day).problems(now)[LotteryField.REGISTRATION]
        assertEquals(LotteryProblem.RegistrationClosesInPast, problem)
    }

    // US 02.04.06: once registration has opened its start can't move.
    @Test
    fun movingStartAfterOpening_isAProblem() {
        val problem = valid.problems(now, openedAt = opens - day)[LotteryField.REGISTRATION]
        assertEquals(LotteryProblem.RegistrationStartLocked, problem)
        assertTrue(valid.problems(now, openedAt = opens).isEmpty())
    }

    @Test
    fun drawTime_isWhenRegistrationCloses() {
        assertEquals(closes, valid.drawTime)
    }

    // US 02.04.04: booking can open right when registration closes, not before (Figma F13).
    @Test
    fun bookingStartingAtClose_isFine() {
        assertNull(valid.problems(now)[LotteryField.BOOKING])
    }

    @Test
    fun bookingOverlappingRegistration_isAProblem() {
        val problem = valid.copy(bookingStart = closes - 3 * day).problems(now)[LotteryField.BOOKING]
        assertEquals(LotteryProblem.BookingBeforeRegistrationCloses(closes), problem)
    }

    @Test
    fun bookingEndingBeforeItStarts_isAProblem() {
        val problem = valid.copy(bookingEnd = closes).problems(now)[LotteryField.BOOKING]
        assertEquals(LotteryProblem.BookingEndsBeforeStart, problem)
    }

    // US 02.04.05: winners stay between 1 and the maximum.
    @Test
    fun withWinners_staysInRange() {
        assertEquals(1, valid.withWinners(0).numWinners)
        assertEquals(MAX_WINNERS, valid.withWinners(MAX_WINNERS + 5).numWinners)
        assertEquals(6, valid.withWinners(6).numWinners)
    }

    // US 02.04.07: the waiting list limit always fits the winners.
    @Test
    fun moreWinnersThanTheLimit_growsTheLimit() {
        val draft = valid.copy(waitlistCapacity = 10).withWinners(12)
        assertEquals(12, draft.waitlistCapacity)
        assertTrue(draft.problems(now).isEmpty())
    }

    @Test
    fun withWaitlistCapacity_neverBelowWinners() {
        assertEquals(5, valid.withWaitlistCapacity(2).waitlistCapacity)
        assertEquals(110, valid.withWaitlistCapacity(110).waitlistCapacity)
    }

    @Test
    fun limitBelowCurrentEntrants_isAProblem() {
        val problem = valid.copy(waitlistCapacity = 20).problems(now, entrantCount = 24)[LotteryField.WAITLIST]
        assertEquals(LotteryProblem.WaitlistBelowEntrants(24), problem)
    }

    @Test
    fun noLimit_savesNullCapacity() {
        val lottery = valid.withWaitlistLimit(false).toLottery(null, TestUsers.OWNER_ID)
        assertNull(lottery.waitlistCapacity)
        assertEquals(100, valid.toLottery(null, TestUsers.OWNER_ID).waitlistCapacity)
    }

    @Test
    fun toLottery_newLotteryGetsOwnerAndTrimmedName() {
        val lottery = valid.copy(name = "  May long weekend ").toLottery(null, TestUsers.OWNER_ID)
        assertEquals("", lottery.id)
        assertEquals(TestUsers.OWNER_ID, lottery.ownerId)
        assertEquals("May long weekend", lottery.name)
        assertEquals(closes, lottery.registrationEnd.millis())
        assertEquals(5, lottery.numWinners)
    }

    // Editing keeps the original id and owner (US 02.04.06).
    @Test
    fun toLottery_editKeepsIdAndOwner() {
        val base = Lottery(id = "lottery-may", ownerId = "owner-original")
        val lottery = valid.toLottery(base, ownerId = "someone-else")
        assertEquals("lottery-may", lottery.id)
        assertEquals("owner-original", lottery.ownerId)
    }

    @Test
    fun from_roundTripsASavedLottery() {
        val saved = valid.toLottery(null, TestUsers.OWNER_ID)
        assertEquals(valid, LotteryDraft.from(saved))
    }

    @Test
    fun from_unlimitedLottery_suggestsDefaultLimit() {
        val draft = LotteryDraft.from(Lottery(numWinners = 3, waitlistCapacity = null))
        assertFalse(draft.limitWaitlist)
        assertEquals(DEFAULT_WAITLIST, draft.waitlistCapacity)
    }

    // The "Changes saved" banner names the setting when only one changed (Figma F22).
    @Test
    fun describeChange_singleSettingOrGeneral() {
        val before = valid.toLottery(null, TestUsers.OWNER_ID)
        assertEquals(LotteryChange.Winners(6), describeChange(before, before.copy(numWinners = 6)))
        assertEquals(LotteryChange.General, describeChange(before, before.copy(numWinners = 6, name = "June")))
        assertEquals(LotteryChange.General, describeChange(before, before))
    }
}
