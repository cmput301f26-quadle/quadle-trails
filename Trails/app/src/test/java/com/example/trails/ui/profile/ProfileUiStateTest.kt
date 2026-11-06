// Unit tests for profile state, validation and role rules (US 01.09.01, 01.07.03, 01.08.01, 03.11.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: None currently known.
// AI assistance: Used AI to help write profile name, email, and phone validation tests.
package com.example.trails.ui.profile

import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileUiStateTest {

    private val priya = User(
        deviceId = "device-entrant-001",
        name = "Priya Sharma",
        email = "priya.sharma@example.com",
        phone = "780-555-0142"
    )
    private val morgan = User(
        deviceId = "device-admin-001",
        name = "Morgan Lee",
        email = "morgan.lee@example.com",
        roles = listOf(Role.ENTRANT, Role.ADMIN),
        activeMode = Role.ADMIN
    )

    private fun form(name: String = "Priya", email: String = "priya@example.com", phone: String = "") =
        ProfileForm(name, email, phone)

    // US 01.09.01

    @Test
    fun buildState_noProfile_isWelcome() {
        assertEquals(ProfileUiState.NoProfile, buildProfileState(null))
    }

    @Test
    fun buildState_profile_isLoaded() {
        assertEquals(ProfileUiState.Loaded(priya), buildProfileState(priya))
    }

    @Test
    fun newProfile_isEntrantOnly() {
        val created = newProfileFor("uid-1")
        assertEquals("uid-1", created.deviceId)
        assertEquals(listOf(Role.ENTRANT), created.roles)
        assertEquals(Role.ENTRANT, created.activeMode)
    }

    // US 01.08.01

    @Test
    fun withDeletion_deletedWinsOverAnyState() {
        assertEquals(ProfileUiState.Deleted, withDeletion(ProfileUiState.Loaded(priya), deleted = true))
        assertEquals(ProfileUiState.Deleted, withDeletion(ProfileUiState.NoProfile, deleted = true))
    }

    @Test
    fun withDeletion_notDeleted_keepsTheState() {
        assertEquals(ProfileUiState.Loaded(priya), withDeletion(ProfileUiState.Loaded(priya), deleted = false))
    }

    // US 01.07.03

    @Test
    fun toProfileForm_copiesTheEditableFields() {
        assertEquals(ProfileForm("Priya Sharma", "priya.sharma@example.com", "780-555-0142"), priya.toProfileForm())
    }

    @Test
    fun trimmed_removesSurroundingSpaces() {
        assertEquals(
            ProfileForm("Morgan Lee", "morgan@example.com", "780-555-0142"),
            ProfileForm("  Morgan Lee ", " morgan@example.com", "780-555-0142  ").trimmed()
        )
    }

    @Test
    fun validate_goodForm_hasNoErrors() {
        assertFalse(validateProfileForm(form(phone = "780-555-0142")).hasErrors)
    }

    @Test
    fun validate_blankName_isRejected() {
        assertEquals(ProfileFieldError.NAME_REQUIRED, validateProfileForm(form(name = "   ")).name)
    }

    @Test
    fun validate_blankEmail_isRejected() {
        assertEquals(ProfileFieldError.EMAIL_REQUIRED, validateProfileForm(form(email = "")).email)
    }

    @Test
    fun validate_emailWithoutDomain_isRejected() {
        assertEquals(ProfileFieldError.EMAIL_INVALID, validateProfileForm(form(email = "priya@example")).email)
    }

    @Test
    fun validate_emailWithoutAt_isRejected() {
        assertEquals(ProfileFieldError.EMAIL_INVALID, validateProfileForm(form(email = "priya.example.com")).email)
    }

    @Test
    fun validate_emptyPhone_isAllowed() {
        assertNull(validateProfileForm(form(phone = "")).phone)
    }

    @Test
    fun validate_internationalPhone_isAllowed() {
        assertNull(validateProfileForm(form(phone = "+44 20 7946 0958")).phone)
    }

    @Test
    fun validate_sevenDigitPhone_isAllowed() {
        assertNull(validateProfileForm(form(phone = "555-0142")).phone)
    }

    @Test
    fun validate_sixDigitPhone_isRejected() {
        assertEquals(ProfileFieldError.PHONE_INVALID, validateProfileForm(form(phone = "555-014")).phone)
    }

    @Test
    fun validate_sixteenDigitPhone_isRejected() {
        assertEquals(ProfileFieldError.PHONE_INVALID, validateProfileForm(form(phone = "1234567890123456")).phone)
    }

    @Test
    fun validate_phoneWithLetters_isRejected() {
        assertEquals(ProfileFieldError.PHONE_INVALID, validateProfileForm(form(phone = "780-CALL-NOW")).phone)
    }

    @Test
    fun validate_severalProblems_areAllReported() {
        val errors = validateProfileForm(ProfileForm("", "bad", "12"))
        assertEquals(ProfileFieldError.NAME_REQUIRED, errors.name)
        assertEquals(ProfileFieldError.EMAIL_INVALID, errors.email)
        assertEquals(ProfileFieldError.PHONE_INVALID, errors.phone)
    }

    // US 03.11.01

    @Test
    fun authorisedModes_entrantOnly() {
        assertEquals(listOf(Role.ENTRANT), authorisedModes(priya))
    }

    @Test
    fun authorisedModes_alwaysInEntrantOwnerAdminOrder() {
        val all = priya.copy(roles = listOf(Role.ADMIN, Role.ENTRANT, Role.OWNER))
        assertEquals(listOf(Role.ENTRANT, Role.OWNER, Role.ADMIN), authorisedModes(all))
    }

    @Test
    fun canSwitchTo_onlyHeldRoles() {
        assertTrue(canSwitchTo(morgan, Role.ADMIN))
        assertTrue(canSwitchTo(morgan, Role.ENTRANT))
        assertFalse(canSwitchTo(morgan, Role.OWNER))
    }

    @Test
    fun switchMode_toHeldRole_changesOnlyTheMode() {
        val switched = switchMode(morgan, Role.ENTRANT)
        assertEquals(Role.ENTRANT, switched.activeMode)
        assertEquals(morgan.roles, switched.roles)
        assertEquals(morgan.deviceId, switched.deviceId)
    }

    @Test
    fun switchMode_toRoleNotHeld_isRefused() {
        assertEquals(priya, switchMode(priya, Role.ADMIN))
    }

    @Test
    fun effectiveMode_validStoredMode_isUsed() {
        assertEquals(Role.ADMIN, effectiveMode(morgan))
    }

    @Test
    fun effectiveMode_staleStoredMode_fallsBackToAHeldRole() {
        val adminRemoved = morgan.copy(roles = listOf(Role.ENTRANT, Role.OWNER))
        assertEquals(Role.ENTRANT, effectiveMode(adminRemoved))
    }

    @Test
    fun effectiveMode_noRoles_isEntrant() {
        assertEquals(Role.ENTRANT, effectiveMode(morgan.copy(roles = emptyList())))
    }
}
