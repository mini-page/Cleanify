package com.cleanify.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FileCategory(val displayName: String) {
    Image("Images"),
    Video("Videos"),
    Audio("Audio"),
    Document("Documents"),
    Archive("Archives"),
    Other("Other")
}

@Entity(tableName = "recycle_bin")
data class RecycleBinEntry(
    @PrimaryKey
    val id: String,
    val originalPath: String,
    val recycledPath: String,
    val fileName: String,
    val fileSize: Long,
    val fileCategory: FileCategory,
    val mimeType: String,
    val deletedAt: Long,
    val expiresAt: Long,
    val sourceTool: String
)
