// Enum model representing lifecycle states of a waitlist entry from enrollment to reservation (Model in MVVM).
// implements US 01.02.01, US 01.02.02
// Outstanding issues: None
// OWNER: shared — Waitlist domain
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
