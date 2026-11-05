// Read/write access to campsite listings.
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Extend with the methods your stories need, and write the Firestore
//  implementation. Others: open a small PR and tag the owner.
// FIRESTORE IMPLEMENTATION: Ibrahim (ibr2309)
package com.example.trails.data.repository

import com.example.trails.data.model.Campsite
import kotlinx.coroutines.flow.Flow

/** Storage for [Campsite] listings. Screens depend on this, never on Firestore directly. */
interface CampsiteRepository {
    /** Emits every campsite, and again whenever any of them changes. */
    fun observeAll(): Flow<List<Campsite>>

    /** Emits the campsites owned by [ownerId], and again on every change (US 02.03.01). */
    fun observeByOwner(ownerId: String): Flow<List<Campsite>>

    /** Emits the campsite with [id] (null if it doesn't exist), and again on every change (US 02.01.04). */
    fun observeById(id: String): Flow<Campsite?>

    /** Returns the campsite with [id], or null if it does not exist (US 02.01.04). */
    suspend fun getById(id: String): Campsite?

    /**
     * Creates the campsite if [Campsite.id] is blank (US 02.01.01), otherwise overwrites
     * the existing one (US 02.01.03).
     *
     * @return the campsite's id, newly generated when it was created.
     */
    suspend fun save(campsite: Campsite): String

    /**
     * Uploads a promo image the owner picked on the device (US 02.01.02).
     *
     * @param localUri the picked image, e.g. a "content://" URI as a string.
     * @return a URL that can be stored in [Campsite.imageUrl] and shown anywhere.
     */
    suspend fun uploadImage(localUri: String): String
}
