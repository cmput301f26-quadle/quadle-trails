// Lifecycle states of a waitlist entry, from joining through the draw to booking.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.model

/** Where a waitlist entry stands in its lottery. */
enum class WaitlistStatus {
    /** On the waitlist; the draw hasn't picked this entry yet. */
    WAITING,
    /** Drawn as a winner and invited to book. */
    WON,
    /** Not drawn. */
    LOST,
    /** Won but turned down the invitation. */
    DECLINED,
    /** Won and confirmed the reservation. */
    RESERVED
}
