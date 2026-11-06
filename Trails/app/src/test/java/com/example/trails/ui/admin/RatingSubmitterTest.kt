// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 01.13.01
// PURPOSE: Unit tests for RatingSubmitter, which saves ratings and updates campsite stats.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.ui.admin

import com.example.trails.data.fake.FakeCampsiteRepository
import com.example.trails.data.fake.FakeRatingRepository
import com.example.trails.data.model.Campsite
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RatingSubmitterTest {

    private val jasperPines = Campsite(
        id = "jasper-pines",
        ownerId = "device-maya",
        name = "Jasper Pines",
        description = "Forested sites along the Athabasca River.",
        locationName = "Jasper, AB",
        latitude = 52.8737,
        longitude = -118.0814,
        maxOccupancy = 6,
        pricePerNight = 40.0,
        tags = listOf("Fire pit", "Riverside"),
        rules = "Quiet hours 11pm-7am.",
        cancellationPolicy = "Full refund up to 7 days before arrival.",
        activeLotteryId = "lottery-summer"
    )

    private lateinit var ratings: FakeRatingRepository
    private lateinit var campsites: FakeCampsiteRepository
    private lateinit var submitter: RatingSubmitter

    @Before
    fun setUp() {
        ratings = FakeRatingRepository(initial = emptyList())
        campsites = FakeCampsiteRepository(initial = listOf(jasperPines))
        submitter = RatingSubmitter(ratings, campsites)
    }

    private suspend fun savedCampsite() = campsites.getById("jasper-pines")!!

    // This checks that the first rating sets the campsite's average to that score and count to 1.
    @Test
    fun firstRating_setsAverageAndCount() = runBlocking {
        val result = submitter.submit("jasper-pines", "device-ava", 4, "Lovely river views.")
        assertEquals(RatingStats(4.0, 1), result.getOrNull())
        assertEquals(4.0, savedCampsite().ratingAverage, 0.0001)
        assertEquals(1, savedCampsite().ratingCount)
    }

    // This checks that a second user's rating is averaged in with the first.
    @Test
    fun secondUser_updatesAverage() = runBlocking {
        submitter.submit("jasper-pines", "device-ava", 5, "Perfect weekend.")
        val result = submitter.submit("jasper-pines", "device-skanda", 2, "Too many mosquitoes.")
        assertEquals(RatingStats(3.5, 2), result.getOrNull())
        assertEquals(3.5, savedCampsite().ratingAverage, 0.0001)
        assertEquals(2, savedCampsite().ratingCount)
    }

    // This checks that rating the same campsite twice replaces the first rating instead of adding one.
    @Test
    fun sameUserRatesAgain_replacesNotAdds() = runBlocking {
        submitter.submit("jasper-pines", "device-ava", 2, "Rainy and muddy.")
        submitter.submit("jasper-pines", "device-ava", 5, "Came back in July, much better.")
        assertEquals(1, ratings.observeForCampsite("jasper-pines").first().size)
        assertEquals(5, ratings.getRating("jasper-pines", "device-ava")?.score)
        assertEquals(5.0, savedCampsite().ratingAverage, 0.0001)
        assertEquals(1, savedCampsite().ratingCount)
    }

    // This checks that a score outside 1 to 5 fails and saves nothing.
    @Test
    fun invalidScore_fails_nothingSaved() = runBlocking {
        val result = submitter.submit("jasper-pines", "device-ava", 6, "Off the charts!")
        assertTrue(result.isFailure)
        assertNull(ratings.getRating("jasper-pines", "device-ava"))
        assertEquals(jasperPines, savedCampsite())
    }

    // This checks that the owner can't rate their own campsite and nothing is saved.
    @Test
    fun owner_fails_nothingSaved() = runBlocking {
        val result = submitter.submit("jasper-pines", "device-maya", 5, "Best site ever (I'm biased).")
        assertTrue(result.isFailure)
        assertNull(ratings.getRating("jasper-pines", "device-maya"))
        assertEquals(jasperPines, savedCampsite())
    }

    // This checks that rating a campsite that doesn't exist fails without saving a rating.
    @Test
    fun missingCampsite_fails() = runBlocking {
        val result = submitter.submit("no-such-site", "device-ava", 4, "Where am I?")
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull()?.message)
        assertNull(ratings.getRating("no-such-site", "device-ava"))
    }

    // This checks that only ratingAverage and ratingCount change, and every other campsite field is kept.
    @Test
    fun onlyRatingFieldsChange_otherCampsiteFieldsKept() = runBlocking {
        submitter.submit("jasper-pines", "device-ava", 3, "Decent.")
        val expected = jasperPines.copy(ratingAverage = 3.0, ratingCount = 1)
        assertEquals(expected, savedCampsite())
    }

    // This checks that the review has its outer spaces removed and is cut to MAX_REVIEW_LENGTH characters.
    @Test
    fun longReview_isTrimmedAndCut() = runBlocking {
        val longText = "   " + "a".repeat(MAX_REVIEW_LENGTH + 100) + "   "
        submitter.submit("jasper-pines", "device-ava", 4, longText)
        val saved = ratings.getRating("jasper-pines", "device-ava")!!.review
        assertEquals(MAX_REVIEW_LENGTH, saved.length)
        assertEquals("a".repeat(MAX_REVIEW_LENGTH), saved)

        submitter.submit("jasper-pines", "device-ava", 4, "  Nice and quiet.  ")
        assertEquals("Nice and quiet.", ratings.getRating("jasper-pines", "device-ava")!!.review)
    }
}
