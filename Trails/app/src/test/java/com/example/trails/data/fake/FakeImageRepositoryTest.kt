// Unit tests for the in-memory image repository and isStorageUrl (US 02.01.02, 01.08.02).
// OWNER: Ibrahim (ibr2309) — shared image upload
// Outstanding issues: none.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.data.fake

import com.example.trails.data.repository.ImageFolder
import com.example.trails.data.repository.isStorageUrl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeImageRepositoryTest {

    private val repo = FakeImageRepository()

    // An upload returns a URL that can be shown, and is listed under its folder.
    @Test
    fun upload_returnsUrl_andIsListed() = runBlocking {
        val url = repo.upload("content://media/picker/0/42", ImageFolder.PROFILES)
        assertEquals("content://media/picker/0/42", url)
        val listed = repo.listAll().single()
        assertEquals(url, listed.url)
        assertEquals(ImageFolder.PROFILES, listed.folder)
        assertTrue(listed.path.startsWith("profile-images/"))
    }

    // Images from both features are listed together, for admin moderation.
    @Test
    fun listAll_includesEveryFolder() = runBlocking {
        repo.upload("content://a", ImageFolder.CAMPSITES)
        repo.upload("content://b", ImageFolder.PROFILES)
        assertEquals(setOf(ImageFolder.CAMPSITES, ImageFolder.PROFILES), repo.listAll().map { it.folder }.toSet())
    }

    // Deleting removes the image from the listing.
    @Test
    fun delete_removesFromListing() = runBlocking {
        val url = repo.upload("content://a", ImageFolder.CAMPSITES)
        repo.delete(url)
        assertTrue(repo.listAll().isEmpty())
        assertEquals(listOf(url), repo.deleted)
    }

    // Only Firebase Storage URLs count as our uploads.
    @Test
    fun isStorageUrl_onlyMatchesFirebaseStorage() {
        assertTrue(isStorageUrl("https://firebasestorage.googleapis.com/v0/b/x.appspot.com/o/profile-images%2Fa.jpg?alt=media"))
        assertTrue(isStorageUrl("gs://x.appspot.com/profile-images/a.jpg"))
        assertFalse(isStorageUrl("content://media/picker/0/42"))
        assertFalse(isStorageUrl("https://images.unsplash.com/photo-1"))
    }
}
