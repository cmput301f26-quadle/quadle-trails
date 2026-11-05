// Firestore-backed CampsiteRepository for the campsites collection.
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Outstanding issues:
//  - A listener error closes the flow; screens should show an error state.
//  - Not unit-tested (needs a live Firestore); covered by UI tests on the campsite screens.
package com.example.trails.data.firestore

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Collections
import com.example.trails.data.repository.CampsiteRepository
import android.net.Uri
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
 * @param db the Firestore instance; defaults to the app's default instance.
 */
class FirestoreCampsiteRepository(
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : CampsiteRepository {

    private val campsites = db.collection(Collections.CAMPSITES)

    override fun observeAll(): Flow<List<Campsite>> = observe(campsites)

    override fun observeByOwner(ownerId: String): Flow<List<Campsite>> =
        observe(campsites.whereEqualTo("ownerId", ownerId))

    override fun observeById(id: String): Flow<Campsite?> = callbackFlow {
        val registration = campsites.document(id).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(Campsite::class.java))
        }
        awaitClose { registration.remove() }
    }

    override suspend fun getById(id: String): Campsite? =
        campsites.document(id).get().await().toObject(Campsite::class.java)

    override suspend fun save(campsite: Campsite): String {
        val doc = if (campsite.id.isBlank()) campsites.document() else campsites.document(campsite.id)
        doc.set(campsite).await() // the @DocumentId field is not written into the document
        return doc.id
    }

    /** Uploads to Firebase Storage under campsite-images/ and returns the download URL. */
    override suspend fun uploadImage(localUri: String): String {
        val ref = storage.reference.child("campsite-images/${UUID.randomUUID()}.jpg")
        ref.putFile(Uri.parse(localUri)).await()
        return ref.downloadUrl.await().toString()
    }

    /** Turns a Firestore query into a Flow that re-emits on every change. */
    private fun observe(query: Query): Flow<List<Campsite>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObjects(Campsite::class.java).orEmpty())
        }
        awaitClose { registration.remove() }
    }
}
