package com.example.trails.data.repository

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Notification
import com.example.trails.data.model.NotificationType
import com.example.trails.data.model.Reservation

interface NotificationRepository {
    fun getNotif(userId: String) : List<Notification>

    fun sendNotif(userId: String, title: String, content: String, type: NotificationType, related: Reservation?)
}