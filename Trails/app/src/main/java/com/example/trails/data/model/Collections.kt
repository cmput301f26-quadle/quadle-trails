// Firestore collection names, so they're never typed as raw strings elsewhere.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.model

/**
 * Names of the Firestore collections the app reads and writes.
 *
 * @property USERS Top-level collection of user profiles.
 * @property CAMPSITES Top-level collection of campsite listings.
 * @property RATINGS Subcollection of ratings under each campsite.
 * @property LOTTERIES Top-level collection of lotteries.
 * @property ENTRIES Subcollection of waitlist entries under each lottery.
 */
object Collections {
    const val USERS = "users"
    const val CAMPSITES = "campsites"
    const val RATINGS = "ratings"   // under campsites/{id}
    const val LOTTERIES = "lotteries"
    const val ENTRIES = "entries"   // under lotteries/{id}
}
