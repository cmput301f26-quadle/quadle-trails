// Unit test suite verifying in-memory fake waitlist repository logic (Repository pattern test double).
// implements US 01.02.01, US 01.02.02, US 01.04.03, US 02.04.02, US 02.09.01, US 02.11.01, and Bonus Rolling Lottery
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.data.fake

import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.model.WaitlistStatus
import com.example.trails.data.repository.RulesNotAcceptedException
import com.example.trails.data.repository.WaitlistFullException
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.Date

/**
 * Unit test suite verifying the in-memory behaviors of [FakeWaitlistRepository].
 */
class FakeWaitlistRepositoryTest {

    private lateinit var repo: FakeWaitlistRepository
    private val lotteryId = "test-lottery-id"
    private val userId = "test-user-id"

    // used agent for the repetitive setup here
    @Before
    fun setUp() {
        repo = FakeWaitlistRepository(initial = emptyList())
    }

    @Test
    fun save_validEntry_persistsAndRetrievable(): Unit = runBlocking {
        val entry = WaitlistEntry(
            userId = userId,
            userName = "Test User",
            lotteryId = lotteryId,
            partySize = 2,
            rulesAccepted = true
        )
        repo.save(entry)

        val retrieved = repo.getEntry(lotteryId, userId)
        assertNotNull(retrieved)
        assertEquals("Test User", retrieved?.userName)
        assertEquals(1, repo.getEntryCount(lotteryId))
    }

    @Test
    fun save_existingEntry_updatesInPlace(): Unit = runBlocking {
        val entry = WaitlistEntry(
            userId = userId,
            userName = "Test User",
            lotteryId = lotteryId,
            partySize = 2,
            rulesAccepted = true
        )
        repo.save(entry)
        repo.save(entry.copy(partySize = 4))

        val retrieved = repo.getEntry(lotteryId, userId)
        assertEquals(4, retrieved?.partySize)
        assertEquals(1, repo.getEntryCount(lotteryId))
    }

    @Test
    fun deleteEntry_removesEntry(): Unit = runBlocking {
        val entry = WaitlistEntry(
            userId = userId,
            userName = "Test User",
            lotteryId = lotteryId,
            partySize = 2,
            rulesAccepted = true
        )
        repo.save(entry)
        assertEquals(1, repo.getEntryCount(lotteryId))

        repo.deleteEntry(lotteryId, userId)
        assertNull(repo.getEntry(lotteryId, userId))
        assertEquals(0, repo.getEntryCount(lotteryId))
    }

    @Test
    fun joinWaitlist_validParams_createsEntry(): Unit = runBlocking {
        val entry = repo.joinWaitlist(
            lotteryId = lotteryId,
            userId = userId,
            userName = "Chitkarsh",
            partySize = 3,
            rulesAccepted = true,
            maxCapacity = 10
        )

        assertEquals(userId, entry.userId)
        assertEquals(lotteryId, entry.lotteryId)
        assertEquals(3, entry.partySize)
        assertEquals(WaitlistStatus.WAITING, entry.status)
        assertTrue(entry.rulesAccepted)
        assertNotNull(entry.joinedAt)

        val stored = repo.getEntry(lotteryId, userId)
        assertEquals(entry, stored)
    }

    @Test(expected = RulesNotAcceptedException::class)
    fun joinWaitlist_rulesNotAccepted_throwsRulesNotAcceptedException(): Unit = runBlocking {
        repo.joinWaitlist(
            lotteryId = lotteryId,
            userId = userId,
            userName = "Chitkarsh",
            partySize = 3,
            rulesAccepted = false
        )
    }

    @Test(expected = WaitlistFullException::class)
    fun joinWaitlist_atCapacity_throwsWaitlistFullException(): Unit = runBlocking {
        // fill up to capacity 2
        repo.joinWaitlist(lotteryId, "user-1", "User 1", 2, true, maxCapacity = 2)
        repo.joinWaitlist(lotteryId, "user-2", "User 2", 2, true, maxCapacity = 2)

        // attempting to add 3rd user should fail
        repo.joinWaitlist(lotteryId, "user-3", "User 3", 2, true, maxCapacity = 2)
    }

    @Test
    fun joinWaitlist_atCapacity_existingUserUpdatesSuccessfully(): Unit = runBlocking {
        // add 2 users for capacity 2
        repo.joinWaitlist(lotteryId, "user-1", "User 1", 2, true, maxCapacity = 2)
        repo.joinWaitlist(lotteryId, "user-2", "User 2", 2, true, maxCapacity = 2)

        // existing user updating party size should succeed
        val updated = repo.joinWaitlist(lotteryId, "user-1", "User 1", 4, true, maxCapacity = 2)
        assertEquals(4, updated.partySize)
        assertEquals(2, repo.getEntryCount(lotteryId))
    }

    @Test
    fun observeForLottery_emitsSortedByJoinedAt(): Unit = runBlocking {
        val t1 = Timestamp(Date(100000))
        val t2 = Timestamp(Date(200000))

        repo.save(WaitlistEntry(userId = "u2", lotteryId = lotteryId, joinedAt = t2, rulesAccepted = true))
        repo.save(WaitlistEntry(userId = "u1", lotteryId = lotteryId, joinedAt = t1, rulesAccepted = true))

        val list = repo.observeForLottery(lotteryId).first()
        assertEquals(listOf("u1", "u2"), list.map { it.userId })
    }

    @Test
    fun getEntriesForLottery_returnsSortedByJoinedAt(): Unit = runBlocking {
        val t1 = Timestamp(Date(100000))
        val t2 = Timestamp(Date(200000))
        val t3 = Timestamp(Date(300000))

        repo.save(WaitlistEntry(userId = "u3", lotteryId = lotteryId, joinedAt = t3, rulesAccepted = true))
        repo.save(WaitlistEntry(userId = "u1", lotteryId = lotteryId, joinedAt = t1, rulesAccepted = true))
        repo.save(WaitlistEntry(userId = "u2", lotteryId = lotteryId, joinedAt = t2, rulesAccepted = true))

        val entries = repo.getEntriesForLottery(lotteryId)
        assertEquals(listOf("u1", "u2", "u3"), entries.map { it.userId })
    }

    @Test
    fun observeEntry_emitsUserEntry(): Unit = runBlocking {
        assertNull(repo.observeEntry(lotteryId, userId).first())

        repo.joinWaitlist(lotteryId, userId, "Chitkarsh", 2, true)
        val observed = repo.observeEntry(lotteryId, userId).first()
        assertNotNull(observed)
        assertEquals(userId, observed?.userId)
    }

    @Test(expected = IOException::class)
    fun failureInjected_throwsExceptionOnAllCalls(): Unit = runBlocking {
        repo.failure = IOException("simulated network outage")
        repo.getEntry(lotteryId, userId)
    }

    @Test
    fun updateStatus_existingEntry_updatesAndPersists(): Unit = runBlocking {
        repo.joinWaitlist(lotteryId, userId, "Chitkarsh", 2, true)
        val initial = repo.getEntry(lotteryId, userId)
        assertEquals(WaitlistStatus.WAITING, initial?.status)

        val won = repo.updateStatus(lotteryId, userId, WaitlistStatus.WON)
        assertNotNull(won)
        assertEquals(WaitlistStatus.WON, won?.status)
        assertEquals(WaitlistStatus.WON, repo.getEntry(lotteryId, userId)?.status)

        val declined = repo.updateStatus(lotteryId, userId, WaitlistStatus.DECLINED)
        assertNotNull(declined)
        assertEquals(WaitlistStatus.DECLINED, declined?.status)
        assertEquals(WaitlistStatus.DECLINED, repo.getEntry(lotteryId, userId)?.status)
    }

    @Test
    fun updateStatus_nonExistingEntry_returnsNull(): Unit = runBlocking {
        val result = repo.updateStatus(lotteryId, "unknown-user", WaitlistStatus.WON)
        assertNull(result)
    }

    @Test
    fun drawNextReplacement_picksRandomWaitingEntrant(): Unit = runBlocking {
        val entry1 = WaitlistEntry(
            userId = "user-waiting-1",
            lotteryId = lotteryId,
            joinedAt = Timestamp(Date(1000000L)),
            status = WaitlistStatus.WAITING,
            rulesAccepted = true
        )
        val entry2 = WaitlistEntry(
            userId = "user-waiting-2",
            lotteryId = lotteryId,
            joinedAt = Timestamp(Date(2000000L)),
            status = WaitlistStatus.WAITING,
            rulesAccepted = true
        )
        repo.save(entry1)
        repo.save(entry2)

        val replacement = repo.drawNextReplacement(lotteryId)
        assertNotNull(replacement)
        assertTrue(replacement?.userId in setOf("user-waiting-1", "user-waiting-2"))
        assertEquals(WaitlistStatus.WON, replacement?.status)

        // Verify the remaining entry stays WAITING
        val remainingUserId = if (replacement?.userId == "user-waiting-1") "user-waiting-2" else "user-waiting-1"
        assertEquals(WaitlistStatus.WAITING, repo.getEntry(lotteryId, remainingUserId)?.status)
    }

    @Test
    fun drawLotteryRound_drawsMultipleRandomWinners(): Unit = runBlocking {
        for (i in 1..5) {
            repo.save(
                WaitlistEntry(
                    userId = "user-$i",
                    lotteryId = lotteryId,
                    status = WaitlistStatus.WAITING,
                    rulesAccepted = true
                )
            )
        }

        val drawn = repo.drawLotteryRound(lotteryId, 3)
        assertEquals(3, drawn.size)
        assertTrue(drawn.all { it.status == WaitlistStatus.WON })
        // Remaining 2 should stay WAITING
        val allEntries = repo.getEntriesForLottery(lotteryId)
        assertEquals(2, allEntries.count { it.status == WaitlistStatus.WAITING })
        assertEquals(3, allEntries.count { it.status == WaitlistStatus.WON })
    }

    @Test
    fun drawNextReplacement_noWaitingEntrants_returnsNull(): Unit = runBlocking {
        val replacement = repo.drawNextReplacement("non-existent-lottery")
        assertNull(replacement)
    }

    @Test
    fun autoRollOnDecline_updatesDecliningUserAndPromotesNextWaitingEntrant(): Unit = runBlocking {
        val winnerEntry = WaitlistEntry(
            userId = "winner-user",
            lotteryId = lotteryId,
            joinedAt = Timestamp(Date(500000L)),
            status = WaitlistStatus.WON,
            rulesAccepted = true
        )
        val waitingEntry = WaitlistEntry(
            userId = "waiting-user",
            lotteryId = lotteryId,
            joinedAt = Timestamp(Date(1500000L)),
            status = WaitlistStatus.WAITING,
            rulesAccepted = true
        )
        repo.save(winnerEntry)
        repo.save(waitingEntry)

        val newWinner = repo.autoRollOnDecline(lotteryId, "winner-user")

        // Winner should be updated to DECLINED
        val declined = repo.getEntry(lotteryId, "winner-user")
        assertEquals(WaitlistStatus.DECLINED, declined?.status)

        // Next waiting user should be promoted to WON
        assertNotNull(newWinner)
        assertEquals("waiting-user", newWinner?.userId)
        assertEquals(WaitlistStatus.WON, newWinner?.status)
    }

    @Test
    fun reconcileExpiredInvites_declinesExpiredUsersAndDrawsReplacements(): Unit = runBlocking {
        val expired1 = WaitlistEntry(
            userId = "expired-1",
            lotteryId = lotteryId,
            joinedAt = Timestamp(Date(100000L)),
            status = WaitlistStatus.WON,
            rulesAccepted = true
        )
        val waiting1 = WaitlistEntry(
            userId = "waiting-1",
            lotteryId = lotteryId,
            joinedAt = Timestamp(Date(200000L)),
            status = WaitlistStatus.WAITING,
            rulesAccepted = true
        )
        repo.save(expired1)
        repo.save(waiting1)

        val replacements = repo.reconcileExpiredInvites(lotteryId, listOf("expired-1"))

        assertEquals(WaitlistStatus.DECLINED, repo.getEntry(lotteryId, "expired-1")?.status)
        assertEquals(1, replacements.size)
        assertEquals("waiting-1", replacements.first().userId)
        assertEquals(WaitlistStatus.WON, replacements.first().status)
    }
}
