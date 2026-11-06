// Handles loading, editing and deleting the profile, and switching modes (US 01.09.01, 01.07.03, 01.08.01, 03.11.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: Profile photos are not supported, and deleting a profile does not remove reservations yet.
// AI assistance: Used AI to help implement profile updates, deletion, and related data handling.
package com.example.trails.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.device.formatDeviceId
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.example.trails.data.repository.LotteryRepository
import com.example.trails.data.repository.UserRepository
import com.example.trails.data.repository.WaitlistRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Loads this device's profile and carries out the four profile stories.
 *
 * @param repository where profiles are stored.
 * @param deviceId the current user's id.
 * @param lotteryRepository used to find the lotteries the user may have joined.
 * @param waitlistRepository used to remove the user's waitlist entries on deletion.
 * @param deleteIdentity removes the user's identity after the profile is deleted.
 * @param startNewSession ends the current session, so a new identity is established.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val repository: UserRepository = AppContainer.userRepository,
    private val deviceId: String = CurrentUser.id(),
    private val lotteryRepository: LotteryRepository = AppContainer.lotteryRepository,
    private val waitlistRepository: WaitlistRepository = AppContainer.waitlistRepository,
    private val deleteIdentity: suspend () -> Unit = { CurrentUser.deleteIdentity() },
    private val startNewSession: () -> Unit = { CurrentUser.endSession() }
) : ViewModel() {

    private val reloads = MutableStateFlow(0)
    private val deleted = MutableStateFlow(false)

    /** The profile screen state; starts as [ProfileUiState.Loading]. */
    val uiState: StateFlow<ProfileUiState> =
        combine(reloads.flatMapLatest { profileStates() }, deleted, ::withDeletion)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState.Loading)

    /** The masked id for the "Device identifier" row. */
    val deviceIdLabel: String = formatDeviceId(deviceId)

    private val _form = MutableStateFlow(ProfileForm())

    /** The values currently typed into Edit Profile. */
    val form: StateFlow<ProfileForm> = _form.asStateFlow()

    private val _errors = MutableStateFlow(ProfileFormErrors())

    /** Validation problems; empty until Save is pressed. */
    val errors: StateFlow<ProfileFormErrors> = _errors.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)

    /** Progress of the latest save. */
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)

    /** Progress of deleting the profile. */
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    private val _modeSwitch = MutableStateFlow<ModeSwitchState>(ModeSwitchState.Idle)

    /** Progress of switching modes. */
    val modeSwitch: StateFlow<ModeSwitchState> = _modeSwitch.asStateFlow()

    private fun profileStates(): Flow<ProfileUiState> {
        if (deviceId.isBlank()) return flowOf(ProfileUiState.Error(null))
        return flow { emitAll(repository.observe(deviceId)) }
            .map { buildProfileState(it) }
            .onStart { emit(ProfileUiState.Loading) }
            .catch { emit(ProfileUiState.Error(it.message)) }
    }

    /** Loads the profile again after [ProfileUiState.Error]. */
    fun retry() {
        reloads.value += 1
    }

    /** Creates an entrant profile for this device if it has none (US 01.09.01). */
    fun ensureProfileExists() {
        if (deviceId.isBlank()) return
        viewModelScope.launch {
            try {
                if (repository.getById(deviceId) == null) repository.save(newProfileFor(deviceId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _saveState.value = SaveState.Failed(e.message)
            }
        }
    }

    /**
     * Loads the profile's current values into the form.
     *
     * @param user the profile being edited.
     */
    fun startEditing(user: User) {
        _form.value = user.toProfileForm()
        _errors.value = ProfileFormErrors()
        _saveState.value = SaveState.Idle
    }

    /** @param value the new name. Clears any name error. */
    fun onNameChange(value: String) {
        _form.value = _form.value.copy(name = value)
        if (_errors.value.name != null) _errors.value = _errors.value.copy(name = null)
    }

    /** @param value the new email. Clears any email error. */
    fun onEmailChange(value: String) {
        _form.value = _form.value.copy(email = value)
        if (_errors.value.email != null) _errors.value = _errors.value.copy(email = null)
    }

    /** @param value the new phone. Clears any phone error. */
    fun onPhoneChange(value: String) {
        _form.value = _form.value.copy(phone = value)
        if (_errors.value.phone != null) _errors.value = _errors.value.copy(phone = null)
    }

    /**
     * Validates the form and saves the name, email and phone (US 01.07.03).
     * Nothing else on the profile is written. On failure the typed values are kept.
     */
    fun onSaveClicked() {
        if (_saveState.value == SaveState.Saving) return
        if (uiState.value !is ProfileUiState.Loaded) return

        val found = validateProfileForm(_form.value)
        _errors.value = found
        if (found.hasErrors) return

        val values = _form.value.trimmed()
        _saveState.value = SaveState.Saving
        viewModelScope.launch {
            _saveState.value = try {
                repository.updateProfile(deviceId, values.name, values.email, values.phone)
                SaveState.Saved
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SaveState.Failed(e.message)
            }
        }
    }

    /** Clears a finished or failed save once the screen has shown it. */
    fun onSaveStateShown() {
        if (_saveState.value != SaveState.Saving) _saveState.value = SaveState.Idle
    }

    /**
     * Deletes the profile (US 01.08.01): the user's waitlist entries first, then the
     * profile, then the identity. If any step fails the profile stays available.
     */
    fun onDeleteConfirmed() {
        if (_deleteState.value == DeleteState.Deleting) return
        _deleteState.value = DeleteState.Deleting
        viewModelScope.launch {
            _deleteState.value = try {
                removeWaitlistEntries()
                repository.delete(deviceId)
                deleteIdentity()
                deleted.value = true
                DeleteState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DeleteState.Failed(e.message)
            }
        }
    }

    private suspend fun removeWaitlistEntries() {
        lotteryRepository.observeAll().first().forEach { lottery ->
            waitlistRepository.deleteEntry(lottery.id, deviceId)
        }
    }

    /** Clears a failed deletion once the screen has shown it. */
    fun onDeleteStateShown() {
        if (_deleteState.value != DeleteState.Deleting) _deleteState.value = DeleteState.Idle
    }

    /** After deletion, starts over as a new user on this device. */
    fun onStartOver() {
        deleted.value = false
        _deleteState.value = DeleteState.Idle
        startNewSession()
    }

    /**
     * Switches which role the app acts as (US 03.11.01). A role the user does not
     * hold, or the mode already in use, is ignored.
     *
     * @param role the mode to act as.
     */
    fun onModeSelected(role: Role) {
        if (_modeSwitch.value == ModeSwitchState.Switching) return
        val current = (uiState.value as? ProfileUiState.Loaded)?.user ?: return
        if (!canSwitchTo(current, role) || effectiveMode(current) == role) return

        _modeSwitch.value = ModeSwitchState.Switching
        viewModelScope.launch {
            _modeSwitch.value = try {
                repository.updateActiveMode(deviceId, role)
                ModeSwitchState.Switched(role)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ModeSwitchState.Failed(e.message)
            }
        }
    }

    /** Clears [modeSwitch] once the screen has navigated or shown the error. */
    fun onModeSwitchHandled() {
        if (_modeSwitch.value != ModeSwitchState.Switching) _modeSwitch.value = ModeSwitchState.Idle
    }
}
