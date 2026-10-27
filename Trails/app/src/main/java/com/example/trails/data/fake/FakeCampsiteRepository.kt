// In-memory CampsiteRepository seeded from TestCampsites.
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Extend with the methods your stories need, and write the Firestore
//  implementation. Others: open a small PR and tag the owner.
package com.example.trails.data.fake

import com.example.trails.data.model.Campsite
import com.example.trails.data.repository.CampsiteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeCampsiteRepository(initial: List<Campsite> = TestCampsites.all) : CampsiteRepository {
    private val campsites = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<Campsite>> = campsites

    override fun observeByOwner(ownerId: String): Flow<List<Campsite>> =
        campsites.map { list -> list.filter { it.ownerId == ownerId } }

    override suspend fun getById(id: String): Campsite? =
        campsites.value.find { it.id == id }

    override suspend fun save(campsite: Campsite) {
        campsites.update { list -> list.filterNot { it.id == campsite.id } + campsite }
    }
}
