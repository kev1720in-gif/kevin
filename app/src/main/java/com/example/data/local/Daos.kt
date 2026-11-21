package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class MediaUriRef(
    val id: Long,
    val sourceUri: String
)

@Dao
interface FolderDao {
    @Query(
        """
        SELECT 
            f.id, f.name, f.bucketId, f.treeUri, f.isPinned, f.createdAtMillis, f.lastBackedUpMillis,
            COUNT(m.id) AS itemCount,
            COALESCE(SUM(m.sizeBytes), 0) AS totalSizeBytes,
            (SELECT m2.localPath FROM media_items m2 
             WHERE m2.folderId = f.id AND m2.status = 'ACTIVE' 
             ORDER BY m2.dateTakenMillis DESC LIMIT 1) AS coverPath
        FROM folders f
        LEFT JOIN media_items m ON m.folderId = f.id AND m.status = 'ACTIVE'
        GROUP BY f.id
        ORDER BY f.isPinned DESC, itemCount DESC, f.createdAtMillis ASC
        """
    )
    fun observeFoldersWithStats(): Flow<List<FolderWithStats>>

    @Query("SELECT * FROM folders ORDER BY isPinned DESC, createdAtMillis ASC")
    suspend fun getAllFoldersOnce(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE id = :folderId LIMIT 1")
    suspend fun getFolderById(folderId: Long): FolderEntity?

    @Query("SELECT * FROM folders WHERE bucketId = :bucketId LIMIT 1")
    suspend fun getFolderByBucketId(bucketId: String): FolderEntity?

    @Query("SELECT * FROM folders WHERE name = :name LIMIT 1")
    suspend fun getFolderByName(name: String): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Query("UPDATE folders SET lastBackedUpMillis = :timestamp")
    suspend fun markAllFoldersBackedUp(timestamp: Long)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: Long)
}

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items WHERE status = 'ACTIVE' ORDER BY dateTakenMillis DESC")
    fun observeActiveMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE status = 'ACTIVE' AND folderId = :folderId ORDER BY dateTakenMillis DESC")
    fun observeMediaByFolder(folderId: Long): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE status = 'HIDDEN' ORDER BY hiddenAtMillis DESC, dateTakenMillis DESC")
    fun observeHiddenMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE status = 'TRASHED' ORDER BY trashedAtMillis DESC")
    fun observeTrashedMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE status != 'TRASHED' ORDER BY dateTakenMillis DESC")
    suspend fun getBackupCandidateMediaOnce(): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE status = 'TRASHED'")
    suspend fun getTrashedMediaOnce(): List<MediaItemEntity>

    @Query("SELECT id, sourceUri FROM media_items WHERE sourceUri LIKE 'content://media/%'")
    suspend fun getMediaStoreRefsOnce(): List<MediaUriRef>

    @Query("SELECT sourceUri FROM media_items WHERE sourceUri IS NOT NULL")
    suspend fun getAllSourceUrisOnce(): List<String>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MediaItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(item: MediaItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(items: List<MediaItemEntity>)

    @Query("UPDATE media_items SET status = :status, trashedAtMillis = :trashedAt, hiddenAtMillis = :hiddenAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: MediaStatus, trashedAt: Long?, hiddenAt: Long?)

    @Query("UPDATE media_items SET status = 'ACTIVE', trashedAtMillis = NULL, hiddenAtMillis = NULL WHERE status = 'TRASHED'")
    suspend fun restoreAllFromTrash()

    @Query("UPDATE media_items SET folderId = :targetFolderId WHERE id = :id")
    suspend fun moveToFolder(id: Long, targetFolderId: Long)

    @Query("UPDATE media_items SET folderId = :fallbackFolderId WHERE folderId = :oldFolderId")
    suspend fun reassignFolder(oldFolderId: Long, fallbackFolderId: Long)

    @Query("UPDATE media_items SET cameraModel = :cameraModel, isoValue = :isoValue, apertureValue = :apertureValue WHERE id = :id")
    suspend fun updateExifDetails(id: Long, cameraModel: String?, isoValue: String?, apertureValue: String?)

    @Query("UPDATE media_items SET isBackedUp = 1, lastBackupMillis = :timestamp WHERE status != 'TRASHED'")
    suspend fun markAllActiveAndHiddenBackedUp(timestamp: Long)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deletePermanently(id: Long)

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM media_items WHERE status = 'TRASHED'")
    suspend fun emptyTrash()
}

@Dao
interface BackupDao {
    @Query("SELECT * FROM backup_records ORDER BY createdAtMillis DESC")
    fun observeBackupRecords(): Flow<List<BackupRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackupRecord(record: BackupRecordEntity): Long
}
