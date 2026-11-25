// In-memory ImageRepository for tests and previews.
// OWNER: Ibrahim (ibr2309) — Campsite Management (shared image upload)
// USER STORIES: 02.01.02, 01.08.02; listAll supports 03.06.01 / 03.03.01.
// Outstanding issues: none.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.data.fake

import com.example.trails.data.repository.ImageFolder
import com.example.trails.data.repository.ImageRepository
import com.example.trails.data.repository.StoredImage

/**
 * Keeps "uploaded" images in memory. An upload's URL is the local URI itself, so a picked
 * image still shows for the rest of the app session, as the old campsite fake did.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param initial images that already exist.
 */
class FakeImageRepository(initial: List<StoredImage> = emptyList()) : ImageRepository {

    private val images = initial.toMutableList()

    /** Thrown by [upload] when set. */
    var uploadFailure: Exception? = null

    /** Thrown by [delete] when set. */
    var deleteFailure: Exception? = null

    /** Every URL passed to [delete], in order. */
    val deleted = mutableListOf<String>()

    override suspend fun upload(localUri: String, folder: ImageFolder): String {
        uploadFailure?.let { throw it }
        images.removeAll { it.url == localUri }
        images += StoredImage(url = localUri, folder = folder, path = "${folder.path}/${images.size}.jpg")
        return localUri
    }

    override suspend fun delete(url: String) {
        deleteFailure?.let { throw it }
        deleted += url
        images.removeAll { it.url == url }
    }

    override suspend fun listAll(): List<StoredImage> = images.toList()
}
