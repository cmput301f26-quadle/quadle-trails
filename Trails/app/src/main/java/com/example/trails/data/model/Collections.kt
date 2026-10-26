// Firestore collection names, so they're never typed as raw strings elsewhere.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.model

object Collections {
    const val USERS = "users"
    const val CAMPSITES = "campsites"
    const val RATINGS = "ratings"   // under campsites/{id}
    const val LOTTERIES = "lotteries"
    const val ENTRIES = "entries"   // under lotteries/{id}
}
