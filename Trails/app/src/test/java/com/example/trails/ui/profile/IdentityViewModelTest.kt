// Unit tests for establishing the device identity (US 01.09.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: None currently known.
// AI assistance: Used AI to help write device identity initialization and retry tests.
package com.example.trails.ui.profile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class IdentityViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val session = MutableStateFlow<String?>(null)
    private var nextId = 1
    private var failuresLeft = 0

    /** Issues "uid-1", "uid-2", ... and publishes it, failing [failuresLeft] times first. */
    private suspend fun establish(): String {
        if (failuresLeft > 0) {
            failuresLeft--
            throw IOException("offline")
        }
        val id = "uid-${nextId++}"
        session.value = id
        return id
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun firstLaunch_establishesAnIdentity() = runTest {
        val viewModel = IdentityViewModel(session, ::establish)
        advanceUntilIdle()
        assertEquals(IdentityState.Ready("uid-1"), viewModel.state.value)
    }

    @Test
    fun existingSession_isUsedWithoutSigningInAgain() = runTest {
        session.value = "uid-existing"
        val viewModel = IdentityViewModel(session, ::establish)
        advanceUntilIdle()
        assertEquals(IdentityState.Ready("uid-existing"), viewModel.state.value)
        assertEquals(1, nextId)
    }

    @Test
    fun failure_isReported() = runTest {
        failuresLeft = 1
        val viewModel = IdentityViewModel(session, ::establish)
        advanceUntilIdle()
        assertEquals(IdentityState.Failed("offline"), viewModel.state.value)
    }

    @Test
    fun retry_afterFailure_establishesTheIdentity() = runTest {
        failuresLeft = 1
        val viewModel = IdentityViewModel(session, ::establish)
        advanceUntilIdle()
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(IdentityState.Ready("uid-1"), viewModel.state.value)
    }

    @Test
    fun endedSession_establishesANewIdentity() = runTest {
        val viewModel = IdentityViewModel(session, ::establish)
        advanceUntilIdle()
        session.value = null
        advanceUntilIdle()
        assertEquals(IdentityState.Ready("uid-2"), viewModel.state.value)
    }
}
