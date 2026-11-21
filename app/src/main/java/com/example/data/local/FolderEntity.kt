package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    indices = [Index(value = ["bucketId"])]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val bucketId: String? = null,
    val treeUri: String? = null,
    val isPinned: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastBackedUpMillis: Long? = null
)

data class FolderWithStats(
    val id: Long,
    val name: String,
    val bucketId: String?,
    val treeUri: String?,
    val isPinned: Boolean,
    val createdAtMillis: Long,
    val lastBackedUpMillis: Long?,
    val itemCount: Int,
    val totalSizeBytes: Long,
    val coverPath: String?
)
