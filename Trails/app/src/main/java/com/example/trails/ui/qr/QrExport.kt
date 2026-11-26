// OWNER: Neaz (NeazMZ06) — QR codes
// USER STORIES: 02.02.01
// PURPOSE: Saves a campsite QR image to Photos (Download) and hands it to other apps (Share).
// Outstanding issues: Android 9 and older can't save to Photos without a storage permission,
//   so Download opens the share menu there instead.
// AI assistance: used ai to understand and modify lines of codes to tailor to my requirements
package com.example.trails.ui.qr

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** File provider for QR images in the app's cache, declared in the manifest. */
class QrFileProvider : FileProvider()

private const val TAG = "QrExport"

/** Size of a downloaded or shared QR image, big enough to print. */
const val QR_EXPORT_SIZE_PX = 1024

/**
 * File name for a campsite's QR image.
 *
 * @param campsiteId the campsite's id.
 * @return e.g. "trails-qr-campsite-banff-tunnel.png", with unsafe characters replaced.
 */
fun qrFileName(campsiteId: String): String =
    "trails-qr-" + campsiteId.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".png"

/** True when [saveQrToPhotos] can work without asking for a storage permission. */
val canSaveQrToPhotos: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

/**
 * Saves the image to Photos, in Pictures/Trails.
 *
 * @param context any context.
 * @param bitmap the QR image.
 * @param fileName the file name to save under.
 * @return true when the image was saved. Always false before Android 10.
 */
suspend fun saveQrToPhotos(context: Context, bitmap: Bitmap, fileName: String): Boolean =
    withContext(Dispatchers.IO) {
        if (!canSaveQrToPhotos) return@withContext false
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Trails")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext false
        try {
            val written = resolver.openOutputStream(uri)?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            } ?: false
            if (!written) {
                resolver.delete(uri, null, null)
                return@withContext false
            }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't save QR image", e)
            resolver.delete(uri, null, null)
            false
        }
    }

/**
 * Writes the image to the app's cache so it can be shared.
 *
 * @param context any context.
 * @param bitmap the QR image.
 * @param fileName the file name to write.
 * @return a content URI other apps can read once they are granted access.
 */
suspend fun qrShareUri(context: Context, bitmap: Bitmap, fileName: String): Uri =
    withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "qr").apply { mkdirs() }
        val file = File(dir, fileName)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.qrfiles", file)
    }

/**
 * Builds the Android share menu for a QR image.
 *
 * @param uri the image, from [qrShareUri].
 * @param text message sent with the image.
 * @param title title of the share menu.
 * @return the chooser intent to start.
 */
fun qrShareIntent(uri: Uri, text: String, title: String): Intent {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, text)
        // ClipData carries the read grant through the chooser to the app the user picks.
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(send, title)
}
