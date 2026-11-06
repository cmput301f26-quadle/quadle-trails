// Establishes the device identity before the app is shown (US 01.09.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: None currently known.
// AI assistance: Used AI to help implement device identity initialization and retry handling.
package com.example.trails.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.fake.CurrentUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Whether the user's identity is ready. */
sealed interface IdentityState {

    /** The identity is being established. */
    data object Starting : IdentityState

    /**
     * The identity is established.
     *
     * @property id the user's id.
     */
    data class Ready(val id: String) : IdentityState

    /**
     * The identity could not be established.
     *
     * @property reason the error message, if there is one.
     */
    data class Failed(val reason: String?) : IdentityState
}

/**
 * Establishes the identity whenever there is no session: on first launch, and
 * again after a profile is deleted and a new session starts.
 *
 * @param session the current session id, or null when none is established.
 * @param establish establishes an identity and publishes it on [session].
 */
class IdentityViewModel(
    private val session: StateFlow<String?> = CurrentUser.session,
    private val establish: suspend () -> String = { CurrentUser.establish() }
) : ViewModel() {

    private val _state = MutableStateFlow<IdentityState>(IdentityState.Starting)

    /** Whether the identity is ready. */
    val state: StateFlow<IdentityState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            session.collect { id ->
                if (id != null) _state.value = IdentityState.Ready(id) else attempt()
            }
        }
    }

    /** Tries again after [IdentityState.Failed]. */
    fun retry() {
        if (_state.value is IdentityState.Failed) viewModelScope.launch { attempt() }
    }

    private suspend fun attempt() {
        _state.value = IdentityState.Starting
        try {
            establish()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = IdentityState.Failed(e.message)
        }
    }
}
