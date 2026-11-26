// Controllers for Sent notifications and one sent notification's details (US 02.10.02).
// OWNER: Aarav (aarav-kapoor-cs) — Owner notifications
// Outstanding issues: None currently known.
// AI assistance: Used AI to help implement the sent-notification history and its filters.
package com.example.trails.ui.ownernotify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.SentNotification
import com.example.trails.data.repository.SentNotificationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Everything Sent notifications needs to draw.
 *
 * @property isLoading true until the first data arrives.
 * @property filter the selected chip.
 * @property items the notifications under that chip, newest first.
 * @property error the loading error, or null.
 */
data class SentListUiState(
    val isLoading: Boolean = true,
    val filter: SentFilter = SentFilter.ALL,
    val items: List<SentNotification> = emptyList(),
    val error: String? = null
)

/**
 * Watches everything the owner has sent, filtered by the selected chip.
 *
 * @param ownerId the owner.
 * @param repository where sent notifications are stored.
 * @param now the current time in milliseconds; replaceable in tests.
 */
class SentNotificationsViewModel(
    private val ownerId: String = CurrentUser.id(),
    private val repository: SentNotificationRepository = OwnerNotifyContainer.sentNotificationRepository,
    private val now: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val filter = MutableStateFlow(SentFilter.ALL)
    private val _state = MutableStateFlow(SentListUiState())

    /** The list state; starts loading. */
    val state: StateFlow<SentListUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.observeByOwner(ownerId), filter) { sent, chip ->
                SentListUiState(isLoading = false, filter = chip, items = filterSent(sent, chip, now()))
            }
                .catch { emit(SentListUiState(isLoading = false, filter = filter.value, error = it.message ?: "")) }
                .collect { _state.value = it }
        }
    }

    /** @param chip the chip tapped. */
    fun onFilterChange(chip: SentFilter) {
        filter.value = chip
    }
}

/** Every situation the details screen can be in. */
sealed interface SentDetailsUiState {

    /** Loading. */
    data object Loading : SentDetailsUiState

    /** No notification has this id. */
    data object NotFound : SentDetailsUiState

    /**
     * Loading failed.
     *
     * @property reason the error message, if there is one.
     */
    data class Error(val reason: String?) : SentDetailsUiState

    /**
     * Loaded.
     *
     * @property notification the sent notification.
     */
    data class Loaded(val notification: SentNotification) : SentDetailsUiState
}

/**
 * Loads one sent notification for its details screen.
 *
 * @param id the notification's id.
 * @param repository where sent notifications are stored.
 */
class SentNotificationDetailsViewModel(
    private val id: String,
    private val repository: SentNotificationRepository = OwnerNotifyContainer.sentNotificationRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SentDetailsUiState>(SentDetailsUiState.Loading)

    /** The details state; starts as Loading. */
    val state: StateFlow<SentDetailsUiState> = _state.asStateFlow()

    init {
        load()
    }

    /** Loads (or reloads after an error) the notification. */
    fun load() {
        _state.value = SentDetailsUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                repository.getById(id)?.let { SentDetailsUiState.Loaded(it) } ?: SentDetailsUiState.NotFound
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SentDetailsUiState.Error(e.message)
            }
        }
    }
}
