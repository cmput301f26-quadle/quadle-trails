// Unit tests for lottery result notifications: wording, who gets one, and sending only once (US 01.03.01, 01.03.02).
// OWNER: Aarav (aarav-kapoor-cs) — Notifications
// Outstanding issues: None currently known.
// AI assistance: Used AI to help write tests for the win / lose notification rules.
package com.example.trails.ui.notification

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeLotteryRepository
import com.example.trails.data.fake.FakeNotificationRepository
import com.example.trails.data.fake.FakeUserRepository
import com.example.trails.data.fake.FakeWaitlistRepository
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Lottery
import com.example.trails.data.model.NotificationType
import com.example.trails.data.model.User
import com.example.trails.data.model.WaitlistEntry
import com.example.trails.data.model.WaitlistStatus
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/** Tests [lotteryResultNotification] and [LotteryResultNotifier] with in-memory repositories. */
class LotteryResultNotificationsTest {

    private val priya = TestUsers.ENTRANT_ID
    private val now = Timestamp(Date(1_800_000_000_000L))

    private val banffDraw = Lottery(
        id = "lottery-banff-may", name = "May long weekend",
        campsiteIds = listOf(TestCampsites.BANFF_ID), numWinners = 1, drawStatus = "COMPLETED"
    )
    private val rockiesDraw = Lottery(
        id = "lottery-rockies", name = "Summer Rockies",
        campsiteIds = listOf(TestCampsites.BANFF_ID, TestCampsites.JASPER_ID), drawStatus = "COMPLETED"
    )
    private val notDrawnYet = Lottery(
        id = "lottery-june", name = "June weekend",
        campsiteIds = listOf(TestCampsites.JASPER_ID), drawStatus = "PENDING"
    )

    private fun entry(lottery: Lottery, status: WaitlistStatus, userId: String = priya) =
        WaitlistEntry(userId = userId, userName = "Priya Sharma", lotteryId = lottery.id, status = status)

    private fun notifier(
        lotteries: List<Lottery>,
        entries: List<WaitlistEntry>,
        notifications: FakeNotificationRepository,
        users: List<User> = TestUsers.all
    ) = LotteryResultNotifier(
        FakeLotteryRepository(lotteries),
        FakeWaitlistRepository(entries),
        FakeCampsiteRepository(),
        FakeUserRepository(users),
        notifications,
        clock = { now }
    )

    @Test
    fun won_singleCampsite_namesTheCampsite() {
        val n = lotteryResultNotification(banffDraw, entry(banffDraw, WaitlistStatus.WON), "Tunnel Mountain Site 14", now)!!
        assertEquals(priya, n.recipient)
        assertEquals("You've won the lottery for Tunnel Mountain Site 14", n.title)
        assertEquals(NotificationType.LOTTERY_SINGLE, n.type)
        assertTrue(n.content.startsWith("May long weekend"))
        assertEquals(now, n.timestamp)
    }

    @Test
    fun won_multiCampsite_isAMultiSiteWin() {
        val n = lotteryResultNotification(rockiesDraw, entry(rockiesDraw, WaitlistStatus.WON), "Tunnel Mountain Site 14", now)!!
        assertEquals("You're a multi site winner!", n.title)
        assertEquals(NotificationType.LOTTERY_MULTI, n.type)
    }

    @Test
    fun lost_saysNotPicked() {
        val n = lotteryResultNotification(banffDraw, entry(banffDraw, WaitlistStatus.LOST), "Tunnel Mountain Site 14", now)!!
        assertEquals("Lottery not picked for Tunnel Mountain Site 14 this time", n.title)
        assertEquals(NotificationType.LOTTERY_LOST, n.type)
    }

    @Test
    fun otherStatuses_haveNoNotification() {
        for (status in listOf(WaitlistStatus.WAITING, WaitlistStatus.RESERVED, WaitlistStatus.DECLINED)) {
            assertNull(lotteryResultNotification(banffDraw, entry(banffDraw, status), "Tunnel Mountain Site 14", now))
        }
    }

    @Test
    fun unknownCampsite_fallsBackToTheLotteryName() {
        val n = lotteryResultNotification(banffDraw, entry(banffDraw, WaitlistStatus.WON), "", now)!!
        assertEquals("You've won the lottery for May long weekend", n.title)
    }

    @Test
    fun resultKey_isStablePerLotteryAndUser() {
        assertEquals("lottery-lottery-banff-may-device-entrant-001", lotteryResultKey(banffDraw.id, priya))
    }

    @Test
    fun sync_sendsWinAndLossForDrawnLotteries() = runTest {
        val notifications = FakeNotificationRepository(emptyList())
        val sent = notifier(
            listOf(banffDraw, rockiesDraw, notDrawnYet),
            listOf(
                entry(banffDraw, WaitlistStatus.WON),
                entry(rockiesDraw, WaitlistStatus.LOST),
                entry(notDrawnYet, WaitlistStatus.WAITING)
            ),
            notifications
        ).sync(priya)

        assertEquals(2, sent)
        val types = notifications.getNotif(priya).map { it.type }.toSet()
        assertEquals(setOf(NotificationType.LOTTERY_SINGLE, NotificationType.LOTTERY_LOST), types)
    }

    @Test
    fun sync_usesTheCampsiteNameFromTheRepository() = runTest {
        val notifications = FakeNotificationRepository(emptyList())
        notifier(listOf(banffDraw), listOf(entry(banffDraw, WaitlistStatus.WON)), notifications).sync(priya)
        assertEquals("You've won the lottery for Tunnel Mountain Site 14", notifications.getNotif(priya).single().title)
    }

    @Test
    fun sync_twice_neverRepeatsAResult() = runTest {
        val notifications = FakeNotificationRepository(emptyList())
        val notifier = notifier(listOf(banffDraw), listOf(entry(banffDraw, WaitlistStatus.WON)), notifications)
        assertEquals(1, notifier.sync(priya))
        assertEquals(0, notifier.sync(priya))
        assertEquals(1, notifications.getNotif(priya).size)
    }

    @Test
    fun sync_lotteryNotDrawn_sendsNothing() = runTest {
        val notifications = FakeNotificationRepository(emptyList())
        val sent = notifier(listOf(notDrawnYet), listOf(entry(notDrawnYet, WaitlistStatus.WAITING)), notifications).sync(priya)
        assertEquals(0, sent)
        assertTrue(notifications.getNotif(priya).isEmpty())
    }

    @Test
    fun sync_userNotOnTheWaitlist_sendsNothing() = runTest {
        val notifications = FakeNotificationRepository(emptyList())
        val sent = notifier(
            listOf(banffDraw), listOf(entry(banffDraw, WaitlistStatus.WON, userId = TestUsers.ADMIN_ID)), notifications
        ).sync(priya)
        assertEquals(0, sent)
    }

    @Test
    fun sync_optedOutUser_getsNothing() = runTest {
        val notifications = FakeNotificationRepository(emptyList())
        val users = TestUsers.all.map { if (it.deviceId == priya) it.copy(notificationsOptOut = true) else it }
        val sent = notifier(listOf(banffDraw), listOf(entry(banffDraw, WaitlistStatus.WON)), notifications, users).sync(priya)
        assertEquals(0, sent)
        assertTrue(notifications.getNotif(priya).isEmpty())
    }

    @Test
    fun sync_blankUser_sendsNothing() = runTest {
        val notifications = FakeNotificationRepository(emptyList())
        assertEquals(0, notifier(listOf(banffDraw), emptyList(), notifications).sync(""))
    }
}
