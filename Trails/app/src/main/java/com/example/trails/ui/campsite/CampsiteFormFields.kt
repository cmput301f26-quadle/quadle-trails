// Form logic for registering and editing a campsite (US 02.01.01, 02.01.02 tags, 02.01.03).
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Plain Kotlin with no Android or coroutine types, so it is unit-tested directly.
// Outstanding issues: none.
package com.example.trails.ui.campsite

import com.example.trails.data.model.Campsite

/**
 * What the owner has typed into the campsite form. Numbers are kept as text
 * until saving so half-typed values like "6" or "$" don't get rejected while typing.
 *
 * @property name campsite name.
 * @property locationName human-readable location, e.g. "Hinton, AB".
 * @property description description and amenities.
 * @property maxOccupancy maximum guests, as typed.
 * @property pricePerNight nightly price, as typed; a leading "$" is allowed.
 * @property tags tags in the order added.
 * @property rules house rules.
 * @property cancellationPolicy refund and cancellation terms.
 * @property imageUrl the promo photo: an uploaded URL, a just-picked local URI, or null for none.
 * @property latitude map pin latitude, or null if no pin has been dropped.
 * @property longitude map pin longitude, or null if no pin has been dropped.
 * @property availableDays day keys the owner marked available (see [dayKey]).
 */
data class CampsiteFormFields(
    val name: String = "",
    val locationName: String = "",
    val description: String = "",
    val maxOccupancy: String = "",
    val pricePerNight: String = "",
    val tags: List<String> = emptyList(),
    val rules: String = "",
    val cancellationPolicy: String = "",
    val imageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val availableDays: Set<Long> = emptySet()
) {
    companion object {
        /** Pre-fills the form from an existing campsite, for editing (US 02.01.03). */
        fun from(campsite: Campsite): CampsiteFormFields {
            val hasPin = campsite.latitude != 0.0 || campsite.longitude != 0.0
            return CampsiteFormFields(
                name = campsite.name,
                locationName = campsite.locationName,
                description = campsite.description,
                maxOccupancy = campsite.maxOccupancy.toString(),
                pricePerNight = formatPrice(campsite.pricePerNight),
                tags = campsite.tags,
                rules = campsite.rules,
                cancellationPolicy = campsite.cancellationPolicy,
                imageUrl = campsite.imageUrl,
                latitude = campsite.latitude.takeIf { hasPin },
                longitude = campsite.longitude.takeIf { hasPin },
                availableDays = availabilityToDays(campsite.availability)
            )
        }

        /** "65" for whole dollars, "42.50" otherwise. */
        fun formatPrice(price: Double): String =
            if (price % 1.0 == 0.0) price.toLong().toString() else "%.2f".format(price)
    }
}

/** Outcome of turning [CampsiteFormFields] into a [Campsite]. */
sealed interface CampsiteFormResult {
    /** The form is valid; [campsite] is ready to save. */
    data class Valid(val campsite: Campsite) : CampsiteFormResult

    /** The form has problems; [errors] maps field names to messages. */
    data class Invalid(val errors: Map<String, String>) : CampsiteFormResult
}

/**
 * Builds the campsite to save from the form.
 *
 * When editing, fields the form doesn't show (id, owner,
 * rating, active lottery) are kept from [base].
 *
 * @param base the campsite being edited, or null when registering a new one.
 * @param ownerId owner to assign when registering.
 * @return [CampsiteFormResult.Valid] with the campsite, or [CampsiteFormResult.Invalid] with field errors.
 */
fun CampsiteFormFields.toCampsite(base: Campsite?, ownerId: String): CampsiteFormResult {
    val occupancy = maxOccupancy.trim().toIntOrNull()
    val price = pricePerNight.trim().removePrefix("$").trim().toDoubleOrNull()

    val candidate = (base ?: Campsite(ownerId = ownerId)).copy(
        name = name.trim(),
        locationName = locationName.trim(),
        description = description.trim(),
        maxOccupancy = occupancy ?: 1,
        pricePerNight = price ?: 0.0,
        tags = tags,
        rules = rules.trim(),
        cancellationPolicy = cancellationPolicy.trim(),
        imageUrl = imageUrl,
        latitude = latitude ?: 0.0,
        longitude = longitude ?: 0.0,
        availability = daysToAvailability(availableDays)
    )

    val errors = candidate.validate().toMutableMap()
    if (occupancy == null) {
        errors["maxOccupancy"] =
            if (maxOccupancy.isBlank()) "Max occupancy is required" else "Enter a whole number, e.g. 8"
    }
    if (price == null) {
        errors["pricePerNight"] =
            if (pricePerNight.isBlank()) "Price is required" else "Enter a price, e.g. 65"
    }
    if (availableDays.isEmpty()) errors["availability"] = "Pick at least one available date"
    return if (errors.isEmpty()) CampsiteFormResult.Valid(candidate) else CampsiteFormResult.Invalid(errors)
}

/** True if [url] is a photo picked on this device that still needs uploading. */
fun isLocalImage(url: String): Boolean = url.startsWith("content://") || url.startsWith("file://")

/**
 * Adds a tag after trimming it. Blank tags and duplicates (ignoring case) are ignored.
 *
 * @return the new tag list.
 */
fun addTag(tags: List<String>, raw: String): List<String> {
    val tag = raw.trim()
    if (tag.isEmpty() || tags.any { it.equals(tag, ignoreCase = true) }) return tags
    return tags + tag
}

/** Field names whose value differs between [old] and this, used to clear their errors. */
fun CampsiteFormFields.changedFrom(old: CampsiteFormFields): Set<String> = buildSet {
    if (name != old.name) add("name")
    if (locationName != old.locationName) add("locationName")
    if (description != old.description) add("description")
    if (maxOccupancy != old.maxOccupancy) add("maxOccupancy")
    if (pricePerNight != old.pricePerNight) add("pricePerNight")
    if (rules != old.rules) add("rules")
    if (cancellationPolicy != old.cancellationPolicy) add("cancellationPolicy")
    if (latitude != old.latitude || longitude != old.longitude) add("location")
    if (availableDays != old.availableDays) add("availability")
}
