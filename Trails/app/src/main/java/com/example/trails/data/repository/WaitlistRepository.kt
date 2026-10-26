// Read/write access to waitlist entries, which live under lotteries/{id}/entries.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
// FIRESTORE IMPLEMENTATION: Chitkarsh (chitkarshrathi)
package com.example.trails.data.repository

import com.example.trails.data.model.WaitlistEntry
import kotlinx.coroutines.flow.Flow

interface WaitlistRepository {
    fun observeForLottery(lotteryId: String): Flow<List<WaitlistEntry>>
    suspend fun getEntry(lotteryId: String, userId: String): WaitlistEntry?
    suspend fun save(entry: WaitlistEntry)
}
