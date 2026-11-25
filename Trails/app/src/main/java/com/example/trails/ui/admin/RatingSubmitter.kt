// Saves a user's campsite rating and keeps the campsite's average and count in sync (US 01.13.01).
// syncRatingStats is also used by AdminRemoval.removeRating (US 03.09.01).
// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// Outstanding issues: the rating and the campsite are saved in two steps,
//   not one Firestore transaction, so two people rating at the same moment
//   could briefly leave a stale average.
//   AppContainer still uses FakeRatingRepository; FirestoreRatingRepository isn't switched on yet.
package com.example.trails.ui.admin

import com.example.trails.data.AppContainer
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.RatingRepository
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Rating
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first

/**
 * Submits ratings. Plain class with no Android parts, so it can be unit-tested with fakes.
 *
 * @param ratingRepository where ratings are stored.
 * @param campsiteRepository where campsites (and their ratingAverage/ratingCount) are stored.
 */
class RatingSubmitter(
    private val ratingRepository: RatingRepository = AppContainer.ratingRepository,
    private val campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository
) {
    /**
     * Saves [userId]'s rating of a campsite, replacing any earlier one by the same user,
     * then updates only the campsite's ratingAverage and ratingCount.
     *
     * @param campsiteId id of the campsite being rated.
     * @param userId device id of the user rating it.
     * @param score star score, 1 to 5.
     * @param review written review; trimmed and cut to [MAX_REVIEW_LENGTH] characters.
     * @return the campsite's new stats, or a failure if the score is invalid, the campsite
     *   doesn't exist, or the user isn't allowed to rate it. Nothing is saved on failure.
     */
    suspend fun submit(
        campsiteId: String,
        userId: String,
        score: Int,
        review: String
    ): Result<RatingStats> {
        if (!isValidScore(score)) {
            return Result.failure(IllegalArgumentException("Score must be between 1 and 5"))
        }
        val campsite = campsiteRepository.getById(campsiteId)
            ?: return Result.failure(IllegalArgumentException("Campsite not found"))
        if (!canRate(userId, campsite)) {
            return Result.failure(IllegalStateException("You can't rate this campsite"))
        }

        ratingRepository.save(
            Rating(
                userId = userId,
                campsiteId = campsiteId,
                score = score,
                review = review.trim().take(MAX_REVIEW_LENGTH),
                createdAt = Timestamp.now()
            )
        )

        return Result.success(syncRatingStats(campsite, ratingRepository, campsiteRepository))
    }
}

/**
 * Recalculates a campsite's ratingAverage and ratingCount from the ratings stored for it now,
 * and saves the campsite with only those two fields changed. Shared by rating submission
 * (US 01.13.01) and admin rating removal (US 03.09.01) so both keep the stats the same way.
 *
 * @param campsite the campsite as last read; every field except the two stats is kept.
 * @param ratingRepository where the campsite's ratings are read from.
 * @param campsiteRepository where the updated campsite is saved.
 * @return the campsite's new stats.
 */
internal suspend fun syncRatingStats(
    campsite: Campsite,
    ratingRepository: RatingRepository,
    campsiteRepository: CampsiteRepository
): RatingStats {
    val stats = ratingStats(ratingRepository.observeForCampsite(campsite.id).first())
    campsiteRepository.save(campsite.copy(ratingAverage = stats.average, ratingCount = stats.count))
    return stats
}
