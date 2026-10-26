// Roles a user can hold; activeMode picks which one the UI is currently acting as.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.model

/** A role a user can hold in the app. */
enum class Role {
    /** Browses campsites and joins lotteries. */
    ENTRANT,
    /** Lists campsites and runs lotteries. */
    OWNER,
    /** Moderates campsites, profiles and policy. */
    ADMIN
}
