// domain exceptions for waitlist operations
// owner: chitkarsh (chitkarshrathi) - waitlist
package com.example.trails.data.repository

// thrown when lottery waitlist reaches max capacity
class WaitlistFullException(
    message: String = "The waitlist for this lottery has reached maximum capacity."
) : IllegalStateException(message)

// thrown when entrant tries to join without agreeing to campsite rules
class RulesNotAcceptedException(
    message: String = "You must review and accept the campsite rules before joining the waitlist."
) : IllegalArgumentException(message)
