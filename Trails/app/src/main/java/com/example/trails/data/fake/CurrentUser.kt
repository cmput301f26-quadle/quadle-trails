// Holds the identity the app treats as the current user (US 01.09.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: None currently known.
package com.example.trails.data.fake

import com.example.trails.data.device.DeviceIdProvider
import com.example.trails.data.device.FixedDeviceIdProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The identity the app treats as the current user.
 *
 * Until [install] is called the id is [TestUsers.ENTRANT_ID], so sample data,
 * previews and tests keep working. Once a real provider is installed, no other
 * user's id is ever returned: before an identity is established [id] is blank,
 * and the identity gate keeps the app on a loading or error screen.
 */
object CurrentUser {

    private var provider: DeviceIdProvider = FixedDeviceIdProvider(TestUsers.ENTRANT_ID)

    private val _session = MutableStateFlow<String?>(null)

    /** The established id, or null while none is established. */
    val session: StateFlow<String?> = _session.asStateFlow()

    /**
     * Sets where the identity comes from. Call once, from `MainActivity.onCreate`.
     *
     * @param provider the identity source.
     */
    fun install(provider: DeviceIdProvider) {
        this.provider = provider
        _session.value = null
    }

    /**
     * The current user's id.
     *
     * @return the id, or an empty string if a real provider is installed and no
     *  identity has been established yet.
     */
    fun id(): String = _session.value ?: provider.currentId().orEmpty()

    /**
     * Establishes the identity, signing in if needed, and publishes it on [session].
     *
     * @return the established id.
     * @throws Exception if the provider cannot establish an identity.
     */
    suspend fun establish(): String {
        val id = provider.ensureId()
        _session.value = id
        return id
    }

    /** Permanently removes the identity behind the current session (US 01.08.01). */
    suspend fun deleteIdentity() {
        provider.deleteIdentity()
    }

    /** Ends the session, so the identity gate establishes a new one. */
    fun endSession() {
        _session.value = null
    }

    /** Puts the sample entrant back. Used by tests. */
    fun reset() {
        provider = FixedDeviceIdProvider(TestUsers.ENTRANT_ID)
        _session.value = null
    }
}
