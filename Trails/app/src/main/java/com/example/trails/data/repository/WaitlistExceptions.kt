// Domain exceptions for waitlist operations.
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
package com.example.trails.data.repository

/**
 * Thrown when attempting to join a waitlist that has reached maximum capacity.
 */
class WaitlistFullException(
    message: String = "The waitlist for this lottery has reached maximum capacity."
) : IllegalStateException(message)

/**
 * Thrown when attempting to join a waitlist or confirm a reservation without accepting rules.
 */
class RulesNotAcceptedException(
    message: String = "You must review and accept the campsite rules before joining the waitlist."
) : IllegalArgumentException(message)
