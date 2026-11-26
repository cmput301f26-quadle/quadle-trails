// Unit tests for the fixed identity source, the background Firebase session and device id masking (US 01.09.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: FirebaseDeviceIdProvider is not covered because it needs a live Firebase project.
// AI assistance: Used AI to help write mock-based tests for the background Firebase sign-in.
package com.example.trails.data.device

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.io.IOException

class DeviceIdProviderTest {

    @Test
    fun fixedProvider_currentIdIsTheGivenId() {
        assertEquals("device-entrant-001", FixedDeviceIdProvider("device-entrant-001").currentId())
    }

    @Test
    fun fixedProvider_ensureIdReturnsTheSameId() = runTest {
        val provider = FixedDeviceIdProvider("device-entrant-001")
        assertEquals("device-entrant-001", provider.ensureId())
        assertEquals("device-entrant-001", provider.ensureId())
    }

    @Test
    fun fixedProvider_deleteIdentityKeepsTheId() = runTest {
        val provider = FixedDeviceIdProvider("device-entrant-001")
        provider.deleteIdentity()
        assertEquals("device-entrant-001", provider.currentId())
    }

    @Test
    fun formatDeviceId_firebaseUid_showsLastFour() {
        assertEquals("QD-••••-1A2F", formatDeviceId("Xy7Qk2LmP9aBcDeF1a2f"))
    }

    @Test
    fun formatDeviceId_ignoresPunctuation() {
        assertEquals("QD-••••-ABCD", formatDeviceId("dev-ice--ab.cd"))
    }

    @Test
    fun formatDeviceId_tooShort_fallsBack() {
        assertEquals("QD-••••-????", formatDeviceId("ab"))
        assertEquals("QD-••••-????", formatDeviceId(""))
    }

    // ensureFirebaseSession: sample users also get a Firebase session

    @Test
    fun ensureFirebaseSession_alreadySignedIn_doesNotSignInAgain() = runTest {
        val auth = mock(FirebaseAuth::class.java)
        `when`(auth.currentUser).thenReturn(mock(FirebaseUser::class.java))

        assertTrue(ensureFirebaseSession(auth = { auth }))
        verify(auth, never()).signInAnonymously()
    }

    @Test
    fun ensureFirebaseSession_noSession_signsInAnonymously() = runTest {
        val auth = mock(FirebaseAuth::class.java)
        val result = mock(AuthResult::class.java)
        `when`(result.user).thenReturn(mock(FirebaseUser::class.java))
        `when`(auth.signInAnonymously()).thenReturn(Tasks.forResult(result))

        assertTrue(ensureFirebaseSession(auth = { auth }))
        verify(auth).signInAnonymously()
    }

    @Test
    fun ensureFirebaseSession_signInFails_returnsFalseWithoutThrowing() = runTest {
        val auth = mock(FirebaseAuth::class.java)
        `when`(auth.signInAnonymously()).thenReturn(Tasks.forException(IOException("offline")))

        assertFalse(ensureFirebaseSession(auth = { auth }))
    }

    @Test
    fun ensureFirebaseSession_firebaseUnavailable_returnsFalseWithoutThrowing() = runTest {
        assertFalse(ensureFirebaseSession(auth = { throw IllegalStateException("Firebase is not initialized") }))
    }
}
