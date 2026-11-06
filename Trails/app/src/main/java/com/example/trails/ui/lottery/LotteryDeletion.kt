// Deleting a lottery nobody has joined yet, shared by the Edit and Details screens.
// OWNER: Skanda (skandarajeev) — Lottery Setup
// Outstanding issues: none.
package com.example.trails.ui.lottery

import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.LotteryRepository
import com.example.trails.data.repository.WaitlistRepository

/** What happened when the owner tried to delete a lottery. */
sealed interface DeleteResult {
    /** The lottery is gone and its campsites are free again. */
    data object Deleted : DeleteResult

    /** [entrants] people joined in the meantime, so nothing was deleted. */
    data class HasEntrants(val entrants: Int) : DeleteResult
}

/**
 * Deletes a lottery only if its waiting list is still empty, then frees every campsite that
 * pointed at it. The count is checked again here because someone may have joined since the
 * screen loaded. Repository errors are thrown to the caller.
 */
suspend fun deleteLotteryIfEmpty(
    lotteryId: String,
    lotteryRepository: LotteryRepository,
    campsiteRepository: CampsiteRepository,
    waitlistRepository: WaitlistRepository
): DeleteResult {
    val entrants = waitlistRepository.getEntryCount(lotteryId)
    if (entrants > 0) return DeleteResult.HasEntrants(entrants)
    val lottery = lotteryRepository.getById(lotteryId)
    lotteryRepository.delete(lotteryId)
    for (id in lottery?.campsiteIds.orEmpty()) {
        val campsite = campsiteRepository.getById(id) ?: continue
        if (campsite.activeLotteryId == lotteryId) campsiteRepository.save(campsite.copy(activeLotteryId = null))
    }
    return DeleteResult.Deleted
}
