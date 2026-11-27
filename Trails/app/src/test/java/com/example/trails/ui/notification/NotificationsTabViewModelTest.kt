// Unit tests for the Notifications tab's ViewModel and the in-memory notification store
// (US 01.03.01, 01.03.02, 01.10.01).
// OWNER: Aarav (aarav-kapoor-cs) — Notifications
// Outstanding issues: None currently known.
// AI assistance: Used AI to help write coroutine-based tests for the live notification list.
package com.example.trails.ui.notification

import com.example.trails.data.fake.FakeNotificationRepository
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Notification
import com.example.trails.data.model.NotificationType
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.Date

/** Tests [NotificationsTabViewModel] and [FakeNotificationRepository]. */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsTabViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val priya = TestUsers.ENTRANT_ID

    private fun note(title: String, atMs: Long, recipient: String = priya) = Notification(
        recipient = recipient, title = title, timestamp = Timestamp(Date(atMs)), type = NotificationType.OWNER_MESSAGE
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsLoading_thenShowsThisUsersNotificationsNewestFirst() = runTest {
        val repo = FakeNotificationRepository(
            listOf(note("Older", 1_000), note("Newer", 2_000), note("Someone else's", 3_000, TestUsers.OWNER_ID))
        )
        val vm = NotificationsTabViewModel(priya, repo, syncLotteryResults = {})
        assertEquals(NotificationsUiState.Loading, vm.uiState.value)

        advanceUntilIdle()
        val loaded = vm.uiState.value as NotificationsUiState.Loaded
        assertEquals(listOf("Newer", "Older"), loaded.items.map { it.title })
    }

    @Test
    fun noNotifications_isLoadedAndEmpty() = runTest {
        val vm = NotificationsTabViewModel(priya, FakeNotificationRepository(emptyList()), syncLotteryResults = {})
        advanceUntilIdle()
        assertEquals(NotificationsUiState.Loaded(emptyList()), vm.uiState.value)
    }

    @Test
    fun opening_checksForLotteryResults_andTheyAppear() = runTest {
        val repo = FakeNotificationRepository(emptyList())
        val synced = mutableListOf<String>()
        val vm = NotificationsTabViewModel(priya, repo, syncLotteryResults = { id ->
            synced.add(id)
            repo.sendOnce("lottery-banff-$id", note("You've won the lottery for Tunnel Mountain Site 14", 5_000))
        })
        advanceUntilIdle()

        assertEquals(listOf(priya), synced)
        val loaded = vm.uiState.value as NotificationsUiState.Loaded
        assertEquals("You've won the lottery for Tunnel Mountain Site 14", loaded.items.single().title)
    }

    @Test
    fun resultCheckFailing_stillShowsStoredNotifications() = runTest {
        val repo = FakeNotificationRepository(listOf(note("Reservation Confirmed", 1_000)))
        val vm = NotificationsTabViewModel(priya, repo, syncLotteryResults = { throw IOException("offline") })
        advanceUntilIdle()
        assertEquals(1, (vm.uiState.value as NotificationsUiState.Loaded).items.size)
    }

    @Test
    fun loadFailure_showsError_thenRetryRecovers() = runTest {
        val repo = FakeNotificationRepository(listOf(note("Reservation Confirmed", 1_000)))
        repo.failure = IOException("offline")
        val vm = NotificationsTabViewModel(priya, repo, syncLotteryResults = {})
        advanceUntilIdle()
        assertEquals(NotificationsUiState.Error("offline"), vm.uiState.value)

        repo.failure = null
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.uiState.value is NotificationsUiState.Loaded)
    }

    @Test
    fun fake_sendOnce_storesOnlyTheFirstTime() = runTest {
        val repo = FakeNotificationRepository(emptyList())
        assertTrue(repo.sendOnce("lottery-a-$priya", note("You won", 1_000)))
        assertFalse(repo.sendOnce("lottery-a-$priya", note("You won", 2_000)))
        assertEquals(1, repo.getNotif(priya).size)
    }

    @Test
    fun fake_sendNotif_skipsUsersWhoOptedOut() {
        val repo = FakeNotificationRepository(emptyList(), optedOut = { it == priya })
        repo.sendNotif(priya, "Lottery draw date update", "Moved to May 24", NotificationType.OWNER_MESSAGE, null)
        repo.sendNotif(TestUsers.OWNER_ID, "Lottery draw date update", "Moved to May 24", NotificationType.OWNER_MESSAGE, null)
        assertTrue(repo.getNotif(priya).isEmpty())
        assertEquals(1, repo.getNotif(TestUsers.OWNER_ID).size)
    }
}
