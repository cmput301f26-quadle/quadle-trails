package com.example.trails.ui.lottery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.model.canInvite

@Composable
fun LotterySharingScreen(navController: NavController, lotteryId: String? = null) {
    val controller: LotterySharingViewModel = viewModel(key = "sharing_${lotteryId ?: "incoming"}") {
        LotterySharingViewModel(lotteryId)
    }
    val state by controller.state.collectAsState()
    Scaffold(topBar = {
        LotteryHeader(stringResource(if (lotteryId == null) R.string.lottery_invitations else R.string.lottery_sharing_title),
            onBack = { navController.popBackStack() })
    }) { padding ->
        LotterySharingContent(state, lotteryId == null, controller::invite, controller::respond, Modifier.padding(padding))
    }
}

/** Reuses the lottery cards, spacing and Material colours from the existing wizard. */
@Composable
fun LotterySharingContent(
    state: LotterySharingState,
    incoming: Boolean,
    onInvite: (List<String>) -> Unit,
    onRespond: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (state.loading) { CircularProgressIndicator(); return@Column }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!incoming && state.lottery == null) {
            Text(stringResource(R.string.lottery_not_found)); return@Column
        }
        state.lottery?.let {
            Text(it.name, style = LotteryType.Title)
            Text(stringResource(R.string.lottery_shared_owner_count, (it.participantOwnerIds + it.ownerId).distinct().size), style = LotteryType.Body)
        }
        Text(stringResource(R.string.lottery_sharing_explanation), style = LotteryType.Hint)
        state.invitations.forEach { invite ->
            val pending = invite.status == "PENDING"
            val expired = (invite.registrationStart.millis() ?: 0) <= state.now
            LotteryCard(modifier = Modifier.testTag("invitation_${invite.id}")) {
                Text(invite.lotteryName, style = LotteryType.CardTitle)
                val nameId = if (incoming) invite.inviterId else invite.inviteeId
                Text(state.ownerNames[nameId] ?: nameId, style = LotteryType.Body)
                Text(invite.campsiteNames.joinToString(" · "), style = LotteryType.Body)
                Text(stringResource(R.string.lottery_shared_booking,
                    invite.bookingStart.millis()?.let(::formatDateTime) ?: "—",
                    invite.bookingEnd.millis()?.let(::formatDateTime) ?: "—"), style = LotteryType.Small)
                Text(stringResource(R.string.lottery_shared_registration,
                    invite.registrationStart.millis()?.let(::formatDateTime) ?: "—",
                    invite.registrationEnd.millis()?.let(::formatDateTime) ?: "—"), style = LotteryType.Small)
                Text(stringResource(when {
                    pending && expired -> R.string.lottery_invite_expired
                    invite.status == "ACCEPTED" -> R.string.lottery_invite_accepted
                    invite.status == "DECLINED" -> R.string.lottery_invite_declined
                    else -> R.string.lottery_invite_pending
                }), color = MaterialTheme.colorScheme.primary)
                if (incoming && pending && !expired) {
                    LotteryButton(stringResource(R.string.lottery_invite_accept), { onRespond(invite.id, true) },
                        enabled = !state.busy, modifier = Modifier.testTag("accept_${invite.id}"))
                    TextButton(onClick = { onRespond(invite.id, false) }, enabled = !state.busy) {
                        Text(stringResource(R.string.lottery_invite_decline))
                    }
                }
            }
        }
        if (incoming && state.invitations.isEmpty()) Text(stringResource(R.string.lottery_invitations_empty))
        if (!incoming && state.lottery?.canInvite(state.ownerId, state.now) == true) {
            val pendingOwners = state.invitations.filter { it.status == "PENDING" }.map { it.inviteeId }.toSet()
            state.availableSites.groupBy { it.ownerId }.forEach { (owner, sites) ->
                key(owner) {
                    var selected by rememberSaveable { mutableStateOf(emptyList<String>()) }
                    LotteryCard {
                        Text(state.ownerNames[owner] ?: owner, style = LotteryType.CardTitle)
                        sites.forEach { site ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(site.id in selected, onCheckedChange = { checked ->
                                    selected = if (checked) selected + site.id else selected - site.id
                                }, enabled = !state.busy && owner !in pendingOwners)
                                CampsitePhoto(site.imageUrl, size = 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(site.name, modifier = Modifier.weight(1f))
                            }
                        }
                        LotteryButton(stringResource(if (owner in pendingOwners) R.string.lottery_invite_pending else R.string.lottery_invite_send),
                            onClick = { onInvite(selected.filter { id -> sites.any { it.id == id } }); selected = emptyList() },
                            enabled = !state.busy && owner !in pendingOwners && selected.isNotEmpty())
                    }
                }
            }
            if (state.availableSites.isEmpty()) Text(stringResource(R.string.lottery_no_shared_sites))
        } else if (!incoming) Text(stringResource(R.string.lottery_sharing_closed))
    }
}
