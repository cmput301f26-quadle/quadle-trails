// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.01.01 (03.02.01 will add profile removal here)
// PURPOSE: Removes things an administrator takes down, and everything that
//   depends on them, so no lottery or waitlist entry is left pointing at
//   something that no longer exists.
// Outstanding issues:
//   - Reservations and notifications to affected guests don't exist yet (Part 4).
//   - The removed campsite's ratings stay in storage.
//   - The steps are separate writes, not one Firestore batch, so a failure
//     part-way through can leave some lotteries already changed.
package com.example.trails.ui.admin

import com.example.trails.data.AppContainer
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.LotteryRepository
import com.example.trails.data.repository.WaitlistRepository
import kotlinx.coroutines.flow.first

/**
 * What removing a campsite takes with it, shown in the confirmation dialog.
 *
 * @property name the campsite's display name.
 * @property lotteryCount lotteries that will be deleted because this is their only campsite.
 * @property entryCount waitlist entries in those lotteries, deleted with them.
 */
data class CampsiteImpact(val name: String, val lotteryCount: Int, val entryCount: Int)

/**
 * Administrator removals. Plain class with no Android parts, so it can be unit-tested with fakes.
 *
 * @param campsiteRepository where campsites are stored.
 * @param lotteryRepository where lotteries are stored.
 * @param waitlistRepository where each lottery's waitlist entries are stored.
 */
class AdminRemoval(
    private val campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    private val lotteryRepository: LotteryRepository = AppContainer.lotteryRepository,
    private val waitlistRepository: WaitlistRepository = AppContainer.waitlistRepository
) {
    /**
     * Counts what [removeCampsite] would delete, without changing anything.
     *
     * @param campsiteId id of the campsite the admin is about to remove.
     * @return the campsite's name with the number of lotteries and waitlist entries that
     *   go with it, or null if the campsite doesn't exist.
     */
    suspend fun campsiteImpact(campsiteId: String): CampsiteImpact? {
        val campsite = campsiteRepository.getById(campsiteId) ?: return null
        val doomed = lotteriesFor(campsiteId).filter { it.campsiteIds.size == 1 }
        val entries = doomed.sumOf { waitlistRepository.getEntryCount(it.id) }
        return CampsiteImpact(campsite.name, doomed.size, entries)
    }

    /**
     * Removes a campsite for everyone (US 03.01.01). Each lottery covering it either loses
     * it from [com.example.trails.data.model.Lottery.campsiteIds], or, if it was the
     * lottery's only campsite, is deleted together with every waitlist entry in it.
     * The campsite itself is deleted last.
     *
     * @param campsiteId id of the campsite to remove.
     * @return success, or a failure if the campsite doesn't exist or a write fails.
     */
    suspend fun removeCampsite(campsiteId: String): Result<Unit> = runCatching {
        campsiteRepository.getById(campsiteId)
            ?: throw IllegalArgumentException("Campsite not found")
        for (lottery in lotteriesFor(campsiteId)) {
            if (lottery.campsiteIds.size == 1) {
                waitlistRepository.getEntriesForLottery(lottery.id).forEach {
                    waitlistRepository.deleteEntry(lottery.id, it.userId)
                }
                lotteryRepository.delete(lottery.id)
            } else {
                lotteryRepository.save(lottery.copy(campsiteIds = lottery.campsiteIds - campsiteId))
            }
        }
        campsiteRepository.delete(campsiteId)
    }

    /** Every lottery whose campsiteIds include [campsiteId]. */
    private suspend fun lotteriesFor(campsiteId: String) =
        lotteryRepository.observeAll().first().filter { campsiteId in it.campsiteIds }
}
