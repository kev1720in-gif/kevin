package com.example.data.media

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaType
import com.example.ui.util.MediaUriHelper
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScannedMediaRow(
    val contentUri: String,
    val displayName: String,
    val mediaType: MediaType,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val dateTakenMillis: Long,
    val bucketId: String,
    val bucketName: String
)

data class ExifCameraDetails(
    val cameraModel: String?,
    val isoValue: String?,
    val apertureValue: String?
)

class MediaStoreScanner(private val context: Context) {

    suspend fun scanAllDeviceMedia(): List<ScannedMediaRow> = withContext(Dispatchers.IO) {
        val results = ArrayList<ScannedMediaRow>(512)
        results.addAll(queryImages())
        results.addAll(queryVideos())
        results.sortByDescending { it.dateTakenMillis }
        results
    }

    private fun queryImages(): List<ScannedMediaRow> {
        val list = mutableListOf<ScannedMediaRow>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        runCatching {
            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                val takenCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                val addedCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                val modifiedCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id).toString()
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "IMG_$id.jpg" else "IMG_$id.jpg"
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "image/jpeg" else "image/jpeg"
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol).coerceAtLeast(0L) else 0L
                    val width = if (widthCol >= 0) cursor.getInt(widthCol).coerceAtLeast(0) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol).coerceAtLeast(0) else 0
                    val taken = if (takenCol >= 0) cursor.getLong(takenCol) else 0L
                    val added = if (addedCol >= 0) cursor.getLong(addedCol) * 1000L else 0L
                    val modified = if (modifiedCol >= 0) cursor.getLong(modifiedCol) * 1000L else 0L
                    val dateMillis = when {
                        taken > 0L -> taken
                        modified > 0L -> modified
                        added > 0L -> added
                        else -> System.currentTimeMillis()
                    }
                    val bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) ?: "default" else "default"
                    val bucketName = if (bucketNameCol >= 0) {
                        cursor.getString(bucketNameCol)?.takeIf { it.isNotBlank() } ?: "Teléfono"
                    } else {
                        "Teléfono"
                    }

                    list.add(
                        ScannedMediaRow(
                            contentUri = uri,
                            displayName = name,
                            mediaType = MediaType.IMAGE,
                            mimeType = mime,
                            sizeBytes = size,
                            width = width,
                            height = height,
                            durationMs = 0L,
                            dateTakenMillis = dateMillis,
                            bucketId = bucketId,
                            bucketName = bucketName
                        )
                    )
                }
            }
        }
        return list
    }

    private fun queryVideos(): List<ScannedMediaRow> {
        val list = mutableListOf<ScannedMediaRow>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        runCatching {
            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val takenCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN)
                val addedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val modifiedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id).toString()
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "VID_$id.mp4" else "VID_$id.mp4"
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "video/mp4" else "video/mp4"
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol).coerceAtLeast(0L) else 0L
                    val width = if (widthCol >= 0) cursor.getInt(widthCol).coerceAtLeast(0) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol).coerceAtLeast(0) else 0
                    val duration = if (durCol >= 0) cursor.getLong(durCol).coerceAtLeast(0L) else 0L
                    val taken = if (takenCol >= 0) cursor.getLong(takenCol) else 0L
                    val added = if (addedCol >= 0) cursor.getLong(addedCol) * 1000L else 0L
                    val modified = if (modifiedCol >= 0) cursor.getLong(modifiedCol) * 1000L else 0L
                    val dateMillis = when {
                        taken > 0L -> taken
                        modified > 0L -> modified
                        added > 0L -> added
                        else -> System.currentTimeMillis()
                    }
                    val bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) ?: "default" else "default"
                    val bucketName = if (bucketNameCol >= 0) {
                        cursor.getString(bucketNameCol)?.takeIf { it.isNotBlank() } ?: "Videos"
                    } else {
                        "Videos"
                    }

                    list.add(
                        ScannedMediaRow(
                            contentUri = uri,
                            displayName = name,
                            mediaType = MediaType.VIDEO,
                            mimeType = mime,
                            sizeBytes = size,
                            width = width,
                            height = height,
                            durationMs = duration,
                            dateTakenMillis = dateMillis,
                            bucketId = bucketId,
                            bucketName = bucketName
                        )
                    )
                }
            }
        }
        return list
    }

    suspend fun readLazyExifDetails(item: MediaItemEntity): ExifCameraDetails? = withContext(Dispatchers.IO) {
        if (item.mediaType != MediaType.IMAGE) return@withContext null
        return@withContext runCatching {
            val exif = if (MediaUriHelper.isContentUri(item.localPath)) {
                context.contentResolver.openInputStream(MediaUriHelper.toAndroidUri(item.localPath))?.use {
                    ExifInterface(it)
                }
            } else {
                val f = File(item.localPath)
                if (f.exists()) ExifInterface(f.absolutePath) else null
            } ?: return@runCatching null

            val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim().orEmpty()
            val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty()
            val combinedModel = listOf(make, model).filter { it.isNotBlank() }.distinct().joinToString(" ")
                .ifBlank { null }
            val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                ?.takeIf { it.isNotBlank() }
                ?.let { "ISO $it" }
            val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                ?.takeIf { it.isNotBlank() }
                ?.let { "f/$it" }

            if (combinedModel == null && iso == null && aperture == null) null
            else ExifCameraDetails(combinedModel, iso, aperture)
        }.getOrNull()
    }
}
