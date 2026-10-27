// Date helpers for the sample data; relative dates keep the sample lottery open whenever the app is run.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.fake

import com.google.firebase.Timestamp
import java.util.Date
import java.util.GregorianCalendar
import java.util.concurrent.TimeUnit

internal fun daysFromNow(days: Long) =
    Timestamp(Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days)))

// Midnight local time on a fixed date; month is 1-based (5 = May).
internal fun dateOf(year: Int, month: Int, day: Int) =
    Timestamp(GregorianCalendar(year, month - 1, day).time)
