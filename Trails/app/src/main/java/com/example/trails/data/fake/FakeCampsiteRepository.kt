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

/**
 * In-memory [CampsiteRepository] for tests and for teammates before Firestore is wired in.
 *
 * @param initial campsites to start with; defaults to [TestCampsites.all].
 */
class FakeCampsiteRepository(initial: List<Campsite> = TestCampsites.all) : CampsiteRepository {
    private val campsites = MutableStateFlow(initial)
    private var nextId = 1

    override fun observeAll(): Flow<List<Campsite>> = campsites

    override fun observeByOwner(ownerId: String): Flow<List<Campsite>> =
        campsites.map { list -> list.filter { it.ownerId == ownerId } }

    override fun observeById(id: String): Flow<Campsite?> =
        campsites.map { list -> list.find { it.id == id } }

    override suspend fun getById(id: String): Campsite? =
        campsites.value.find { it.id == id }

    override suspend fun save(campsite: Campsite): String {
        val toSave = if (campsite.id.isBlank()) campsite.copy(id = "fake-new-${nextId++}") else campsite
        campsites.update { list -> list.filterNot { it.id == toSave.id } + toSave }
        return toSave.id
    }
}
