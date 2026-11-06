// OWNER: Neaz (NeazMZ06) — Search & Discovery
// USER STORIES: 01.01.04
// PURPOSE: Reusable campsite summary card for entrants.
// Outstanding issues: none.
package com.example.trails.ui.discovery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.trails.R
import com.example.trails.data.fake.TestCampsites
import com.example.trails.data.model.Campsite
import com.example.trails.ui.campsite.CampsiteFormFields
import com.example.trails.ui.campsite.formatRating
import com.example.trails.ui.theme.TrailsTheme

/**
 * Summary of one campsite: photo, name, location, capacity, price, rating, and amenities.
 *
 * @param campsite the site to show.
 * @param onClick called when the card is tapped.
 * @param modifier applied to the card.
 */
@Composable
fun CampsiteSummaryCard(
    campsite: Campsite,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = modifier
            .fillMaxWidth()
            .testTag(CampsiteSummaryCardTags.card(campsite.id))
    ) {
        Column {
            Banner(campsite.imageUrl)
            Column(Modifier.padding(12.dp)) {
                Text(campsite.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        R.string.discovery_card_location_guests,
                        campsite.locationName,
                        campsite.maxOccupancy
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(
                        R.string.discovery_card_price,
                        CampsiteFormFields.formatPrice(campsite.pricePerNight)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                if (campsite.ratingCount > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(R.string.discovery_card_rating, formatRating(campsite.ratingAverage)),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            pluralStringResource(
                                R.plurals.discovery_rating_count,
                                campsite.ratingCount,
                                campsite.ratingCount
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.discovery_card_no_rating),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (campsite.tags.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        campsite.tags.take(3).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.discovery_lottery_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Wide promo photo, or a plain tile when the campsite has none.
 *
 * @param url the photo address, or null when the campsite has no photo.
 */
@Composable
private fun Banner(url: String?) {
    val mod = Modifier
        .fillMaxWidth()
        .height(160.dp)
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = stringResource(R.string.discovery_photo_desc),
            contentScale = ContentScale.Crop,
            modifier = mod
        )
    } else {
        Box(mod.background(MaterialTheme.colorScheme.surfaceContainer))
    }
}

/** Test tags for the summary card. */
object CampsiteSummaryCardTags {
    /**
     * Tag for one campsite card.
     *
     * @param id the campsite id.
     * @return a tag unique to that card.
     */
    fun card(id: String) = "discovery_card_$id"
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CampsiteSummaryCardRatedPreview() {
    TrailsTheme {
        CampsiteSummaryCard(
            campsite = TestCampsites.all.first { it.id == TestCampsites.BANFF_ID },
            onClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CampsiteSummaryCardUnratedPreview() {
    TrailsTheme {
        CampsiteSummaryCard(
            campsite = TestCampsites.all.first { it.id == TestCampsites.HINTON_ID },
            onClick = {}
        )
    }
}
