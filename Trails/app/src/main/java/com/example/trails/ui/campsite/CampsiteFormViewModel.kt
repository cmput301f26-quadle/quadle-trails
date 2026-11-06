// Controller for the Register / Edit campsite form (US 02.01.01, 02.01.03).
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Outstanding issues: picking a photo makes the save fail until Firebase Storage is enabled
//  (see FirestoreCampsiteRepository); saving without a photo works.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trails.data.model.Campsite
import com.example.trails.data.repository.CampsiteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Everything [CampsiteFormContent] needs to draw.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @property fields what the owner has typed.
 * @property errors field name to error message, shown under each field.
 * @property isEditing true when editing an existing campsite, false when registering.
 * @property isLoading true while the campsite being edited loads.
 * @property isSaving true while a save is in progress.
 * @property notFound true if the campsite being edited does not exist.
 * @property savedId the campsite's id once saved; the screen navigates when this is set.
 * @property saveFailed true if the last save failed; the screen shows a retry message.
 */
data class CampsiteFormUiState(
    val fields: CampsiteFormFields = CampsiteFormFields(),
    val errors: Map<String, String> = emptyMap(),
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val notFound: Boolean = false,
    val savedId: String? = null,
    val saveFailed: Boolean = false
)

/**
 * Controller for [CampsiteFormScreen]: loads the campsite when editing, validates, and saves.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param repository where campsites are stored.
 * @param ownerId owner assigned to newly registered campsites.
 * @param campsiteId campsite to edit, or null to register a new one.
 */
class CampsiteFormViewModel(
    private val repository: CampsiteRepository,
    private val ownerId: String,
    campsiteId: String?
) : ViewModel() {

    private var base: Campsite? = null
    private val _state = MutableStateFlow(
        CampsiteFormUiState(isEditing = campsiteId != null, isLoading = campsiteId != null)
    )

    /** The form state. */
    val state: StateFlow<CampsiteFormUiState> = _state.asStateFlow()

    init {
        if (campsiteId != null) {
            viewModelScope.launch {
                val campsite = repository.getById(campsiteId)
                base = campsite
                _state.update {
                    if (campsite == null) it.copy(isLoading = false, notFound = true)
                    else it.copy(isLoading = false, fields = CampsiteFormFields.from(campsite))
                }
            }
        }
    }

    /** Records new field values and clears errors on the fields that changed. */
    fun onFieldsChange(fields: CampsiteFormFields) {
        _state.update { s ->
            val changed = fields.changedFrom(s.fields)
            s.copy(fields = fields, errors = s.errors - changed, saveFailed = false)
        }
    }

    /**
     * Validates and, if valid, saves the campsite, uploading a newly picked photo first.
     * Ignored while a save is in progress.
     */
    fun onSave() {
        val current = _state.value
        if (current.isSaving || current.isLoading || current.notFound) return
        when (val result = current.fields.toCampsite(base, ownerId)) {
            is CampsiteFormResult.Invalid -> _state.update { it.copy(errors = result.errors) }
            is CampsiteFormResult.Valid -> {
                _state.update { it.copy(isSaving = true, errors = emptyMap(), saveFailed = false) }
                viewModelScope.launch {
                    try {
                        val image = result.campsite.imageUrl
                        val toSave = if (image != null && isLocalImage(image)) {
                            result.campsite.copy(imageUrl = repository.uploadImage(image))
                        } else {
                            result.campsite
                        }
                        val id = repository.save(toSave)
                        _state.update { it.copy(isSaving = false, savedId = id) }
                    } catch (e: Exception) {
                        _state.update { it.copy(isSaving = false, saveFailed = true) }
                    }
                }
            }
        }
    }
}
