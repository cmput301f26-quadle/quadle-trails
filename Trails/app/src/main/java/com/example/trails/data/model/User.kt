// A user profile, keyed by device id, stored in the users collection.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * A user's profile, identified by their device.
 *
 * @property deviceId Device id; also the Firestore document id.
 * @property name Display name.
 * @property email Contact email.
 * @property phone Optional contact phone number.
 * @property profileImageUrl URL of the profile photo, or null if none.
 * @property roles Roles the user holds.
 * @property activeMode Role the UI is currently acting as.
 * @property notificationsOptOut True if the user has opted out of notifications.
 * @property createdAt When the profile was created.
 */
data class User(
    @DocumentId val deviceId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImageUrl: String? = null,
    val roles: List<Role> = listOf(Role.ENTRANT),
    val activeMode: Role = Role.ENTRANT,
    val notificationsOptOut: Boolean = false,
    val createdAt: Timestamp? = null
)
