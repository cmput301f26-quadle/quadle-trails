// OWNER: Neaz (NeazMZ06) — QR codes
// USER STORIES: 01.12.01
// PURPOSE: Opens the camera to scan a campsite QR code and reports which campsite it is.
// Outstanding issues: none.
// AI assistance: used ai to understand and modify lines of codes to tailor to my requirements
package com.example.trails.ui.qr

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.example.trails.R
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * What a finished scan means for the app.
 */
sealed interface QrScanOutcome {
    /** The user closed the scanner without scanning anything. */
    data object Cancelled : QrScanOutcome

    /** A code was scanned but it isn't a Trails campsite code. */
    data object NotTrails : QrScanOutcome

    /** A campsite code was scanned. [campsiteId] is the campsite to open. */
    data class Campsite(val campsiteId: String) : QrScanOutcome
}

/**
 * Turns the scanner's result into a [QrScanOutcome].
 *
 * @param contents the scanned text, or null when the scanner was closed.
 * @return the outcome.
 */
fun qrScanOutcome(contents: String?): QrScanOutcome {
    if (contents == null) return QrScanOutcome.Cancelled
    return campsiteIdFromQr(contents)?.let { QrScanOutcome.Campsite(it) } ?: QrScanOutcome.NotTrails
}

/**
 * Remembers a camera scanner for campsite QR codes.
 *
 * The scanner asks for the camera permission itself the first time.
 *
 * @param onCampsite called with the campsite id when a campsite code is scanned.
 * @param onNotTrails called when the scanned code isn't a Trails campsite.
 * @return call it to open the camera.
 */
@Composable
fun rememberCampsiteQrScanner(
    onCampsite: (String) -> Unit,
    onNotTrails: () -> Unit
): () -> Unit {
    val currentOnCampsite by rememberUpdatedState(onCampsite)
    val currentOnNotTrails by rememberUpdatedState(onNotTrails)
    val prompt = stringResource(R.string.qr_scan_prompt)
    val launcher = rememberLauncherForActivityResult(ScanContract()) { result ->
        when (val outcome = qrScanOutcome(result.contents)) {
            QrScanOutcome.Cancelled -> Unit
            QrScanOutcome.NotTrails -> currentOnNotTrails()
            is QrScanOutcome.Campsite -> currentOnCampsite(outcome.campsiteId)
        }
    }
    return {
        launcher.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt(prompt)
                .setBeepEnabled(false)
                .setOrientationLocked(false)
        )
    }
}
