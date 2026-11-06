// Test double: a user repository whose operations can be made to fail (US 01.07.03, 01.08.01, 03.11.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: None currently known.
package com.example.trails.ui.profile

import com.example.trails.data.fake.FakeUserRepository
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.example.trails.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Behaves like [FakeUserRepository] until one of the failure fields is set, then
 * that operation throws. Used to test error states.
 *
 * @param inner the working repository.
 */
class FlakyUserRepository(private val inner: FakeUserRepository) : UserRepository by inner {

    /** Thrown by the observe flow when set. */
    var observeFailure: Exception? = null

    /** Thrown by [updateProfile] when set. */
    var updateFailure: Exception? = null

    /** Thrown by [updateActiveMode] when set. */
    var modeFailure: Exception? = null

    /** Thrown by [delete] when set. */
    var deleteFailure: Exception? = null

    override fun observe(deviceId: String): Flow<User?> {
        val failure = observeFailure ?: return inner.observe(deviceId)
        return flow { throw failure }
    }

    override suspend fun updateProfile(deviceId: String, name: String, email: String, phone: String) {
        updateFailure?.let { throw it }
        inner.updateProfile(deviceId, name, email, phone)
    }

    override suspend fun updateActiveMode(deviceId: String, mode: Role) {
        modeFailure?.let { throw it }
        inner.updateActiveMode(deviceId, mode)
    }

    override suspend fun delete(deviceId: String) {
        deleteFailure?.let { throw it }
        inner.delete(deviceId)
    }
}
