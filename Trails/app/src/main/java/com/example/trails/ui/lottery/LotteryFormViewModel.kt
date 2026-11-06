// Controller for the New lottery wizard and the Edit lottery screen (US 02.04.01–02.04.07).
// OWNER: Skanda (skandarajeev) — Lottery Setup
// Outstanding issues: none.
package com.example.trails.ui.lottery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Lottery
import com.example.trails.data.repository.CampsiteRepository
import com.example.trails.data.repository.LotteryRepository
import com.example.trails.data.repository.WaitlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The wizard's pages, in order (Figma F07–F17). */
enum class LotteryStep { CAMPSITES, DATES, WINNERS, REVIEW }

/**
 * A campsite the owner could put in the lottery.
 *
 * @property campsite the campsite.
 * @property takenBy name of another open or upcoming lottery it is already in, or null if free.
 */
data class CampsiteOption(val campsite: Campsite, val takenBy: String? = null) {
    val available: Boolean get() = takenBy == null
}

/**
 * Pairs each of the owner's campsites with the other lottery it is in, if any. A campsite is free
 * when it has no active lottery, the active lottery is the one being edited, or that lottery's
 * registration has already closed.
 */
fun campsiteOptions(campsites: List<Campsite>, lotteries: List<Lottery>, editingId: String?, now: Long): List<CampsiteOption> =
    campsites.map { campsite ->
        val other = lotteries.find { it.id == campsite.activeLotteryId }
        val stillRunning = other != null && other.id != editingId && (other.registrationEnd.millis()?.let { now < it } ?: true)
        CampsiteOption(campsite, if (stillRunning) other?.name else null)
    }

/**
 * Everything the lottery form screens need to draw.
 *
 * @property draft what the owner has chosen so far.
 * @property original the saved lottery when editing, or null for a new one.
 * @property isEditing true on the Edit lottery screen, false in the New lottery wizard.
 * @property isLoading true while the lottery being edited (or reused) loads.
 * @property notFound true if the lottery being edited doesn't exist.
 * @property step which wizard page is showing.
 * @property returnToReview true after tapping Edit on the review page; the step's button goes back there.
 * @property campsites the owner's campsites, for the picker.
 * @property entrantCount people already on the waiting list (editing only).
 * @property now the time the rules are checked against.
 * @property isSaving true while saving.
 * @property saveFailed true if the last save failed; the screen offers "Try again".
 * @property savedId the lottery's id once saved; the screen moves on when this is set.
 * @property savedChange what an edit changed, for the "Changes saved" banner.
 * @property isDeleting true while deleting.
 * @property deleted true once the lottery is deleted; the screen leaves.
 * @property deleteFailed true if the last delete failed.
 */
data class LotteryFormUiState(
    val draft: LotteryDraft = LotteryDraft(),
    val original: Lottery? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val step: LotteryStep = LotteryStep.CAMPSITES,
    val returnToReview: Boolean = false,
    val campsites: List<CampsiteOption> = emptyList(),
    val entrantCount: Int = 0,
    val now: Long = 0L,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
    val savedId: String? = null,
    val savedChange: LotteryChange? = null,
    val isDeleting: Boolean = false,
    val deleted: Boolean = false,
    val deleteFailed: Boolean = false
) {
    /** Status of the lottery being edited; null for a new one. */
    val status: LotteryStatus? get() = original?.let { lotteryStatus(it, entrantCount, now) }

    /** When registration opened, if it already has; its start is then fixed. */
    val openedAt: Long? get() = original?.registrationStart.millis()?.takeIf { it <= now }

    /** True if the lottery has closed and can't be edited any more. */
    val isLocked: Boolean get() = status?.isClosed == true

    /** What's stopping the lottery from being saved, by field. */
    val problems: Map<LotteryField, LotteryProblem> get() = draft.problems(now, entrantCount, openedAt)

    /** True if the current wizard page is complete, so its button is enabled. */
    val canContinue: Boolean
        get() = when (step) {
            LotteryStep.CAMPSITES -> LotteryField.CAMPSITES !in problems
            LotteryStep.DATES -> LotteryField.REGISTRATION !in problems && LotteryField.BOOKING !in problems
            LotteryStep.WINNERS -> LotteryField.WAITLIST !in problems
            LotteryStep.REVIEW -> problems.isEmpty()
        }

    /** True if Publish / Save changes can be tapped. */
    val canSave: Boolean get() = problems.isEmpty() && !isSaving && !isLoading && !notFound && !isLocked

    /** True if Delete lottery can be offered: editing, and nobody has joined the waiting list. */
    val canDelete: Boolean get() = original != null && entrantCount == 0 && !isLocked && !isSaving && !isDeleting

    /** Names of the picked campsites, in the order picked. */
    val campsiteNames: List<String>
        get() = draft.campsiteIds.mapNotNull { id -> campsites.find { it.campsite.id == id }?.campsite?.name }
}

/**
 * Controller for [LotteryFormScreen]: loads the owner's campsites (and the lottery when editing),
 * moves through the wizard, validates and saves.
 *
 * @param lotteryRepository where lotteries are stored.
 * @param campsiteRepository where campsites are stored; covered campsites point at their lottery.
 * @param waitlistRepository used to count people already on the waiting list when editing.
 * @param ownerId owner whose campsites can be picked, and who owns a new lottery.
 * @param lotteryId lottery to edit, or null to create one.
 * @param campsiteId campsite to pre-select, e.g. from "Create Lottery" on a campsite page.
 * @param reuseLotteryId closed lottery whose settings (not dates) start a new one.
 * @param clock current time; tests pass a fixed one.
 */
class LotteryFormViewModel(
    private val lotteryRepository: LotteryRepository = AppContainer.lotteryRepository,
    private val campsiteRepository: CampsiteRepository = AppContainer.campsiteRepository,
    private val waitlistRepository: WaitlistRepository = AppContainer.waitlistRepository,
    private val ownerId: String = CurrentUser.id(),
    private val lotteryId: String? = null,
    private val campsiteId: String? = null,
    private val reuseLotteryId: String? = null,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _state = MutableStateFlow(
        LotteryFormUiState(
            isEditing = lotteryId != null,
            isLoading = lotteryId != null || reuseLotteryId != null,
            now = clock()
        )
    )
    private var preselectDone = campsiteId == null

    /** The form state. */
    val state: StateFlow<LotteryFormUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(campsiteRepository.observeByOwner(ownerId), lotteryRepository.observeAll()) { campsites, lotteries ->
                campsiteOptions(campsites, lotteries, lotteryId, clock())
            }.collect(::onCampsitesLoaded)
        }
        if (lotteryId != null) viewModelScope.launch { loadForEdit(lotteryId) }
        if (reuseLotteryId != null) viewModelScope.launch { loadForReuse(reuseLotteryId) }
    }

    private fun onCampsitesLoaded(options: List<CampsiteOption>) {
        _state.update { s ->
            var draft = s.draft
            if (!s.isEditing) {
                // A campsite picked earlier may have joined another lottery since.
                val taken = options.filterNot { it.available }.map { it.campsite.id }.toSet()
                draft = draft.copy(campsiteIds = draft.campsiteIds.filterNot { it in taken })
            }
            if (!preselectDone) {
                preselectDone = true
                val pick = options.find { it.campsite.id == campsiteId && it.available }
                if (pick != null && pick.campsite.id !in draft.campsiteIds) draft = draft.toggleCampsite(pick.campsite.id)
            }
            s.copy(campsites = options, draft = draft)
        }
    }

    private suspend fun loadForEdit(id: String) {
        val lottery = lotteryRepository.getById(id)
        val entrants = if (lottery == null) 0 else runCatching { waitlistRepository.getEntryCount(id) }.getOrDefault(0)
        _state.update {
            if (lottery == null) it.copy(isLoading = false, notFound = true)
            else it.copy(isLoading = false, original = lottery, draft = LotteryDraft.from(lottery), entrantCount = entrants, now = clock())
        }
    }

    private suspend fun loadForReuse(id: String) {
        val lottery = lotteryRepository.getById(id)
        _state.update { s ->
            if (lottery == null) return@update s.copy(isLoading = false)
            val free = s.campsites.filter { it.available }.map { it.campsite.id }.toSet()
            val draft = LotteryDraft.from(lottery).copy(
                campsiteIds = lottery.campsiteIds.filter { s.campsites.isEmpty() || it in free },
                registrationStart = null, registrationEnd = null, bookingStart = null, bookingEnd = null
            )
            s.copy(isLoading = false, draft = draft)
        }
    }

    private fun edit(change: (LotteryDraft) -> LotteryDraft) {
        _state.update { it.copy(draft = change(it.draft), saveFailed = false) }
    }

    /** Records the lottery name. */
    fun onNameChange(name: String) = edit { it.copy(name = name) }

    /** Ticks or unticks a campsite; campsites in another running lottery can't be ticked (US 02.04.01, 02.04.02). */
    fun onToggleCampsite(id: String) {
        val option = _state.value.campsites.find { it.campsite.id == id } ?: return
        if (option.available || id in _state.value.draft.campsiteIds) edit { it.toggleCampsite(id) }
    }

    /** Sets the registration period (US 02.04.03). */
    fun onRegistrationChange(start: Long, end: Long) = edit { it.copy(registrationStart = start, registrationEnd = end) }

    /** Sets the booking period (US 02.04.04). */
    fun onBookingChange(start: Long, end: Long) = edit { it.copy(bookingStart = start, bookingEnd = end) }

    /** Sets the number of winners (US 02.04.05). */
    fun onWinnersChange(count: Int) = edit { it.withWinners(count) }

    /** Turns the waiting list limit on or off (US 02.04.07). */
    fun onWaitlistLimitChange(on: Boolean) = edit { it.withWaitlistLimit(on) }

    /** Sets the waiting list limit (US 02.04.07). */
    fun onWaitlistCapacityChange(capacity: Int) = edit { it.withWaitlistCapacity(capacity) }

    /** Goes to the next wizard page, or back to the review after an Edit. Ignored if the page isn't complete. */
    fun onNext() {
        _state.update { s ->
            if (!s.canContinue || s.step == LotteryStep.REVIEW) return@update s
            val next = if (s.returnToReview) LotteryStep.REVIEW else LotteryStep.entries[s.step.ordinal + 1]
            s.copy(step = next, returnToReview = false, now = clock())
        }
    }

    /**
     * Goes back one wizard page.
     *
     * @return false on the first page, so the screen leaves the wizard instead.
     */
    fun onBack(): Boolean {
        val s = _state.value
        if (s.isEditing || s.step == LotteryStep.CAMPSITES && !s.returnToReview) return false
        val previous = if (s.returnToReview) LotteryStep.REVIEW else LotteryStep.entries[s.step.ordinal - 1]
        _state.update { it.copy(step = previous, returnToReview = false) }
        return true
    }

    /** Opens a wizard page from an Edit link on the review page. */
    fun onEditStep(step: LotteryStep) {
        _state.update { it.copy(step = step, returnToReview = step != LotteryStep.REVIEW) }
    }

    /** Hides the "Couldn't save" message. */
    fun onDismissSaveError() {
        _state.update { it.copy(saveFailed = false) }
    }

    /**
     * Saves the lottery (publishes a new one, or saves changes), then points each covered
     * campsite at it and frees campsites that were removed. Ignored while a save is in progress.
     */
    fun onSave() {
        _state.update { it.copy(now = clock()) }
        val current = _state.value
        if (!current.canSave) return
        _state.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                val lottery = current.draft.toLottery(current.original, ownerId)
                val id = lotteryRepository.save(lottery)
                linkCampsites(id, lottery.campsiteIds, current.original?.campsiteIds.orEmpty())
                val change = current.original?.let { describeChange(it, lottery) }
                _state.update { it.copy(isSaving = false, savedId = id, savedChange = change) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    /**
     * Deletes the lottery being edited if nobody has joined its waiting list, and frees its
     * campsites. If someone joined since the screen loaded, nothing is deleted and the
     * entrant count is updated so the screen explains why.
     */
    fun onDelete() {
        val current = _state.value
        val id = current.original?.id ?: return
        if (!current.canDelete) return
        _state.update { it.copy(isDeleting = true, deleteFailed = false) }
        viewModelScope.launch {
            try {
                when (val result = deleteLotteryIfEmpty(id, lotteryRepository, campsiteRepository, waitlistRepository)) {
                    DeleteResult.Deleted -> _state.update { it.copy(isDeleting = false, deleted = true) }
                    is DeleteResult.HasEntrants -> _state.update { it.copy(isDeleting = false, entrantCount = result.entrants) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isDeleting = false, deleteFailed = true) }
            }
        }
    }

    /** Hides the "Couldn't delete" message. */
    fun onDismissDeleteError() {
        _state.update { it.copy(deleteFailed = false) }
    }

    private suspend fun linkCampsites(lotteryId: String, now: List<String>, before: List<String>) {
        for (id in now) {
            val campsite = campsiteRepository.getById(id) ?: continue
            if (campsite.activeLotteryId != lotteryId) campsiteRepository.save(campsite.copy(activeLotteryId = lotteryId))
        }
        for (id in before - now.toSet()) {
            val campsite = campsiteRepository.getById(id) ?: continue
            if (campsite.activeLotteryId == lotteryId) campsiteRepository.save(campsite.copy(activeLotteryId = null))
        }
    }
}
