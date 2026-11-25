// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.07.01
// PURPOSE: Firestore-backed ReportRepository for the reports collection.
// Outstanding issues:
//  - Needs a live Firestore, so it isn't unit-tested itself; FakeReportRepository and
//    Report.validate() are.
//  - Campers have no screen to file reports (no user story covers it), so reports are added
//    as sample data or from the Firebase console.
//  - Read errors (e.g. PERMISSION_DENIED) are logged and treated as "no reports" so the app
//    doesn't crash; write errors are logged and passed on to the caller.
// AI assistance: Used AI (Claude Code) to help write this file.
package com.example.trails.data.firestore

import android.util.Log
import com.example.trails.data.model.Collections
import com.example.trails.data.model.Report
import com.example.trails.data.model.ReportStatus
import com.example.trails.data.repository.ReportRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Reads and writes [Report]s in the [Collections.REPORTS] Firestore collection.
 *
 * Firebase is only touched on first use, so creating this class under a JVM unit test
 * never starts Firebase.
 *
 * @param db the Firestore instance; defaults to the app's default instance.
 */
class FirestoreReportRepository(
    db: Lazy<FirebaseFirestore> = lazy { FirebaseFirestore.getInstance() }
) : ReportRepository {

    private val reports by lazy { db.value.collection(Collections.REPORTS) }

    override fun observeAll(): Flow<List<Report>> = observe(reports)

    override fun observeForOwner(ownerId: String): Flow<List<Report>> =
        observe(reports.whereEqualTo("ownerId", ownerId))

    override suspend fun save(report: Report) {
        val errors = report.validate()
        require(errors.isEmpty()) { "Invalid report: $errors" }
        val doc = if (report.id.isBlank()) reports.document() else reports.document(report.id)
        try {
            doc.set(report).await()
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't save report ${doc.id}", e)
            throw e
        }
    }

    /** Updates every matching report in one batch, so they all change or none do. */
    override suspend fun setStatusForOwner(ownerId: String, from: ReportStatus, to: ReportStatus) {
        try {
            val snapshot = reports
                .whereEqualTo("ownerId", ownerId)
                .whereEqualTo("status", from.name)
                .get().await()
            if (snapshot.isEmpty) return
            val batch = reports.firestore.batch()
            snapshot.documents.forEach { batch.update(it.reference, "status", to.name) }
            batch.commit().await()
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't move reports for $ownerId from $from to $to", e)
            throw e
        }
    }

    /** Turns a Firestore query into a Flow that re-emits on every change, newest first. */
    private fun observe(query: Query): Flow<List<Report>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Couldn't read reports", error)
                trySend(emptyList())
                close()
                return@addSnapshotListener
            }
            try {
                val list = snapshot?.toObjects(Report::class.java).orEmpty()
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: Long.MIN_VALUE }
                trySend(list)
            } catch (e: Exception) { // a document that doesn't match Report
                Log.w(TAG, "Couldn't read reports", e)
                trySend(emptyList())
            }
        }
        awaitClose { registration.remove() }
    }

    private companion object {
        const val TAG = "FirestoreReportRepo"
    }
}
