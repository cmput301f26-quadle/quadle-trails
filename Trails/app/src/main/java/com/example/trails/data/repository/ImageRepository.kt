// Shared upload, delete and listing of images in Firebase Storage.
// OWNER: Ibrahim (ibr2309) — Campsite Management (shared image upload)
// USER STORIES: 02.01.02 (campsite promo photo), 01.08.02 (profile photo); listAll is for
//   Zainab's image moderation (03.06.01 / 03.03.01).
// Implemented by FirebaseImageRepository (used by the app) and FakeImageRepository (tests).
// Outstanding issues: none.
// AI assistance: written in assistance with Claude AI by Anthropic.
package com.example.trails.data.repository

/**
 * Where an image is kept. Every upload goes in exactly one folder, so moderation can list
 * all images by walking these folders.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @property path the Storage folder, without a trailing slash.
 */
enum class ImageFolder(val path: String) {
    /** Campsite promo photos (US 02.01.02). Same folder the campsite upload always used. */
    CAMPSITES("campsite-images"),

    /** Profile photos (US 01.08.02). */
    PROFILES("profile-images")
}

/**
 * One uploaded image, as returned by [ImageRepository.listAll].
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @property url the download URL; the same string stored in Campsite.imageUrl or
 *   User.profileImageUrl, so callers can find what uses the image.
 * @property folder the folder the image is in.
 * @property path the full Storage path, e.g. "profile-images/abc.jpg".
 */
data class StoredImage(val url: String, val folder: ImageFolder, val path: String)

/**
 * Uploads, deletes and lists images. Screens and ViewModels depend on this, never on
 * Firebase Storage directly.
 *
 * Written in assistance with Claude AI by Anthropic.
 */
interface ImageRepository {
    /**
     * Uploads an image picked on the device under a new unique name in [folder].
     *
     * @param localUri the picked image, e.g. a "content://" URI as a string.
     * @param folder where to keep it.
     * @return a URL that can be stored on a campsite or profile and shown anywhere.
     */
    suspend fun upload(localUri: String, folder: ImageFolder): String

    /**
     * Deletes the image at [url]. Does nothing if [url] is not one of our uploads (e.g. a
     * sample image or a local URI) or the image is already gone.
     *
     * @param url a URL returned by [upload].
     */
    suspend fun delete(url: String)

    /**
     * Lists every uploaded image in every [ImageFolder], for admin moderation
     * (US 03.06.01 / 03.03.01).
     *
     * @return all stored images.
     */
    suspend fun listAll(): List<StoredImage>
}

/**
 * True if [url] points at Firebase Storage, i.e. it is one of our uploads and can be deleted.
 *
 * Written in assistance with Claude AI by Anthropic.
 *
 * @param url any image URL.
 */
fun isStorageUrl(url: String): Boolean =
    url.startsWith("gs://") || url.startsWith("https://firebasestorage.googleapis.com/")
