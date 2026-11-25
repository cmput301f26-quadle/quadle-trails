package com.example.trails.ui.testing

import androidx.lifecycle.SavedStateHandle
import com.example.trails.data.fake.FakeUserRepository
import com.example.trails.data.fake.TestUsers
import com.example.trails.data.model.Role
import com.example.trails.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TestingRoleViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val users = FakeUserRepository()
    private var activatedId: String? = null

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun freshLaunch_waitsForSelection() = runTest {
        val controller = TestingRoleViewModel(SavedStateHandle(), users) { activatedId = it }
        advanceUntilIdle()
        assertFalse(controller.state.value.ready)
        assertNull(activatedId)
    }

    @Test fun everyRole_selectsItsUserAndRestoresTheRequestedMode() = runTest {
        for ((role, id) in listOf(
            Role.ENTRANT to TestUsers.ENTRANT_ID,
            Role.OWNER to TestUsers.OWNER_ID,
            Role.ADMIN to TestUsers.ADMIN_ID
        )) {
            users.updateActiveMode(id, Role.ENTRANT)
            val controller = TestingRoleViewModel(SavedStateHandle(), users) {
                // Activation must happen only after the mode has been saved.
                activatedId = it
            }
            controller.selectRole(role)
            advanceUntilIdle()
            assertEquals(id, activatedId)
            assertEquals(role, users.getById(id)?.activeMode)
            assertTrue(controller.state.value.ready)
        }
    }

    @Test fun deletedSample_canBeSelectedAgain() = runTest {
        users.delete(TestUsers.OWNER_ID)
        val controller = TestingRoleViewModel(SavedStateHandle(), users) { activatedId = it }
        controller.selectRole(Role.OWNER)
        advanceUntilIdle()
        assertTrue(controller.state.value.ready)
        assertEquals(Role.OWNER, users.getById(TestUsers.OWNER_ID)?.activeMode)
    }

    @Test fun savedSelection_restoresSessionAfterProcessDeath() = runTest {
        val saved = SavedStateHandle()
        val first = TestingRoleViewModel(saved, users) { activatedId = it }
        first.selectRole(Role.ADMIN)
        advanceUntilIdle()
        activatedId = null
        val restored = TestingRoleViewModel(saved, FakeUserRepository()) { activatedId = it }
        advanceUntilIdle()
        assertTrue(restored.state.value.ready)
        assertEquals(TestUsers.ADMIN_ID, activatedId)
    }

    @Test fun failure_keepsAppClosedAndAllowsRetry() = runTest {
        var fail = true
        val flaky = object : UserRepository by users {
            override suspend fun updateActiveMode(deviceId: String, mode: Role) {
                if (fail) error("Could not save mode")
                users.updateActiveMode(deviceId, mode)
            }
        }
        val controller = TestingRoleViewModel(SavedStateHandle(), flaky) { activatedId = it }
        controller.selectRole(Role.ADMIN)
        advanceUntilIdle()
        assertTrue(controller.state.value.failed)
        assertFalse(controller.state.value.ready)
        assertNull(activatedId)
        fail = false
        controller.selectRole(Role.ADMIN)
        advanceUntilIdle()
        assertTrue(controller.state.value.ready)
        assertEquals(TestUsers.ADMIN_ID, activatedId)
    }

    @Test fun repeatedTaps_cannotStartTwoSessions() = runTest {
        val activations = mutableListOf<String>()
        val controller = TestingRoleViewModel(SavedStateHandle(), users) { activations.add(it) }
        controller.selectRole(Role.OWNER)
        controller.selectRole(Role.ADMIN)
        advanceUntilIdle()
        assertEquals(listOf(TestUsers.OWNER_ID), activations)
    }
}
