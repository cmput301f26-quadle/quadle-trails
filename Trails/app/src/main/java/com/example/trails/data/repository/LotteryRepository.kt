// Read/write access to lotteries.
// OWNER: Skanda (skandarajeev) — Lottery Setup
// Extend with the methods your stories need, and write the Firestore
//  implementation. Others: open a small PR and tag the owner.
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
