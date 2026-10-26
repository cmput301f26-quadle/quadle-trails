// In-memory LotteryRepository seeded from TestLotteries.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.fake

import com.example.trails.data.model.Lottery
import com.example.trails.data.repository.LotteryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeLotteryRepository(initial: List<Lottery> = TestLotteries.all) : LotteryRepository {
    private val lotteries = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<Lottery>> = lotteries

    override fun observeByOwner(ownerId: String): Flow<List<Lottery>> =
        lotteries.map { list -> list.filter { it.ownerId == ownerId } }

    override suspend fun getById(id: String): Lottery? =
        lotteries.value.find { it.id == id }

    override suspend fun save(lottery: Lottery) {
        lotteries.update { list -> list.filterNot { it.id == lottery.id } + lottery }
    }
}
