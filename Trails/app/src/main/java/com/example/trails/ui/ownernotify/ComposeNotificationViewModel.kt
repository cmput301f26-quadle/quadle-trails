// Controller for New notification: loads a lottery's waitlist and sends it a notification (US 02.10.01).
// OWNER: Aarav (aarav-kapoor-cs) — Owner notifications
// Outstanding issues: The notification always goes to the whole waitlist; picking recipients is not built.
// AI assistance: Used AI to help implement sending a notification to every waitlist entrant.
package com.example.trails.ui.ownernotify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.NotificationType
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.LotteryRepository
import com.example.trails.data.repository.NotificationRepository
import com.example.trails.data.repository.SentNotificationRepository
import com.example.trails.data.repository.WaitlistRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Loads one lottery and its waitlist, and sends the owner's notification to every entrant.
 *
 * @param lotteryId the lottery whose waitlist is notified.
 * @param ownerId the owner sending it.
 * @param lotteryRepository used to read the lottery.
 * @param campsiteRepository used to show the campsite's name.
 * @param waitlistRepository used to find the entrants.
 * @param notificationRepository delivers the notification to each entrant.
 * @param sentRepository keeps the owner's copy for Sent notifications.
 * @param clock the send time; replaceable in tests.
 */
class ComposeNotificationViewModel(
    private val lotteryId: String,
    private val ownerId: String = CurrentUser.id(),
    private val lotteryRepository: LotteryRepository = AppContainer.lotteryRepository,
    private val campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    private val waitlistRepository: WaitlistRepository = AppContainer.waitlistRepository,
    private val notificationRepository: NotificationRepository = AppContainer.notificationRepository,
    private val sentRepository: SentNotificationRepository = OwnerNotifyContainer.sentNotificationRepository,
    private val clock: () -> Timestamp = { Timestamp.now() }
) : ViewModel() {

    private val _uiState = MutableStateFlow<ComposeUiState>(ComposeUiState.Loading)

    /** The lottery and its recipients; starts as Loading. */
    val uiState: StateFlow<ComposeUiState> = _uiState.asStateFlow()

    private val _form = MutableStateFlow(NotifyForm())

    /** What the owner has typed. */
    val form: StateFlow<NotifyForm> = _form.asStateFlow()

    private val _errors = MutableStateFlow(NotifyFormErrors())

    /** Problems found when Send was pressed. */
    val errors: StateFlow<NotifyFormErrors> = _errors.asStateFlow()

    private val _sendState = MutableStateFlow<SendState>(SendState.Idle)

    /** Progress of sending. */
    val sendState: StateFlow<SendState> = _sendState.asStateFlow()

    init {
        load()
    }

    /** Loads (or reloads after an error) the lottery, its campsite name and its waitlist. */
    fun load() {
        _uiState.value = ComposeUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val lottery = lotteryRepository.getById(lotteryId)
                if (lottery == null) {
                    ComposeUiState.NotFound
                } else {
                    val campsiteName = lottery.campsiteIds.firstOrNull()
                        ?.let { runCatching { campsiteRepository.getById(it)?.name }.getOrNull() }
                        .orEmpty()
                    val entries = waitlistRepository.getEntriesForLottery(lotteryId)
                    ComposeUiState.Ready(lottery, campsiteName, recipientsFrom(entries))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ComposeUiState.Error(e.message)
            }
        }
    }

    /** @param value the new subject. Clears its error. */
    fun onSubjectChange(value: String) {
        _form.value = _form.value.copy(subject = value)
        if (_errors.value.subject != null) _errors.value = _errors.value.copy(subject = null)
    }

    /** @param value the new message, cut at [MAX_MESSAGE] characters. Clears its error. */
    fun onMessageChange(value: String) {
        _form.value = _form.value.copy(message = value.take(MAX_MESSAGE))
        if (_errors.value.message != null) _errors.value = _errors.value.copy(message = null)
    }

    /**
     * Checks the form and sends it to everyone on the waitlist (US 02.10.01). The owner's
     * copy is saved first, so a failure leaves no entrant with a message the owner can't see.
     */
    fun onSend() {
        if (_sendState.value == SendState.Sending) return
        val ready = _uiState.value as? ComposeUiState.Ready ?: return
        if (ready.recipients.isEmpty()) return

        val found = validateNotifyForm(_form.value)
        _errors.value = found
        if (found.hasErrors) return

        val record = buildSentNotification(ownerId, ready.lottery, ready.campsiteName, _form.value, ready.recipients, clock())
        _sendState.value = SendState.Sending
        viewModelScope.launch {
            _sendState.value = try {
                sentRepository.save(record)
                record.recipients.forEach { recipient ->
                    notificationRepository.sendNotif(
                        userId = recipient.userId,
                        title = record.subject,
                        content = record.message,
                        type = NotificationType.OWNER_MESSAGE,
                        related = null
                    )
                }
                SendState.Sent(record.subject, record.recipients.size, waitlistLabel(record.campsiteName, record.lotteryName))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SendState.Failed(e.message)
            }
        }
    }

    /** Clears a failed send once the screen has shown it. */
    fun onSendStateShown() {
        if (_sendState.value is SendState.Failed) _sendState.value = SendState.Idle
    }
}
