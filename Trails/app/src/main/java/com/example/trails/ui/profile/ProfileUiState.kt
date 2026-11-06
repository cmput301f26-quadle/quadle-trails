// UI state, form validation and role rules for the profile screens (US 01.09.01, 01.07.03, 01.08.01, 03.11.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: None currently known.
package com.example.trails.ui.profile

import com.example.trails.data.model.Role
import com.example.trails.data.model.User

/** Every situation the profile screens can be in. */
sealed interface ProfileUiState {

    /** Waiting for the profile to load. */
    data object Loading : ProfileUiState

    /** This device has no profile yet, so the user sees Welcome (US 01.09.01). */
    data object NoProfile : ProfileUiState

    /**
     * A profile exists for this device.
     *
     * @property user the profile.
     */
    data class Loaded(val user: User) : ProfileUiState

    /** The profile was deleted in this session (US 01.08.01). */
    data object Deleted : ProfileUiState

    /**
     * The profile could not be loaded.
     *
     * @property reason the error message, if there is one.
     */
    data class Error(val reason: String?) : ProfileUiState
}

/**
 * Picks the screen state from the latest profile.
 *
 * @param user the profile for this device, or null if there is none.
 * @return [ProfileUiState.NoProfile] or [ProfileUiState.Loaded].
 */
fun buildProfileState(user: User?): ProfileUiState =
    if (user == null) ProfileUiState.NoProfile else ProfileUiState.Loaded(user)

/**
 * Applies a completed deletion on top of the loaded state, so "Profile deleted"
 * stays on screen instead of changing to Welcome when the profile disappears.
 *
 * @param state the state from the repository.
 * @param deleted true once the profile was deleted in this session.
 * @return [ProfileUiState.Deleted] when [deleted], otherwise [state].
 */
fun withDeletion(state: ProfileUiState, deleted: Boolean): ProfileUiState =
    if (deleted) ProfileUiState.Deleted else state

/**
 * The editable fields on Edit Profile (US 01.07.03).
 *
 * @property name display name; required.
 * @property email contact email; required.
 * @property phone contact phone; optional.
 */
data class ProfileForm(
    val name: String = "",
    val email: String = "",
    val phone: String = ""
)

/** Why a field is not acceptable. The screen maps each value to a message. */
enum class ProfileFieldError {
    /** The name is empty. */
    NAME_REQUIRED,

    /** The email is empty. */
    EMAIL_REQUIRED,

    /** The email is not in the form name@domain.tld. */
    EMAIL_INVALID,

    /** The phone contains letters, or has fewer than 7 or more than 15 digits. */
    PHONE_INVALID
}

/**
 * The problems found in a form, one per field.
 *
 * @property name the name problem, or null.
 * @property email the email problem, or null.
 * @property phone the phone problem, or null.
 */
data class ProfileFormErrors(
    val name: ProfileFieldError? = null,
    val email: ProfileFieldError? = null,
    val phone: ProfileFieldError? = null
) {
    /** True when at least one field must be fixed before saving. */
    val hasErrors: Boolean get() = name != null || email != null || phone != null
}

private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

private const val PHONE_SYMBOLS = " -()+."

/**
 * Checks a form before it is saved.
 *
 * Name and email are required. Phone is optional; when filled in it may contain
 * digits, spaces, dashes, brackets, dots and a leading "+", and must have 7 to 15
 * digits. 15 is the international maximum; no single country format is assumed.
 *
 * @param form the values typed in.
 * @return the problems found; [ProfileFormErrors.hasErrors] is false when the form
 *  can be saved.
 */
fun validateProfileForm(form: ProfileForm): ProfileFormErrors {
    val name = form.name.trim()
    val email = form.email.trim()
    val phone = form.phone.trim()
    val digits = phone.count { it.isDigit() }

    return ProfileFormErrors(
        name = if (name.isEmpty()) ProfileFieldError.NAME_REQUIRED else null,
        email = when {
            email.isEmpty() -> ProfileFieldError.EMAIL_REQUIRED
            !EMAIL_PATTERN.matches(email) -> ProfileFieldError.EMAIL_INVALID
            else -> null
        },
        phone = when {
            phone.isEmpty() -> null
            phone.any { !it.isDigit() && it !in PHONE_SYMBOLS } -> ProfileFieldError.PHONE_INVALID
            digits !in 7..15 -> ProfileFieldError.PHONE_INVALID
            else -> null
        }
    )
}

/**
 * Fills a form from an existing profile.
 *
 * @return the form for this profile.
 */
fun User.toProfileForm(): ProfileForm = ProfileForm(name = name, email = email, phone = phone)

/**
 * Removes leading and trailing whitespace from every field.
 *
 * @return the trimmed form.
 */
fun ProfileForm.trimmed(): ProfileForm =
    ProfileForm(name = name.trim(), email = email.trim(), phone = phone.trim())

/**
 * A new profile for a device that has none (US 01.09.01). It starts as an entrant
 * only; other roles are never granted from the app.
 *
 * @param deviceId the device the profile belongs to.
 * @return the profile to save.
 */
fun newProfileFor(deviceId: String): User =
    User(deviceId = deviceId, roles = listOf(Role.ENTRANT), activeMode = Role.ENTRANT)

/**
 * The modes this user may switch between (US 03.11.01), in the order
 * ENTRANT, OWNER, ADMIN.
 *
 * @return the roles the user holds.
 */
fun authorisedModes(user: User): List<Role> = Role.entries.filter { it in user.roles }

/**
 * Whether this user may act as [role].
 *
 * @return true only when the role is already on the profile.
 */
fun canSwitchTo(user: User, role: Role): Boolean = role in user.roles

/**
 * The mode the app should actually use. If the stored mode is no longer one of the
 * user's roles (for example a role was removed), the first held role is used, and
 * ENTRANT if the user holds none.
 *
 * @return a mode the user is authorised for.
 */
fun effectiveMode(user: User): Role =
    if (user.activeMode in user.roles) user.activeMode
    else authorisedModes(user).firstOrNull() ?: Role.ENTRANT

/**
 * Changes the active mode. A role the user does not hold is refused, so roles can
 * be switched here but never granted.
 *
 * @param user the profile to change.
 * @param role the mode to act as.
 * @return the updated profile, or [user] unchanged if [role] is not held.
 */
fun switchMode(user: User, role: Role): User =
    if (canSwitchTo(user, role)) user.copy(activeMode = role) else user

/** Progress of saving profile edits. */
sealed interface SaveState {

    /** Nothing in progress. */
    data object Idle : SaveState

    /** A save is running; the button should be disabled. */
    data object Saving : SaveState

    /** The repository confirmed the save. */
    data object Saved : SaveState

    /**
     * The save failed; the typed values are kept.
     *
     * @property reason the error message, if there is one.
     */
    data class Failed(val reason: String?) : SaveState
}

/** Progress of deleting the profile (US 01.08.01). */
sealed interface DeleteState {

    /** Nothing in progress. */
    data object Idle : DeleteState

    /** Deletion is running; the dialog buttons should be disabled. */
    data object Deleting : DeleteState

    /**
     * Deletion failed and the profile is still available.
     *
     * @property reason the error message, if there is one.
     */
    data class Failed(val reason: String?) : DeleteState
}

/** Progress of switching modes (US 03.11.01). */
sealed interface ModeSwitchState {

    /** Nothing in progress. */
    data object Idle : ModeSwitchState

    /** The new mode is being saved. */
    data object Switching : ModeSwitchState

    /**
     * The new mode was saved; the screen should navigate to its home.
     *
     * @property mode the mode switched to.
     */
    data class Switched(val mode: Role) : ModeSwitchState

    /**
     * The switch failed and the previous mode is still active.
     *
     * @property reason the error message, if there is one.
     */
    data class Failed(val reason: String?) : ModeSwitchState
}
