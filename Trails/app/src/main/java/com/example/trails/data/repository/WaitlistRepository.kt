// Read/write access to waitlist entries, which live under lotteries/{id}/entries.
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
// Extend with the methods your stories need, and write the Firestore
//  implementation. Others: open a small PR and tag the owner.
// FIRESTORE IMPLEMENTATION: Chitkarsh (chitkarshrathi)
package com.example.trails.data.repository

import com.example.trails.data.model.WaitlistEntry
import kotlinx.coroutines.flow.Flow

interface WaitlistRepository {
    fun observeForLottery(lotteryId: String): Flow<List<WaitlistEntry>>
    suspend fun getEntry(lotteryId: String, userId: String): WaitlistEntry?
    suspend fun save(entry: WaitlistEntry)
}
