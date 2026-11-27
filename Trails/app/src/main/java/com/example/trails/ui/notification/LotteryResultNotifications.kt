// Creates the "You won" / "not picked" notifications once a lottery has been drawn (US 01.03.01, 01.03.02).
// OWNER: Aarav (aarav-kapoor-cs) — Notifications
// Plain Kotlin (no Compose), so it can be unit-tested.
// Outstanding issues: The notification is created the next time the user opens the app's Notifications
//  tab after the draw, not at the moment of the draw; there are no system push notifications.
// AI assistance: Used AI to help write the result wording rules and the send-once check.
package com.example.trails.ui.notification

import com.example.trails.data.model.Lottery
import com.example.trails.data.model.Notification
import com.example.trails.data.model.NotificationType
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.model.WaitlistStatus
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.LotteryRepository
import com.example.trails.data.repository.NotificationRepository
import com.example.trails.data.repository.UserRepository
import com.example.trails.data.repository.WaitlistRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/** Value of [Lottery.drawStatus] before the draw has started. */
const val DRAW_PENDING = "PENDING"

/**
 * The id a lottery result notification is stored under, so each entrant gets it only once.
 *
 * @param lotteryId the lottery that was drawn.
 * @param userId the entrant.
 */
fun lotteryResultKey(lotteryId: String, userId: String): String = "lottery-$lotteryId-$userId"

/**
 * The notification for one entrant's lottery result.
 *
 * @param lottery the lottery that was drawn.
 * @param entry the entrant's waitlist entry after the draw.
 * @param campsiteName the lottery's first campsite, or blank if unknown.
 * @param now when the notification is created.
 * @return a win notification for WON, a loss notification for LOST, and null for every
 *  other status (still waiting, or the entrant already reserved or declined).
 */
fun lotteryResultNotification(
    lottery: Lottery,
    entry: WaitlistEntry,
    campsiteName: String,
    now: Timestamp
): Notification? {
    val multi = lottery.campsiteIds.size > 1
    val place = (if (multi) lottery.name else campsiteName).ifBlank { lottery.name }.ifBlank { "your lottery" }
    val block = lottery.name.ifBlank { place }
    return when (entry.status) {
        WaitlistStatus.WON -> Notification(
            recipient = entry.userId,
            title = if (multi) "You're a multi site winner!" else "You've won the lottery for $place",
            content = if (multi) "$block\nYou won access to this lottery. Select which campsite and dates to book."
            else "$block\nYour next trip is one step closer. Select your dates to complete your booking.",
            timestamp = now,
            type = if (multi) NotificationType.LOTTERY_MULTI else NotificationType.LOTTERY_SINGLE,
            related = null
        )
        WaitlistStatus.LOST -> Notification(
            recipient = entry.userId,
            title = "Lottery not picked for $place this time",
            content = "$block\nYou were not selected for $place this time.",
            timestamp = now,
            type = NotificationType.LOTTERY_LOST,
            related = null
        )
        else -> null
    }
}

/**
 * Looks at the lotteries that have been drawn and sends the user their result for each
 * one they entered. Safe to run any number of times: each result is sent at most once.
 *
 * @param lotteries where lotteries are stored.
 * @param waitlist where waitlist entries (with the draw's WON / LOST status) are stored.
 * @param campsites used for the campsite's name.
 * @param users used to skip users who opted out of notifications (US 01.10.01).
 * @param notifications where notifications are stored.
 * @param clock the time stamped on a new notification; replaceable in tests.
 */
class LotteryResultNotifier(
    private val lotteries: LotteryRepository,
    private val waitlist: WaitlistRepository,
    private val campsites: CampsiteRepository,
    private val users: UserRepository,
    private val notifications: NotificationRepository,
    private val clock: () -> Timestamp = { Timestamp.now() }
) {

    /**
     * Sends any lottery results the user has not been told about yet.
     *
     * @param userId the user to check.
     * @return how many notifications were sent now.
     */
    suspend fun sync(userId: String): Int {
        if (userId.isBlank()) return 0
        if (users.getById(userId)?.notificationsOptOut == true) return 0

        var sent = 0
        val drawn = lotteries.observeAll().first().filter { it.drawStatus != DRAW_PENDING }
        for (lottery in drawn) {
            // One lottery failing (e.g. offline) must not stop the others.
            try {
                val entry = waitlist.getEntry(lottery.id, userId) ?: continue
                val campsiteName = lottery.campsiteIds.firstOrNull()?.let { campsites.getById(it)?.name }.orEmpty()
                val notification = lotteryResultNotification(lottery, entry, campsiteName, clock()) ?: continue
                if (notifications.sendOnce(lotteryResultKey(lottery.id, userId), notification)) sent++
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
        return sent
    }
}
