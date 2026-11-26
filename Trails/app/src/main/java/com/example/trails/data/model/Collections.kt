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
 * @property RESERVATIONS confirmed reservations collection
 * @property REPORTS Top-level collection of policy reports against campsite owners.
 * @property MESSAGES Top-level collection of owner–camper chat messages.
 */
object Collections {
    const val USERS = "users"
    const val CAMPSITES = "campsites"
    const val RATINGS = "ratings"   // under campsites/{id}
    const val LOTTERIES = "lotteries"
    const val ENTRIES = "entries"   // under lotteries/{id}
    const val RESERVATIONS = "reservations" // added by chitkarsh on 11/05/2026
    const val REPORTS = "reports" // policy reports against owners; added by Zainab (US 03.07.01)
    const val NOTIFICATIONS = "notifications"
    const val MESSAGES = "messages" // owner–camper chat; added by Neaz (US 02.12.01, 01.14.01)
}
