package com.example.trails.ui.notification

import androidx.lifecycle.ViewModel
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.FakeNotificationRepository
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Notification
import com.example.trails.data.model.NotificationType
import com.example.trails.data.model.Reservation
import com.example.trails.data.repository.NotificationRepository

class NotificationViewModel(
    private val repository: NotificationRepository = AppContainer.notificationRepository,
) : ViewModel() {
    fun fetchAll(userId: String) : List<Notification> {
        return repository.getNotif(userId)
    }
    fun sendNotif(userId: String, title: String, content: String, type: NotificationType, related: Reservation?) {
        repository.sendNotif(userId, title, content, type, related)
    }
}