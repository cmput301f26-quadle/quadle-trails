// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.01.01, 03.02.01
// PURPOSE: Removes campsites and user profiles an administrator takes down,
//   and everything that depends on them, so no lottery or waitlist entry is
//   left pointing at something that no longer exists.
// Outstanding issues:
//   - Reservations and notifications to affected guests don't exist yet (Part 4).
//   - The removed campsite's ratings stay in storage.
//   - The removed user's ratings stay in storage.
//   - The steps are separate writes, not one Firestore batch, so a failure
//     part-way through can leave some lotteries already changed.
package com.example.trails.ui.admin

import com.example.trails.data.AppContainer
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.LotteryRepository
import com.example.trails.data.repository.UserRepository
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
 * What removing a user profile takes with it, shown in the confirmation dialog.
 *
 * @property name the user's display name.
 * @property campsiteCount campsites the user owns, removed with them.
 * @property entryCount the user's own waitlist entries across all lotteries.
 */
data class ProfileImpact(val name: String, val campsiteCount: Int, val entryCount: Int)

/**
 * Administrator removals. Plain class with no Android parts, so it can be unit-tested with fakes.
 *
 * @param campsiteRepository where campsites are stored.
 * @param lotteryRepository where lotteries are stored.
 * @param waitlistRepository where each lottery's waitlist entries are stored.
 * @param userRepository where user profiles are stored.
 */
class AdminRemoval(
    private val campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    private val lotteryRepository: LotteryRepository = AppContainer.lotteryRepository,
    private val waitlistRepository: WaitlistRepository = AppContainer.waitlistRepository,
    private val userRepository: UserRepository = AppContainer.userRepository
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

    /**
     * Counts what [removeProfile] would delete, without changing anything.
     *
     * @param userId device id of the user the admin is about to remove.
     * @return the user's name with the number of campsites they own and their waitlist
     *   entries across all lotteries, or null if the user doesn't exist.
     */
    suspend fun profileImpact(userId: String): ProfileImpact? {
        val user = userRepository.getById(userId) ?: return null
        val campsites = campsiteRepository.observeByOwner(userId).first()
        val entries = lotteryRepository.observeAll().first()
            .count { waitlistRepository.getEntry(it.id, userId) != null }
        return ProfileImpact(user.name, campsites.size, entries)
    }

    /**
     * Removes a user profile for everyone (US 03.02.01). Each campsite the user owns is
     * removed with [removeCampsite], then the user's waitlist entries in every lottery are
     * deleted, and the profile itself is deleted last.
     *
     * @param userId device id of the user to remove.
     * @param currentUserId device id of the admin doing the removal; an admin can't remove
     *   their own profile.
     * @return success, or a failure (with nothing removed) if [userId] is the admin's own id
     *   or the user doesn't exist, or a failure if a write fails.
     */
    suspend fun removeProfile(userId: String, currentUserId: String): Result<Unit> = runCatching {
        require(userId != currentUserId) { "An admin can't remove their own profile" }
        userRepository.getById(userId) ?: throw IllegalArgumentException("User not found")
        for (campsite in campsiteRepository.observeByOwner(userId).first()) {
            removeCampsite(campsite.id).getOrThrow()
        }
        for (lottery in lotteryRepository.observeAll().first()) {
            if (waitlistRepository.getEntry(lottery.id, userId) != null) {
                waitlistRepository.deleteEntry(lottery.id, userId)
            }
        }
        userRepository.delete(userId)
    }

    /** Every lottery whose campsiteIds include [campsiteId]. */
    private suspend fun lotteriesFor(campsiteId: String) =
        lotteryRepository.observeAll().first().filter { campsiteId in it.campsiteIds }
}
