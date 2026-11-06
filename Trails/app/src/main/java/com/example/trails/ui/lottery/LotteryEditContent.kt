// OWNER: Skanda (skandarajeev) — Lottery Setup
// USER STORIES: 02.04.06 (update lottery), plus Delete lottery while nobody has joined
// PURPOSE: Edit lottery on one screen: name, a settings card whose rows open sheets, and the
//  winners stepper showing what changed (Figma F20).
// Outstanding issues: none.
package com.example.trails.ui.lottery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.trails.R

/** Sheets the edit screen can open besides the period pickers. */
private enum class EditSheet { CAMPSITES, WAITLIST }

/** Body of the Edit lottery screen. */
@Composable
internal fun LotteryEditBody(state: LotteryFormUiState, actions: LotteryFormActions, modifier: Modifier) {
    var periodSheet by rememberSaveable { mutableStateOf<PeriodSheet?>(null) }
    var editSheet by rememberSaveable { mutableStateOf<EditSheet?>(null) }
    val draft = state.draft
    val dash = stringResource(R.string.lottery_dash)

    StepBody(modifier, spacing = 12) {
        Note(stringResource(if (state.openedAt == null) R.string.lottery_edit_scheduled_note else R.string.lottery_edit_open_note))
        LotteryNameField(draft.name, actions.onNameChange, showError = draft.name.isBlank())
        LotteryCard(spacing = 12.dp) {
            SettingsRow(stringResource(R.string.lottery_row_campsites),
                state.campsiteNames.joinToString(stringResource(R.string.lottery_list_separator)).ifEmpty { dash },
                { editSheet = EditSheet.CAMPSITES }, LotteryTags.settingsRow("campsites"))
            CardDivider()
            SettingsRow(stringResource(R.string.lottery_row_registration),
                draft.registrationStart?.let { s -> draft.registrationEnd?.let { e -> dayRange(s, e) } } ?: dash,
                { periodSheet = PeriodSheet.REGISTRATION }, LotteryTags.settingsRow("registration"))
            CardDivider()
            SettingsRow(stringResource(R.string.lottery_row_booking),
                draft.bookingStart?.let { s -> draft.bookingEnd?.let { e -> dayRange(s, e) } } ?: dash,
                { periodSheet = PeriodSheet.BOOKING }, LotteryTags.settingsRow("booking"))
            CardDivider()
            SettingsRow(stringResource(R.string.lottery_row_waitlist),
                waitlistText(if (draft.limitWaitlist) draft.waitlistCapacity else null, short = true),
                { editSheet = EditSheet.WAITLIST }, LotteryTags.settingsRow("waitlist"))
        }
        // Problems other than the name, which shows under its own field.
        (state.problems - LotteryField.NAME).values.forEach { ErrorText(problemText(it)) }
        WinnersEditCard(draft.numWinners, state.original?.numWinners, actions.onWinnersChange)
        Spacer(Modifier.height(4.dp))
        DeleteLotterySection(state.entrantCount, state.isDeleting, onConfirm = actions.onDelete)
    }

    PeriodSheets(state, periodSheet, onClose = { periodSheet = null }, actions)
    when (editSheet) {
        EditSheet.CAMPSITES -> LotterySheet(stringResource(R.string.lottery_row_campsites), onDismiss = { editSheet = null }) {
            CampsiteChoices(state, actions.onToggleCampsite)
            LotteryButton(stringResource(R.string.lottery_done), onClick = { editSheet = null })
        }
        EditSheet.WAITLIST -> LotterySheet(stringResource(R.string.lottery_row_waitlist), onDismiss = { editSheet = null }) {
            WaitlistLimitCard(state, actions)
            LotteryButton(stringResource(R.string.lottery_done), onClick = { editSheet = null })
        }
        null -> Unit
    }
}

/** "Apr 1 → Apr 8". */
@Composable
private fun dayRange(start: Long, end: Long) = stringResource(R.string.lottery_range_compact, formatDay(start), formatDay(end))

/** Grey bordered note at the top, e.g. "Registration hasn't opened yet, so everything can still change." */
@Composable
private fun Note(text: String) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, LotteryColors.Line, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text, style = LotteryType.Small, color = LotteryColors.Muted)
    }
}

/** Winners with a small stepper, and "Changed from 5" once it differs from the saved value. */
@Composable
private fun WinnersEditCard(winners: Int, savedWinners: Int?, onChange: (Int) -> Unit) {
    LotteryCard(highlighted = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.lottery_row_winners), style = LotteryType.BodyStrong)
                if (savedWinners != null && savedWinners != winners) {
                    Text(
                        stringResource(R.string.lottery_changed_from, savedWinners),
                        style = LotteryType.Small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            StepButton("−", stringResource(R.string.lottery_decrease), { onChange(winners - 1) },
                size = 34.dp, primary = false, enabled = winners > 1, tag = LotteryTags.WINNERS_DECREASE)
            Text(winners.toString(), style = LotteryType.Number, modifier = Modifier.testTag(LotteryTags.WINNERS_VALUE))
            StepButton("+", stringResource(R.string.lottery_increase), { onChange(winners + 1) },
                size = 34.dp, primary = true, enabled = winners < MAX_WINNERS, tag = LotteryTags.WINNERS_INCREASE)
        }
    }
}
