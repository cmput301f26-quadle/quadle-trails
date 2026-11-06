// In-memory LotteryRepository seeded from TestLotteries.
// OWNER: Skanda (skandarajeev) — Lottery Setup
// Extend with the methods your stories need, and write the Firestore
//  implementation. Others: open a small PR and tag the owner.
package com.example.trails.data.fake

import com.example.trails.data.model.Lottery
import com.example.trails.data.repository.LotteryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [LotteryRepository] for tests and for teammates before Firestore is wired in.
 *
 * @param initial lotteries to start with; defaults to [TestLotteries.all].
 */
class FakeLotteryRepository(initial: List<Lottery> = TestLotteries.all) : LotteryRepository {
    private val lotteries = MutableStateFlow(initial)
    private var nextId = 1

    /** Set to make the next save or delete throw, so tests can check the failure paths. */
    var failure: Exception? = null

    override fun observeAll(): Flow<List<Lottery>> = lotteries

    override fun observeByOwner(ownerId: String): Flow<List<Lottery>> =
        lotteries.map { list -> list.filter { it.ownerId == ownerId } }

    override fun observeById(id: String): Flow<Lottery?> =
        lotteries.map { list -> list.find { it.id == id } }

    override suspend fun getById(id: String): Lottery? =
        lotteries.value.find { it.id == id }

    override suspend fun save(lottery: Lottery): String {
        failure?.let { failure = null; throw it }
        val toSave = if (lottery.id.isBlank()) lottery.copy(id = "fake-lottery-${nextId++}") else lottery
        lotteries.update { list -> list.filterNot { it.id == toSave.id } + toSave }
        return toSave.id
    }

    override suspend fun delete(id: String) {
        failure?.let { failure = null; throw it }
        lotteries.update { list -> list.filterNot { it.id == id } }
    }
}
