// A user profile, keyed by device id, stored in the users collection.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

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
