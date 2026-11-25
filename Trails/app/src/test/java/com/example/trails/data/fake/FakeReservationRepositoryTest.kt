// Unit test suite verifying in-memory fake reservation repository logic (Repository pattern test double).
// implements US 01.04.01, US 01.04.02, US 01.04.03, US 02.04.02
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Booking
package com.example.trails.data.fake

import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus
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
 * Unit tests verifying in-memory CRUD operations and reactive flows in [FakeReservationRepository].
 */
class FakeReservationRepositoryTest {

    private lateinit var repo: FakeReservationRepository
    private val campsiteId = "campsite-test-01"
    private val userId = "user-test-01"

    @Before
    fun setUp() {
        repo = FakeReservationRepository(initial = emptyList())
    }

    @Test
    fun defaultConstructor_seededWithTestReservations() {
        val seededRepo = FakeReservationRepository()
        runBlocking {
            val banffReservations = seededRepo.observeForCampsite(TestCampsites.BANFF_ID).first()
            assertTrue(banffReservations.isNotEmpty())
            assertEquals(TestReservations.RESERVATION_BANFF_ID, banffReservations.first().id)
        }
    }

    @Test
    fun save_newReservation_assignsIdAndPersists(): Unit = runBlocking {
        val now = Timestamp(Date())
        val res = Reservation(
            userId = userId,
            campsiteId = campsiteId,
            partySize = 2,
            startDate = now,
            endDate = now,
            totalPrice = 90.0,
            status = ReservationStatus.ACTIVE
        )
        val generatedId = repo.save(res)
        assertTrue(generatedId.isNotBlank())

        val retrieved = repo.getById(generatedId)
        assertNotNull(retrieved)
        assertEquals(userId, retrieved?.userId)
        assertEquals(campsiteId, retrieved?.campsiteId)
        assertEquals(ReservationStatus.ACTIVE, retrieved?.status)
    }

    @Test
    fun save_existingReservation_updatesInPlace(): Unit = runBlocking {
        val now = Timestamp(Date())
        val res = Reservation(
            id = "res-fixed-id",
            userId = userId,
            campsiteId = campsiteId,
            partySize = 2,
            startDate = now,
            endDate = now,
            totalPrice = 90.0
        )
        repo.save(res)

        val updated = res.copy(partySize = 4, totalPrice = 180.0)
        repo.save(updated)

        val retrieved = repo.getById("res-fixed-id")
        assertEquals(4, retrieved?.partySize)
        assertEquals(180.0, retrieved?.totalPrice ?: 0.0, 0.001)
    }

    @Test
    fun observeForCampsite_returnsMatchingOnly(): Unit = runBlocking {
        val now = Timestamp(Date())
        repo.save(Reservation(id = "r1", campsiteId = "site-A", userId = "u1", startDate = now, endDate = now))
        repo.save(Reservation(id = "r2", campsiteId = "site-B", userId = "u2", startDate = now, endDate = now))
        repo.save(Reservation(id = "r3", campsiteId = "site-A", userId = "u3", startDate = now, endDate = now))

        val siteAReservations = repo.observeForCampsite("site-A").first()
        assertEquals(2, siteAReservations.size)
        assertEquals(setOf("r1", "r3"), siteAReservations.map { it.id }.toSet())
    }

    @Test
    fun observeForUser_returnsMatchingOnly(): Unit = runBlocking {
        val now = Timestamp(Date())
        repo.save(Reservation(id = "r1", campsiteId = "site-A", userId = "user-1", startDate = now, endDate = now))
        repo.save(Reservation(id = "r2", campsiteId = "site-B", userId = "user-2", startDate = now, endDate = now))
        repo.save(Reservation(id = "r3", campsiteId = "site-C", userId = "user-1", startDate = now, endDate = now))

        val user1Reservations = repo.observeForUser("user-1").first()
        assertEquals(2, user1Reservations.size)
        assertEquals(setOf("r1", "r3"), user1Reservations.map { it.id }.toSet())
    }

    @Test
    fun updateStatus_changesStatusSuccessfully(): Unit = runBlocking {
        val res = Reservation(id = "res-status-test", campsiteId = campsiteId, userId = userId)
        repo.save(res)

        repo.updateStatus("res-status-test", ReservationStatus.CANCELLED)
        val retrieved = repo.getById("res-status-test")
        assertEquals(ReservationStatus.CANCELLED, retrieved?.status)

        repo.updateStatus("res-status-test", ReservationStatus.REJECTED)
        val rejected = repo.getById("res-status-test")
        assertEquals(ReservationStatus.REJECTED, rejected?.status)
    }

    @Test
    fun delete_removesReservation(): Unit = runBlocking {
        val res = Reservation(id = "res-to-delete", campsiteId = campsiteId, userId = userId)
        repo.save(res)
        assertNotNull(repo.getById("res-to-delete"))

        repo.delete("res-to-delete")
        assertNull(repo.getById("res-to-delete"))
    }

    @Test(expected = IOException::class)
    fun failure_whenConfigured_throwsException(): Unit = runBlocking {
        repo.failure = IOException("Simulated network failure")
        repo.getById("any-id")
    }
}
