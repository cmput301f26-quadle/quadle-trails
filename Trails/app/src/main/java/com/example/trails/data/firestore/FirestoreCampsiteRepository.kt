// Firestore-backed CampsiteRepository for the campsites collection.
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Outstanding issues:
//  - Read errors (e.g. PERMISSION_DENIED from the security rules) are logged and treated as
//    "no data" so the app doesn't crash; screens don't show a dedicated error state yet.
//  - Photo upload needs Firebase Storage, which isn't enabled on the project yet (Blaze plan),
//    so uploadImage fails for now; deferred to the final part.
//  - Needs a live Firestore, so it isn't unit-tested itself; the document mapping it relies
//    on (CampsiteDocument.kt) is.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.data.firestore

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Collections
import com.example.trails.data.repository.CampsiteRepository
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Reads and writes [Campsite]s in the [Collections.CAMPSITES] Firestore collection.
 *
 * Firebase is only touched on first use, so creating this class (e.g. as a default
 * argument in a ViewModel under a JVM unit test) never starts Firebase.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param db the Firestore instance; defaults to the app's default instance.
 * @param storage the Storage instance for promo photos; defaults to the app's default instance.
 */
class FirestoreCampsiteRepository(
    db: Lazy<FirebaseFirestore> = lazy { FirebaseFirestore.getInstance() },
    storage: Lazy<FirebaseStorage> = lazy { FirebaseStorage.getInstance() }
) : CampsiteRepository {

    private val campsites by lazy { db.value.collection(Collections.CAMPSITES) }
    private val storage by storage

    override fun observeAll(): Flow<List<Campsite>> = observe(campsites)

    override fun observeByOwner(ownerId: String): Flow<List<Campsite>> =
        observe(campsites.whereEqualTo("ownerId", ownerId))

    override fun observeById(id: String): Flow<Campsite?> = callbackFlow {
        val registration = campsites.document(id).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Couldn't read campsite $id", error)
                trySend(null)
                close()
                return@addSnapshotListener
            }
            trySend(snapshot?.takeIf { it.exists() }?.let { campsiteFromDocument(it.id, it.data.orEmpty()) })
        }
        awaitClose { registration.remove() }
    }

    override suspend fun getById(id: String): Campsite? = try {
        val snapshot = campsites.document(id).get().await()
        if (snapshot.exists()) campsiteFromDocument(snapshot.id, snapshot.data.orEmpty()) else null
    } catch (e: Exception) { // denied by rules, offline, or Firebase not available
        Log.w(TAG, "Couldn't read campsite $id", e)
        null
    }

    override suspend fun save(campsite: Campsite): String {
        val doc = if (campsite.id.isBlank()) campsites.document() else campsites.document(campsite.id)
        doc.set(campsiteToDocument(campsite)).await()
        return doc.id
    }

    /** Firestore's delete already succeeds when the document doesn't exist. */
    override suspend fun delete(id: String) {
        campsites.document(id).delete().await()
    }

    /** Uploads to Firebase Storage under campsite-images/ and returns the download URL. */
    override suspend fun uploadImage(localUri: String): String {
        val ref = storage.reference.child("campsite-images/${UUID.randomUUID()}.jpg")
        ref.putFile(Uri.parse(localUri)).await()
        return ref.downloadUrl.await().toString()
    }

    private companion object {
        const val TAG = "FirestoreCampsiteRepo"
    }

    /** Turns a Firestore query into a Flow that re-emits on every change. */
    private fun observe(query: Query): Flow<List<Campsite>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Couldn't read campsites", error)
                trySend(emptyList())
                close()
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty().map { campsiteFromDocument(it.id, it.data.orEmpty()) })
        }
        awaitClose { registration.remove() }
    }
}
