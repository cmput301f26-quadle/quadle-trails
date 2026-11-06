// Unit tests for the profile ViewModel (US 01.09.01, 01.07.03, 01.08.01, 03.11.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: None currently known.
// AI assistance: Used AI to help write coroutine-based profile ViewModel tests.
package com.example.trails.ui.profile

import com.example.trails.data.fake.FakeLotteryRepository
import com.example.trails.data.fake.FakeUserRepository
import com.example.trails.data.fake.FakeWaitlistRepository
import com.example.trails.data.model.Lottery
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.example.trails.data.model.WaitlistEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val deviceId = "device-a"

    private val priya = User(
        deviceId = deviceId,
        name = "Priya Sharma",
        email = "priya.sharma@example.com",
        phone = "780-555-0142"
    )
    private val morgan = User(
        deviceId = deviceId,
        name = "Morgan Lee",
        email = "morgan.lee@example.com",
        roles = listOf(Role.ENTRANT, Role.ADMIN),
        activeMode = Role.ADMIN
    )
    private val otherUser = User(deviceId = "device-b", name = "Daniel Whitehorse", email = "d@example.com")

    private lateinit var repo: FlakyUserRepository
    private lateinit var waitlist: FakeWaitlistRepository
    private lateinit var viewModel: ProfileViewModel
    private var identityDeletions = 0
    private var newSessions = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Builds the ViewModel for [id] and keeps uiState collected; uiState is a
     * WhileSubscribed flow, so without a collector it would stay Loading.
     *
     * @param users the users to seed the repository with.
     * @param id the device id of the user being tested.
     * @return the collector job.
     */
    private fun TestScope.start(users: List<User>, id: String = deviceId): Job {
        repo = FlakyUserRepository(FakeUserRepository(users))
        waitlist = FakeWaitlistRepository(
            listOf(
                WaitlistEntry(userId = deviceId, lotteryId = "lottery-1"),
                WaitlistEntry(userId = deviceId, lotteryId = "lottery-2"),
                WaitlistEntry(userId = "device-b", lotteryId = "lottery-1")
            )
        )
        identityDeletions = 0
        newSessions = 0
        viewModel = ProfileViewModel(
            repository = repo,
            deviceId = id,
            lotteryRepository = FakeLotteryRepository(listOf(Lottery(id = "lottery-1"), Lottery(id = "lottery-2"))),
            waitlistRepository = waitlist,
            deleteIdentity = { identityDeletions++ },
            startNewSession = { newSessions++ }
        )
        val job = launch(testDispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
        return job
    }

    // US 01.09.01

    @Test
    fun deviceWithNoProfile_showsWelcome() = runTest {
        val job = start(emptyList())
        assertEquals(ProfileUiState.NoProfile, viewModel.uiState.value)
        job.cancel()
    }

    @Test
    fun deviceWithProfile_showsThatProfile() = runTest {
        val job = start(listOf(priya, otherUser))
        assertEquals(ProfileUiState.Loaded(priya), viewModel.uiState.value)
        job.cancel()
    }

    @Test
    fun noIdentity_showsErrorAndNeverAnotherUser() = runTest {
        val job = start(listOf(priya, otherUser), id = "")
        assertTrue(viewModel.uiState.value is ProfileUiState.Error)
        job.cancel()
    }

    @Test
    fun loadFailure_showsError_thenRetryRecovers() = runTest {
        val job = start(listOf(priya))
        repo.observeFailure = IOException("offline")
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(ProfileUiState.Error("offline"), viewModel.uiState.value)

        repo.observeFailure = null
        viewModel.retry()
        advanceUntilIdle()
        assertEquals(ProfileUiState.Loaded(priya), viewModel.uiState.value)
        job.cancel()
    }

    @Test
    fun ensureProfileExists_createsAnEntrantProfile() = runTest {
        val job = start(emptyList())
        viewModel.ensureProfileExists()
        advanceUntilIdle()

        val created = repo.getById(deviceId)
        assertNotNull(created)
        assertEquals(listOf(Role.ENTRANT), created!!.roles)
        assertEquals(ProfileUiState.Loaded(created), viewModel.uiState.value)
        job.cancel()
    }

    @Test
    fun ensureProfileExists_keepsAnExistingProfile() = runTest {
        val job = start(listOf(priya))
        viewModel.ensureProfileExists()
        advanceUntilIdle()
        assertEquals(priya, repo.getById(deviceId))
        job.cancel()
    }

    @Test
    fun deviceIdLabel_isMasked() = runTest {
        val job = start(listOf(priya))
        assertEquals("QD-••••-ICEA", viewModel.deviceIdLabel)
        job.cancel()
    }

    // US 01.07.03

    @Test
    fun startEditing_fillsTheFormWithStoredValues() = runTest {
        val job = start(listOf(priya))
        viewModel.startEditing(priya)
        assertEquals(ProfileForm("Priya Sharma", "priya.sharma@example.com", "780-555-0142"), viewModel.form.value)
        job.cancel()
    }

    @Test
    fun save_validChanges_areStoredAndReportedAsSaved() = runTest {
        val job = start(listOf(priya))
        viewModel.startEditing(priya)
        viewModel.onNameChange("  Priya S. ")
        viewModel.onPhoneChange("")
        viewModel.onSaveClicked()
        advanceUntilIdle()

        val saved = repo.getById(deviceId)!!
        assertEquals("Priya S.", saved.name)
        assertEquals("", saved.phone)
        assertEquals(SaveState.Saved, viewModel.saveState.value)
        job.cancel()
    }

    @Test
    fun save_keepsRolesAndIdentity() = runTest {
        val job = start(listOf(morgan))
        viewModel.startEditing(morgan)
        viewModel.onEmailChange("morgan@example.com")
        viewModel.onSaveClicked()
        advanceUntilIdle()

        val saved = repo.getById(deviceId)!!
        assertEquals(morgan.roles, saved.roles)
        assertEquals(morgan.activeMode, saved.activeMode)
        assertEquals(deviceId, saved.deviceId)
        job.cancel()
    }

    @Test
    fun save_doesNotChangeOtherUsers() = runTest {
        val job = start(listOf(priya, otherUser))
        viewModel.startEditing(priya)
        viewModel.onNameChange("Priya S.")
        viewModel.onSaveClicked()
        advanceUntilIdle()
        assertEquals(otherUser, repo.getById("device-b"))
        job.cancel()
    }

    @Test
    fun save_blankName_isRejectedAndNothingIsWritten() = runTest {
        val job = start(listOf(priya))
        viewModel.startEditing(priya)
        viewModel.onNameChange("")
        viewModel.onSaveClicked()
        advanceUntilIdle()

        assertEquals(ProfileFieldError.NAME_REQUIRED, viewModel.errors.value.name)
        assertEquals(SaveState.Idle, viewModel.saveState.value)
        assertEquals(priya, repo.getById(deviceId))
        job.cancel()
    }

    @Test
    fun save_failure_reportsFailedAndKeepsTheEdits() = runTest {
        val job = start(listOf(priya))
        viewModel.startEditing(priya)
        viewModel.onNameChange("Priya S.")
        repo.updateFailure = IOException("offline")
        viewModel.onSaveClicked()
        advanceUntilIdle()

        assertEquals(SaveState.Failed("offline"), viewModel.saveState.value)
        assertEquals("Priya S.", viewModel.form.value.name)
        assertEquals(priya, repo.getById(deviceId))
        job.cancel()
    }

    @Test
    fun typing_clearsThatFieldsError() = runTest {
        val job = start(listOf(priya))
        viewModel.startEditing(priya)
        viewModel.onNameChange("")
        viewModel.onSaveClicked()
        advanceUntilIdle()
        assertNotNull(viewModel.errors.value.name)

        viewModel.onNameChange("P")
        assertNull(viewModel.errors.value.name)
        job.cancel()
    }

    // US 01.08.01

    @Test
    fun delete_removesProfileEntriesAndIdentity() = runTest {
        val job = start(listOf(priya, otherUser))
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertNull(repo.getById(deviceId))
        assertNull(waitlist.getEntry("lottery-1", deviceId))
        assertNull(waitlist.getEntry("lottery-2", deviceId))
        assertEquals(1, identityDeletions)
        assertEquals(ProfileUiState.Deleted, viewModel.uiState.value)
        assertEquals(DeleteState.Idle, viewModel.deleteState.value)
        job.cancel()
    }

    @Test
    fun delete_leavesOtherUsersAlone() = runTest {
        val job = start(listOf(priya, otherUser))
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertEquals(otherUser, repo.getById("device-b"))
        assertNotNull(waitlist.getEntry("lottery-1", "device-b"))
        job.cancel()
    }

    @Test
    fun delete_failure_keepsTheProfileAndNeverShowsDeleted() = runTest {
        val job = start(listOf(priya))
        repo.deleteFailure = IOException("offline")
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertEquals(DeleteState.Failed("offline"), viewModel.deleteState.value)
        assertEquals(ProfileUiState.Loaded(priya), viewModel.uiState.value)
        assertEquals(0, identityDeletions)
        job.cancel()
    }

    @Test
    fun delete_waitlistFailure_stopsBeforeTheProfileIsRemoved() = runTest {
        val job = start(listOf(priya))
        waitlist.failure = IOException("offline")
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertTrue(viewModel.deleteState.value is DeleteState.Failed)
        assertEquals(priya, repo.getById(deviceId))
        assertEquals(0, identityDeletions)
        job.cancel()
    }

    @Test
    fun startOver_afterDelete_startsANewSession() = runTest {
        val job = start(listOf(priya))
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()
        viewModel.onStartOver()
        advanceUntilIdle()

        assertEquals(1, newSessions)
        assertFalse(viewModel.uiState.value is ProfileUiState.Deleted)
        job.cancel()
    }

    // US 03.11.01

    @Test
    fun switchMode_toHeldRole_isStoredAndSignalsNavigation() = runTest {
        val job = start(listOf(morgan))
        viewModel.onModeSelected(Role.ENTRANT)
        advanceUntilIdle()

        assertEquals(Role.ENTRANT, repo.getById(deviceId)?.activeMode)
        assertEquals(ModeSwitchState.Switched(Role.ENTRANT), viewModel.modeSwitch.value)
        job.cancel()
    }

    @Test
    fun switchMode_backToAdmin_works() = runTest {
        val job = start(listOf(morgan.copy(activeMode = Role.ENTRANT)))
        viewModel.onModeSelected(Role.ADMIN)
        advanceUntilIdle()
        assertEquals(Role.ADMIN, repo.getById(deviceId)?.activeMode)
        job.cancel()
    }

    @Test
    fun switchMode_keepsAllRolesAndIdentity() = runTest {
        val job = start(listOf(morgan))
        viewModel.onModeSelected(Role.ENTRANT)
        advanceUntilIdle()

        val stored = repo.getById(deviceId)!!
        assertEquals(morgan.roles, stored.roles)
        assertEquals(deviceId, stored.deviceId)
        job.cancel()
    }

    @Test
    fun switchMode_handled_returnsToIdle() = runTest {
        val job = start(listOf(morgan))
        viewModel.onModeSelected(Role.ENTRANT)
        advanceUntilIdle()
        viewModel.onModeSwitchHandled()
        assertEquals(ModeSwitchState.Idle, viewModel.modeSwitch.value)
        job.cancel()
    }

    @Test
    fun switchMode_entrantCannotBecomeAdmin() = runTest {
        val job = start(listOf(priya))
        viewModel.onModeSelected(Role.ADMIN)
        advanceUntilIdle()

        assertEquals(priya, repo.getById(deviceId))
        assertEquals(ModeSwitchState.Idle, viewModel.modeSwitch.value)
        job.cancel()
    }

    @Test
    fun switchMode_toCurrentMode_doesNothing() = runTest {
        val job = start(listOf(morgan))
        viewModel.onModeSelected(Role.ADMIN)
        advanceUntilIdle()
        assertEquals(ModeSwitchState.Idle, viewModel.modeSwitch.value)
        job.cancel()
    }

    @Test
    fun switchMode_failure_keepsThePreviousMode() = runTest {
        val job = start(listOf(morgan))
        repo.modeFailure = IOException("offline")
        viewModel.onModeSelected(Role.ENTRANT)
        advanceUntilIdle()

        assertEquals(ModeSwitchState.Failed("offline"), viewModel.modeSwitch.value)
        assertEquals(Role.ADMIN, repo.getById(deviceId)?.activeMode)
        job.cancel()
    }

    @Test
    fun switchMode_fromStaleMode_treatsTheFallbackAsCurrent() = runTest {
        // Stored mode is ADMIN but the ADMIN role was removed, so the effective mode is ENTRANT.
        val job = start(listOf(morgan.copy(roles = listOf(Role.ENTRANT, Role.OWNER))))
        viewModel.onModeSelected(Role.ENTRANT)
        advanceUntilIdle()
        assertEquals(ModeSwitchState.Idle, viewModel.modeSwitch.value)

        viewModel.onModeSelected(Role.OWNER)
        advanceUntilIdle()
        assertEquals(Role.OWNER, repo.getById(deviceId)?.activeMode)
        job.cancel()
    }
}
