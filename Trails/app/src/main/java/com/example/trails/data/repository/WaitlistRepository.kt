// Read/write access to waitlist entries, which live under lotteries/{id}/entries.
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
// FIRESTORE IMPLEMENTATION: Chitkarsh (chitkarshrathi)
package com.example.trails.data.repository

import com.example.trails.data.model.WaitlistEntry
import kotlinx.coroutines.flow.Flow

interface WaitlistRepository {
    /** Observes all waitlist entries for the specified lottery in real time. */
    fun observeForLottery(lotteryId: String): Flow<List<WaitlistEntry>>

    /** Observes a specific user's waitlist entry for the given lottery in real time. */
    fun observeEntry(lotteryId: String, userId: String): Flow<WaitlistEntry?>

    /** Gets a single entry for a user in a lottery, or null if not found. */
    suspend fun getEntry(lotteryId: String, userId: String): WaitlistEntry?

    /** Gets all entries for a lottery at a point in time. */
    suspend fun getEntriesForLottery(lotteryId: String): List<WaitlistEntry>

    /** Gets the count of entrants on the waitlist for a lottery. */
    suspend fun getEntryCount(lotteryId: String): Int

    /** Saves or updates a waitlist entry. */
    suspend fun save(entry: WaitlistEntry)

    /** Removes a user from a lottery waitlist (US 01.02.02). */
    suspend fun deleteEntry(lotteryId: String, userId: String)

    /**
     * Joins a waitlist, checking capacity and requiring rules agreement (US 01.02.01, 02.11.01).
     *
     * @throws WaitlistFullException if maxCapacity is reached and user is not already on waitlist.
     * @throws RulesNotAcceptedException if rulesAccepted is false.
     * @throws IllegalArgumentException if required fields are missing or invalid.
     */
    suspend fun joinWaitlist(
        lotteryId: String,
        userId: String,
        userName: String,
        partySize: Int = 1,
        rulesAccepted: Boolean = true,
        maxCapacity: Int? = null
    ): WaitlistEntry

    /** Leaves a lottery waitlist (US 01.02.02). */
    suspend fun leaveWaitlist(lotteryId: String, userId: String)
}
