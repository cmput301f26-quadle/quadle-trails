package com.example.trails.data.repository

import com.example.trails.data.model.LotteryInvitation
import kotlinx.coroutines.flow.Flow

/** Invitation and consent operations for US 02.04.08. */
interface LotteryCollaborationRepository {
    fun observeIncoming(ownerId: String): Flow<List<LotteryInvitation>>
    fun observeForLottery(lotteryId: String): Flow<List<LotteryInvitation>>
    suspend fun invite(lotteryId: String, actorId: String, campsiteIds: List<String>)
    suspend fun respond(invitationId: String, actorId: String, accept: Boolean)
}
