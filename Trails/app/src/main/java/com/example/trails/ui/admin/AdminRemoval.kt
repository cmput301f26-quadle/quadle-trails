// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.01.01, 03.02.01, 03.07.01, 03.09.01, 03.10.01
// PURPOSE: Removes campsites and user profiles an administrator takes down,
//   and everything that depends on them, so no lottery or waitlist entry is
//   left pointing at something that no longer exists. Also closes policy
//   reports when the admin removes a reported owner or dismisses the reports,
//   and removes single ratings that break app policy, keeping the campsite's
//   average and count in step, or only a rating's review text, keeping its stars.
// Outstanding issues:
//   - Guests with reservations aren't notified and reservations aren't cancelled
//     (needs reservations and notifications, Part 4).
//   - The removed user's ratings stay in storage.
//   - The steps are separate writes, not one Firestore batch, so a failure
//     part-way through can leave some lotteries already changed, some of a
//     removed campsite's ratings already deleted (or a rating deleted before
//     the campsite's average is saved).
//   - AppContainer still uses FakeRatingRepository; FirestoreRatingRepository
//     isn't switched on until ratings are seeded in Firestore.
package com.example.trails.ui.admin

import com.example.trails.data.AppContainer
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.model.ReportStatus
import com.example.trails.data.repository.LotteryRepository
import com.example.trails.data.repository.RatingRepository
import com.example.trails.data.repository.ReportRepository
import com.example.trails.data.repository.UserRepository
import com.example.trails.data.repository.WaitlistRepository
import kotlinx.coroutines.flow.first

/**
 * What removing a campsite takes with it, shown in the confirmation dialog.
 *
 * @property name the campsite's display name.
 * @property lotteryCount lotteries that will be deleted because this is their only campsite.
 * @property entryCount waitlist entries in those lotteries, deleted with them.
 * @property ratingCount the campsite's ratings, deleted with it.
 */
data class CampsiteImpact(val name: String, val lotteryCount: Int, val entryCount: Int, val ratingCount: Int = 0)

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
 * @param reportRepository where policy reports against owners are stored.
 * @param ratingRepository where campsite ratings are stored.
 */
class AdminRemoval(
    private val campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    private val lotteryRepository: LotteryRepository = AppContainer.lotteryRepository,
    private val waitlistRepository: WaitlistRepository = AppContainer.waitlistRepository,
    private val userRepository: UserRepository = AppContainer.userRepository,
    private val reportRepository: ReportRepository = AppContainer.reportRepository,
    private val ratingRepository: RatingRepository = AppContainer.ratingRepository
) {
    /**
     * Counts what [removeCampsite] would delete, without changing anything.
     *
     * @param campsiteId id of the campsite the admin is about to remove.
     * @return the campsite's name with the number of lotteries, waitlist entries and ratings
     *   that go with it, or null if the campsite doesn't exist.
     */
    suspend fun campsiteImpact(campsiteId: String): CampsiteImpact? {
        val campsite = campsiteRepository.getById(campsiteId) ?: return null
        val doomed = lotteriesFor(campsiteId).filter { it.campsiteIds.size == 1 }
        val entries = doomed.sumOf { waitlistRepository.getEntryCount(it.id) }
        val ratings = ratingRepository.observeForCampsite(campsiteId).first().size
        return CampsiteImpact(campsite.name, doomed.size, entries, ratings)
    }

    /**
     * Removes a campsite for everyone (US 03.01.01). Each lottery covering it either loses
     * it from [com.example.trails.data.model.Lottery.campsiteIds], or, if it was the
     * lottery's only campsite, is deleted together with every waitlist entry in it.
     * Then every rating of the campsite is deleted, and the campsite itself is deleted last.
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
        ratingRepository.deleteForCampsite(campsiteId)
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

    /**
     * Removes a reported campsite owner after policy review (US 03.07.01): the owner is removed
     * with [removeProfile], then their OPEN reports are marked OWNER_REMOVED.
     *
     * @param ownerId device id of the reported owner.
     * @param currentUserId device id of the admin doing the removal.
     * @return success, or the failure from [removeProfile], in which case the reports stay OPEN;
     *   or a failure if updating the reports fails.
     */
    suspend fun removeOwner(ownerId: String, currentUserId: String): Result<Unit> = runCatching {
        removeProfile(ownerId, currentUserId).getOrThrow()
        reportRepository.setStatusForOwner(ownerId, ReportStatus.OPEN, ReportStatus.OWNER_REMOVED)
    }

    /**
     * Dismisses every OPEN report against one owner after policy review found no violation
     * (US 03.07.01). The owner, their campsites and their entries are left as they are.
     *
     * @param ownerId device id of the reported owner.
     * @return success, or a failure if the write fails.
     */
    suspend fun dismissReports(ownerId: String): Result<Unit> = runCatching {
        reportRepository.setStatusForOwner(ownerId, ReportStatus.OPEN, ReportStatus.DISMISSED)
    }

    /**
     * Removes one rating that violates app policy (US 03.09.01), then recalculates the
     * campsite's ratingAverage and ratingCount from the ratings left and saves the campsite
     * with only those two fields changed.
     *
     * @param campsiteId id of the rated campsite.
     * @param userId device id of the reviewer whose rating is removed.
     * @return success, or a failure (with nothing changed) if the campsite or the rating
     *   doesn't exist, or a failure if a write fails.
     */
    suspend fun removeRating(campsiteId: String, userId: String): Result<Unit> = runCatching {
        val campsite = campsiteRepository.getById(campsiteId)
            ?: throw IllegalArgumentException("Campsite not found")
        ratingRepository.getRating(campsiteId, userId)
            ?: throw IllegalArgumentException("Rating not found")
        ratingRepository.delete(campsiteId, userId)
        syncRatingStats(campsite, ratingRepository, campsiteRepository)
    }

    /**
     * Removes the written review of one rating that violates app policy (US 03.10.01). The
     * rating is saved again with an empty review and reviewRemoved set; its score and createdAt
     * stay, so the campsite's ratingAverage and ratingCount don't change and aren't rewritten.
     *
     * @param campsiteId id of the rated campsite.
     * @param userId device id of the reviewer whose review text is removed.
     * @return success, or a failure (with nothing changed) if the rating doesn't exist or has
     *   no review text, or a failure if the write fails.
     */
    suspend fun removeReview(campsiteId: String, userId: String): Result<Unit> = runCatching {
        val rating = ratingRepository.getRating(campsiteId, userId)
            ?: throw IllegalArgumentException("Rating not found")
        require(rating.review.isNotBlank()) { "Rating has no review text" }
        ratingRepository.save(rating.copy(review = "", reviewRemoved = true))
    }

    /** Every lottery whose campsiteIds include [campsiteId]. */
    private suspend fun lotteriesFor(campsiteId: String) =
        lotteryRepository.observeAll().first().filter { campsiteId in it.campsiteIds }
}
