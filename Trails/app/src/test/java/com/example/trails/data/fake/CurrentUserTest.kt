// Unit tests for the current-user identity (US 01.09.01, 01.08.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: None currently known.
package com.example.trails.data.fake

import com.example.trails.data.device.DeviceIdProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentUserTest {

    /** Hands out ids in order, like a sign-in service issuing new accounts. */
    private class SequenceProvider(vararg ids: String) : DeviceIdProvider {
        private val unused = ArrayDeque(ids.toList())
        var current: String? = null
        var deletions = 0

        override fun currentId(): String? = current
        override suspend fun ensureId(): String = current ?: unused.removeFirst().also { current = it }
        override suspend fun deleteIdentity() {
            deletions++
            current = null
        }
    }

    @After
    fun tearDown() {
        CurrentUser.reset()
    }

    @Test
    fun withNoProviderInstalled_usesTheSampleEntrant() {
        assertEquals(TestUsers.ENTRANT_ID, CurrentUser.id())
    }

    @Test
    fun beforeEstablishing_aRealProviderNeverReturnsAnotherUsersId() {
        CurrentUser.install(SequenceProvider("uid-1"))
        assertEquals("", CurrentUser.id())
        assertNull(CurrentUser.session.value)
    }

    @Test
    fun establish_publishesTheIdOnTheSession() = runTest {
        CurrentUser.install(SequenceProvider("uid-1"))
        assertEquals("uid-1", CurrentUser.establish())
        assertEquals("uid-1", CurrentUser.id())
        assertEquals("uid-1", CurrentUser.session.value)
    }

    @Test
    fun establish_twice_keepsTheSameIdentity() = runTest {
        CurrentUser.install(SequenceProvider("uid-1", "uid-2"))
        CurrentUser.establish()
        assertEquals("uid-1", CurrentUser.establish())
    }

    @Test
    fun deleteIdentityThenNewSession_givesADifferentId() = runTest {
        val provider = SequenceProvider("uid-1", "uid-2")
        CurrentUser.install(provider)
        CurrentUser.establish()

        CurrentUser.deleteIdentity()
        assertEquals("uid-1", CurrentUser.id())
        CurrentUser.endSession()
        assertEquals("", CurrentUser.id())

        assertEquals("uid-2", CurrentUser.establish())
        assertEquals(1, provider.deletions)
    }

    @Test
    fun reset_restoresTheSampleEntrant() = runTest {
        CurrentUser.install(SequenceProvider("uid-1"))
        CurrentUser.establish()
        CurrentUser.reset()
        assertEquals(TestUsers.ENTRANT_ID, CurrentUser.id())
        assertNull(CurrentUser.session.value)
    }
}
