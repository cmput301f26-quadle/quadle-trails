// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.09.01
// PURPOSE: Unit tests for the admin Campsite page's "Ratings & reviews" state and
//   the "Remove rating?" dialog text.
// Outstanding issues: none.
package com.example.trails.ui.admin

import com.example.trails.data.model.Rating
import com.example.trails.data.model.User
import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminRatingsStateTest {

    /** A timestamp at noon on the given day, so the date label is the same in every time zone nearby. */
    private fun on(year: Int, month: Int, day: Int): Timestamp {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            clear()
            set(year, month - 1, day, 12, 0)
        }
        return Timestamp(cal.time)
    }

    private val priya = User(deviceId = "device-priya", name = "Priya Sharma", email = "priya.sharma@ualberta.ca")
    private val ava = User(deviceId = "device-ava", name = "Ava Chen", email = "ava.chen@gmail.com")

    private val priyaOld = Rating(
        userId = "device-priya", campsiteId = "tunnel-mountain", score = 4,
        review = "Clean washrooms, a bit close to the road.", createdAt = on(2026, 9, 14)
    )
    private val avaNew = Rating(
        userId = "device-ava", campsiteId = "tunnel-mountain", score = 1,
        review = "Owner is a scammer, here's his home address...", createdAt = on(2026, 11, 2)
    )
    private val ghostUndated = Rating(userId = "device-deleted", campsiteId = "tunnel-mountain", score = 3)

    // This checks that each rating becomes a card with the reviewer's name and a readable date.
    @Test
    fun buildItems_namesAndDates() {
        val items = buildAdminRatingItems(listOf(priyaOld), listOf(priya, ava), Locale.US)
        assertEquals(listOf(AdminRatingItem(priyaOld, "Priya Sharma", "Sep 14, 2026")), items)
    }

    // This checks that ratings are listed newest first, with undated ones last.
    @Test
    fun buildItems_newestFirst_undatedLast() {
        val items = buildAdminRatingItems(listOf(ghostUndated, priyaOld, avaNew), listOf(priya, ava))
        assertEquals(listOf("device-ava", "device-priya", "device-deleted"), items.map { it.rating.userId })
    }

    // This checks that a reviewer with no profile (or a blank name) shows as "Unknown user" and an
    // undated rating has an empty date label.
    @Test
    fun buildItems_unknownReviewer_showsUnknownUser() {
        val blankName = User(deviceId = "device-blank", name = "")
        val items = buildAdminRatingItems(
            listOf(ghostUndated, ghostUndated.copy(userId = "device-blank")),
            listOf(priya, blankName)
        )
        assertEquals(listOf("Unknown user", "Unknown user"), items.map { it.reviewerName })
        assertEquals("", items.first().dateLabel)
    }

    // This checks that a campsite with no ratings gives no cards.
    @Test
    fun buildItems_empty_isEmpty() {
        assertTrue(buildAdminRatingItems(emptyList(), listOf(priya)).isEmpty())
    }

    // This checks that the dialog text names the reviewer and the score and says it can't be undone.
    @Test
    fun removeRatingMessage_namesReviewerAndScore() {
        assertEquals(
            "Priya Sharma's 4-star rating is removed and the campsite's average is updated. " +
                "This can't be undone.",
            removeRatingMessage("Priya Sharma", 4)
        )
        assertEquals(
            "Unknown user's 1-star rating is removed and the campsite's average is updated. " +
                "This can't be undone.",
            removeRatingMessage("Unknown user", 1)
        )
    }
}
