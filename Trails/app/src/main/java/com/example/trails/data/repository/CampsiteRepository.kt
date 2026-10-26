// Read/write access to campsite listings.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
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
