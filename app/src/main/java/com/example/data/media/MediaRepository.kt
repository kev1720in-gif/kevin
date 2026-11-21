package com.example.data.media

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.local.BackupDao
import com.example.data.local.BackupRecordEntity
import com.example.data.local.FolderDao
import com.example.data.local.FolderEntity
import com.example.data.local.FolderWithStats
import com.example.data.local.MediaDao
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class PermanentDeletePlan(
    val deletedImmediatelyCount: Int,
    val pendingItemIds: List<Long>,
    val pendingIntentSender: IntentSender?
)

class MediaRepository(
    private val folderDao: FolderDao,
    private val mediaDao: MediaDao,
    private val backupDao: BackupDao,
    private val fileHelper: MediaFileHelper,
    private val mediaStoreScanner: MediaStoreScanner? = null,
    private val contentResolver: ContentResolver? = null
) {
    val activeMedia: Flow<List<MediaItemEntity>> = mediaDao.observeActiveMedia()
    val foldersWithStats: Flow<List<FolderWithStats>> = folderDao.observeFoldersWithStats()
    val hiddenMedia: Flow<List<MediaItemEntity>> = mediaDao.observeHiddenMedia()
    val trashedMedia: Flow<List<MediaItemEntity>> = mediaDao.observeTrashedMedia()
    val backupRecords: Flow<List<BackupRecordEntity>> = backupDao.observeBackupRecords()

    fun observeFolderMedia(folderId: Long): Flow<List<MediaItemEntity>> =
        mediaDao.observeMediaByFolder(folderId)

    suspend fun ensureDefaultFoldersExist(): Long {
        val existing = folderDao.getAllFoldersOnce()
        if (existing.isNotEmpty()) {
            return existing.first().id
        }
        return folderDao.insertFolder(
            FolderEntity(name = "Cámara", isPinned = true)
        )
    }

    suspend fun syncDeviceMedia(): Int = withContext(Dispatchers.IO) {
        val scanner = mediaStoreScanner ?: return@withContext 0
        val scannedRows = scanner.scanAllDeviceMedia()

        val existingFolders = folderDao.getAllFoldersOnce()
        val folderByBucket = HashMap<String, Long>()
        val folderByName = HashMap<String, Long>()
        existingFolders.forEach { folder ->
            folder.bucketId?.let { folderByBucket[it] = folder.id }
            folderByName[folder.name.lowercase()] = folder.id
        }

        val existingUris = mediaDao.getAllSourceUrisOnce().toHashSet()
        val scannedUriSet = HashSet<String>(scannedRows.size)
        val newItems = ArrayList<MediaItemEntity>(scannedRows.size)

        for (row in scannedRows) {
            scannedUriSet.add(row.contentUri)
            if (row.contentUri in existingUris) continue

            val folderId = folderByBucket[row.bucketId]
                ?: folderByName[row.bucketName.lowercase()]?.also { existingId ->
                    folderByBucket[row.bucketId] = existingId
                }
                ?: run {
                    val isCamera = row.bucketName.equals("Camera", ignoreCase = true) ||
                        row.bucketName.equals("Cámara", ignoreCase = true)
                    val newId = folderDao.insertFolder(
                        FolderEntity(
                            name = row.bucketName,
                            bucketId = row.bucketId,
                            isPinned = isCamera
                        )
                    )
                    folderByBucket[row.bucketId] = newId
                    folderByName[row.bucketName.lowercase()] = newId
                    newId
                }

            newItems.add(
                MediaItemEntity(
                    folderId = folderId,
                    displayName = row.displayName,
                    localPath = row.contentUri,
                    sourceUri = row.contentUri,
                    mediaType = row.mediaType,
                    mimeType = row.mimeType,
                    sizeBytes = row.sizeBytes,
                    width = row.width,
                    height = row.height,
                    durationMs = row.durationMs,
                    dateTakenMillis = row.dateTakenMillis,
                    status = MediaStatus.ACTIVE
                )
            )
        }

        if (newItems.isNotEmpty()) {
            newItems.chunked(500).forEach { chunk ->
                mediaDao.insertAllIgnore(chunk)
            }
        }

        val currentRefs = mediaDao.getMediaStoreRefsOnce()
        val staleIds = currentRefs.filter { it.sourceUri !in scannedUriSet }.map { it.id }
        if (staleIds.isNotEmpty()) {
            staleIds.chunked(500).forEach { chunk ->
                mediaDao.deleteByIds(chunk)
            }
        }

        newItems.size
    }

    suspend fun enrichExifIfNeeded(item: MediaItemEntity): MediaItemEntity {
        if (!item.cameraModel.isNullOrBlank() || mediaStoreScanner == null) return item
        val details = mediaStoreScanner.readLazyExifDetails(item) ?: return item
        mediaDao.updateExifDetails(
            id = item.id,
            cameraModel = details.cameraModel,
            isoValue = details.isoValue,
            apertureValue = details.apertureValue
        )
        return item.copy(
            cameraModel = details.cameraModel,
            isoValue = details.isoValue,
            apertureValue = details.apertureValue
        )
    }

    suspend fun createFolder(name: String, treeUri: String? = null): Long {
        val cleanName = name.trim().ifBlank { "Álbum Nuevo" }
        val existing = folderDao.getFolderByName(cleanName)
        if (existing != null) return existing.id
        return folderDao.insertFolder(
            FolderEntity(name = cleanName, treeUri = treeUri)
        )
    }

    suspend fun deleteFolder(folderId: Long) {
        val fallbackId = ensureDefaultFoldersExist()
        if (folderId == fallbackId) return
        mediaDao.reassignFolder(oldFolderId = folderId, fallbackFolderId = fallbackId)
        folderDao.deleteFolderById(folderId)
    }

    suspend fun importPickedUris(uris: List<Uri>, targetFolderId: Long?): Int {
        if (uris.isEmpty()) return 0
        val folderId = targetFolderId ?: ensureDefaultFoldersExist()
        val imported = mutableListOf<MediaItemEntity>()
        for (uri in uris) {
            fileHelper.importUriToLocalStorage(uri = uri, folderId = folderId)?.let {
                imported.add(it)
            }
        }
        if (imported.isNotEmpty()) {
            mediaDao.insertAllIgnore(imported)
        }
        return imported.size
    }

    suspend fun importSafFolderTree(treeUri: Uri): Pair<Int, String> = withContext(Dispatchers.IO) {
        val collected = fileHelper.collectDocumentFilesInTree(treeUri)
        if (collected.isEmpty()) return@withContext 0 to ""

        val rootFolderName = collected.first().second
        val folderCache = mutableMapOf<String, Long>()
        val existingUris = mediaDao.getAllSourceUrisOnce().toHashSet()
        val imported = mutableListOf<MediaItemEntity>()

        for ((docFile, folderName) in collected) {
            val uriStr = docFile.uri.toString()
            if (uriStr in existingUris) continue

            val folderId = folderCache.getOrPut(folderName) {
                val existing = folderDao.getFolderByName(folderName)
                existing?.id ?: folderDao.insertFolder(
                    FolderEntity(name = folderName, treeUri = treeUri.toString())
                )
            }
            val item = fileHelper.importUriToLocalStorage(
                uri = docFile.uri,
                folderId = folderId,
                fallbackName = docFile.name,
                fallbackModifiedMillis = docFile.lastModified().takeIf { it > 0L }
            )
            if (item != null) imported.add(item)
        }

        if (imported.isNotEmpty()) {
            imported.chunked(200).forEach { mediaDao.insertAllIgnore(it) }
        }
        imported.size to rootFolderName
    }

    suspend fun moveToTrash(id: Long) {
        mediaDao.updateStatus(
            id = id,
            status = MediaStatus.TRASHED,
            trashedAt = System.currentTimeMillis(),
            hiddenAt = null
        )
    }

    suspend fun hideInVault(id: Long) {
        mediaDao.updateStatus(
            id = id,
            status = MediaStatus.HIDDEN,
            trashedAt = null,
            hiddenAt = System.currentTimeMillis()
        )
    }

    suspend fun restoreToActive(id: Long) {
        mediaDao.updateStatus(
            id = id,
            status = MediaStatus.ACTIVE,
            trashedAt = null,
            hiddenAt = null
        )
    }

    suspend fun restoreAllFromTrash() {
        mediaDao.restoreAllFromTrash()
    }

    suspend fun moveItemToFolder(id: Long, targetFolderId: Long) {
        mediaDao.moveToFolder(id, targetFolderId)
    }

    suspend fun requestPermanentDeleteSingle(item: MediaItemEntity): PermanentDeletePlan =
        requestPermanentDeleteBatch(listOf(item))

    suspend fun requestEmptyTrash(): PermanentDeletePlan =
        requestPermanentDeleteBatch(mediaDao.getTrashedMediaOnce())

    private suspend fun requestPermanentDeleteBatch(
        items: List<MediaItemEntity>
    ): PermanentDeletePlan = withContext(Dispatchers.IO) {
        if (items.isEmpty()) {
            return@withContext PermanentDeletePlan(0, emptyList(), null)
        }

        val resolver = contentResolver
        val immediateRemoveIds = mutableListOf<Long>()
        val existingMediaStoreItems = mutableListOf<Pair<MediaItemEntity, Uri>>()

        for (item in items) {
            if (!item.localPath.startsWith("content://media/", ignoreCase = true)) {
                fileHelper.deletePhysicalFile(item.localPath)
                immediateRemoveIds.add(item.id)
            } else if (resolver != null) {
                val uri = Uri.parse(item.localPath)
                if (mediaStoreUriExists(resolver, uri)) {
                    existingMediaStoreItems.add(item to uri)
                } else {
                    // Already gone from device MediaStore
                    immediateRemoveIds.add(item.id)
                }
            } else {
                immediateRemoveIds.add(item.id)
            }
        }

        if (immediateRemoveIds.isNotEmpty()) {
            immediateRemoveIds.chunked(500).forEach { mediaDao.deleteByIds(it) }
        }

        if (existingMediaStoreItems.isEmpty() || resolver == null) {
            return@withContext PermanentDeletePlan(immediateRemoveIds.size, emptyList(), null)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uris = existingMediaStoreItems.map { it.second }
            val pendingIntent = runCatching {
                MediaStore.createDeleteRequest(resolver, uris)
            }.getOrNull()

            if (pendingIntent != null) {
                return@withContext PermanentDeletePlan(
                    deletedImmediatelyCount = immediateRemoveIds.size,
                    pendingItemIds = existingMediaStoreItems.map { it.first.id },
                    pendingIntentSender = pendingIntent.intentSender
                )
            }
        }

        // Android 10 (Q) or Android 9 and below (<= P)
        var deletedDirectCount = immediateRemoveIds.size
        val directDeletedIds = mutableListOf<Long>()
        var recoverableSender: IntentSender? = null
        val recoverableIds = mutableListOf<Long>()

        for ((item, uri) in existingMediaStoreItems) {
            try {
                val rows = resolver.delete(uri, null, null)
                if (rows > 0 || !mediaStoreUriExists(resolver, uri)) {
                    directDeletedIds.add(item.id)
                    deletedDirectCount++
                }
            } catch (sec: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && sec is RecoverableSecurityException) {
                    if (recoverableSender == null) {
                        recoverableSender = sec.userAction.actionIntent.intentSender
                        recoverableIds.add(item.id)
                    }
                }
            }
        }

        if (directDeletedIds.isNotEmpty()) {
            directDeletedIds.chunked(500).forEach { mediaDao.deleteByIds(it) }
        }

        PermanentDeletePlan(
            deletedImmediatelyCount = deletedDirectCount,
            pendingItemIds = recoverableIds,
            pendingIntentSender = recoverableSender
        )
    }

    suspend fun finalizePermanentDelete(ids: List<Long>) {
        if (ids.isEmpty()) return
        ids.chunked(500).forEach { mediaDao.deleteByIds(it) }
    }

    private fun mediaStoreUriExists(resolver: ContentResolver, uri: Uri): Boolean {
        return runCatching {
            resolver.query(uri, arrayOf(MediaStore.MediaColumns._ID), null, null, null)?.use { cursor ->
                cursor.moveToFirst()
            } ?: false
        }.getOrDefault(false)
    }

    suspend fun exportBackup(destinationUri: Uri): BackupExportResult {
        val folders = folderDao.getAllFoldersOnce()
        val items = mediaDao.getBackupCandidateMediaOnce()
        val result = fileHelper.exportBackupZip(destinationUri, folders, items)
        val now = System.currentTimeMillis()
        mediaDao.markAllActiveAndHiddenBackedUp(now)
        folderDao.markAllFoldersBackedUp(now)
        backupDao.insertBackupRecord(
            BackupRecordEntity(
                fileName = result.fileName,
                destinationUri = destinationUri.toString(),
                itemCount = result.itemCount,
                totalBytes = result.totalBytes,
                createdAtMillis = now
            )
        )
        return result
    }

    suspend fun restoreBackup(sourceUri: Uri): BackupRestoreResult {
        val (items, result) = fileHelper.restoreBackupZip(sourceUri) { folderName ->
            createFolder(folderName)
        }
        if (items.isNotEmpty()) {
            items.chunked(200).forEach { mediaDao.insertAllIgnore(it) }
        }
        return result
    }
}
