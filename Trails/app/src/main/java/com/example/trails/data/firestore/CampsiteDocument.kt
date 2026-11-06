// Converts Campsite to and from the fields stored in a Firestore document.
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Why not toObject()/set(campsite)? Firestore fills objects through setters, and the
// shared models use `val` (no setters), so automatic mapping would load blank campsites.
// Doing it by hand keeps the models unchanged and makes the mapping unit-testable.
// Outstanding issues: none.
package com.example.trails.data.firestore

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.DateRange
import com.google.firebase.Timestamp

/**
 * The Firestore fields for [campsite]. The id is not stored; it is the document id.
 *
 * @return field name to value, ready for `DocumentReference.set(...)`.
 */
fun campsiteToDocument(campsite: Campsite): Map<String, Any?> = mapOf(
    "ownerId" to campsite.ownerId,
    "name" to campsite.name,
    "description" to campsite.description,
    "locationName" to campsite.locationName,
    "latitude" to campsite.latitude,
    "longitude" to campsite.longitude,
    "maxOccupancy" to campsite.maxOccupancy,
    "pricePerNight" to campsite.pricePerNight,
    "tags" to campsite.tags,
    "rules" to campsite.rules,
    "cancellationPolicy" to campsite.cancellationPolicy,
    "imageUrl" to campsite.imageUrl,
    "availability" to campsite.availability.map { mapOf("start" to it.start, "end" to it.end) },
    "ratingAverage" to campsite.ratingAverage,
    "ratingCount" to campsite.ratingCount,
    "activeLotteryId" to campsite.activeLotteryId
)

/**
 * Rebuilds a campsite from a document. Missing or wrongly typed fields fall back to the
 * model's defaults, so a half-written or hand-edited document never crashes the app.
 * Numbers are read as [Number] because Firestore returns whole numbers as Long.
 *
 * @param id the document id.
 * @param data the document's fields (DocumentSnapshot.data).
 */
fun campsiteFromDocument(id: String, data: Map<String, Any?>): Campsite {
    val defaults = Campsite()
    return Campsite(
        id = id,
        ownerId = data["ownerId"] as? String ?: defaults.ownerId,
        name = data["name"] as? String ?: defaults.name,
        description = data["description"] as? String ?: defaults.description,
        locationName = data["locationName"] as? String ?: defaults.locationName,
        latitude = (data["latitude"] as? Number)?.toDouble() ?: defaults.latitude,
        longitude = (data["longitude"] as? Number)?.toDouble() ?: defaults.longitude,
        maxOccupancy = (data["maxOccupancy"] as? Number)?.toInt() ?: defaults.maxOccupancy,
        pricePerNight = (data["pricePerNight"] as? Number)?.toDouble() ?: defaults.pricePerNight,
        tags = (data["tags"] as? List<*>)?.filterIsInstance<String>() ?: defaults.tags,
        rules = data["rules"] as? String ?: defaults.rules,
        cancellationPolicy = data["cancellationPolicy"] as? String ?: defaults.cancellationPolicy,
        imageUrl = data["imageUrl"] as? String,
        availability = (data["availability"] as? List<*>)?.mapNotNull { item ->
            (item as? Map<*, *>)?.let { DateRange(it["start"] as? Timestamp, it["end"] as? Timestamp) }
        } ?: defaults.availability,
        ratingAverage = (data["ratingAverage"] as? Number)?.toDouble() ?: defaults.ratingAverage,
        ratingCount = (data["ratingCount"] as? Number)?.toInt() ?: defaults.ratingCount,
        activeLotteryId = data["activeLotteryId"] as? String
    )
}
