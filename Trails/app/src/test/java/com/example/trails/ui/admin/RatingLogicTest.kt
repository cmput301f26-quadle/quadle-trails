// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 01.13.01
// PURPOSE: Unit tests for the rating rules and average calculation.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.ui.admin

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Rating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingLogicTest {

    private val jasperPines = Campsite(
        id = "jasper-pines", ownerId = "device-maya", name = "Jasper Pines",
        locationName = "Jasper, AB", maxOccupancy = 6
    )

    // This checks that a campsite with no ratings has an average of 0.0 and a count of 0.
    @Test
    fun noRatings_zeroAverageAndCount() {
        assertEquals(RatingStats(0.0, 0), ratingStats(emptyList()))
    }

    // This checks that scores of 5 and 4 give an average of 4.5 from 2 ratings.
    @Test
    fun twoRatings_averageAndCount() {
        val ratings = listOf(
            Rating(userId = "device-ava", campsiteId = "jasper-pines", score = 5, review = "Loved the river."),
            Rating(userId = "device-skanda", campsiteId = "jasper-pines", score = 4, review = "Bit noisy.")
        )
        val stats = ratingStats(ratings)
        assertEquals(4.5, stats.average, 0.0001)
        assertEquals(2, stats.count)
    }

    // This checks that the owner of a campsite isn't allowed to rate it.
    @Test
    fun owner_cannotRate() {
        assertFalse(canRate("device-maya", jasperPines))
    }

    // This checks that someone who doesn't own the campsite is allowed to rate it.
    @Test
    fun otherUser_canRate() {
        assertTrue(canRate("device-ava", jasperPines))
    }

    // This checks that an empty or all-spaces user id can't rate.
    @Test
    fun blankUser_cannotRate() {
        assertFalse(canRate("", jasperPines))
        assertFalse(canRate("   ", jasperPines))
    }

    // This checks that only 1 to 5 are valid scores, and 0 and 6 are not.
    @Test
    fun score_validRange() {
        assertFalse(isValidScore(0))
        for (s in 1..5) assertTrue(isValidScore(s))
        assertFalse(isValidScore(6))
        assertFalse(isValidScore(-1))
    }

    // This checks that each score from 1 to 5 has the right word, and others have none.
    @Test
    fun scoreLabel_eachScore() {
        assertEquals("Poor", scoreLabel(1))
        assertEquals("Fair", scoreLabel(2))
        assertEquals("Good", scoreLabel(3))
        assertEquals("Great", scoreLabel(4))
        assertEquals("Excellent", scoreLabel(5))
        assertEquals("", scoreLabel(0))
        assertEquals("", scoreLabel(6))
    }

    // This checks that the summary reads like "4 out of 5 · Great".
    @Test
    fun scoreSummary_format() {
        assertEquals("4 out of 5 · Great", scoreSummary(4))
        assertEquals("1 out of 5 · Poor", scoreSummary(1))
    }
}
