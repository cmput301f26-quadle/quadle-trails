// Unit tests for FakeUserRepository.delete, which admin profile removal relies on.
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: US 03.02.01 (admin removes profiles)
// PURPOSE: Checks that delete removes only the given profile and ignores unknown device ids.
// Outstanding issues: none.
package com.example.trails.data.fake

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FakeUserRepositoryDeleteTest {

    private val priya = TestUsers.all.first { it.deviceId == TestUsers.ENTRANT_ID }
    private val daniel = TestUsers.all.first { it.deviceId == TestUsers.OWNER_ID }
    private val morgan = TestUsers.all.first { it.deviceId == TestUsers.ADMIN_ID }

    private lateinit var repo: FakeUserRepository

    @Before
    fun setUp() {
        repo = FakeUserRepository(initial = listOf(priya, daniel, morgan))
    }

    // This checks that deleting an existing profile removes it from the repository.
    @Test
    fun delete_existing_removesIt() = runBlocking {
        repo.delete(TestUsers.ENTRANT_ID)
        assertNull(repo.getById(TestUsers.ENTRANT_ID))
        assertNull(repo.observe(TestUsers.ENTRANT_ID).first())
    }

    // This checks that deleting a device id that doesn't exist changes nothing and doesn't throw.
    @Test
    fun delete_missingId_doesNothing() = runBlocking {
        repo.delete("device-unknown-999")
        assertEquals(listOf(priya, daniel, morgan), repo.observeAll().first())
    }

    // This checks that deleting one profile leaves every other profile exactly as it was.
    @Test
    fun delete_othersUntouched() = runBlocking {
        repo.delete(TestUsers.ENTRANT_ID)
        assertEquals(listOf(daniel, morgan), repo.observeAll().first())
        assertEquals(daniel, repo.getById(TestUsers.OWNER_ID))
        assertEquals(morgan, repo.getById(TestUsers.ADMIN_ID))
    }
}
