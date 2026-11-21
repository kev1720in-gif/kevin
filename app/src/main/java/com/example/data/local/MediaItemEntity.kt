package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["status"]),
        Index(value = ["folderId"]),
        Index(value = ["dateTakenMillis"]),
        Index(value = ["mediaType"]),
        Index(value = ["sourceUri"], unique = true)
    ]
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val folderId: Long,
    val displayName: String,
    val localPath: String,
    val sourceUri: String? = null,
    val mediaType: MediaType,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
    val dateTakenMillis: Long,
    val cameraModel: String? = null,
    val isoValue: String? = null,
    val apertureValue: String? = null,
    val status: MediaStatus = MediaStatus.ACTIVE,
    val trashedAtMillis: Long? = null,
    val hiddenAtMillis: Long? = null,
    val isBackedUp: Boolean = false,
    val lastBackupMillis: Long? = null
)
