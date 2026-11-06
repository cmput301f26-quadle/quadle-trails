// Read and write access to user profiles (US 01.09.01, 01.07.03, 01.08.01, 03.11.01).
// OWNER: Aarav (aarav-kapoor-cs) — Identity, Profile & Roles
// Outstanding issues: None currently known.
// AI assistance: Used AI to help design profile update, deletion, and role-switching operations.
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
     * Changes which of its roles the profile is acting as (US 03.11.01).
     *
     * @param deviceId the profile to change.
     * @param mode the role to act as; must already be one of the profile's roles.
     * @throws IllegalArgumentException if the profile does not hold [mode].
     * @throws Exception if the profile does not exist or the write fails.
     */
    suspend fun updateActiveMode(deviceId: String, mode: Role)

    /**
     * Removes a profile (US 01.08.01). Removing a profile that does not exist
     * succeeds and changes nothing.
     *
     * @param deviceId the profile to remove.
     * @throws Exception if the delete fails.
     */
    suspend fun delete(deviceId: String)
}
