// In-memory UserRepository seeded from TestUsers.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.fake

import com.example.trails.data.model.User
import com.example.trails.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

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
}
