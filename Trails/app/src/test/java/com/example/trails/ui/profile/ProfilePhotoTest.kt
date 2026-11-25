// Unit tests for adding, changing and removing the profile photo (US 01.08.02).
// OWNER: Ibrahim (ibr2309) — shared image upload, on Aarav's ProfileViewModel
// Outstanding issues: none.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.profile

import com.example.trails.data.fake.FakeImageRepository
import com.example.trails.data.fake.FakeLotteryRepository
import com.example.trails.data.fake.FakeUserRepository
import com.example.trails.data.fake.FakeWaitlistRepository
import com.example.trails.data.model.User
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProfilePhotoTest {

    private val testDispatcher = StandardTestDispatcher()
    private val deviceId = "device-a"
    private val oldPhoto = "https://firebasestorage.googleapis.com/v0/b/x/o/profile-images%2Fold.jpg"
    private val picked = "content://media/picker/0/7"

    private lateinit var repo: FlakyUserRepository
    private lateinit var images: FakeImageRepository
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Builds the ViewModel for a profile with [photo] and keeps uiState collected. */
    private fun TestScope.start(photo: String?): Job {
        repo = FlakyUserRepository(
            FakeUserRepository(listOf(User(deviceId = deviceId, name = "Priya", email = "p@example.com", profileImageUrl = photo)))
        )
        images = FakeImageRepository()
        viewModel = ProfileViewModel(
            repository = repo,
            deviceId = deviceId,
            lotteryRepository = FakeLotteryRepository(emptyList()),
            waitlistRepository = FakeWaitlistRepository(emptyList()),
            deleteIdentity = {},
            startNewSession = {},
            images = images
        )
        val job = launch(testDispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()
        return job
    }

    private suspend fun storedPhoto(): String? = repo.getById(deviceId)?.profileImageUrl

    // Adding a photo uploads it and saves its URL on the profile.
    @Test
    fun pick_withNoPhoto_uploadsAndSaves() = runTest(testDispatcher) {
        val job = start(photo = null)
        viewModel.onPhotoPicked(picked)
        advanceUntilIdle()
        assertEquals(picked, storedPhoto())
        assertEquals(PhotoState.Idle, viewModel.photoState.value)
        assertTrue(images.deleted.isEmpty())
        job.cancel()
    }

    // Changing the photo saves the new one, then deletes the old one from storage.
    @Test
    fun pick_withPhoto_replacesAndDeletesOld() = runTest(testDispatcher) {
        val job = start(photo = oldPhoto)
        viewModel.onPhotoPicked(picked)
        advanceUntilIdle()
        assertEquals(picked, storedPhoto())
        assertEquals(listOf(oldPhoto), images.deleted)
        job.cancel()
    }

    // A failed upload keeps the old photo and shows an error.
    @Test
    fun pick_uploadFails_keepsOldPhoto() = runTest(testDispatcher) {
        val job = start(photo = oldPhoto)
        images.uploadFailure = IOException("offline")
        viewModel.onPhotoPicked(picked)
        advanceUntilIdle()
        assertEquals(oldPhoto, storedPhoto())
        assertTrue(viewModel.photoState.value is PhotoState.Failed)
        assertTrue(images.deleted.isEmpty())
        job.cancel()
    }

    // If the profile can't be saved, the new upload is cleaned up and the old photo kept.
    @Test
    fun pick_saveFails_deletesNewUpload_keepsOld() = runTest(testDispatcher) {
        val job = start(photo = oldPhoto)
        repo.photoFailure = IOException("denied")
        viewModel.onPhotoPicked(picked)
        advanceUntilIdle()
        assertEquals(oldPhoto, storedPhoto())
        assertEquals(listOf(picked), images.deleted)
        assertTrue(viewModel.photoState.value is PhotoState.Failed)
        job.cancel()
    }

    // Removing clears the photo on the profile and deletes it from storage.
    @Test
    fun remove_clearsAndDeletes() = runTest(testDispatcher) {
        val job = start(photo = oldPhoto)
        viewModel.onPhotoRemoved()
        advanceUntilIdle()
        assertNull(storedPhoto())
        assertEquals(listOf(oldPhoto), images.deleted)
        assertEquals(PhotoState.Idle, viewModel.photoState.value)
        job.cancel()
    }

    // A failed delete from storage doesn't undo the removal.
    @Test
    fun remove_storageDeleteFails_stillRemoved() = runTest(testDispatcher) {
        val job = start(photo = oldPhoto)
        images.deleteFailure = IOException("offline")
        viewModel.onPhotoRemoved()
        advanceUntilIdle()
        assertNull(storedPhoto())
        assertEquals(PhotoState.Idle, viewModel.photoState.value)
        job.cancel()
    }

    // Removing when there is no photo does nothing.
    @Test
    fun remove_withNoPhoto_doesNothing() = runTest(testDispatcher) {
        val job = start(photo = null)
        viewModel.onPhotoRemoved()
        advanceUntilIdle()
        assertEquals(PhotoState.Idle, viewModel.photoState.value)
        assertTrue(images.deleted.isEmpty())
        job.cancel()
    }

    // Deleting the profile (US 01.08.01) also deletes its photo from storage.
    @Test
    fun deleteProfile_deletesPhoto() = runTest(testDispatcher) {
        val job = start(photo = oldPhoto)
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()
        assertEquals(listOf(oldPhoto), images.deleted)
        job.cancel()
    }
}
