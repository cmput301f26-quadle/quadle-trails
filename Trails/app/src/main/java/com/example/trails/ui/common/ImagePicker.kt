// Shared "pick an image from the device" launcher for campsite and profile photos.
// OWNER: Ibrahim (ibr2309) — Campsite Management (shared image upload)
// USER STORIES: 02.01.02, 01.08.02
// Outstanding issues: none.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.ui.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Remembers the system photo picker (images only; no storage permission needed).
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param onPicked called with the picked image's URI as a string; not called if the user
 *   backs out of the picker.
 * @return call this to open the picker.
 */
@Composable
fun rememberImagePicker(onPicked: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(uri.toString())
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}
