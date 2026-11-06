// OWNER: Skanda (skandarajeev) — Lottery Setup
// USER STORIES: 02.04.01–02.04.07
// PURPOSE: Create a lottery in three steps plus a review (Figma F07–F18), or edit one when
//  lotteryId is set (F20, F21). Pre-selects the campsite when campsiteId is passed in.
// Outstanding issues: none.
package com.example.trails.ui.lottery

import android.content.res.Resources
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.fake.TestCampsites
import com.example.trails.ui.theme.TrailsTheme

/** Connects [LotteryFormViewModel] to navigation and renders [LotteryFormContent]. */
@Composable
fun LotteryFormScreen(
    navController: NavController,
    lotteryId: String?,
    campsiteId: String?,
    modifier: Modifier = Modifier,
    reuseLotteryId: String? = null,
    viewModel: LotteryFormViewModel = viewModel(key = "lottery_form_${lotteryId}_${campsiteId}_$reuseLotteryId") {
        LotteryFormViewModel(lotteryId = lotteryId, campsiteId = campsiteId, reuseLotteryId = reuseLotteryId)
    }
) {
    val state by viewModel.state.collectAsState()
    val resources = LocalContext.current.resources
    val leave: () -> Unit = { if (!viewModel.onBack()) navController.popBackStack() }
    BackHandler(onBack = leave)

    // After an edit is saved, tell the details page what changed and go back to it (Figma F22).
    LaunchedEffect(state.savedId) {
        if (state.savedId != null && state.isEditing) {
            navController.previousBackStackEntry?.savedStateHandle?.set(LOTTERY_SAVED_MESSAGE, savedMessage(resources, state.savedChange))
            navController.popBackStack()
        }
    }

    // After deleting, skip the details page of the deleted lottery and go back to the list.
    LaunchedEffect(state.deleted) {
        if (state.deleted && !navController.popBackStack(LotteryRoutes.LOTTERY_DETAILS, inclusive = true)) {
            navController.popBackStack()
        }
    }

    LotteryFormContent(
        state = state,
        actions = LotteryFormActions(
            onBack = leave,
            onNameChange = viewModel::onNameChange,
            onToggleCampsite = viewModel::onToggleCampsite,
            onRegistrationChange = viewModel::onRegistrationChange,
            onBookingChange = viewModel::onBookingChange,
            onWinnersChange = viewModel::onWinnersChange,
            onWaitlistLimitChange = viewModel::onWaitlistLimitChange,
            onWaitlistCapacityChange = viewModel::onWaitlistCapacityChange,
            onNext = viewModel::onNext,
            onEditStep = viewModel::onEditStep,
            onSave = viewModel::onSave,
            onDismissSaveError = viewModel::onDismissSaveError,
            onDelete = viewModel::onDelete,
            onDismissDeleteError = viewModel::onDismissDeleteError,
            // "Done" on the published dialog opens the new lottery in place of the wizard.
            onPublishedDone = { id ->
                navController.navigate(LotteryRoutes.lotteryDetails(id)) {
                    popUpTo(LotteryRoutes.LOTTERY_FORM) { inclusive = true }
                }
            }
        ),
        modifier = modifier
    )
}

/** Everything the form can ask its controller to do; defaults do nothing, for previews and tests. */
class LotteryFormActions(
    val onBack: () -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onToggleCampsite: (String) -> Unit = {},
    val onRegistrationChange: (Long, Long) -> Unit = { _, _ -> },
    val onBookingChange: (Long, Long) -> Unit = { _, _ -> },
    val onWinnersChange: (Int) -> Unit = {},
    val onWaitlistLimitChange: (Boolean) -> Unit = {},
    val onWaitlistCapacityChange: (Int) -> Unit = {},
    val onNext: () -> Unit = {},
    val onEditStep: (LotteryStep) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDismissSaveError: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onDismissDeleteError: () -> Unit = {},
    val onPublishedDone: (String) -> Unit = {}
)

/**
 * Stateless lottery form, split out so previews and UI tests can drive it directly.
 *
 * @param state what to show.
 * @param actions callbacks for every button and field.
 */
@Composable
fun LotteryFormContent(state: LotteryFormUiState, actions: LotteryFormActions, modifier: Modifier = Modifier) {
    // Publish stays tappable without a name so tapping it can point at the empty field.
    var showNameError by rememberSaveable { mutableStateOf(false) }
    val title = when {
        state.isEditing -> R.string.lottery_edit_title
        state.step == LotteryStep.REVIEW -> R.string.lottery_review_title
        else -> R.string.lottery_new_title
    }
    Scaffold(
        modifier = modifier.background(lotteryBackground()),
        containerColor = Color.Transparent,
        topBar = { LotteryHeader(stringResource(title), actions.onBack) },
        bottomBar = {
            if (!state.isLoading && !state.notFound && !state.isLocked) {
                FormFooter(state, actions, onNameMissing = { showNameError = true })
            }
        }
    ) { padding ->
        val inner = Modifier
            .padding(padding)
            .fillMaxSize()
        when {
            state.isLoading -> Box(inner, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.testTag(LotteryTags.LOADING))
            }
            state.notFound -> CenteredNote(stringResource(R.string.lottery_not_found), inner)
            state.isLocked -> CenteredNote(stringResource(R.string.lottery_closed_note), inner)
            state.isEditing -> LotteryEditBody(state, actions, inner)
            else -> when (state.step) {
                LotteryStep.CAMPSITES -> CampsitesStep(state, actions, inner)
                LotteryStep.DATES -> DatesStep(state, actions, inner)
                LotteryStep.WINNERS -> WinnersStep(state, actions, inner)
                LotteryStep.REVIEW -> ReviewStep(state, actions, showNameError && state.draft.name.isBlank(), inner)
            }
        }
    }

    val savedId = state.savedId
    if (savedId != null && !state.isEditing) {
        PublishedDialog(state.draft, onDone = { actions.onPublishedDone(savedId) })
    }
    if (state.saveFailed) {
        SaveFailedDialog(
            title = stringResource(if (state.isEditing) R.string.lottery_save_failed_title else R.string.lottery_publish_failed_title),
            onRetry = actions.onSave,
            onDismiss = actions.onDismissSaveError
        )
    }
    if (state.deleteFailed) {
        DeleteFailedDialog(onRetry = actions.onDelete, onDismiss = actions.onDismissDeleteError)
    }
}

/** The step's main button, with the "pick a campsite" hint above it on the first step. */
@Composable
private fun FormFooter(state: LotteryFormUiState, actions: LotteryFormActions, onNameMissing: () -> Unit) {
    LotteryFooter {
        if (state.isEditing) {
            LotteryButton(
                text = stringResource(if (state.isSaving) R.string.lottery_saving else R.string.lottery_save_changes),
                enabled = state.canSave,
                onClick = actions.onSave,
                modifier = Modifier.testTag(LotteryTags.PRIMARY_BUTTON)
            )
            return@LotteryFooter
        }
        val picked = state.draft.campsiteIds.size
        if (state.step == LotteryStep.CAMPSITES && picked == 0) {
            Text(stringResource(R.string.lottery_campsites_required), style = LotteryType.Small, color = LotteryColors.Muted)
        }
        val label = when {
            state.step == LotteryStep.REVIEW ->
                stringResource(if (state.isSaving) R.string.lottery_publishing else R.string.lottery_publish)
            state.returnToReview || state.step == LotteryStep.WINNERS -> stringResource(R.string.lottery_to_review)
            state.step == LotteryStep.CAMPSITES && picked > 0 -> pluralStringResource(R.plurals.lottery_next_campsites, picked, picked)
            else -> stringResource(R.string.lottery_next)
        }
        val reviewReady = (state.problems - LotteryField.NAME).isEmpty() && !state.isSaving
        LotteryButton(
            text = label,
            enabled = if (state.step == LotteryStep.REVIEW) reviewReady else state.canContinue,
            onClick = {
                when {
                    state.step != LotteryStep.REVIEW -> actions.onNext()
                    state.draft.name.isBlank() -> onNameMissing()
                    else -> actions.onSave()
                }
            },
            modifier = Modifier.testTag(LotteryTags.PRIMARY_BUTTON)
        )
    }
}

@Composable
private fun CenteredNote(text: String, modifier: Modifier) {
    Box(modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = LotteryType.Body, color = LotteryColors.Muted, textAlign = TextAlign.Center)
    }
}

/** "Lottery published · Registration opens Apr 1 at 09:00. 5 winners are drawn automatically on Apr 8." */
@Composable
private fun PublishedDialog(draft: LotteryDraft, onDone: () -> Unit) {
    val opens = draft.registrationStart ?: 0L
    val draw = draft.drawTime ?: 0L
    val winners = pluralStringResource(R.plurals.lottery_published_winners, draft.numWinners, draft.numWinners)
    val body = if (opens <= System.currentTimeMillis()) {
        stringResource(R.string.lottery_published_body_open, winners, formatDay(draw))
    } else {
        stringResource(R.string.lottery_published_body, formatDay(opens), formatTime(opens), winners, formatDay(draw))
    }
    LotteryDialog(stringResource(R.string.lottery_published_title), body, stringResource(R.string.lottery_done), false, onDone, onDone)
}

/** "Couldn't save changes · Your edits are still here." with a red Try again. */
@Composable
private fun SaveFailedDialog(title: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    LotteryDialog(title, stringResource(R.string.lottery_save_failed_body), stringResource(R.string.lottery_try_again), true, onRetry, onDismiss)
}

/** "Couldn't delete the lottery" with a red Try again. */
@Composable
internal fun DeleteFailedDialog(onRetry: () -> Unit, onDismiss: () -> Unit) {
    LotteryDialog(
        stringResource(R.string.lottery_delete_failed_title), stringResource(R.string.lottery_delete_failed_body),
        stringResource(R.string.lottery_try_again), true, onRetry, onDismiss, stringResource(R.string.lottery_cancel)
    )
}

/** The "Changes saved" banner text for [change] (Figma F22). */
internal fun savedMessage(resources: Resources, change: LotteryChange?): String = when (change) {
    is LotteryChange.Winners -> resources.getString(R.string.lottery_saved_winners, change.count)
    is LotteryChange.Waitlist -> change.capacity?.let { resources.getString(R.string.lottery_saved_waitlist, it) }
        ?: resources.getString(R.string.lottery_saved_waitlist_unlimited)
    LotteryChange.Name -> resources.getString(R.string.lottery_saved_name)
    LotteryChange.Campsites -> resources.getString(R.string.lottery_saved_campsites)
    LotteryChange.Registration -> resources.getString(R.string.lottery_saved_registration)
    LotteryChange.Booking -> resources.getString(R.string.lottery_saved_booking)
    LotteryChange.General, null -> resources.getString(R.string.lottery_saved_general)
}

/** Design-time preview of step 1 with one campsite picked (Figma F08). */
@Preview(showBackground = true, heightDp = 800)
@Composable
private fun CampsitesStepPreview() {
    TrailsTheme {
        LotteryFormContent(
            state = LotteryFormUiState(
                draft = LotteryDraft(campsiteIds = listOf(TestCampsites.BANFF_ID)),
                campsites = TestCampsites.all.map { CampsiteOption(it) }
            ),
            actions = LotteryFormActions()
        )
    }
}
