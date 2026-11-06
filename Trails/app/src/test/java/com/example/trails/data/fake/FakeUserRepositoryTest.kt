// Unit tests for the in-memory user repository (US 01.07.03, 01.08.01, 03.11.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: None currently known.
// AI assistance: Used AI to help write profile update, deletion, and mode-switching tests.
package com.example.trails.data.fake

import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeUserRepositoryTest {

    private val priya = User(
        deviceId = "device-a",
        name = "Priya Sharma",
        email = "priya@example.com",
        phone = "780-555-0142",
        profileImageUrl = "https://example.com/priya.png"
    )
    private val daniel = User(
        deviceId = "device-b",
        name = "Daniel Whitehorse",
        email = "daniel@example.com",
        roles = listOf(Role.ENTRANT, Role.OWNER),
        activeMode = Role.OWNER
    )

    @Test
    fun observe_unknownDevice_emitsNull() = runTest {
        assertNull(FakeUserRepository(listOf(priya)).observe("device-nobody").first())
    }

    @Test
    fun observe_knownDevice_emitsTheProfile() = runTest {
        assertEquals(daniel, FakeUserRepository(listOf(priya, daniel)).observe("device-b").first())
    }

    @Test
    fun save_newProfile_isAdded() = runTest {
        val repo = FakeUserRepository(emptyList())
        repo.save(priya)
        assertEquals(priya, repo.getById("device-a"))
    }

    @Test
    fun save_existingProfile_replacesWithoutDuplicating() = runTest {
        val repo = FakeUserRepository(listOf(priya))
        repo.save(priya.copy(name = "Priya S."))
        assertEquals(listOf("Priya S."), repo.observeAll().first().map { it.name })
    }

    @Test
    fun updateProfile_changesOnlyNameEmailAndPhone() = runTest {
        val repo = FakeUserRepository(listOf(daniel))
        repo.updateProfile("device-b", "Dan W.", "dan@example.com", "403-555-0100")

        val updated = repo.getById("device-b")!!
        assertEquals("Dan W.", updated.name)
        assertEquals("dan@example.com", updated.email)
        assertEquals("403-555-0100", updated.phone)
        assertEquals(daniel.roles, updated.roles)
        assertEquals(daniel.activeMode, updated.activeMode)
        assertEquals(daniel.deviceId, updated.deviceId)
    }

    @Test
    fun updateProfile_keepsThePhoto() = runTest {
        val repo = FakeUserRepository(listOf(priya))
        repo.updateProfile("device-a", "Priya", "priya@example.com", "")
        assertEquals("https://example.com/priya.png", repo.getById("device-a")?.profileImageUrl)
    }

    @Test
    fun updateProfile_doesNotTouchOtherUsers() = runTest {
        val repo = FakeUserRepository(listOf(priya, daniel))
        repo.updateProfile("device-a", "Priya S.", "priya@example.com", "")
        assertEquals(daniel, repo.getById("device-b"))
    }

    @Test
    fun updateProfile_missingProfile_throws() = runTest {
        val error = runCatching {
            FakeUserRepository(emptyList()).updateProfile("device-a", "P", "p@example.com", "")
        }.exceptionOrNull()
        assertTrue(error is NoSuchElementException)
    }

    @Test
    fun updateActiveMode_toAHeldRole_switches() = runTest {
        val repo = FakeUserRepository(listOf(daniel))
        repo.updateActiveMode("device-b", Role.ENTRANT)
        assertEquals(Role.ENTRANT, repo.getById("device-b")?.activeMode)
        assertEquals(daniel.roles, repo.getById("device-b")?.roles)
    }

    @Test
    fun updateActiveMode_toARoleNotHeld_isRefusedAndNothingChanges() = runTest {
        val repo = FakeUserRepository(listOf(priya))
        val error = runCatching { repo.updateActiveMode("device-a", Role.ADMIN) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals(priya, repo.getById("device-a"))
    }

    @Test
    fun updateActiveMode_missingProfile_throws() = runTest {
        val error = runCatching {
            FakeUserRepository(emptyList()).updateActiveMode("device-a", Role.ENTRANT)
        }.exceptionOrNull()
        assertTrue(error is NoSuchElementException)
    }

    @Test
    fun delete_removesOnlyThatProfile() = runTest {
        val repo = FakeUserRepository(listOf(priya, daniel))
        repo.delete("device-a")
        assertNull(repo.getById("device-a"))
        assertNotNull(repo.getById("device-b"))
    }

    @Test
    fun delete_thenObserve_emitsNull() = runTest {
        val repo = FakeUserRepository(listOf(priya))
        repo.delete("device-a")
        assertNull(repo.observe("device-a").first())
    }

    @Test
    fun delete_unknownDevice_changesNothing() = runTest {
        val repo = FakeUserRepository(listOf(priya))
        repo.delete("device-nobody")
        assertEquals(listOf(priya), repo.observeAll().first())
    }
}
