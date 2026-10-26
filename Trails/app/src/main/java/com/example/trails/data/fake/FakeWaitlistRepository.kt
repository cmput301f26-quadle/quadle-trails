// In-memory WaitlistRepository seeded from TestWaitlistEntries.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.fake

import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.repository.WaitlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeWaitlistRepository(
    initial: List<WaitlistEntry> = TestWaitlistEntries.all
) : WaitlistRepository {
    private val entries = MutableStateFlow(initial)

    override fun observeForLottery(lotteryId: String): Flow<List<WaitlistEntry>> =
        entries.map { list -> list.filter { it.lotteryId == lotteryId } }

    override suspend fun getEntry(lotteryId: String, userId: String): WaitlistEntry? =
        entries.value.find { it.lotteryId == lotteryId && it.userId == userId }

    override suspend fun save(entry: WaitlistEntry) {
        entries.update { list ->
            list.filterNot { it.lotteryId == entry.lotteryId && it.userId == entry.userId } + entry
        }
    }
}
