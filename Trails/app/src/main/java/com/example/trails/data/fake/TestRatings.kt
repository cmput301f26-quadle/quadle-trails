// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 01.13.01
// PURPOSE: Sample campsite ratings, matching the rating averages in TestCampsites.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help write this file header (owner, user stories, purpose and outstanding issues).
package com.example.trails.data.fake

import com.example.trails.data.model.Rating

/** Sample ratings used to seed [FakeRatingRepository]. */
object TestRatings {
    /** Every sample rating: two of Banff and one of Jasper. */
    val all = listOf(
        Rating(
            userId = TestUsers.ENTRANT_ID,
            campsiteId = TestCampsites.BANFF_ID,
            score = 5,
            review = "Great spot, woke up to elk wandering past the site.",
            createdAt = daysFromNow(-20)
        ),
        Rating(
            userId = TestUsers.ADMIN_ID,
            campsiteId = TestCampsites.BANFF_ID,
            score = 4,
            review = "Clean washrooms, a bit close to the road.",
            createdAt = daysFromNow(-15)
        ),
        Rating(
            userId = TestUsers.ENTRANT_ID,
            campsiteId = TestCampsites.JASPER_ID,
            score = 5,
            review = "Amazing stars at night.",
            createdAt = daysFromNow(-10)
        )
    )
}
