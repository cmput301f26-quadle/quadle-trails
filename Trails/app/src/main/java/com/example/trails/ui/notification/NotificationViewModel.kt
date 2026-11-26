package com.example.trails.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.fake.FakeNotificationRepository
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Notification
import com.example.trails.data.model.NotificationType
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus
import com.example.trails.data.repository.NotificationRepository
import com.example.trails.data.repository.ReservationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch
import kotlin.to

class NotificationViewModel(
    private val repository: NotificationRepository = AppContainer.notificationRepository,
    private val reservationRepo: ReservationRepository = AppContainer.reservationRepository
) : ViewModel() {
    fun fetchAll(userId: String) : List<Notification> {
        return repository.getNotif(userId)
    }
    fun sendNotif(userId: String, title: String, content: String, type: NotificationType, related: Reservation?) {
        repository.sendNotif(userId, title, content, type, related)
    }

    init {

    }
}