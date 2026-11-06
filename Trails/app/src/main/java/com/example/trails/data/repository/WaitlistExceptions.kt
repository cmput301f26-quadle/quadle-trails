// Domain exception definitions enforcing business constraints for lottery waitlists.
// implements US 01.02.01, US 02.11.01
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
package com.example.trails.data.repository

// thrown when lottery waitlist reaches max capacity
/**
 * Thrown when attempting to join a lottery waitlist that has reached maximum capacity.
 *
 * @param message description of the capacity limit error.
 */
class WaitlistFullException(
    message: String = "The waitlist for this lottery has reached maximum capacity."
) : IllegalStateException(message)

// thrown when entrant tries to join without agreeing to campsite rules
/**
 * Thrown when an entrant attempts to join a waitlist without agreeing to campsite rules.
 *
 * @param message description of the rules requirement error.
 */
class RulesNotAcceptedException(
    message: String = "You must review and accept the campsite rules before joining the waitlist."
) : IllegalArgumentException(message)
