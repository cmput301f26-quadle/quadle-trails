// Stores user profiles in the Firestore users collection (US 01.09.01, 01.07.03, 01.08.01, 03.11.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: Security Rules must also stop users changing their own roles; not unit-tested (needs a live Firestore).
// AI assistance: Used AI to help implement field-limited Firestore profile updates.
package com.example.trails.data.firestore

import com.example.trails.data.model.Collections
import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import com.example.trails.data.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Stores profiles in Firestore, one document per user, keyed by device id.
 *
 * @property db the Firestore instance.
 */
class FirestoreUserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    private fun usersCollection() = db.collection(Collections.USERS)

    override fun observeAll(): Flow<List<User>> = callbackFlow {
        val registration = usersCollection().addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            try {
                trySend(snapshot?.toObjects(User::class.java).orEmpty().sortedBy { it.name })
            } catch (e: Exception) {
                close(e)
            }
        }
        awaitClose { registration.remove() }
    }

    override fun observe(deviceId: String): Flow<User?> = callbackFlow {
        if (deviceId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registration = usersCollection().document(deviceId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                try {
                    val user = if (snapshot != null && snapshot.exists()) {
                        snapshot.toObject(User::class.java)
                    } else {
                        null
                    }
                    trySend(user)
                } catch (e: Exception) {
                    close(e)
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getById(deviceId: String): User? {
        if (deviceId.isBlank()) return null
        return usersCollection().document(deviceId).get().await().toObject(User::class.java)
    }

    override suspend fun save(user: User) {
        require(user.deviceId.isNotBlank()) { "A profile needs a device id before it can be saved." }
        usersCollection().document(user.deviceId).set(user).await()
    }

    override suspend fun updateProfile(deviceId: String, name: String, email: String, phone: String) {
        require(deviceId.isNotBlank()) { "A device id is required." }
        // update() writes only these fields and fails if the document does not exist,
        // so roles and activeMode can never be overwritten by an edit.
        usersCollection().document(deviceId)
            .update(mapOf("name" to name, "email" to email, "phone" to phone))
            .await()
    }

    override suspend fun updateActiveMode(deviceId: String, mode: Role) {
        val user = getById(deviceId) ?: throw NoSuchElementException("No profile for $deviceId.")
        require(mode in user.roles) { "Profile $deviceId does not hold the $mode role." }
        usersCollection().document(deviceId).update("activeMode", mode.name).await()
    }

    override suspend fun delete(deviceId: String) {
        if (deviceId.isBlank()) return
        usersCollection().document(deviceId).delete().await()
    }
}
