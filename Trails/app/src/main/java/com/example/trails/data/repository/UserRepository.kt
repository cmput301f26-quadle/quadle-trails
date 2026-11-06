// Read/write access to user profiles.
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Extend with the methods your stories need, and write the Firestore
//  implementation. Others: open a small PR and tag the owner.
// FIRESTORE IMPLEMENTATION: Aarav (aarav-kapoor-cs)
package com.example.trails.data.repository

import com.example.trails.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeAll(): Flow<List<User>>
    fun observe(deviceId: String): Flow<User?>
    suspend fun getById(deviceId: String): User?
    suspend fun save(user: User)

    /**
     * Deletes the user profile with [deviceId]; does nothing if it doesn't exist (US 03.02.01).
     *
     * @param deviceId device id of the user whose profile should be deleted.
     */
    suspend fun delete(deviceId: String)
}
