// Plug-and-play suggested amenity chips component for campsite owners when listing or editing campsites (UI Controller component).
// implements Bonus CampMatch Recommendation Engine
// Outstanding issues: None
// OWNER: Chitkarsh (chitkarshrathi) — Waitlist & Matching
package com.example.trails.matcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.trails.matcher.engine.CampsiteMatcher
import com.example.trails.matcher.model.CamperTagOption
import com.example.trails.matcher.model.CamperTagRegistry
import com.example.trails.ui.theme.Forest
import com.example.trails.ui.theme.Mist
import com.example.trails.ui.theme.Moss
import com.example.trails.ui.theme.NightSurface
import com.example.trails.ui.theme.TrailGreen
import com.example.trails.ui.theme.TrailsTheme

/**
 * Suggested amenity tag chips for campsite owners to standardize tags during campsite registration.
 *
 * NOTE FOR TEAMMATES:
 * - Ibrahim (CampsiteFormScreen): You can embed this component into your `TagsSection` so hosts
 *   can tap popular tags (e.g. Fire pit, Lakeside, Showers) with one click instead of typing:
 *   ```kotlin
 *   OwnerAmenitySuggestions(
 *       currentTags = f.tags,
 *       onAddTag = { tag -> onFieldsChange(f.copy(tags = addTag(f.tags, tag))) }
 *   )
 *   ```
 *
 * @param currentTags list of tags currently attached to the campsite form.
 * @param onAddTag callback triggered when the host taps a suggested amenity to add it.
 * @param modifier optional layout modifier.
 */
@Composable
fun OwnerAmenitySuggestions(
    currentTags: List<String>,
    onAddTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val unaddedSuggestions = CamperTagRegistry.options.filter { option ->
        currentTags.none { CampsiteMatcher.matchesTag(it, option.tag) }
    }

    if (unaddedSuggestions.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("owner_amenity_suggestions_section")
    ) {
        Text(
            text = "Suggested Amenities (Tap to add)",
            style = MaterialTheme.typography.labelSmall,
            color = Mist,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            unaddedSuggestions.forEach { option ->
                SuggestedTagChip(
                    option = option,
                    onClick = { onAddTag(option.tag) }
                )
            }
        }
    }
}

@Composable
private fun SuggestedTagChip(
    option: CamperTagOption,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(NightSurface, RoundedCornerShape(14.dp))
            .border(1.dp, Moss, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("suggested_tag_${option.tag.replace(" ", "_")}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add ${option.tag}",
            tint = TrailGreen,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

@Preview(name = "Owner Amenity Suggestions", showBackground = true, backgroundColor = 0xFF141915)
@Composable
private fun OwnerAmenitySuggestionsPreview() {
    TrailsTheme {
        OwnerAmenitySuggestions(
            currentTags = listOf("Fire pit"),
            onAddTag = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
