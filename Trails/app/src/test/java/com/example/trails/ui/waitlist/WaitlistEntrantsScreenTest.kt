// unit tests for waitlist entrants screen state and helpers
// owner: chitkarsh (chitkarshrathi) - waitlist
package com.example.trails.ui.waitlist

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeLotteryRepository
import com.example.trails.data.fake.FakeWaitlistRepository
import com.example.trails.data.model.WaitlistEntry
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class WaitlistEntrantsScreenTest {

    private val testDispatcher = StandardTestDispatcher()
    private val lotteryId = "test-lottery"

    private lateinit var waitlistRepo: FakeWaitlistRepository
    private lateinit var lotteryRepo: FakeLotteryRepository
    private lateinit var campsiteRepo: FakeCampsiteRepository
    private lateinit var viewModel: WaitlistViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        waitlistRepo = FakeWaitlistRepository(initial = emptyList())
        lotteryRepo = FakeLotteryRepository()
        campsiteRepo = FakeCampsiteRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun formatJoinedDate_validTimestamp_returnsFormattedString() {
        val date = Date(1682640000000L)
        val timestamp = Timestamp(date)
        val formatted = formatJoinedDate(timestamp)
        assertNotNull(formatted)
        assertEquals(true, formatted.contains("Apr"))
    }

    @Test
    fun formatJoinedDate_nullTimestamp_returnsFallback() {
        val formatted = formatJoinedDate(null)
        assertEquals("May 20, 2027", formatted)
    }

    @Test
    fun entrants_sortedOrderPreservedInViewModel() {
        val t1 = Timestamp(Date(1000000))
        val t2 = Timestamp(Date(2000000))
        val e1 = WaitlistEntry(userId = "entrant-1", userName = "Maya Singh", lotteryId = lotteryId, joinedAt = t1, rulesAccepted = true)
        val e2 = WaitlistEntry(userId = "entrant-2", userName = "Daniel Kim", lotteryId = lotteryId, joinedAt = t2, rulesAccepted = true)

        waitlistRepo = FakeWaitlistRepository(initial = listOf(e2, e1))
        viewModel = WaitlistViewModel(
            lotteryId = lotteryId,
            waitlistRepository = waitlistRepo,
            lotteryRepository = lotteryRepo,
            campsiteRepository = campsiteRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("entrant-1", "entrant-2"), viewModel.entrants.value.map { it.userId })
    }
}
