package com.example.trails.ui.reservation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.NotificationType
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus
import com.example.trails.data.repository.ReservationRepository
import com.example.trails.ui.notification.NotificationViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch

class ReservationViewModel(
    private val repository: ReservationRepository = AppContainer.reservationRepository
) : ViewModel() {
    var currentList : List<Reservation> = listOf()

    fun userReserveListener() : Flow<List<Reservation>> {
        return repository.observeForUser(CurrentUser.id())
    }

    init {
        val reserveList = repository.observeForUser(CurrentUser.id())

        viewModelScope.launch {
            reserveList
                .distinctUntilChanged()
                .runningFold(emptyList<Reservation>() to emptyList<Reservation>()) { (_, previous), current ->
                    previous to current
                }
                .drop(1)
                .collect { (oldList, newList) ->
                    val added = newList - oldList.toSet();
                    val removed = oldList - newList.toSet();
                    currentList = newList
                    for (item in added) {
                        if (item.status == ReservationStatus.ACTIVE) {
                            if (AppContainer.notificationRepository.checkNotif(item)) {
                                NotificationViewModel().sendNotif(userId = CurrentUser.id(),
                                    title = "Reservation Confirmed",
                                    content = "",
                                    type = NotificationType.RESERVE_CONFIRM,
                                    related = item)
                            }
                        }
                    }
                }
        }
    }
}