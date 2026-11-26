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
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch

/**
 * View model class for reservations.
 *
 * When initialized, will check current list of reservations in the repository, and if anything is changed, send the corresponding notification.
 *
 * @param repository The reservation repository to use.
 * @property currentList The current list of reservations that this model is aware of.
 */
class ReservationViewModel(
    private val repository: ReservationRepository = AppContainer.reservationRepository
) : ViewModel() {
    var currentList : List<Reservation> = listOf()

    /**
     * To emit a flow of reservations of the current user.
     */
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
                    val added = newList - oldList.toSet()
                    val removed = oldList - newList.toSet()
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
                        } else if (item.status == ReservationStatus.CANCELLED) {
                            if (AppContainer.notificationRepository.checkCancelledNotif(item)) {
                                NotificationViewModel().sendNotif(userId = CurrentUser.id(),
                                    title = "Reservation Cancelled",
                                    content = "",
                                    type = NotificationType.RESERVE_CANCEL,
                                    related = item)
                            }
                        }
                    }
                }
        }
    }
}