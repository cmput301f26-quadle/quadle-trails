// A start/end window, used for campsite availability.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.model

import com.google.firebase.Timestamp

data class DateRange(val start: Timestamp? = null, val end: Timestamp? = null)
