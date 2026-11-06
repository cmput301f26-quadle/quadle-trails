// A start/end window, used for campsite availability.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
// Outstanding issues: none.
// AI assistance: Used AI (Claude Code) to help add the outstanding issues line to this header.
package com.example.trails.data.model

import com.google.firebase.Timestamp

/**
 * A window of time between two timestamps.
 *
 * @property start When the window opens.
 * @property end When the window closes.
 */
data class DateRange(val start: Timestamp? = null, val end: Timestamp? = null)
