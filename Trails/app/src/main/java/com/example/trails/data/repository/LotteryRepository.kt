// Read/write access to lotteries.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
// FIRESTORE IMPLEMENTATION: Skanda (skandarajeev)
package com.example.trails.data.repository

import com.example.trails.data.model.Lottery
import kotlinx.coroutines.flow.Flow

interface LotteryRepository {
    fun observeAll(): Flow<List<Lottery>>
    fun observeByOwner(ownerId: String): Flow<List<Lottery>>
    suspend fun getById(id: String): Lottery?
    suspend fun save(lottery: Lottery)
}
