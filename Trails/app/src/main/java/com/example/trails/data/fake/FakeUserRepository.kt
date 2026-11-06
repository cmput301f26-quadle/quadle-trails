// In-memory user repository seeded from TestUsers (US 01.07.03, 01.08.01, 03.11.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: None currently known.
package com.example.trails.data.fake

import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.example.trails.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Keeps profiles in memory, so the profile screens work without a server.
 *
 * @param initial the profiles to start with.
 */
class FakeUserRepository(initial: List<User> = TestUsers.all) : UserRepository {
    private val users = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<User>> = users

    override fun observe(deviceId: String): Flow<User?> =
        users.map { list -> list.find { it.deviceId == deviceId } }

    override suspend fun getById(deviceId: String): User? =
        users.value.find { it.deviceId == deviceId }

    override suspend fun save(user: User) {
        users.update { list -> list.filterNot { it.deviceId == user.deviceId } + user }
    }

    override suspend fun updateProfile(deviceId: String, name: String, email: String, phone: String) {
        requireProfile(deviceId)
        replace(deviceId) { it.copy(name = name, email = email, phone = phone) }
    }

    override suspend fun updateActiveMode(deviceId: String, mode: Role) {
        val user = requireProfile(deviceId)
        require(mode in user.roles) { "Profile $deviceId does not hold the $mode role." }
        replace(deviceId) { it.copy(activeMode = mode) }
    }

    override suspend fun delete(deviceId: String) {
        users.update { list -> list.filterNot { it.deviceId == deviceId } }
    }

    private fun requireProfile(deviceId: String): User =
        users.value.find { it.deviceId == deviceId }
            ?: throw NoSuchElementException("No profile for $deviceId.")

    private fun replace(deviceId: String, change: (User) -> User) {
        users.update { list -> list.map { if (it.deviceId == deviceId) change(it) else it } }
    }
}