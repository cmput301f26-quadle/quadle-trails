// OWNER: Chitkarsh (chitkarshrathi) — Waitlist
// USER STORIES: 02.11.01
// PURPOSE: Asks an entrant to accept the campsite rules before joining.
// TODO: Show the campsite's rules and record rulesAcceptedAt on accept.
package com.example.trails.ui.waitlist

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun RulesAgreementDialog(onAccept: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rules Agreement") },
        confirmButton = { TextButton(onClick = onAccept) { Text("Accept") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
