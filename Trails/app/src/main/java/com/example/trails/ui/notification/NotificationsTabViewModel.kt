// Controller for the Notifications tab: loads the user's notifications live and sends any
// lottery results they have not seen yet (US 01.03.01, 01.03.02).
// OWNER: Aarav (aarav-kapoor-cs) — Notifications
// Outstanding issues: None currently known.
// AI assistance: Used AI to help implement the live notification list with loading and error states.
package com.example.trails.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.Notification
import com.example.trails.data.repository.NotificationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Every situation the Notifications tab can be in. */
sealed interface NotificationsUiState {

    /** The notifications are loading. */
    data object Loading : NotificationsUiState

    /**
     * The notifications are loaded.
     *
     * @property items the user's notifications, newest first; may be empty.
     */
    data class Loaded(val items: List<Notification>) : NotificationsUiState

    /**
     * The notifications could not be loaded, for example with no connection.
     *
     * @property reason the error message, if there is one.
     */
    data class Error(val reason: String?) : NotificationsUiState
}

/**
 * Watches the current user's notifications, and on opening checks for drawn lotteries
 * so "You won" / "not picked" appear without the user doing anything.
 *
 * @param userId the current user.
 * @param repository where notifications are stored.
 * @param syncLotteryResults sends lottery results the user has not been told about yet.
 */
class NotificationsTabViewModel(
    private val userId: String = CurrentUser.id(),
    private val repository: NotificationRepository = AppContainer.notificationRepository,
    private val syncLotteryResults: suspend (String) -> Unit = { id ->
        LotteryResultNotifier(
            AppContainer.lotteryRepository,
            AppContainer.waitlistRepository,
            AppContainer.campsiteRepository,
            AppContainer.userRepository,
            AppContainer.notificationRepository
        ).sync(id)
    }
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)

    /** What the tab shows; starts as Loading. */
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private var watching: Job? = null

    init {
        load()
    }

    /** Starts (or restarts after an error) watching the notifications. */
    fun load() {
        watching?.cancel()
        _uiState.value = NotificationsUiState.Loading
        watching = viewModelScope.launch {
            try {
                repository.observeForUser(userId).collect { _uiState.value = NotificationsUiState.Loaded(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = NotificationsUiState.Error(e.message)
            }
        }
        // Separate job: a failed result check must not hide the notifications already stored.
        viewModelScope.launch {
            try {
                syncLotteryResults(userId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }
}
