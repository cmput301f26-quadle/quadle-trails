// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.01.01, 03.02.01, 03.07.01, 03.09.01, 03.10.01
// PURPOSE: Unit tests for AdminRemoval, which removes a campsite or a user
//   profile together with the campsites, lotteries and waitlist entries that
//   depend on it, closes policy reports against a reviewed owner, and removes
//   single ratings while keeping the campsite's average and count right, or
//   only a rating's review text while keeping its stars.
// Outstanding issues: none.
package com.example.trails.ui.admin

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeLotteryRepository
import com.example.trails.data.fake.FakeRatingRepository
import com.example.trails.data.fake.FakeReportRepository
import com.example.trails.data.fake.FakeUserRepository
import com.example.trails.data.fake.FakeWaitlistRepository
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.data.model.Rating
import com.example.trails.data.model.Report
import com.example.trails.data.model.ReportStatus
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

    private fun report(id: String, ownerId: String, campsiteId: String, status: ReportStatus = ReportStatus.OPEN) =
        Report(
            id = id,
            ownerId = ownerId,
            campsiteId = campsiteId,
            reason = "Misleading photos",
            ruleBroken = "Accurate listings",
            status = status,
            createdAt = Timestamp.now()
        )

    private val mayaPhotos = report("maya-photos", "device-maya", "jasper-pines")
    private val mayaBooking = report("maya-booking", "device-maya", "wapiti-flats")
    private val mayaOld = report("maya-old", "device-maya", "jasper-pines", ReportStatus.DISMISSED)
    private val omarPhotos = report("omar-photos", "device-omar", "tunnel-mountain")

    private fun rating(campsiteId: String, userId: String, score: Int, review: String) = Rating(
        userId = userId,
        campsiteId = campsiteId,
        score = score,
        review = review,
        createdAt = Timestamp.now()
    )

    /** Tunnel Mountain has three ratings; Priya's 1-star rant is the one that breaks policy. */
    private val priyaTunnelRating = rating(
        "tunnel-mountain", "device-priya", 1, "The owner is a crook, call him at 403-555-0199 and tell him."
    )
    private val avaTunnelRating = rating("tunnel-mountain", "device-ava", 4, "Clean washrooms, a bit close to the road.")
    private val mayaTunnelRating = rating("tunnel-mountain", "device-maya", 5, "Woke up to elk wandering past the site.")

    /** Jasper Pines has a single rating. */
    private val avaPinesRating = rating("jasper-pines", "device-ava", 3, "Nice trees, noisy neighbours.")

    private val tunnelRated = tunnelMountain.copy(ratingAverage = 10.0 / 3, ratingCount = 3)
    private val pinesRated = jasperPines.copy(ratingAverage = 3.0, ratingCount = 1)

    private lateinit var campsites: FakeCampsiteRepository
    private lateinit var lotteries: FakeLotteryRepository
    private lateinit var waitlist: FakeWaitlistRepository
    private lateinit var users: FakeUserRepository
    private lateinit var reports: FakeReportRepository
    private lateinit var ratings: FakeRatingRepository
    private lateinit var removal: AdminRemoval

    @Before
    fun setUp() {
        campsites = FakeCampsiteRepository(initial = listOf(pinesRated, wapitiFlats, tunnelRated))
        lotteries = FakeLotteryRepository(initial = listOf(pinesSummer, jasperFall, banffWinter))
        waitlist = FakeWaitlistRepository(initial = listOf(priyaPines, avaPines, priyaFall, avaBanff))
        users = FakeUserRepository(initial = listOf(maya, omar, priya, ava, jordan))
        reports = FakeReportRepository(initial = listOf(mayaPhotos, mayaBooking, mayaOld, omarPhotos))
        ratings = FakeRatingRepository(initial = listOf(priyaTunnelRating, avaTunnelRating, mayaTunnelRating, avaPinesRating))
        removal = AdminRemoval(campsites, lotteries, waitlist, users, reports, ratings)
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
        assertEquals(tunnelRated, campsites.getById("tunnel-mountain"))
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

    /** Each report's status by id, for comparing whole sets at once. */
    private suspend fun statuses() = reports.observeAll().first().associate { it.id to it.status }

    // This checks that removing a reported owner deletes them and marks only their open reports as resolved.
    @Test
    fun removeOwner_removesUserAndResolvesReports() = runBlocking {
        val result = removal.removeOwner("device-maya", "device-jordan")
        assertTrue(result.isSuccess)
        assertNull(users.getById("device-maya"))
        assertNull(campsites.getById("jasper-pines"))
        assertEquals(
            mapOf(
                "maya-photos" to ReportStatus.OWNER_REMOVED,
                "maya-booking" to ReportStatus.OWNER_REMOVED,
                "maya-old" to ReportStatus.DISMISSED,
                "omar-photos" to ReportStatus.OPEN
            ),
            statuses()
        )
    }

    // This checks that an admin can't remove themselves as a reported owner, and their reports stay open.
    @Test
    fun removeOwner_self_fails_reportsStayOpen() = runBlocking {
        val result = removal.removeOwner("device-maya", "device-maya")
        assertTrue(result.isFailure)
        assertEquals(maya, users.getById("device-maya"))
        assertEquals(ReportStatus.OPEN, statuses()["maya-photos"])
        assertEquals(ReportStatus.OPEN, statuses()["maya-booking"])
    }

    // This checks that dismissing reports only changes that owner's open reports.
    @Test
    fun dismissReports_onlyThatOwner() = runBlocking {
        val result = removal.dismissReports("device-maya")
        assertTrue(result.isSuccess)
        assertEquals(
            mapOf(
                "maya-photos" to ReportStatus.DISMISSED,
                "maya-booking" to ReportStatus.DISMISSED,
                "maya-old" to ReportStatus.DISMISSED,
                "omar-photos" to ReportStatus.OPEN
            ),
            statuses()
        )
    }

    // This checks that dismissing reports leaves the owner, their campsites and their lotteries in place.
    @Test
    fun dismissReports_keepsUserAndCampsites() = runBlocking {
        removal.dismissReports("device-maya")
        assertEquals(maya, users.getById("device-maya"))
        assertEquals(pinesRated, campsites.getById("jasper-pines"))
        assertEquals(wapitiFlats, campsites.getById("wapiti-flats"))
        assertEquals(pinesSummer, lotteries.getById("pines-summer"))
        assertEquals(jasperFall, lotteries.getById("jasper-fall"))
    }

    // This checks that removing a rating deletes it and saves the average and count of the ratings left.
    @Test
    fun removeRating_deletesAndUpdatesAverage() = runBlocking {
        val result = removal.removeRating("tunnel-mountain", "device-priya")
        assertTrue(result.isSuccess)
        assertNull(ratings.getRating("tunnel-mountain", "device-priya"))
        val saved = campsites.getById("tunnel-mountain")!!
        assertEquals(4.5, saved.ratingAverage, 0.0001)
        assertEquals(2, saved.ratingCount)
        assertEquals(tunnelMountain.copy(ratingAverage = 4.5, ratingCount = 2), saved)
    }

    // This checks that removing a campsite's only rating sets its average and count back to zero.
    @Test
    fun removeRating_lastRating_averageZeroCountZero() = runBlocking {
        val result = removal.removeRating("jasper-pines", "device-ava")
        assertTrue(result.isSuccess)
        assertTrue(ratings.observeForCampsite("jasper-pines").first().isEmpty())
        val saved = campsites.getById("jasper-pines")!!
        assertEquals(0.0, saved.ratingAverage, 0.0)
        assertEquals(0, saved.ratingCount)
    }

    // This checks that removing a rating that doesn't exist, or one for a campsite that doesn't exist,
    // fails and changes no rating or campsite.
    @Test
    fun removeRating_missing_fails_nothingChanged() = runBlocking {
        assertTrue(removal.removeRating("tunnel-mountain", "device-jordan").isFailure)
        assertTrue(removal.removeRating("no-such-site", "device-priya").isFailure)
        assertEquals(tunnelRated, campsites.getById("tunnel-mountain"))
        assertEquals(3, ratings.observeForCampsite("tunnel-mountain").first().size)
    }

    // This checks that a rating whose campsite is gone fails and is left in storage.
    @Test
    fun removeRating_campsiteMissing_keepsRating() = runBlocking {
        campsites.delete("jasper-pines")
        assertTrue(removal.removeRating("jasper-pines", "device-ava").isFailure)
        assertEquals(avaPinesRating, ratings.getRating("jasper-pines", "device-ava"))
    }

    // This checks that removing one rating leaves the other ratings and other campsites exactly as they were.
    @Test
    fun removeRating_otherRatingsUntouched() = runBlocking {
        removal.removeRating("tunnel-mountain", "device-priya")
        assertEquals(avaTunnelRating, ratings.getRating("tunnel-mountain", "device-ava"))
        assertEquals(mayaTunnelRating, ratings.getRating("tunnel-mountain", "device-maya"))
        assertEquals(avaPinesRating, ratings.getRating("jasper-pines", "device-ava"))
        assertEquals(pinesRated, campsites.getById("jasper-pines"))
        assertEquals(wapitiFlats, campsites.getById("wapiti-flats"))
    }

    // ---- Remove review (US 03.10.01) ----

    // This checks that removing a review empties the text and marks it removed, but keeps the score and date.
    @Test
    fun removeReview_clearsTextKeepsScore() = runBlocking {
        val result = removal.removeReview("tunnel-mountain", "device-priya")
        assertTrue(result.isSuccess)
        val saved = ratings.getRating("tunnel-mountain", "device-priya")!!
        assertEquals("", saved.review)
        assertTrue(saved.reviewRemoved)
        assertEquals(priyaTunnelRating.copy(review = "", reviewRemoved = true), saved)
        assertEquals(1, saved.score)
        assertEquals(priyaTunnelRating.createdAt, saved.createdAt)
    }

    // This checks that removing a review leaves the campsite's average, count and the other ratings as they were.
    @Test
    fun removeReview_averageUnchanged() = runBlocking {
        removal.removeReview("tunnel-mountain", "device-priya")
        assertEquals(tunnelRated, campsites.getById("tunnel-mountain"))
        val stats = ratingStats(ratings.observeForCampsite("tunnel-mountain").first())
        assertEquals(10.0 / 3, stats.average, 0.0001)
        assertEquals(3, stats.count)
        assertEquals(avaTunnelRating, ratings.getRating("tunnel-mountain", "device-ava"))
        assertEquals(mayaTunnelRating, ratings.getRating("tunnel-mountain", "device-maya"))
    }

    // This checks that a rating with no review text can't have its review removed, and is left as it was.
    @Test
    fun removeReview_noReviewText_fails() = runBlocking {
        val starsOnly = rating("jasper-pines", "device-jordan", 2, "")
        ratings.save(starsOnly)
        assertTrue(removal.removeReview("jasper-pines", "device-jordan").isFailure)
        assertEquals(starsOnly, ratings.getRating("jasper-pines", "device-jordan"))

        removal.removeReview("tunnel-mountain", "device-priya")
        assertTrue(removal.removeReview("tunnel-mountain", "device-priya").isFailure)
    }

    // This checks that removing the review of a rating that doesn't exist fails and changes nothing.
    @Test
    fun removeReview_missing_fails() = runBlocking {
        assertTrue(removal.removeReview("tunnel-mountain", "device-jordan").isFailure)
        assertTrue(removal.removeReview("no-such-site", "device-priya").isFailure)
        assertNull(ratings.getRating("tunnel-mountain", "device-jordan"))
        assertEquals(3, ratings.observeForCampsite("tunnel-mountain").first().size)
        assertEquals(tunnelRated, campsites.getById("tunnel-mountain"))
    }
}
