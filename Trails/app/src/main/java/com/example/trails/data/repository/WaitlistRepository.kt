// read and write access to waitlist entries under lotteries/{id}/entries
// owner: chitkarsh (chitkarshrathi) - waitlist
package com.example.trails.data.repository

import com.example.trails.data.model.WaitlistEntry
import kotlinx.coroutines.flow.Flow

// based on kotlin flow documentation for repository streaming
interface WaitlistRepository {
    // observes all entries for a lottery in real time
    fun observeForLottery(lotteryId: String): Flow<List<WaitlistEntry>>

    // observes one user's entry for a lottery in real time
    fun observeEntry(lotteryId: String, userId: String): Flow<WaitlistEntry?>

    // gets one entry for a user in a lottery
    suspend fun getEntry(lotteryId: String, userId: String): WaitlistEntry?

    // gets all entries for a lottery
    suspend fun getEntriesForLottery(lotteryId: String): List<WaitlistEntry>

    // counts entries for a lottery
    suspend fun getEntryCount(lotteryId: String): Int

    // saves or updates an entry
    suspend fun save(entry: WaitlistEntry)

    // deletes an entry by lottery and user id
    suspend fun deleteEntry(lotteryId: String, userId: String)

    // joins waitlist and checks rules acceptance plus capacity limits
    suspend fun joinWaitlist(
        lotteryId: String,
        userId: String,
        userName: String,
        partySize: Int = 1,
        rulesAccepted: Boolean = true,
        maxCapacity: Int? = null
    ): WaitlistEntry

    // leaves a waitlist
    suspend fun leaveWaitlist(lotteryId: String, userId: String)
}
