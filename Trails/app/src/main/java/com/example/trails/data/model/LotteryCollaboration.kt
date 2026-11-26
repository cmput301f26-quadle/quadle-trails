package com.example.trails.data.model

/** Shared dates are agreed before registration opens, so entrants see a fixed campsite list. */
fun Lottery.canInvite(actorId: String, now: Long): Boolean = ownerId == actorId &&
    drawStatus == "PENDING" && (registrationStart?.toDate()?.time ?: 0L) > now

fun validateInvitation(lottery: Lottery, actorId: String, sites: List<Campsite>, now: Long) {
    require(lottery.canInvite(actorId, now)) { "Only the creator can invite campsites before registration opens." }
    require(sites.isNotEmpty() && sites.size <= 20) { "Choose between 1 and 20 campsites." }
    require(sites.map { it.id }.distinct().size == sites.size) { "Choose each campsite once." }
    require(sites.map { it.ownerId }.distinct().size == 1 && sites.first().ownerId != actorId) {
        "Each invitation must be for one other owner's campsites."
    }
    require(sites.none { it.id in lottery.campsiteIds }) { "A selected campsite is already in this lottery." }
}

fun validateAcceptance(invite: LotteryInvitation, lottery: Lottery, actorId: String, sites: List<Campsite>, now: Long) {
    require(invite.inviteeId == actorId) { "Only the invited owner can respond." }
    require(invite.status == "PENDING") { "This invitation has already been answered." }
    require(lottery.canInvite(invite.inviterId, now)) { "Registration has opened; this invitation has expired." }
    require(invite.lotteryId == lottery.id && invite.registrationStart == lottery.registrationStart &&
        invite.registrationEnd == lottery.registrationEnd && invite.bookingStart == lottery.bookingStart &&
        invite.bookingEnd == lottery.bookingEnd) { "The lottery dates changed. Ask for a new invitation." }
    require(sites.map { it.id }.toSet() == invite.campsiteIds.toSet() && sites.all { it.ownerId == actorId }) {
        "A campsite was removed or changed owners. Ask for a new invitation."
    }
}
