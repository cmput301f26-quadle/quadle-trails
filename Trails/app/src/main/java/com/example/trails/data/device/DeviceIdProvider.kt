// Identifies the user by device, with no username or password (US 01.09.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: Needs Anonymous sign-in enabled in Firebase. FirebaseDeviceIdProvider has no automated tests.
// AI assistance: Used AI to help implement Firebase anonymous authentication for device identity and sample sessions.
package com.example.trails.data.device

import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

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

/** How long [ensureFirebaseSession] waits for Firebase before giving up. */
const val FIREBASE_SIGN_IN_TIMEOUT_MS = 5_000L

/**
 * Makes sure Firebase has a signed-in (anonymous) session, without changing who the app
 * treats as the current user. Sample users keep their fixed ids, but Storage and Firestore
 * rules that check `request.auth` still work for them.
 *
 * Never throws for a Firebase problem (offline, Anonymous sign-in disabled): the caller
 * can carry on, and requests are simply unauthenticated as before.
 *
 * @param timeoutMs how long to wait for the sign-in.
 * @param auth supplies the Firebase Auth instance; replaceable in tests.
 * @return true if a Firebase session exists afterwards.
 */
suspend fun ensureFirebaseSession(
    timeoutMs: Long = FIREBASE_SIGN_IN_TIMEOUT_MS,
    auth: () -> FirebaseAuth = { FirebaseAuth.getInstance() }
): Boolean =
    try {
        val firebase = auth()
        if (firebase.currentUser != null) {
            true
        } else {
            withTimeoutOrNull(timeoutMs) { firebase.signInAnonymously().await() }?.user != null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
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
