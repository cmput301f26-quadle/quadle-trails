// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.01.01, 03.02.01
// PURPOSE: Unit tests for AdminRemoval, which removes a campsite or a user
//   profile together with the campsites, lotteries and waitlist entries that
//   depend on it.
// Outstanding issues: none.
package com.example.trails.ui.admin

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeLotteryRepository
import com.example.trails.data.fake.FakeUserRepository
import com.example.trails.data.fake.FakeWaitlistRepository
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.example.trails.data.model.WaitlistEntry
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

    private val maya = User(
        deviceId = "device-maya",
        name = "Maya Thompson",
        email = "maya.thompson@gmail.com",
        roles = listOf(Role.ENTRANT, Role.OWNER),
        activeMode = Role.OWNER
    )
    private val omar = User(
        deviceId = "device-omar",
        name = "Omar Haddad",
        email = "omar.haddad@outlook.com",
        roles = listOf(Role.ENTRANT, Role.OWNER),
        activeMode = Role.OWNER
    )
    private val priya = User(deviceId = "device-priya", name = "Priya Sharma", email = "priya.sharma@ualberta.ca")
    private val ava = User(deviceId = "device-ava", name = "Ava Chen", email = "ava.chen@gmail.com")

    /** The administrator doing the removals. */
    private val jordan = User(
        deviceId = "device-jordan",
        name = "Jordan Lee",
        email = "jordan.lee@trails.ca",
        roles = listOf(Role.ENTRANT, Role.ADMIN),
        activeMode = Role.ADMIN
    )

    private lateinit var campsites: FakeCampsiteRepository
    private lateinit var lotteries: FakeLotteryRepository
    private lateinit var waitlist: FakeWaitlistRepository
    private lateinit var users: FakeUserRepository
    private lateinit var removal: AdminRemoval

    @Before
    fun setUp() {
        campsites = FakeCampsiteRepository(initial = listOf(jasperPines, wapitiFlats, tunnelMountain))
        lotteries = FakeLotteryRepository(initial = listOf(pinesSummer, jasperFall, banffWinter))
        waitlist = FakeWaitlistRepository(initial = listOf(priyaPines, avaPines, priyaFall, avaBanff))
        users = FakeUserRepository(initial = listOf(maya, omar, priya, ava, jordan))
        removal = AdminRemoval(campsites, lotteries, waitlist, users)
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

    // This checks that removing an entrant deletes their profile and their entry in every lottery.
    @Test
    fun removeProfile_deletesUserAndEntries() = runBlocking {
        val result = removal.removeProfile("device-priya", "device-jordan")
        assertTrue(result.isSuccess)
        assertNull(users.getById("device-priya"))
        assertNull(waitlist.getEntry("pines-summer", "device-priya"))
        assertNull(waitlist.getEntry("jasper-fall", "device-priya"))
    }

    // This checks that removing a campsite owner also removes their campsites and the lotteries left without one.
    @Test
    fun removeProfile_owner_alsoRemovesTheirCampsites() = runBlocking {
        val result = removal.removeProfile("device-maya", "device-jordan")
        assertTrue(result.isSuccess)
        assertNull(users.getById("device-maya"))
        assertNull(campsites.getById("jasper-pines"))
        assertNull(campsites.getById("wapiti-flats"))
        assertNull(lotteries.getById("pines-summer"))
        assertNull(lotteries.getById("jasper-fall"))
        assertTrue(waitlist.getEntriesForLottery("pines-summer").isEmpty())
        assertTrue(waitlist.getEntriesForLottery("jasper-fall").isEmpty())
    }

    // This checks that an admin can't remove their own profile, and that nothing is removed when they try.
    @Test
    fun removeProfile_self_fails_nothingRemoved() = runBlocking {
        val result = removal.removeProfile("device-maya", "device-maya")
        assertTrue(result.isFailure)
        assertEquals(maya, users.getById("device-maya"))
        assertEquals(3, campsites.observeAll().first().size)
        assertEquals(pinesSummer, lotteries.getById("pines-summer"))
        assertEquals(jasperFall, lotteries.getById("jasper-fall"))
        assertEquals(2, waitlist.getEntryCount("pines-summer"))
    }

    // This checks that removing a user who doesn't exist fails and leaves every user in place.
    @Test
    fun removeProfile_missingUser_fails() = runBlocking {
        val result = removal.removeProfile("device-nobody", "device-jordan")
        assertTrue(result.isFailure)
        assertEquals(5, users.observeAll().first().size)
        assertEquals(2, waitlist.getEntryCount("pines-summer"))
    }

    // This checks that other users, their campsites and their entries are left exactly as they were.
    @Test
    fun removeProfile_otherUsersUntouched() = runBlocking {
        removal.removeProfile("device-priya", "device-jordan")
        assertEquals(ava, users.getById("device-ava"))
        assertEquals(maya, users.getById("device-maya"))
        assertNotNull(users.getById("device-jordan"))
        assertEquals(3, campsites.observeAll().first().size)
        assertEquals(listOf(avaPines), waitlist.getEntriesForLottery("pines-summer"))
        assertEquals(listOf(avaBanff), waitlist.getEntriesForLottery("banff-winter"))
        assertEquals(jasperFall, lotteries.getById("jasper-fall"))
    }

    // This checks that the impact counts the campsites a user owns and their entries across all lotteries.
    @Test
    fun profileImpact_countsCampsitesAndEntries() = runBlocking {
        assertEquals(ProfileImpact("Maya Thompson", 2, 0), removal.profileImpact("device-maya"))
        assertEquals(ProfileImpact("Omar Haddad", 1, 0), removal.profileImpact("device-omar"))
        assertEquals(ProfileImpact("Priya Sharma", 0, 2), removal.profileImpact("device-priya"))
        assertEquals(ProfileImpact("Ava Chen", 0, 2), removal.profileImpact("device-ava"))
    }

    // This checks that the impact of a user who doesn't exist is null.
    @Test
    fun profileImpact_missing_isNull() = runBlocking {
        assertNull(removal.profileImpact("device-nobody"))
    }
}
