// In-memory WaitlistRepository seeded from TestWaitlistEntries.
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
package com.example.trails.data.fake

import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.model.WaitlistStatus
import com.example.trails.data.repository.RulesNotAcceptedException
import com.example.trails.data.repository.WaitlistFullException
import com.example.trails.data.repository.WaitlistRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.Date

class FakeWaitlistRepository(
    initial: List<WaitlistEntry> = TestWaitlistEntries.all
) : WaitlistRepository {

    private val entries = MutableStateFlow(initial)

    /** Optional exception to throw on repository operations, useful for testing error edge cases. */
    var failure: Exception? = null

    private fun checkFailure() {
        failure?.let { throw it }
    }

    override fun observeForLottery(lotteryId: String): Flow<List<WaitlistEntry>> {
        checkFailure()
        return entries.map { list ->
            list.filter { it.lotteryId == lotteryId }
                .sortedBy { it.joinedAt?.toDate() }
        }
    }

    override fun observeEntry(lotteryId: String, userId: String): Flow<WaitlistEntry?> {
        checkFailure()
        return entries.map { list ->
            list.find { it.lotteryId == lotteryId && it.userId == userId }
        }
    }

    override suspend fun getEntry(lotteryId: String, userId: String): WaitlistEntry? {
        checkFailure()
        return entries.value.find { it.lotteryId == lotteryId && it.userId == userId }
    }

    override suspend fun getEntriesForLottery(lotteryId: String): List<WaitlistEntry> {
        checkFailure()
        return entries.value.filter { it.lotteryId == lotteryId }
            .sortedBy { it.joinedAt?.toDate() }
    }

    override suspend fun getEntryCount(lotteryId: String): Int {
        checkFailure()
        return entries.value.count { it.lotteryId == lotteryId }
    }

    override suspend fun save(entry: WaitlistEntry) {
        checkFailure()
        val errors = entry.validate()
        require(errors.isEmpty()) { "Invalid waitlist entry: $errors" }
        entries.update { list ->
            list.filterNot { it.lotteryId == entry.lotteryId && it.userId == entry.userId } + entry
        }
    }

    override suspend fun deleteEntry(lotteryId: String, userId: String) {
        checkFailure()
        entries.update { list ->
            list.filterNot { it.lotteryId == lotteryId && it.userId == userId }
        }
    }

    override suspend fun joinWaitlist(
        lotteryId: String,
        userId: String,
        userName: String,
        partySize: Int,
        rulesAccepted: Boolean,
        maxCapacity: Int?
    ): WaitlistEntry {
        checkFailure()
        if (lotteryId.isBlank()) throw IllegalArgumentException("Lottery ID cannot be blank.")
        if (userId.isBlank()) throw IllegalArgumentException("User ID cannot be blank.")
        if (partySize < 1) throw IllegalArgumentException("Party size must be at least 1.")
        if (!rulesAccepted) throw RulesNotAcceptedException("You must accept the campsite rules before joining the waitlist.")

        val existing = getEntry(lotteryId, userId)
        if (existing == null && maxCapacity != null) {
            val currentCount = getEntryCount(lotteryId)
            if (currentCount >= maxCapacity) {
                throw WaitlistFullException("The waitlist for this lottery has reached maximum capacity ($maxCapacity).")
            }
        }

        val now = Timestamp(Date())
        val entry = WaitlistEntry(
            userId = userId,
            userName = userName,
            lotteryId = lotteryId,
            partySize = partySize,
            joinedAt = existing?.joinedAt ?: now,
            status = existing?.status ?: WaitlistStatus.WAITING,
            rulesAcceptedAt = now,
            rulesAccepted = true
        )
        save(entry)
        return entry
    }

    override suspend fun leaveWaitlist(lotteryId: String, userId: String) {
        checkFailure()
        deleteEntry(lotteryId, userId)
    }
}
