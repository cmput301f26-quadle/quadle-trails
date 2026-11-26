// Firebase Storage-backed ImageRepository.
// OWNER: Ibrahim (ibr2309) — Campsite Management (shared image upload)
// USER STORIES: 02.01.02, 01.08.02; listAll supports 03.06.01 / 03.03.01.
// Signs in anonymously before writing if nobody is signed in, since the Storage rules only
// accept uploads and deletes from signed-in users.
// Outstanding issues:
//  - Needs a live Storage bucket, so it isn't unit-tested itself; FakeImageRepository is.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.data.firestore

import android.net.Uri
import com.example.trails.data.repository.ImageFolder
import com.example.trails.data.repository.ImageRepository
import com.example.trails.data.repository.StoredImage
import com.example.trails.data.repository.isStorageUrl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Keeps images in Firebase Storage, one flat folder per [ImageFolder].
 *
 * Firebase is only touched on first use, so creating this class (e.g. as a default
 * argument in a ViewModel under a JVM unit test) never starts Firebase.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param storage the Storage instance; defaults to the app's default instance.
 * @param auth signs in anonymously before writes; defaults to the app's default instance.
 */
class FirebaseImageRepository(
    storage: Lazy<FirebaseStorage> = lazy { FirebaseStorage.getInstance() },
    auth: Lazy<FirebaseAuth> = lazy { FirebaseAuth.getInstance() }
) : ImageRepository {

    private val storage by storage
    private val auth by auth

    /** The Storage rules need a signed-in user to write; sign in anonymously if nobody is. */
    private suspend fun ensureSignedIn() {
        if (auth.currentUser == null) auth.signInAnonymously().await()
    }

    override suspend fun upload(localUri: String, folder: ImageFolder): String {
        ensureSignedIn()
        val ref = storage.reference.child("${folder.path}/${UUID.randomUUID()}.jpg")
        ref.putFile(Uri.parse(localUri)).await()
        return ref.downloadUrl.await().toString()
    }

    override suspend fun delete(url: String) {
        if (!isStorageUrl(url)) return
        ensureSignedIn()
        try {
            storage.getReferenceFromUrl(url).delete().await()
        } catch (e: StorageException) {
            if (e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) throw e
        }
    }

    override suspend fun listAll(): List<StoredImage> = ImageFolder.entries.flatMap { folder ->
        storage.reference.child(folder.path).listAll().await().items.map { ref ->
            StoredImage(url = ref.downloadUrl.await().toString(), folder = folder, path = ref.path.removePrefix("/"))
        }
    }
}
