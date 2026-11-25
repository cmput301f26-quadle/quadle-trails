// OWNER: Neaz (NeazMZ06) — QR codes
// USER STORIES: 02.02.01
// PURPOSE: "Campsite QR code" card on the owner's campsite page (Figma · Campsite Details - Owner),
//   with Download and Share.
// Outstanding issues: none.
// AI assistance: used ai to understand and modify lines of codes to tailor to my requirements
package com.example.trails.ui.qr

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.trails.R
import com.example.trails.data.fake.TestCampsites
import com.example.trails.ui.theme.TrailsTheme
import kotlinx.coroutines.launch

/** Size of the QR image drawn on the card. */
private const val CARD_QR_SIZE_PX = 256

/**
 * The campsite's QR code with Download and Share.
 *
 * The code is made from the campsite id, so it exists as soon as the campsite is saved
 * and needs nothing stored. Download saves it to Photos; on Android 9 and older it opens
 * the share menu instead.
 *
 * @param campsiteId the campsite's id. Nothing is shown when it is blank.
 * @param campsiteName used in the message sent with a shared code.
 * @param modifier applied to the card.
 */
@Composable
fun CampsiteQrCard(campsiteId: String, campsiteName: String, modifier: Modifier = Modifier) {
    if (campsiteId.isBlank()) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val qrText = remember(campsiteId) { campsiteQrText(campsiteId) }
    val image = remember(qrText) { qrBitmap(qrText, CARD_QR_SIZE_PX).asImageBitmap() }
    val saved = stringResource(R.string.qr_saved)
    val failed = stringResource(R.string.qr_save_failed)
    val shareTitle = stringResource(R.string.qr_share_title)
    val shareText = stringResource(R.string.qr_share_text, campsiteName)

    fun share() = scope.launch {
        try {
            val bitmap = qrBitmap(qrText, QR_EXPORT_SIZE_PX, margin = 4)
            val uri = qrShareUri(context, bitmap, qrFileName(campsiteId))
            context.startActivity(qrShareIntent(uri, shareText, shareTitle))
        } catch (_: Exception) {
            Toast.makeText(context, failed, Toast.LENGTH_SHORT).show()
        }
    }

    CampsiteQrCardContent(
        image = image,
        onDownload = {
            if (!canSaveQrToPhotos) {
                share()
            } else {
                scope.launch {
                    val bitmap = qrBitmap(qrText, QR_EXPORT_SIZE_PX, margin = 4)
                    val ok = saveQrToPhotos(context, bitmap, qrFileName(campsiteId))
                    Toast.makeText(context, if (ok) saved else failed, Toast.LENGTH_SHORT).show()
                }
            }
        },
        onShare = { share() },
        modifier = modifier
    )
}

/**
 * Stateless card, split out so previews and UI tests can drive it directly.
 *
 * @param image the QR code to show.
 * @param onDownload called when Download is tapped.
 * @param onShare called when Share is tapped.
 * @param modifier applied to the card.
 */
@Composable
fun CampsiteQrCardContent(
    image: ImageBitmap,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.primary, shape)
            .padding(12.dp)
            .testTag(CampsiteQrTags.CARD),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // White behind the code so it scans on the dark theme.
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .padding(4.dp)
        ) {
            Image(
                bitmap = image,
                contentDescription = stringResource(R.string.qr_image_desc),
                filterQuality = FilterQuality.None,
                modifier = Modifier
                    .size(56.dp)
                    .testTag(CampsiteQrTags.IMAGE)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.qr_card_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.qr_card_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QrLink(stringResource(R.string.qr_download), onDownload, CampsiteQrTags.DOWNLOAD)
                Text(
                    " · ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                QrLink(stringResource(R.string.qr_share), onShare, CampsiteQrTags.SHARE)
            }
        }
    }
}

/**
 * Small green text link, e.g. "Download".
 *
 * @param text the link text.
 * @param onClick called when it is tapped.
 * @param tag test tag for the link.
 */
@Composable
private fun QrLink(text: String, onClick: () -> Unit, tag: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag(tag)
    )
}

/**
 * Test tags used by UI tests to find parts of the card.
 *
 * @property CARD the whole card.
 * @property IMAGE the QR code image.
 * @property DOWNLOAD the Download link.
 * @property SHARE the Share link.
 */
object CampsiteQrTags {
    const val CARD = "campsite_qr_card"
    const val IMAGE = "campsite_qr_image"
    const val DOWNLOAD = "campsite_qr_download"
    const val SHARE = "campsite_qr_share"
}

/** Design-time preview with the Hinton sample campsite. */
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun CampsiteQrCardPreview() {
    TrailsTheme {
        CampsiteQrCardContent(
            image = qrBitmap(campsiteQrText(TestCampsites.HINTON_ID), CARD_QR_SIZE_PX).asImageBitmap(),
            onDownload = {},
            onShare = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
