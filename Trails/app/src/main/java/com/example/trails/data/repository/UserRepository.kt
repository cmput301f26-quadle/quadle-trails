// Read/write access to user profiles.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
// FIRESTORE IMPLEMENTATION: Aarav (aarav-kapoor-cs)
package com.example.trails.data.repository

import com.example.trails.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeAll(): Flow<List<User>>
    fun observe(deviceId: String): Flow<User?>
    suspend fun getById(deviceId: String): User?
    suspend fun save(user: User)
}
