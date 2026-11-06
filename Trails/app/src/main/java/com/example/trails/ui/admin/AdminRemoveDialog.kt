// OWNER: Zainab (zsaghir) — Admin, Ratings & Policy
// USER STORIES: 03.01.01, 03.02.01
// PURPOSE: The "Remove ...?" confirmation dialog and the muted red Remove button
//   shared by the admin Campsite and Profile pages (Figma Scenario 8), so both look the same.
// Outstanding issues: none.
package com.example.trails.ui.admin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.trails.R
import com.example.trails.ui.theme.Ember

/**
 * Test tags for one use of [AdminRemoveDialog], so each page keeps its own tag names.
 *
 * @property dialog the dialog itself.
 * @property message the body text.
 * @property cancel the Cancel button.
 * @property remove the Remove button.
 */
internal data class AdminRemoveDialogTags(
    val dialog: String,
    val message: String,
    val cancel: String,
    val remove: String
)

/**
 * Confirmation with a trash icon, a title, the [message], an outlined Cancel and a Remove
 * button in the theme's green.
 *
 * @param title e.g. "Remove campsite?".
 * @param message what the removal takes with it.
 * @param removing true while the removal runs; both buttons are disabled and it can't be dismissed.
 * @param tags test tags for the dialog's parts.
 * @param onCancel called when Cancel is tapped or the dialog is dismissed.
 * @param onRemove called when Remove is tapped.
 */
@Composable
internal fun AdminRemoveDialog(
    title: String,
    message: String,
    removing: Boolean,
    tags: AdminRemoveDialogTags,
    onCancel: () -> Unit,
    onRemove: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!removing) onCancel() },
        icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = Ember) },
        title = { Text(title) },
        text = { Text(message, modifier = Modifier.testTag(tags.message)) },
        dismissButton = {
            OutlinedButton(
                onClick = onCancel,
                enabled = !removing,
                modifier = Modifier.testTag(tags.cancel)
            ) {
                Text(stringResource(R.string.admin_remove_cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = onRemove,
                enabled = !removing,
                modifier = Modifier.testTag(tags.remove)
            ) {
                Text(stringResource(R.string.admin_remove_confirm))
            }
        },
        modifier = Modifier.testTag(tags.dialog)
    )
}

/**
 * Full-width muted red button that opens the remove confirmation: a tinted fill with red
 * text, so it reads as destructive without shouting.
 *
 * @param label e.g. "Remove campsite".
 * @param tag test tag for the button.
 * @param onClick called when the button is tapped.
 */
@Composable
internal fun AdminRemoveButton(label: String, tag: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Ember.copy(alpha = 0.15f), contentColor = Ember),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag(tag)
    ) {
        Text(label)
    }
}
