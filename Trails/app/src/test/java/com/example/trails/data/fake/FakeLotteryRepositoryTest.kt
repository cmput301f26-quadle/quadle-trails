// Unit tests for FakeLotteryRepository, which the lottery screens and tests rely on.
// OWNER: Skanda (skandarajeev) — Lottery Setup
package com.example.trails.data.fake

import com.example.trails.data.model.Lottery
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeLotteryRepositoryTest {

    private lateinit var repo: FakeLotteryRepository
    private val mayLongWeekend = Lottery(
        ownerId = TestUsers.OWNER_ID,
        name = "May long weekend",
        campsiteIds = listOf(TestCampsites.BANFF_ID, TestCampsites.JASPER_ID),
        numWinners = 5,
        waitlistCapacity = 100
    )

    @Before
    fun setUp() {
        repo = FakeLotteryRepository(initial = emptyList())
    }

    // A new lottery (blank id) gets an id, and can be read back with it.
    @Test
    fun save_newLottery_assignsId() = runBlocking {
        val id = repo.save(mayLongWeekend)
        assertTrue(id.isNotBlank())
        assertEquals("May long weekend", repo.getById(id)?.name)
    }

    @Test
    fun save_twoNewLotteries_differentIds() = runBlocking {
        assertNotEquals(repo.save(mayLongWeekend), repo.save(mayLongWeekend.copy(name = "Summer weekends")))
    }

    // Saving with an existing id replaces that lottery instead of adding a second one (US 02.04.06).
    @Test
    fun save_existingLottery_overwritesInPlace() = runBlocking {
        val id = repo.save(mayLongWeekend)
        repo.save(repo.getById(id)!!.copy(numWinners = 6))
        assertEquals(6, repo.getById(id)!!.numWinners)
        assertEquals(1, repo.observeAll().first().size)
    }

    @Test
    fun observeByOwner_onlyThatOwnersLotteries() = runBlocking {
        repo.save(mayLongWeekend)
        repo.save(mayLongWeekend.copy(ownerId = "someone-else"))
        val mine = repo.observeByOwner(TestUsers.OWNER_ID).first()
        assertEquals(1, mine.size)
        assertEquals(TestUsers.OWNER_ID, mine.single().ownerId)
    }

    @Test
    fun observeById_emitsLotteryThenNullForUnknownId() = runBlocking {
        val id = repo.save(mayLongWeekend)
        assertEquals("May long weekend", repo.observeById(id).first()?.name)
        assertNull(repo.observeById("missing").first())
    }

    // The failure hook throws once and then lets the retry through.
    @Test
    fun failure_throwsOnceThenSaves() = runBlocking {
        repo.failure = IllegalStateException("offline")
        val failed = runCatching { repo.save(mayLongWeekend) }
        assertTrue(failed.isFailure)
        assertTrue(repo.observeAll().first().isEmpty())
        assertTrue(repo.save(mayLongWeekend).isNotBlank())
    }

    @Test
    fun delete_removesOnlyThatLottery() = runBlocking {
        val keep = repo.save(mayLongWeekend)
        val gone = repo.save(mayLongWeekend.copy(name = "Summer weekends"))
        repo.delete(gone)
        assertNull(repo.getById(gone))
        assertEquals(listOf(keep), repo.observeAll().first().map { it.id })
    }
}
