// OWNER: Neaz (NeazMZ06) — QR codes
// USER STORIES: 02.02.01, 01.12.01
// PURPOSE: What a campsite QR code holds, reading a scanned code back into a campsite id,
//   and drawing the code as an image.
// Outstanding issues: none.
// AI assistance: used ai to understand and modify lines of codes to tailor to my requirements
package com.example.trails.ui.qr

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Start of every campsite QR code; the campsite id follows it. */
const val CAMPSITE_QR_PREFIX = "trails://campsite/"

/** Deep link pattern for the campsite details route, matching [campsiteQrText]. */
const val CAMPSITE_QR_URI_PATTERN = "trails://campsite/{campsiteId}"

/**
 * Builds the text stored in a campsite's QR code.
 *
 * Campsite ids are unique, so every campsite gets its own code, and the code never
 * changes while the campsite exists.
 *
 * @param campsiteId the campsite's id. Must not be blank.
 * @return the link, e.g. "trails://campsite/campsite-banff-tunnel".
 * @throws IllegalArgumentException if [campsiteId] is blank.
 */
fun campsiteQrText(campsiteId: String): String {
    require(campsiteId.isNotBlank()) { "Campsite id cannot be blank." }
    return CAMPSITE_QR_PREFIX + campsiteId.trim()
}

/**
 * Reads a scanned QR code back into a campsite id.
 *
 * Spaces around the text are ignored and the prefix is not case sensitive. Anything
 * after `?` or `#`, and a trailing `/`, are dropped.
 *
 * @param text the scanned text, or null when nothing was scanned.
 * @return the campsite id, or null when the code is not a Trails campsite code.
 */
fun campsiteIdFromQr(text: String?): String? {
    val trimmed = text?.trim() ?: return null
    if (!trimmed.startsWith(CAMPSITE_QR_PREFIX, ignoreCase = true)) return null
    val id = trimmed.substring(CAMPSITE_QR_PREFIX.length)
        .substringBefore('?')
        .substringBefore('#')
        .trimEnd('/')
    return id.takeIf { it.isNotBlank() && '/' !in it && it.none(Char::isWhitespace) }
}

/**
 * Encodes [text] as a square QR code grid.
 *
 * @param text what the code should hold.
 * @param sizePx width and height of the grid, in pixels.
 * @param margin blank modules around the code; scanners need at least 1.
 * @return the grid, where true is a dark square.
 */
fun qrMatrix(text: String, sizePx: Int, margin: Int = 1): BitMatrix =
    QRCodeWriter().encode(
        text,
        BarcodeFormat.QR_CODE,
        sizePx,
        sizePx,
        mapOf(
            EncodeHintType.MARGIN to margin,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
    )

/**
 * Draws [text] as a black-on-white QR code image.
 *
 * @param text what the code should hold.
 * @param sizePx width and height of the image, in pixels.
 * @param margin blank modules around the code.
 * @return the image.
 */
fun qrBitmap(text: String, sizePx: Int, margin: Int = 1): Bitmap {
    val matrix = qrMatrix(text, sizePx, margin)
    val w = matrix.width
    val h = matrix.height
    val pixels = IntArray(w * h) { i -> if (matrix[i % w, i / w]) QR_DARK else QR_LIGHT }
    return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
}

private const val QR_DARK = 0xFF000000.toInt()
private const val QR_LIGHT = 0xFFFFFFFF.toInt()
