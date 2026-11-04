// Unit tests for FakeCampsiteRepository, which screens and tests rely on.
// OWNER: Ibrahim (ibr2309) — Campsite Management
package com.example.trails.data.fake

import com.example.trails.data.model.Campsite
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeCampsiteRepositoryTest {

    private lateinit var repo: FakeCampsiteRepository
    private val hinton = Campsite(
        ownerId = "owner-device-01",
        name = "Wildhorse Lake Campground",
        locationName = "Hinton-Grande Cache, AB",
        maxOccupancy = 8,
        pricePerNight = 65.0
    )

    @Before
    fun setUp() {
        repo = FakeCampsiteRepository(initial = emptyList())
    }

    @Test
    fun save_newCampsite_assignsId() = runBlocking {
        val id = repo.save(hinton)
        assertTrue(id.isNotBlank())
        assertEquals(hinton.name, repo.getById(id)?.name)
    }

    @Test
    fun save_twoNewCampsites_differentIds() = runBlocking {
        assertNotEquals(repo.save(hinton), repo.save(hinton.copy(name = "Second Site")))
    }

    @Test
    fun save_existingCampsite_overwritesInPlace() = runBlocking {
        val id = repo.save(hinton)
        repo.save(repo.getById(id)!!.copy(pricePerNight = 70.0))
        assertEquals(70.0, repo.getById(id)!!.pricePerNight, 0.0)
        assertEquals(1, repo.observeAll().first().size)
    }

    @Test
    fun getById_unknownId_returnsNull() = runBlocking {
        assertNull(repo.getById("does-not-exist"))
    }

    @Test
    fun observeByOwner_otherOwnersSites_excluded() = runBlocking {
        repo.save(hinton)
        repo.save(hinton.copy(ownerId = "someone-else", name = "Jasper Pines"))
        val mine = repo.observeByOwner("owner-device-01").first()
        assertEquals(listOf("Wildhorse Lake Campground"), mine.map { it.name })
    }

    @Test
    fun observeByOwner_noSites_emptyList() = runBlocking {
        assertTrue(repo.observeByOwner("owner-device-01").first().isEmpty())
    }

    @Test
    fun observeById_afterEdit_emitsLatest() = runBlocking {
        val id = repo.save(hinton)
        repo.save(repo.getById(id)!!.copy(pricePerNight = 70.0))
        assertEquals(70.0, repo.observeById(id).first()!!.pricePerNight, 0.0)
    }

    @Test
    fun uploadImage_returnsUsableUrl() = runBlocking {
        assertEquals("content://media/picker/0/42", repo.uploadImage("content://media/picker/0/42"))
    }

    @Test
    fun observeById_unknownId_emitsNull() = runBlocking {
        assertNull(repo.observeById("does-not-exist").first())
    }
}
