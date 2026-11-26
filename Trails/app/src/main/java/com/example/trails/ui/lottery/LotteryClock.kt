package com.example.trails.ui.lottery

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

/** Refresh deadline-based UI even when Firestore has not emitted a new snapshot. */
internal fun lotteryClock(clock: () -> Long) = flow {
    while (true) {
        emit(clock())
        delay(1_000)
    }
}
