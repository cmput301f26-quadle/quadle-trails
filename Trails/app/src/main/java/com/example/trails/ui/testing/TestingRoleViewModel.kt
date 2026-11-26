package com.example.trails.ui.testing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
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
    private val activateUser: (String) -> Unit = CurrentUser::useSampleUser
) : ViewModel() {
    private val _state = MutableStateFlow(TestingRoleState())
    val state = _state.asStateFlow()

    init {
        // The ViewModel survives rotation; saved state also restores after process death.
        savedState.get<String>("testing_role")?.let { name ->
            Role.entries.find { it.name == name }?.let(::selectRole)
        }
    }

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
                // A tester may have deleted the sample profile during a previous session.
                if (users.getById(id) == null) users.save(TestUsers.all.first { it.deviceId == id })
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
}

data class TestingRoleState(
    val loading: Boolean = false,
    val ready: Boolean = false,
    val failed: Boolean = false
)
