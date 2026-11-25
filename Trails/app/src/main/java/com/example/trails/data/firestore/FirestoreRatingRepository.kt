// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 01.13.01, 03.01.01, 03.09.01
// PURPOSE: Firestore-backed RatingRepository for ratings stored under
//   campsites/{campsiteId}/ratings/{userId}, one document per reviewer per campsite.
// Outstanding issues:
//  - AppContainer still uses the fake; switch once ratings are seeded.
//  - Needs a live Firestore, so it isn't unit-tested itself; FakeRatingRepository is.
//  - Read errors (e.g. PERMISSION_DENIED) are logged and treated as "no ratings" so the app
//    doesn't crash; write errors are logged and passed on to the caller.
//  - deleteForCampsite deletes one document at a time, not in a batch, so a failure part-way
//    through can leave some of the campsite's ratings in storage.
// AI assistance: Used AI (Claude Code) to help write this file.
package com.example.trails.data.firestore

import android.util.Log
import com.example.trails.data.model.Collections
import com.example.trails.data.model.Rating
import com.example.trails.data.repository.RatingRepository
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Reads and writes [Rating]s in each campsite's [Collections.RATINGS] subcollection. The
 * reviewer's device id is the document id, so each user has at most one rating per campsite.
 *
 * Firebase is only touched on first use, so creating this class under a JVM unit test
 * never starts Firebase.
 *
 * @param db the Firestore instance; defaults to the app's default instance.
 */
class FirestoreRatingRepository(
    db: Lazy<FirebaseFirestore> = lazy { FirebaseFirestore.getInstance() }
) : RatingRepository {

    private val firestore by db

    /** campsites/{campsiteId}/ratings */
    private fun ratingsOf(campsiteId: String): CollectionReference =
        firestore.collection(Collections.CAMPSITES).document(campsiteId).collection(Collections.RATINGS)

    /**
     * Watches every rating of one campsite, newest first.
     *
     * @param campsiteId id of the campsite whose ratings to watch.
     * @return a flow that re-emits whenever a rating is added, changed or deleted; emits an
     *   empty list for a blank id or after a read error.
     */
    override fun observeForCampsite(campsiteId: String): Flow<List<Rating>> = callbackFlow {
        if (campsiteId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = ratingsOf(campsiteId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Couldn't read ratings of $campsiteId", error)
                trySend(emptyList())
                close()
                return@addSnapshotListener
            }
            try {
                val list = snapshot?.toObjects(Rating::class.java).orEmpty()
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: Long.MIN_VALUE }
                trySend(list)
            } catch (e: Exception) { // a document that doesn't match Rating
                Log.w(TAG, "Couldn't read ratings of $campsiteId", e)
                trySend(emptyList())
            }
        }
        awaitClose { registration.remove() }
    }

    /**
     * Reads one user's rating of a campsite once.
     *
     * @param campsiteId id of the rated campsite.
     * @param userId device id of the reviewer.
     * @return the rating, or null if it doesn't exist or an id is blank.
     * @throws Exception if the read fails (logged first).
     */
    override suspend fun getRating(campsiteId: String, userId: String): Rating? {
        if (campsiteId.isBlank() || userId.isBlank()) return null
        return try {
            val snapshot = ratingsOf(campsiteId).document(userId).get().await()
            if (snapshot.exists()) snapshot.toObject(Rating::class.java) else null
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't read rating of $campsiteId by $userId", e)
            throw e
        }
    }

    /**
     * Creates or replaces a rating at campsites/{campsiteId}/ratings/{userId}.
     *
     * @param rating the rating to store; its campsiteId and userId pick the document.
     * @throws IllegalArgumentException if either id is blank.
     * @throws Exception if the write fails (logged first).
     */
    override suspend fun save(rating: Rating) {
        require(rating.campsiteId.isNotBlank() && rating.userId.isNotBlank()) {
            "A rating needs a campsiteId and a userId"
        }
        try {
            ratingsOf(rating.campsiteId).document(rating.userId).set(rating).await()
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't save rating of ${rating.campsiteId} by ${rating.userId}", e)
            throw e
        }
    }

    /**
     * Deletes one user's rating of a campsite. Deleting a rating that doesn't exist succeeds
     * and changes nothing, as Firestore allows.
     *
     * @param campsiteId id of the rated campsite.
     * @param userId device id of the reviewer.
     * @throws Exception if the delete fails (logged first).
     */
    override suspend fun delete(campsiteId: String, userId: String) {
        if (campsiteId.isBlank() || userId.isBlank()) return
        try {
            ratingsOf(campsiteId).document(userId).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't delete rating of $campsiteId by $userId", e)
            throw e
        }
    }

    /**
     * Deletes every document in campsites/{campsiteId}/ratings. Firestore doesn't delete a
     * subcollection with its parent document, so this must run before the campsite is deleted.
     *
     * @param campsiteId id of the campsite whose ratings are deleted; a blank id does nothing.
     * @throws Exception if the read or a delete fails (logged first).
     */
    override suspend fun deleteForCampsite(campsiteId: String) {
        if (campsiteId.isBlank()) return
        try {
            val snapshot = ratingsOf(campsiteId).get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't delete ratings of $campsiteId", e)
            throw e
        }
    }

    private companion object {
        const val TAG = "FirestoreRatingRepo"
    }
}
