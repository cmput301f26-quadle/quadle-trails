// Identifies the user by device, with no username or password (US 01.09.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: FirebaseDeviceIdProvider needs Anonymous sign-in enabled in Firebase and has no automated tests.
// AI assistance: Used AI to help implement Firebase anonymous authentication for device identity.
package com.example.trails.data.device

import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Supplies the id that identifies the person using this device.
 */
interface DeviceIdProvider {

    /**
     * The id, if one has already been established. Never blocks.
     *
     * @return the id, or null if [ensureId] has not succeeded yet.
     */
    fun currentId(): String?

    /**
     * Returns the existing id, or establishes a new one if there is none.
     *
     * @return the id; never blank.
     * @throws Exception if an identity could not be established, for example offline
     *  on first launch.
     */
    suspend fun ensureId(): String

    /**
     * Permanently removes this identity (US 01.08.01). After this, [ensureId]
     * establishes a different one where the source supports it.
     */
    suspend fun deleteIdentity()
}

/**
 * Always uses the same id. Used for the sample data, previews and tests.
 *
 * @property id the id to use.
 */
class FixedDeviceIdProvider(private val id: String) : DeviceIdProvider {
    override fun currentId(): String = id
    override suspend fun ensureId(): String = id
    override suspend fun deleteIdentity() = Unit
}

/**
 * Uses Firebase anonymous authentication. The first launch signs in silently and
 * Firebase keeps the session on the device, so later launches reuse the same uid.
 *
 * @property auth the Firebase Auth instance.
 */
class FirebaseDeviceIdProvider(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : DeviceIdProvider {

    override fun currentId(): String? = auth.currentUser?.uid

    override suspend fun ensureId(): String {
        auth.currentUser?.uid?.let { return it }
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: throw IllegalStateException("Firebase returned no user.")
    }

    override suspend fun deleteIdentity() {
        val user = auth.currentUser ?: return
        try {
            user.delete().await()
        } catch (e: FirebaseException) {
            // Firebase can refuse to delete an older session. Signing out still means
            // the next launch gets a brand-new identity, which is what deletion requires.
            auth.signOut()
        }
    }
}

/**
 * Masks an id for display, for example "QD-••••-1A2F".
 *
 * @param deviceId the full id.
 * @return the masked form, or "QD-••••-????" when the id has fewer than four
 *  letters or digits.
 */
fun formatDeviceId(deviceId: String): String {
    val tail = deviceId.filter { it.isLetterOrDigit() }.takeLast(4).uppercase()
    return if (tail.length == 4) "QD-••••-$tail" else "QD-••••-????"
}
