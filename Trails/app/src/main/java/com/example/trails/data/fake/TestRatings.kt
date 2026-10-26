// Sample campsite ratings, matching the rating averages in TestCampsites.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.fake

import com.example.trails.data.model.Rating

object TestRatings {
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
