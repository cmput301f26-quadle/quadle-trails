// Unit test suite validating the CampMatch weighted scoring algorithm and ranking engine.
// implements Bonus CampMatch Recommendation Engine
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Matching
package com.example.trails.matcher

import com.example.trails.data.model.Campsite
import com.example.trails.matcher.engine.CampsiteMatcher
import com.example.trails.matcher.model.CamperPreferences
import com.example.trails.matcher.model.LotteryContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying [CampsiteMatcher] scoring math, weight distribution, and ranking.
 */
class CampsiteMatcherTest {

    private val sampleCampsite = Campsite(
        id = "campsite-1",
        name = "Two Jack Lakeside",
        tags = listOf("Fire pit", "Showers", "Lakeside", "Power hookup"),
        pricePerNight = 45.0,
        maxOccupancy = 6,
        ratingAverage = 4.8,
        ratingCount = 12,
        activeLotteryId = "lottery-1"
    )

    @Test
    fun calculateMatch_perfectMatch_returnsNearHundredPercent() {
        val prefs = CamperPreferences(
            preferredTags = setOf("Fire pit", "Lakeside"),
            maxPricePerNight = 50.0,
            minOccupancy = 4,
            prioritizeHighWinOdds = true,
            minRating = 4.0
        )
        val lotteryContext = LotteryContext(
            lotteryId = "lottery-1",
            numWinners = 5,
            currentWaitlistCount = 4 // odds are 100%
        )

        val result = CampsiteMatcher.calculateMatch(sampleCampsite, prefs, lotteryContext)

        assertTrue("Expected score >= 90 but was ${result.matchPercentage}", result.matchPercentage >= 90)
        assertEquals(1.0, result.breakdown.tagScore, 0.01)
        assertEquals(1.0, result.breakdown.oddsScore, 0.01)
        assertEquals(1.0, result.breakdown.budgetScore, 0.01)
        assertTrue(result.matchHighlights.any { it.contains("all 2 desired amenities") })
        assertTrue(result.matchHighlights.any { it.contains("High lottery win odds") })
        assertTrue(result.matchHighlights.any { it.contains("Within your budget") })
    }

    @Test
    fun calculateMatch_partialTagMatch_reducesTagScore() {
        val prefs = CamperPreferences(
            preferredTags = setOf("Fire pit", "Fishing", "NonExistentTag") // only 1 matches
        )

        val result = CampsiteMatcher.calculateMatch(sampleCampsite, prefs)

        // 1 out of 3 = 0.333
        assertEquals(1.0 / 3.0, result.breakdown.tagScore, 0.01)
        assertTrue(result.matchHighlights.any { it.contains("Matches 1 of 3 desired amenities") })
    }

    @Test
    fun calculateMatch_emptyPreferences_providesNeutralBaseline() {
        val prefs = CamperPreferences(preferredTags = emptySet())

        val result = CampsiteMatcher.calculateMatch(sampleCampsite, prefs)

        assertEquals(1.0, result.breakdown.tagScore, 0.01)
        assertEquals(1.0, result.breakdown.budgetScore, 0.01)
        assertTrue(result.matchPercentage in 50..100)
    }

    @Test
    fun calculateMatch_overBudget_appliesSmoothDecayPenalty() {
        val budgetPrefs = CamperPreferences(
            maxPricePerNight = 30.0 // campsite price is 45.0
        )

        val result = CampsiteMatcher.calculateMatch(sampleCampsite, budgetPrefs)

        // 30 / 45 = 0.666
        assertEquals(30.0 / 45.0, result.breakdown.budgetScore, 0.01)
    }

    @Test
    fun calculateMatch_insufficientOccupancy_penalizesOverallScore() {
        val smallSite = sampleCampsite.copy(maxOccupancy = 2)
        val largeGroupPrefs = CamperPreferences(
            minOccupancy = 8 // smallSite only holds 2
        )

        val result = CampsiteMatcher.calculateMatch(smallSite, largeGroupPrefs)

        assertTrue(
            "Overall score should be heavily penalized for insufficient capacity",
            result.matchPercentage < 50
        )
    }

    @Test
    fun calculateMatch_ratingBelowMinimum_appliesQualityPenalty() {
        val lowRatedSite = sampleCampsite.copy(ratingAverage = 2.5, ratingCount = 10)
        val strictPrefs = CamperPreferences(
            minRating = 4.0
        )

        val result = CampsiteMatcher.calculateMatch(lowRatedSite, strictPrefs)

        // (2.5 / 5.0) * 0.5 = 0.25
        assertEquals(0.25, result.breakdown.ratingScore, 0.01)
    }

    @Test
    fun rankCampsites_sortsDescendingByMatchPercentage() {
        val siteA = sampleCampsite.copy(id = "site-A", tags = listOf("Fire pit", "Lakeside"))
        val siteB = sampleCampsite.copy(id = "site-B", tags = listOf("NoMatch1", "NoMatch2"), pricePerNight = 200.0)
        val siteC = sampleCampsite.copy(id = "site-C", tags = listOf("Fire pit"))

        val prefs = CamperPreferences(
            preferredTags = setOf("Fire pit", "Lakeside"),
            maxPricePerNight = 60.0
        )

        val ranked = CampsiteMatcher.rankCampsites(listOf(siteB, siteA, siteC), prefs)

        assertEquals("site-A", ranked[0].campsiteId) // best match
        assertEquals("site-C", ranked[1].campsiteId) // partial match
        assertEquals("site-B", ranked[2].campsiteId) // lowest match
        assertTrue(ranked[0].matchPercentage >= ranked[1].matchPercentage)
        assertTrue(ranked[1].matchPercentage >= ranked[2].matchPercentage)
    }

    @Test
    fun matchesTag_tolerantOfCaseSpacingAndHyphens() {
        assertTrue(CampsiteMatcher.matchesTag("firepit", "Fire pit"))
        assertTrue(CampsiteMatcher.matchesTag("fire-pit", "Fire pit"))
        assertTrue(CampsiteMatcher.matchesTag("Pet friendly", "pet-friendly"))
        assertTrue(CampsiteMatcher.matchesTag("SHOWERS", "showers"))
        assertTrue(CampsiteMatcher.matchesTag("Lakeside", "lake"))
        org.junit.Assert.assertFalse(CampsiteMatcher.matchesTag("Mountain", "Lakeside"))
    }
}
