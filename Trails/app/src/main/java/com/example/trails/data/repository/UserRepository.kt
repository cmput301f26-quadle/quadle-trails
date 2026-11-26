// Read and write access to user profiles, including becoming a host (US 01.09.01, 01.07.03, 01.08.01, 03.11.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: None currently known.
// AI assistance: Used AI to help design profile update, deletion, role-switching and become-a-host operations.
package com.example.trails.data.repository

import com.example.trails.data.model.Role
import com.example.trails.data.model.User
import kotlinx.coroutines.flow.Flow

/** Read and write access to user profiles, keyed by device id. */
interface UserRepository {

    /**
     * Observes every profile.
     *
     * @return a flow that emits the full list whenever any profile changes.
     */
    fun observeAll(): Flow<List<User>>

    /**
     * Observes one profile.
     *
     * @param deviceId the profile's device id.
     * @return a flow that emits the profile, or null when the device has no profile.
     */
    fun observe(deviceId: String): Flow<User?>

    /**
     * Reads one profile once.
     *
     * @param deviceId the profile's device id.
     * @return the profile, or null if none exists.
     */
    suspend fun getById(deviceId: String): User?

    /**
     * Creates or replaces a whole profile. Used to create a new profile (US 01.09.01).
     * Edits to an existing profile go through [updateProfile] or [updateActiveMode],
     * so roles are never rewritten from the client.
     *
     * @param user the profile; its device id is the document id.
     */
    suspend fun save(user: User)

    /**
     * Changes only the name, email and phone of an existing profile (US 01.07.03).
     * Every other field is left as stored.
     *
     * @param deviceId the profile to change.
     * @param name the validated display name.
     * @param email the validated email.
     * @param phone the validated phone, or empty to clear it.
     * @throws Exception if the profile does not exist or the write fails.
     */
    suspend fun updateProfile(deviceId: String, name: String, email: String, phone: String)

    /**
     * Sets or clears only the profile photo of an existing profile (US 01.08.02).
     * Every other field is left as stored.
     *
     * @param deviceId the profile to change.
     * @param url the uploaded photo's URL, or null to remove the photo.
     * @throws Exception if the profile does not exist or the write fails.
     */
    suspend fun updateProfileImage(deviceId: String, url: String?)

    /**
     * Changes which of its roles the profile is acting as (US 03.11.01).
     *
     * @param deviceId the profile to change.
     * @param mode the role to act as; must already be one of the profile's roles.
     * @throws IllegalArgumentException if the profile does not hold [mode].
     * @throws Exception if the profile does not exist or the write fails.
     */
    suspend fun updateActiveMode(deviceId: String, mode: Role)

    /**
     * Lets a camper start hosting (US 03.11.01): adds the OWNER role if the profile
     * does not hold it yet, and switches the profile into Owner mode. This is the only
     * role a user can give themselves; ADMIN is granted by the team in the Firebase console.
     *
     * The default reads the profile and writes it back; Firestore overrides it with a
     * single update so other fields are never rewritten.
     *
     * @param deviceId the profile to change.
     * @throws NoSuchElementException if the profile does not exist.
     * @throws Exception if the write fails.
     */
    suspend fun becomeHost(deviceId: String) {
        val user = getById(deviceId) ?: throw NoSuchElementException("No profile for $deviceId.")
        if (Role.OWNER !in user.roles) save(user.copy(roles = user.roles + Role.OWNER))
        updateActiveMode(deviceId, Role.OWNER)
    }

    /**
     * Removes a profile: by the user themselves (US 01.08.01) or by an admin (US 03.02.01).
     * Removing a profile that does not exist succeeds and changes nothing.
     *
     * @param deviceId the profile to remove.
     * @throws Exception if the delete fails.
     */
    suspend fun delete(deviceId: String)
}