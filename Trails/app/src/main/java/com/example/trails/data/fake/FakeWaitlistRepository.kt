// in memory waitlist repository seeded from test entries
// implements US 01.01.06, US 01.02.01, US 01.02.02, US 01.04.03, US 02.04.02, US 02.09.01, US 02.11.01, and Bonus Rolling Lottery
// outstanding issues: none
// owner: chitkarsh (chitkarshrathi) - waitlist & booking
// used antigravity to scaffold this in-memory test fake
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

    // optional exception to test error edge cases
    var failure: Exception? = null

    private fun checkFailure() {
        failure?.let { throw it }
    }

    override fun observeForLottery(lotteryId: String): Flow<List<WaitlistEntry>> {
        checkFailure()
        return entries.map { list ->
            list.filter { it.lotteryId == lotteryId }
                .sortedWith(compareBy(nullsLast()) { it.joinedAt?.toDate() })
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
            .sortedWith(compareBy(nullsLast()) { it.joinedAt?.toDate() })
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

    override suspend fun updateStatus(
        lotteryId: String,
        userId: String,
        status: WaitlistStatus
    ): WaitlistEntry? {
        checkFailure()
        val entry = getEntry(lotteryId, userId) ?: return null
        val updated = entry.copy(status = status)
        save(updated)
        return updated
    }

    override suspend fun drawNextReplacement(lotteryId: String): WaitlistEntry? {
        checkFailure()
        val waitingEntry = entries.value
            .filter { it.lotteryId == lotteryId && it.status == WaitlistStatus.WAITING }
            .minByOrNull { it.joinedAt?.toDate() ?: Date(Long.MAX_VALUE) }
            ?: return null

        val promoted = waitingEntry.copy(status = WaitlistStatus.WON)
        save(promoted)
        return promoted
    }

    override suspend fun autoRollOnDecline(lotteryId: String, userId: String): WaitlistEntry? {
        checkFailure()
        updateStatus(lotteryId, userId, WaitlistStatus.DECLINED)
        return drawNextReplacement(lotteryId)
    }

    override suspend fun reconcileExpiredInvites(
        lotteryId: String,
        expiredUserIds: List<String>
    ): List<WaitlistEntry> {
        checkFailure()
        val replacements = mutableListOf<WaitlistEntry>()
        for (userId in expiredUserIds) {
            updateStatus(lotteryId, userId, WaitlistStatus.DECLINED)
            drawNextReplacement(lotteryId)?.let { replacements.add(it) }
        }
        return replacements
    }
}
