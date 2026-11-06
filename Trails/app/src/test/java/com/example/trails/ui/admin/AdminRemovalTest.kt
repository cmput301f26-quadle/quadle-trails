// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.01.01
// PURPOSE: Unit tests for AdminRemoval, which removes a campsite together with
//   the lotteries and waitlist entries that depend on it.
// Outstanding issues: none.
package com.example.trails.ui.admin

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeLotteryRepository
import com.example.trails.data.fake.FakeWaitlistRepository
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.data.model.WaitlistEntry
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdminRemovalTest {

    private val jasperPines = Campsite(
        id = "jasper-pines",
        ownerId = "device-maya",
        name = "Jasper Pines",
        locationName = "Jasper, AB",
        maxOccupancy = 6,
        pricePerNight = 40.0
    )
    private val wapitiFlats = Campsite(
        id = "wapiti-flats",
        ownerId = "device-maya",
        name = "Wapiti Flats",
        locationName = "Jasper, AB",
        maxOccupancy = 4,
        pricePerNight = 35.0
    )
    private val tunnelMountain = Campsite(
        id = "tunnel-mountain",
        ownerId = "device-omar",
        name = "Tunnel Mountain Village",
        locationName = "Banff, AB",
        maxOccupancy = 8,
        pricePerNight = 55.0
    )

    /** Only covers Jasper Pines, so it goes when Jasper Pines is removed. */
    private val pinesSummer = Lottery(
        id = "pines-summer",
        ownerId = "device-maya",
        name = "Jasper Pines July long weekend",
        campsiteIds = listOf("jasper-pines"),
        numWinners = 2
    )

    /** Covers both of Maya's sites, so it stays with just Wapiti Flats. */
    private val jasperFall = Lottery(
        id = "jasper-fall",
        ownerId = "device-maya",
        name = "Jasper fall colours",
        campsiteIds = listOf("jasper-pines", "wapiti-flats"),
        numWinners = 3
    )

    /** Has nothing to do with Jasper Pines. */
    private val banffWinter = Lottery(
        id = "banff-winter",
        ownerId = "device-omar",
        name = "Banff winter camping",
        campsiteIds = listOf("tunnel-mountain")
    )

    private fun entry(lotteryId: String, userId: String, name: String, party: Int) = WaitlistEntry(
        userId = userId,
        userName = name,
        lotteryId = lotteryId,
        partySize = party,
        joinedAt = Timestamp.now(),
        rulesAcceptedAt = Timestamp.now()
    )

    private val priyaPines = entry("pines-summer", "device-priya", "Priya Sharma", 3)
    private val avaPines = entry("pines-summer", "device-ava", "Ava Chen", 2)
    private val priyaFall = entry("jasper-fall", "device-priya", "Priya Sharma", 3)
    private val avaBanff = entry("banff-winter", "device-ava", "Ava Chen", 2)

    private lateinit var campsites: FakeCampsiteRepository
    private lateinit var lotteries: FakeLotteryRepository
    private lateinit var waitlist: FakeWaitlistRepository
    private lateinit var removal: AdminRemoval

    @Before
    fun setUp() {
        campsites = FakeCampsiteRepository(initial = listOf(jasperPines, wapitiFlats, tunnelMountain))
        lotteries = FakeLotteryRepository(initial = listOf(pinesSummer, jasperFall, banffWinter))
        waitlist = FakeWaitlistRepository(initial = listOf(priyaPines, avaPines, priyaFall, avaBanff))
        removal = AdminRemoval(campsites, lotteries, waitlist)
    }

    // This checks that removing a campsite succeeds and the campsite is gone afterwards.
    @Test
    fun removeCampsite_deletesCampsite() = runBlocking {
        val result = removal.removeCampsite("jasper-pines")
        assertTrue(result.isSuccess)
        assertNull(campsites.getById("jasper-pines"))
    }

    // This checks that a lottery covering only the removed campsite is deleted along with all its entries.
    @Test
    fun removeCampsite_singleCampsiteLottery_deletesLotteryAndEntries() = runBlocking {
        removal.removeCampsite("jasper-pines")
        assertNull(lotteries.getById("pines-summer"))
        assertTrue(waitlist.getEntriesForLottery("pines-summer").isEmpty())
    }

    // This checks that a lottery covering other campsites too is kept, with only the removed id taken out.
    @Test
    fun removeCampsite_multiCampsiteLottery_keepsLotteryWithoutIt() = runBlocking {
        removal.removeCampsite("jasper-pines")
        assertEquals(jasperFall.copy(campsiteIds = listOf("wapiti-flats")), lotteries.getById("jasper-fall"))
        assertEquals(listOf(priyaFall), waitlist.getEntriesForLottery("jasper-fall"))
    }

    // This checks that other campsites, their lotteries and their entries are left exactly as they were.
    @Test
    fun removeCampsite_otherCampsitesUntouched() = runBlocking {
        removal.removeCampsite("jasper-pines")
        assertEquals(wapitiFlats, campsites.getById("wapiti-flats"))
        assertEquals(tunnelMountain, campsites.getById("tunnel-mountain"))
        assertEquals(banffWinter, lotteries.getById("banff-winter"))
        assertEquals(listOf(avaBanff), waitlist.getEntriesForLottery("banff-winter"))
    }

    // This checks that removing a campsite that doesn't exist fails and changes no lottery or entry.
    @Test
    fun removeCampsite_missing_fails() = runBlocking {
        val result = removal.removeCampsite("no-such-site")
        assertTrue(result.isFailure)
        assertEquals(3, campsites.observeAll().first().size)
        assertEquals(pinesSummer, lotteries.getById("pines-summer"))
        assertEquals(2, waitlist.getEntryCount("pines-summer"))
    }

    // This checks that the impact counts only the lotteries that would be deleted and the entries in them.
    @Test
    fun campsiteImpact_countsLotteriesAndEntries() = runBlocking {
        assertEquals(CampsiteImpact("Jasper Pines", 1, 2), removal.campsiteImpact("jasper-pines"))
        assertEquals(CampsiteImpact("Wapiti Flats", 0, 0), removal.campsiteImpact("wapiti-flats"))
        assertEquals(CampsiteImpact("Tunnel Mountain Village", 1, 1), removal.campsiteImpact("tunnel-mountain"))
    }

    // This checks that the impact of a campsite that doesn't exist is null.
    @Test
    fun campsiteImpact_missing_isNull() = runBlocking {
        assertNull(removal.campsiteImpact("no-such-site"))
    }
}
