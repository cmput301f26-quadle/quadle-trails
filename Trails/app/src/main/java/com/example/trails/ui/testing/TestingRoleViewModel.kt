package com.example.trails.ui.testing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.device.FirebaseDeviceIdProvider
import com.example.trails.data.device.FixedDeviceIdProvider
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Role
import com.example.trails.data.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Starts a sample session before any user-specific screen or ViewModel is created. */
class TestingRoleViewModel(
    private val savedState: SavedStateHandle,
    private val users: UserRepository = AppContainer.userRepository,
    /** Switches to the real device identity (Firebase anonymous sign-in, US 01.09.01). */
    private val useThisDevice: () -> Unit = {
        if (CurrentUser.canUseSampleUsers) CurrentUser.install(FirebaseDeviceIdProvider())
    },
    private val activateUser: (String) -> Unit = { id ->
        // Back to sample identities, even if this process used the real device before.
        CurrentUser.install(FixedDeviceIdProvider(id))
        CurrentUser.useSampleUser(id)
    }
) : ViewModel() {
    private val _state = MutableStateFlow(TestingRoleState())
    val state = _state.asStateFlow()

    init {
        // The ViewModel survives rotation; saved state also restores after process death.
        savedState.get<String>("testing_role")?.let { name ->
            if (name == THIS_DEVICE) selectThisDevice()
            else Role.entries.find { it.name == name }?.let(::selectRole)
        }
    }

    /**
     * Starts a session as the sample user for [role] (Priya, Daniel or Morgan) and
     * opens the app in that mode.
     *
     * @param role the testing role picked in the dialog.
     */
    fun selectRole(role: Role) {
        if (_state.value.loading || _state.value.ready) return
        _state.value = TestingRoleState(loading = true)
        viewModelScope.launch {
            try {
                val id = when (role) {
                    Role.ENTRANT -> TestUsers.ENTRANT_ID
                    Role.OWNER -> TestUsers.OWNER_ID
                    Role.ADMIN -> TestUsers.ADMIN_ID
                }
                // Put back any sample profile that is missing (first run on a fresh Firestore,
                // or a tester deleted it), so admin and owner screens have all three people.
                TestUsers.all.forEach { sample ->
                    if (users.getById(sample.deviceId) == null) users.save(sample)
                }
                users.updateActiveMode(id, role)
                activateUser(id)
                savedState["testing_role"] = role.name
                _state.value = TestingRoleState(ready = true)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = TestingRoleState(failed = true)
            }
        }
    }

    /**
     * Uses this device's real identity instead of a sample profile (US 01.09.01), so
     * Welcome / Welcome back and Firestore profiles can be tested in a debug build.
     */
    fun selectThisDevice() {
        if (_state.value.loading || _state.value.ready) return
        useThisDevice()
        savedState["testing_role"] = THIS_DEVICE
        _state.value = TestingRoleState(ready = true, usesDevice = true)
    }
}

/** Saved-state value meaning "use this device's real identity". */
private const val THIS_DEVICE = "DEVICE"

/**
 * What the testing dialog shows.
 *
 * @property loading a sample session is being started.
 * @property ready the app can be shown.
 * @property failed starting the session failed; the dialog stays open to retry.
 */
data class TestingRoleState(
    val loading: Boolean = false,
    val ready: Boolean = false,
    val failed: Boolean = false,
    /** True when the session uses the real device identity rather than a sample profile. */
    val usesDevice: Boolean = false
)
