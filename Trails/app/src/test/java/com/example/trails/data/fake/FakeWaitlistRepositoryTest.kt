// unit tests for fake waitlist repository
// owner: chitkarsh (chitkarshrathi) - waitlist
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
}
