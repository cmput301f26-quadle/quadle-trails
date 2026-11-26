package com.example.trails.data.model

import com.google.firebase.Timestamp

data class Notification(
    val recipient: String = "",
    val title: String = "",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val type: NotificationType,
    val related: Reservation?
)