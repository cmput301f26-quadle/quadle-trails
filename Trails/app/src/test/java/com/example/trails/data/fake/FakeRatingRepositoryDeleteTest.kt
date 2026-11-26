// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.01.01, 03.09.01
// PURPOSE: Unit tests for FakeRatingRepository.delete and deleteForCampsite, which admin
//   rating removal and campsite removal rely on.
// Outstanding issues: none.
package com.example.trails.data.fake

import com.example.trails.data.model.Rating
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeRatingRepositoryDeleteTest {

    private val priyaPines = Rating(
        userId = "device-priya",
        campsiteId = "jasper-pines",
        score = 2,
        review = "Neighbours were playing music past midnight.",
        createdAt = daysFromNow(-12)
    )
    private val avaPines = Rating(
        userId = "device-ava",
        campsiteId = "jasper-pines",
        score = 5,
        review = "Right on the river, loved it.",
        createdAt = daysFromNow(-4)
    )
    private val priyaTunnel = Rating(
        userId = "device-priya",
        campsiteId = "tunnel-mountain",
        score = 4,
        createdAt = daysFromNow(-30)
    )

    private lateinit var repo: FakeRatingRepository

    @Before
    fun setUp() {
        repo = FakeRatingRepository(initial = listOf(priyaPines, avaPines, priyaTunnel))
    }

    // This checks that deleting an existing rating removes it from the repository.
    @Test
    fun delete_existing_removesIt() = runBlocking {
        repo.delete("jasper-pines", "device-priya")
        assertNull(repo.getRating("jasper-pines", "device-priya"))
        assertEquals(listOf(avaPines), repo.observeForCampsite("jasper-pines").first())
    }

    // This checks that deleting a rating that doesn't exist changes nothing and doesn't throw.
    @Test
    fun delete_missing_doesNothing() = runBlocking {
        repo.delete("jasper-pines", "device-nobody")
        repo.delete("no-such-site", "device-priya")
        assertEquals(listOf(priyaPines, avaPines), repo.observeForCampsite("jasper-pines").first())
        assertEquals(listOf(priyaTunnel), repo.observeForCampsite("tunnel-mountain").first())
    }

    // This checks that deleting a user's rating of one campsite keeps their ratings of other campsites
    // and other users' ratings of the same campsite.
    @Test
    fun delete_onlyThatUsersRating() = runBlocking {
        repo.delete("jasper-pines", "device-priya")
        assertEquals(priyaTunnel, repo.getRating("tunnel-mountain", "device-priya"))
        assertEquals(avaPines, repo.getRating("jasper-pines", "device-ava"))
    }

    // This checks that deleting a campsite's ratings removes all of them and keeps other campsites' ratings.
    @Test
    fun deletesOnlyThatCampsitesRatings() = runBlocking {
        repo.deleteForCampsite("jasper-pines")
        assertTrue(repo.observeForCampsite("jasper-pines").first().isEmpty())
        assertEquals(listOf(priyaTunnel), repo.observeForCampsite("tunnel-mountain").first())
    }

    // This checks that deleting the ratings of a campsite with none changes nothing and doesn't throw.
    @Test
    fun noRatings_doesNothing() = runBlocking {
        repo.deleteForCampsite("no-such-site")
        assertEquals(listOf(priyaPines, avaPines), repo.observeForCampsite("jasper-pines").first())
        assertEquals(listOf(priyaTunnel), repo.observeForCampsite("tunnel-mountain").first())
    }
}
