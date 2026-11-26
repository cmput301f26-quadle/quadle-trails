// Unit tests for New notification and Sent notifications ViewModels (US 02.10.01, 02.10.02).
// OWNER: Aarav (aarav-kapoor-cs) — Owner notifications
// Outstanding issues: None currently known.
// AI assistance: Used AI to help write coroutine-based tests for sending and the sent history.
package com.example.trails.ui.ownernotify

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeLotteryRepository
import com.example.trails.data.fake.FakeNotificationRepository
import com.example.trails.data.fake.FakeSentNotificationRepository
import com.example.trails.data.fake.FakeWaitlistRepository
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.model.Lottery
import com.example.trails.data.model.NotificationType
import com.example.trails.data.model.SentNotification
import com.example.trails.data.model.WaitlistEntry
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.Date

/** Tests [ComposeNotificationViewModel], [SentNotificationsViewModel] and [SentNotificationDetailsViewModel] with fakes. */
@OptIn(ExperimentalCoroutinesApi::class)
class OwnerNotifyViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val owner = "device-owner-001"
    private val now = 1_800_000_000_000L
    private val june = Lottery(id = "june", ownerId = owner, name = "June 2027", campsiteIds = listOf(TestCampsites.BANFF_ID))
    private val entries = listOf(
        WaitlistEntry(userId = "maya", userName = "Maya Singh", lotteryId = "june", partySize = 2, joinedAt = Timestamp(Date(now - 3000))),
        WaitlistEntry(userId = "daniel", userName = "Daniel Kim", lotteryId = "june", partySize = 4, joinedAt = Timestamp(Date(now - 2000))),
        WaitlistEntry(userId = "lena", userName = "Lena Fischer", lotteryId = "june", partySize = 3, joinedAt = Timestamp(Date(now - 1000)))
    )

    private lateinit var notifications: FakeNotificationRepository
    private lateinit var sent: FakeSentNotificationRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        notifications = FakeNotificationRepository(emptyList())
        sent = FakeSentNotificationRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun compose(waitlist: List<WaitlistEntry> = entries, lotteryId: String = "june") = ComposeNotificationViewModel(
        lotteryId = lotteryId,
        ownerId = owner,
        lotteryRepository = FakeLotteryRepository(listOf(june)),
        campsiteRepository = FakeCampsiteRepository(),
        waitlistRepository = FakeWaitlistRepository(waitlist),
        notificationRepository = notifications,
        sentRepository = sent,
        clock = { Timestamp(Date(now)) }
    )

    @Test
    fun load_showsEveryEntrantAndTheCampsite() = runTest {
        val vm = compose()
        advanceUntilIdle()
        val ready = vm.uiState.value as ComposeUiState.Ready
        assertEquals(listOf("maya", "daniel", "lena"), ready.recipients.map { it.userId })
        assertEquals("Tunnel Mountain Site 14", ready.campsiteName)
    }

    @Test
    fun load_unknownLottery_isNotFound() = runTest {
        val vm = compose(lotteryId = "missing")
        advanceUntilIdle()
        assertEquals(ComposeUiState.NotFound, vm.uiState.value)
    }

    @Test
    fun send_notifiesEveryEntrantAndKeepsTheOwnersCopy() = runTest {
        val vm = compose()
        advanceUntilIdle()
        vm.onSubjectChange("Lottery draw date update")
        vm.onMessageChange("The June draw is now on May 24, 2027.")
        vm.onSend()
        advanceUntilIdle()

        assertEquals(SendState.Sent("Lottery draw date update", 3, "Tunnel Mountain Site 14 · June 2027 waitlist"), vm.sendState.value)
        for (id in listOf("maya", "daniel", "lena")) {
            val received = notifications.getNotif(id).single()
            assertEquals("Lottery draw date update", received.title)
            assertEquals(NotificationType.OWNER_MESSAGE, received.type)
        }
        val history = sent.getById("sent-1")!!
        assertEquals(owner, history.ownerId)
        assertEquals(3, history.recipients.size)
    }

    @Test
    fun send_invalidForm_showsErrorsAndSendsNothing() = runTest {
        val vm = compose()
        advanceUntilIdle()
        vm.onSend()
        advanceUntilIdle()

        assertEquals(NotifyFieldError.SUBJECT_REQUIRED, vm.errors.value.subject)
        assertEquals(SendState.Idle, vm.sendState.value)
        assertTrue(notifications.getNotif("maya").isEmpty())
    }

    @Test
    fun send_emptyWaitlist_sendsNothing() = runTest {
        val vm = compose(waitlist = emptyList())
        advanceUntilIdle()
        vm.onSubjectChange("Hello")
        vm.onMessageChange("Hi")
        vm.onSend()
        advanceUntilIdle()
        assertEquals(SendState.Idle, vm.sendState.value)
        assertEquals(null, sent.getById("sent-1"))
    }

    @Test
    fun send_saveFails_reportsErrorAndNobodyIsNotified() = runTest {
        sent.failure = IOException("offline")
        val vm = compose()
        advanceUntilIdle()
        vm.onSubjectChange("Hello")
        vm.onMessageChange("Hi")
        vm.onSend()
        advanceUntilIdle()

        assertEquals(SendState.Failed("offline"), vm.sendState.value)
        assertTrue(notifications.getNotif("maya").isEmpty())
    }

    @Test
    fun message_isCutAtTheLimit() = runTest {
        val vm = compose()
        vm.onMessageChange("x".repeat(MAX_MESSAGE + 20))
        assertEquals(MAX_MESSAGE, vm.form.value.message.length)
    }

    @Test
    fun sentList_showsOnlyThisOwnersNewestFirst_andFilters() = runTest {
        sent.save(SentNotification(ownerId = owner, subject = "Old", sentAt = Timestamp(Date(now - 20 * 86_400_000L))))
        sent.save(SentNotification(ownerId = owner, subject = "New", sentAt = Timestamp(Date(now))))
        sent.save(SentNotification(ownerId = "someone-else", subject = "Not mine", sentAt = Timestamp(Date(now))))
        val vm = SentNotificationsViewModel(owner, sent) { now }
        advanceUntilIdle()
        assertEquals(listOf("New", "Old"), vm.state.value.items.map { it.subject })

        vm.onFilterChange(SentFilter.WEEK)
        advanceUntilIdle()
        assertEquals(listOf("New"), vm.state.value.items.map { it.subject })
    }

    @Test
    fun sentDetails_loadsOrNotFound() = runTest {
        val id = sent.save(SentNotification(ownerId = owner, subject = "Lottery is now open"))
        val vm = SentNotificationDetailsViewModel(id, sent)
        advanceUntilIdle()
        assertEquals("Lottery is now open", (vm.state.value as SentDetailsUiState.Loaded).notification.subject)

        val missing = SentNotificationDetailsViewModel("missing", sent)
        advanceUntilIdle()
        assertEquals(SentDetailsUiState.NotFound, missing.state.value)
    }
}
