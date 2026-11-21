package com.example.data.media

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
import com.example.data.local.FolderEntity
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaStatus
import com.example.data.local.MediaType
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BackupExportResult(
    val fileName: String,
    val itemCount: Int,
    val totalBytes: Long
)

data class BackupRestoreResult(
    val restoredCount: Int,
    val foldersRestored: Int
)

class MediaFileHelper(private val context: Context) {

    private val mediaRootDir: File by lazy {
        File(context.filesDir, "kimagen_media").apply { if (!exists()) mkdirs() }
    }

    suspend fun importUriToLocalStorage(
        uri: Uri,
        folderId: Long,
        fallbackName: String? = null,
        fallbackModifiedMillis: Long? = null
    ): MediaItemEntity? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val queriedMeta = queryOpenableMetadata(resolver, uri)
        val displayName = (queriedMeta.first ?: fallbackName ?: "archivo_${System.currentTimeMillis()}")
            .ifBlank { "archivo_${System.currentTimeMillis()}" }

        val mimeType = resolver.getType(uri) ?: guessMimeType(displayName)
        val isVideo = mimeType.startsWith("video/", ignoreCase = true)
        val isImage = mimeType.startsWith("image/", ignoreCase = true)
        if (!isVideo && !isImage) return@withContext null

        val extension = displayName.substringAfterLast('.', if (isVideo) "mp4" else "jpg")
        val targetFile = File(mediaRootDir, "${UUID.randomUUID()}.$extension")

        try {
            resolver.openInputStream(uri)?.use { input ->
                BufferedOutputStream(FileOutputStream(targetFile)).use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            } ?: return@withContext null
        } catch (_: Exception) {
            if (targetFile.exists()) targetFile.delete()
            return@withContext null
        }

        val sizeBytes = targetFile.length().takeIf { it > 0L } ?: queriedMeta.second ?: 0L
        val mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE

        return@withContext buildMediaEntity(
            file = targetFile,
            folderId = folderId,
            displayName = displayName,
            sourceUri = uri.toString(),
            mediaType = mediaType,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            fallbackDateMillis = fallbackModifiedMillis ?: System.currentTimeMillis()
        )
    }

    fun collectDocumentFilesInTree(treeUri: Uri): List<Pair<DocumentFile, String>> {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val rootName = rootDoc.name ?: "Carpeta"
        val results = mutableListOf<Pair<DocumentFile, String>>()

        fun walk(dir: DocumentFile, folderName: String, depth: Int) {
            if (depth > 2) return
            dir.listFiles().forEach { child ->
                if (child.isDirectory) {
                    val subName = child.name ?: folderName
                    walk(child, subName, depth + 1)
                } else if (child.isFile) {
                    val mime = child.type ?: guessMimeType(child.name ?: "")
                    if (mime.startsWith("image/") || mime.startsWith("video/")) {
                        results.add(child to folderName)
                    }
                }
            }
        }

        walk(rootDoc, rootName, 0)
        return results
    }

    suspend fun deletePhysicalFile(localPath: String) = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(localPath)
            if (file.exists() && file.absolutePath.startsWith(mediaRootDir.absolutePath)) {
                file.delete()
            }
        }
    }

    suspend fun exportBackupZip(
        destinationUri: Uri,
        folders: List<FolderEntity>,
        mediaItems: List<MediaItemEntity>
    ): BackupExportResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val fileName = queryOpenableMetadata(resolver, destinationUri).first
            ?: "KImagen_Respaldo_${System.currentTimeMillis()}.zip"

        val folderMap = folders.associateBy { it.id }
        val manifestJson = JSONObject()
        manifestJson.put("version", 1)
        manifestJson.put("exportedAtMillis", System.currentTimeMillis())

        val foldersArray = JSONArray()
        folders.forEach { folder ->
            foldersArray.put(
                JSONObject().apply {
                    put("id", folder.id)
                    put("name", folder.name)
                    put("isPinned", folder.isPinned)
                }
            )
        }
        manifestJson.put("folders", foldersArray)

        val itemsArray = JSONArray()
        var exportedCount = 0
        var totalBytes = 0L

        resolver.openOutputStream(destinationUri)?.use { rawOut ->
            ZipOutputStream(BufferedOutputStream(rawOut)).use { zipOut ->
                mediaItems.forEach { item ->
                    val safeName = item.displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    val entryPath = "media/${item.id}_$safeName"

                    val copiedBytes = runCatching {
                        if (item.localPath.startsWith("content://", ignoreCase = true)) {
                            val uri = Uri.parse(item.localPath)
                            resolver.openInputStream(uri)?.use { input ->
                                zipOut.putNextEntry(ZipEntry(entryPath))
                                val bytes = input.copyTo(zipOut, bufferSize = 64 * 1024)
                                zipOut.closeEntry()
                                bytes
                            } ?: 0L
                        } else {
                            val sourceFile = File(item.localPath)
                            if (sourceFile.exists() && sourceFile.isFile) {
                                zipOut.putNextEntry(ZipEntry(entryPath))
                                val bytes = BufferedInputStream(FileInputStream(sourceFile)).use { input ->
                                    input.copyTo(zipOut, bufferSize = 64 * 1024)
                                }
                                zipOut.closeEntry()
                                bytes
                            } else {
                                0L
                            }
                        }
                    }.getOrDefault(0L)

                    if (copiedBytes > 0L) {
                        totalBytes += copiedBytes
                        exportedCount++

                        itemsArray.put(
                            JSONObject().apply {
                                put("entryPath", entryPath)
                                put("folderName", folderMap[item.folderId]?.name ?: "Cámara")
                                put("displayName", item.displayName)
                                put("mediaType", item.mediaType.name)
                                put("mimeType", item.mimeType)
                                put("sizeBytes", if (item.sizeBytes > 0L) item.sizeBytes else copiedBytes)
                                put("width", item.width)
                                put("height", item.height)
                                put("durationMs", item.durationMs)
                                put("dateTakenMillis", item.dateTakenMillis)
                                put("cameraModel", item.cameraModel ?: "")
                                put("isoValue", item.isoValue ?: "")
                                put("apertureValue", item.apertureValue ?: "")
                                put("status", item.status.name)
                            }
                        )
                    }
                }

                manifestJson.put("items", itemsArray)
                zipOut.putNextEntry(ZipEntry("kimagen_manifest.json"))
                zipOut.write(manifestJson.toString(2).toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
            }
        } ?: error("No se pudo abrir el archivo de destino para el respaldo.")

        BackupExportResult(
            fileName = fileName,
            itemCount = exportedCount,
            totalBytes = totalBytes
        )
    }

    suspend fun restoreBackupZip(
        sourceUri: Uri,
        resolveOrCreateFolder: suspend (String) -> Long
    ): Pair<List<MediaItemEntity>, BackupRestoreResult> = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "kimagen_restore_${UUID.randomUUID()}").apply { mkdirs() }
        val extractedEntries = mutableMapOf<String, File>()
        var manifestString: String? = null

        try {
            context.contentResolver.openInputStream(sourceUri)?.use { rawIn ->
                ZipInputStream(BufferedInputStream(rawIn)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            if (entry.name == "kimagen_manifest.json") {
                                manifestString = zipIn.readBytes().toString(Charsets.UTF_8)
                            } else if (entry.name.startsWith("media/")) {
                                val outFile = File(tempDir, entry.name.substringAfterLast('/'))
                                BufferedOutputStream(FileOutputStream(outFile)).use { out ->
                                    zipIn.copyTo(out, bufferSize = 64 * 1024)
                                }
                                extractedEntries[entry.name] = outFile
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            } ?: error("No se pudo leer el archivo de respaldo.")

            val restoredItems = mutableListOf<MediaItemEntity>()
            val touchedFolders = mutableSetOf<String>()

            if (manifestString != null) {
                val root = JSONObject(manifestString!!)
                val items = root.optJSONArray("items") ?: JSONArray()
                for (i in 0 until items.length()) {
                    val obj = items.getJSONObject(i)
                    val entryPath = obj.optString("entryPath")
                    val tempFile = extractedEntries[entryPath] ?: continue
                    val folderName = obj.optString("folderName", "Respaldo").ifBlank { "Respaldo" }
                    val folderId = resolveOrCreateFolder(folderName)
                    touchedFolders.add(folderName)

                    val displayName = obj.optString("displayName", tempFile.name)
                    val ext = displayName.substringAfterLast('.', "jpg")
                    val finalFile = File(mediaRootDir, "${UUID.randomUUID()}.$ext")
                    tempFile.copyTo(finalFile, overwrite = true)

                    val status = runCatching {
                        MediaStatus.valueOf(obj.optString("status", "ACTIVE"))
                    }.getOrDefault(MediaStatus.ACTIVE)

                    val mediaType = runCatching {
                        MediaType.valueOf(obj.optString("mediaType", "IMAGE"))
                    }.getOrDefault(MediaType.IMAGE)

                    restoredItems.add(
                        MediaItemEntity(
                            folderId = folderId,
                            displayName = displayName,
                            localPath = finalFile.absolutePath,
                            mediaType = mediaType,
                            mimeType = obj.optString("mimeType", "image/jpeg"),
                            sizeBytes = finalFile.length(),
                            width = obj.optInt("width", 0),
                            height = obj.optInt("height", 0),
                            durationMs = obj.optLong("durationMs", 0L),
                            dateTakenMillis = obj.optLong("dateTakenMillis", System.currentTimeMillis()),
                            cameraModel = obj.optString("cameraModel").ifBlank { null },
                            isoValue = obj.optString("isoValue").ifBlank { null },
                            apertureValue = obj.optString("apertureValue").ifBlank { null },
                            status = status,
                            hiddenAtMillis = if (status == MediaStatus.HIDDEN) System.currentTimeMillis() else null,
                            isBackedUp = true,
                            lastBackupMillis = System.currentTimeMillis()
                        )
                    )
                }
            } else {
                // Fallback if user picks a generic ZIP with photos/videos
                val fallbackFolderId = resolveOrCreateFolder("Importados ZIP")
                touchedFolders.add("Importados ZIP")
                extractedEntries.values.forEach { file ->
                    val mime = guessMimeType(file.name)
                    if (mime.startsWith("image/") || mime.startsWith("video/")) {
                        val mediaType = if (mime.startsWith("video/")) MediaType.VIDEO else MediaType.IMAGE
                        val ext = file.name.substringAfterLast('.', if (mediaType == MediaType.VIDEO) "mp4" else "jpg")
                        val finalFile = File(mediaRootDir, "${UUID.randomUUID()}.$ext")
                        file.copyTo(finalFile, overwrite = true)
                        restoredItems.add(
                            buildMediaEntity(
                                file = finalFile,
                                folderId = fallbackFolderId,
                                displayName = file.name,
                                sourceUri = null,
                                mediaType = mediaType,
                                mimeType = mime,
                                sizeBytes = finalFile.length(),
                                fallbackDateMillis = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

            restoredItems to BackupRestoreResult(
                restoredCount = restoredItems.size,
                foldersRestored = touchedFolders.size
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun buildMediaEntity(
        file: File,
        folderId: Long,
        displayName: String,
        sourceUri: String?,
        mediaType: MediaType,
        mimeType: String,
        sizeBytes: Long,
        fallbackDateMillis: Long
    ): MediaItemEntity {
        var width = 0
        var height = 0
        var durationMs = 0L
        var dateTakenMillis = fallbackDateMillis
        var cameraModel: String? = null
        var isoValue: String? = null
        var apertureValue: String? = null

        if (mediaType == MediaType.IMAGE) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                width = bounds.outWidth.coerceAtLeast(0)
                height = bounds.outHeight.coerceAtLeast(0)
            }
            runCatching {
                val exif = ExifInterface(file.absolutePath)
                val exifDateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                parseExifDateMillis(exifDateStr)?.let { dateTakenMillis = it }

                val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim().orEmpty()
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty()
                val combinedModel = listOf(make, model).filter { it.isNotBlank() }.distinct().joinToString(" ")
                if (combinedModel.isNotBlank()) cameraModel = combinedModel

                exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { isoValue = "ISO $it" }

                exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { apertureValue = "f/$it" }

                // Swap width/height if rotated 90 or 270
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                    orientation == ExifInterface.ORIENTATION_ROTATE_270
                ) {
                    val temp = width
                    width = height
                    height = temp
                }
            }
        } else {
            val retriever = MediaMetadataRetriever()
            runCatching {
                retriever.setDataSource(file.absolutePath)
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 0
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 0
                val videoDate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                parseVideoDateMillis(videoDate)?.let { dateTakenMillis = it }
            }
            runCatching { retriever.release() }
        }

        return MediaItemEntity(
            folderId = folderId,
            displayName = displayName,
            localPath = file.absolutePath,
            sourceUri = sourceUri,
            mediaType = mediaType,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            width = width,
            height = height,
            durationMs = durationMs,
            dateTakenMillis = dateTakenMillis,
            cameraModel = cameraModel,
            isoValue = isoValue,
            apertureValue = apertureValue,
            status = MediaStatus.ACTIVE
        )
    }

    private fun queryOpenableMetadata(resolver: ContentResolver, uri: Uri): Pair<String?, Long?> {
        var name: String? = null
        var size: Long? = null
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) name = cursor.getString(nameIdx)
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
                    }
                }
        }
        return name to size
    }

    private fun guessMimeType(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return when (ext) {
            "mp4", "m4v" -> "video/mp4"
            "mov" -> "video/quicktime"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "3gp" -> "video/3gpp"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "heic", "heif" -> "image/heic"
            "gif" -> "image/gif"
            else -> "image/jpeg"
        }
    }

    private fun parseExifDateMillis(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(raw)?.time
        }.getOrNull()
    }

    private fun parseVideoDateMillis(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val patterns = listOf("yyyyMMdd'T'HHmmss.SSS'Z'", "yyyyMMdd'T'HHmmss'Z'", "yyyy-MM-dd HH:mm:ss")
        for (pattern in patterns) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(raw)?.time
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }
}
