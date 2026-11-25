// Progress of adding, changing or removing the profile photo (US 01.08.02).
// OWNER: Ibrahim (ibr2309) — shared image upload, used on Aarav's Edit profile screen
// Outstanding issues: none.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.profile

/**
 * Progress of the latest profile photo change.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
sealed interface PhotoState {
    /** Nothing in progress. */
    data object Idle : PhotoState

    /** A new photo is being uploaded and saved. */
    data object Uploading : PhotoState

    /** The photo is being removed. */
    data object Removing : PhotoState

    /**
     * The last change failed; the profile keeps its previous photo.
     *
     * @property reason the error message, if there is one.
     */
    data class Failed(val reason: String?) : PhotoState
}
