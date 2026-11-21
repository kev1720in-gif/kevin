package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_records")
data class BackupRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val fileName: String,
    val destinationUri: String,
    val itemCount: Int,
    val totalBytes: Long,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val statusLabel: String = "Completado"
)
