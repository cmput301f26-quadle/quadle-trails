package com.example.trails.data.firestore

import com.example.trails.data.model.Collections
import com.example.trails.data.model.Notification
import com.example.trails.data.model.NotificationType
import com.example.trails.data.model.Reservation
import com.example.trails.data.repository.NotificationRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreNotificationRepository(
    db: Lazy<FirebaseFirestore> = lazy { FirebaseFirestore.getInstance()}
) : NotificationRepository {

    private val firestore by db

    override fun getNotif(userId: String): List<Notification> {
        var reserveList: List<Notification> = listOf()
        firestore
            .collection(Collections.NOTIFICATIONS)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                reserveList = documents.toObjects(Notification::class.java)
                return@addOnSuccessListener
            }
        return reserveList
    }

    override fun sendNotif(
        userId: String,
        title: String,
        content: String,
        type: NotificationType,
        related: Reservation?
    ) {
        val notification = Notification(
            recipient = userId,
            title = title,
            content = content,
            type = type,
            related = related
        )
        firestore.collection(Collections.NOTIFICATIONS).add(notification)
    }

    override suspend fun checkNotif(
        related: Reservation
    ): Boolean {
        return firestore.collection(Collections.NOTIFICATIONS)
            .whereEqualTo("related", related)
            .limit(1)
            .get()
            .await().isEmpty
    }
}