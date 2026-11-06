// Unit tests for the fixed identity source and device id masking (US 01.09.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: FirebaseDeviceIdProvider is not covered because it needs a live Firebase project.
package com.example.trails.data.device

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

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
}
