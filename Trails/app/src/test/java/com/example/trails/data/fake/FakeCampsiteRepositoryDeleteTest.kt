// Unit tests for FakeCampsiteRepository.delete, which admin campsite removal relies on.
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: US 03.01.01 (admin removes registered campsites)
// PURPOSE: Checks that delete removes only the given campsite and ignores unknown ids.
// Outstanding issues: none.
package com.example.trails.data.fake

import com.example.trails.data.model.Campsite
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FakeCampsiteRepositoryDeleteTest {

    private val wildhorse = Campsite(
        id = "wildhorse-lake",
        ownerId = "owner-device-01",
        name = "Wildhorse Lake Campground",
        locationName = "Hinton-Grande Cache, AB",
        maxOccupancy = 8,
        pricePerNight = 65.0
    )
    private val jasperPines = Campsite(
        id = "jasper-pines",
        ownerId = "device-maya",
        name = "Jasper Pines",
        locationName = "Jasper, AB",
        maxOccupancy = 6,
        pricePerNight = 40.0
    )

    private lateinit var repo: FakeCampsiteRepository

    @Before
    fun setUp() {
        repo = FakeCampsiteRepository(initial = listOf(wildhorse, jasperPines))
    }

    // This checks that deleting an existing campsite removes it from the repository.
    @Test
    fun delete_existing_removesIt() = runBlocking {
        repo.delete("wildhorse-lake")
        assertNull(repo.getById("wildhorse-lake"))
        assertNull(repo.observeById("wildhorse-lake").first())
    }

    // This checks that deleting an id that doesn't exist changes nothing and doesn't throw.
    @Test
    fun delete_missingId_doesNothing() = runBlocking {
        repo.delete("no-such-site")
        assertEquals(listOf(wildhorse, jasperPines), repo.observeAll().first())
    }

    // This checks that deleting one campsite leaves every other campsite exactly as it was.
    @Test
    fun delete_othersUntouched() = runBlocking {
        repo.delete("wildhorse-lake")
        assertEquals(listOf(jasperPines), repo.observeAll().first())
        assertEquals(jasperPines, repo.getById("jasper-pines"))
    }
}
