// Read/write access to campsite listings.
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Implemented by FirestoreCampsiteRepository (used by the app) and FakeCampsiteRepository (tests).
// Outstanding issues: uploadImage needs Firebase Storage, which isn't enabled on the project yet
//  (it needs the Blaze plan), so photo upload is deferred to the final part.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.data.repository

import com.example.trails.data.model.Campsite
import kotlinx.coroutines.flow.Flow

/**
 * Storage for [Campsite] listings. Screens depend on this, never on Firestore directly.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
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
     * Deletes the campsite with [id]; does nothing if it doesn't exist (US 03.01.01).
     * Callers take it out of any lotteries first, so none are left pointing at it.
     *
     * @param id id of the campsite to delete.
     */
    suspend fun delete(id: String)

    /**
     * Uploads a promo image the owner picked on the device (US 02.01.02). Needs Firebase
     * Storage, which isn't enabled yet, so this is deferred to the final part.
     *
     * @param localUri the picked image, e.g. a "content://" URI as a string.
     * @return a URL that can be stored in [Campsite.imageUrl] and shown anywhere.
     */
    suspend fun uploadImage(localUri: String): String
}
