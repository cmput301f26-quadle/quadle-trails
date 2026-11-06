// UI state and display logic for the owner's campsite page (US 02.01.04).
// OWNER: Ibrahim (ibr2309) — Campsite Management
// Plain Kotlin (no Compose), so it can be unit-tested.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.campsite

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.DateRange
import com.example.trails.data.model.Lottery
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Every situation the owner's campsite page can be in.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
sealed interface OwnerCampsiteUiState {
    /** Waiting for the campsite to load. */
    data object Loading : OwnerCampsiteUiState

    /** No campsite has that id (e.g. it was deleted). */
    data object NotFound : OwnerCampsiteUiState

    /**
     * The campsite is loaded.
     *
     * @property campsite the campsite to show.
     * @property lotteryName name of the lottery it is in, or null if none.
     */
    data class Loaded(val campsite: Campsite, val lotteryName: String?) : OwnerCampsiteUiState
}

/**
 * Picks the page state from the latest campsite and lotteries.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param campsite the campsite, or null if it doesn't exist.
 * @param lotteries all lotteries, used to find the one this campsite is in.
 * @return [OwnerCampsiteUiState.NotFound] or [OwnerCampsiteUiState.Loaded].
 */
fun buildOwnerCampsiteState(campsite: Campsite?, lotteries: List<Lottery>): OwnerCampsiteUiState =
    if (campsite == null) OwnerCampsiteUiState.NotFound
    else OwnerCampsiteUiState.Loaded(campsite, lotteryFor(campsite, lotteries)?.name)

/**
 * The lottery a campsite is in: its active lottery, or any lottery that lists it.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @return the lottery, or null if the campsite isn't in one.
 */
fun lotteryFor(campsite: Campsite, lotteries: List<Lottery>): Lottery? =
    lotteries.find { it.id == campsite.activeLotteryId }
        ?: lotteries.find { campsite.id in it.campsiteIds }

/**
 * Formats a map pin, e.g. "53.89° N, 118.01° W".
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @return the text, or null when no pin has been dropped (both 0.0, the model's default).
 */
fun formatCoordinates(latitude: Double, longitude: Double): String? {
    if (latitude == 0.0 && longitude == 0.0) return null
    val ns = if (latitude >= 0) "N" else "S"
    val ew = if (longitude >= 0) "E" else "W"
    return "%.2f° %s, %.2f° %s".format(Locale.US, abs(latitude), ns, abs(longitude), ew)
}

/**
 * Formats an availability window, e.g. "May 7 – May 9, 2027".
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @return the text, or null if either end is missing.
 */
fun formatDateRange(range: DateRange, locale: Locale = Locale.getDefault()): String? {
    val start = range.start?.toDate() ?: return null
    val end = range.end?.toDate() ?: return null
    val short = SimpleDateFormat("MMM d", locale)
    val full = SimpleDateFormat("MMM d, yyyy", locale)
    return "${short.format(start)} – ${full.format(end)}"
}

/**
 * Average rating to one decimal, e.g. "4.6".
 *
 * Written in assistance with Claude AI by Anthropic.
 */
fun formatRating(average: Double): String = "%.1f".format(Locale.US, average)
