// Read/write access to campsite listings.
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Extend with the methods your stories need, and write the Firestore
//  implementation. Others: open a small PR and tag the owner.
// FIRESTORE IMPLEMENTATION: Ibrahim (ibr2309)
package com.example.trails.data.repository

import com.example.trails.data.model.Campsite
import kotlinx.coroutines.flow.Flow

interface CampsiteRepository {
    fun observeAll(): Flow<List<Campsite>>
    fun observeByOwner(ownerId: String): Flow<List<Campsite>>
    suspend fun getById(id: String): Campsite?
    suspend fun save(campsite: Campsite)
}
