package com.cleanify.util

import android.content.Context
import android.webkit.MimeTypeMap
import com.cleanify.data.db.dao.RecycleBinDao
import com.cleanify.data.db.entity.FileCategory
import com.cleanify.data.db.entity.RecycleBinEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecycleBinManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recycleBinDao: RecycleBinDao
) {
    enum class StorageLocation { EXTERNAL, INTERNAL }

    private var storageLocation: StorageLocation = StorageLocation.EXTERNAL

    fun setStorageLocation(location: StorageLocation) {
        storageLocation = location
    }

    private fun getBinDirectory(): File {
        val baseDir = when (storageLocation) {
            StorageLocation.EXTERNAL -> context.getExternalFilesDir("recycle_bin")
            StorageLocation.INTERNAL -> File(context.filesDir, "recycle_bin")
        }
        if (baseDir != null && !baseDir.exists()) {
            baseDir.mkdirs()
        }
        return baseDir ?: File(context.filesDir, "recycle_bin").also { it.mkdirs() }
    }

    private fun getCategoryDirectory(category: FileCategory): File {
        val dir = File(getBinDirectory(), category.name)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun classifyFile(fileName: String, mimeType: String?): FileCategory {
        val mime = mimeType ?: getMimeType(fileName)
        return when {
            mime.startsWith("image/") -> FileCategory.Image
            mime.startsWith("video/") -> FileCategory.Video
            mime.startsWith("audio/") -> FileCategory.Audio
            mime.startsWith("text/") -> FileCategory.Document
            mime == "application/pdf" -> FileCategory.Document
            mime in listOf(
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            ) -> FileCategory.Document
            mime.contains("zip") || mime.contains("rar") || mime.contains("tar") || mime.contains("7z") -> FileCategory.Archive
            else -> FileCategory.Other
        }
    }

    private fun getMimeType(fileName: String): String {
        val ext = MimeTypeMap.getFileExtensionFromUrl(fileName)?.lowercase() ?: return "*/*"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }

    suspend fun moveToTrash(
        filePath: String,
        sourceTool: String = "manual"
    ): RecycleBinEntry? {
        val file = File(filePath)
        if (!file.exists()) return null

        val existing = recycleBinDao.getEntryByOriginalPath(filePath)
        if (existing != null) return existing

        val fileName = file.name
        val mimeType = getMimeType(fileName)
        val category = classifyFile(fileName, mimeType)
        val categoryDir = getCategoryDirectory(category)

        val uniqueName = "${UUID.randomUUID()}_$fileName"
        val recycledFile = File(categoryDir, uniqueName)

        try {
            file.copyTo(recycledFile, overwrite = true)
            if (!file.delete()) return null
        } catch (e: Exception) {
            return null
        }

        val isExternal = storageLocation == StorageLocation.EXTERNAL
        val entry = RecycleBinEntry(
            id = UUID.randomUUID().toString(),
            originalPath = filePath,
            recycledPath = recycledFile.absolutePath,
            fileName = fileName,
            fileSize = recycledFile.length(),
            fileCategory = category,
            mimeType = mimeType,
            deletedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + DEFAULT_RETENTION_DAYS * MILLIS_PER_DAY,
            sourceTool = sourceTool
        )
        recycleBinDao.insertEntry(entry)
        return entry
    }

    suspend fun restoreEntry(entry: RecycleBinEntry): Boolean {
        val recycledFile = File(entry.recycledPath)
        val originalFile = File(entry.originalPath)

        return try {
            originalFile.parentFile?.mkdirs()
            recycledFile.copyTo(originalFile, overwrite = true)
            recycledFile.delete()
            recycleBinDao.deleteEntry(entry)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun permanentlyDeleteEntry(entry: RecycleBinEntry): Boolean {
        return try {
            File(entry.recycledPath).delete()
            recycleBinDao.deleteEntry(entry)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun emptyBin(): Int {
        val entries = recycleBinDao.getAllEntries().first()
        var count = 0
        entries.forEach { entry ->
            try {
                File(entry.recycledPath).delete()
                count++
            } catch (_: Exception) {}
        }
        recycleBinDao.clearAll()
        return count
    }

    suspend fun purgeExpired(): Int {
        val now = System.currentTimeMillis()
        val expired = recycleBinDao.getExpiredEntries(now)
        expired.forEach { entry ->
            try {
                File(entry.recycledPath).delete()
            } catch (_: Exception) {}
        }
        recycleBinDao.deleteExpired(now)
        return expired.size
    }

    suspend fun setRetentionDays(days: Int) {
        val clamped = days.coerceIn(1, 30)
        val entries = recycleBinDao.getAllEntries().first()
        entries.forEach { entry ->
            val updated = entry.copy(expiresAt = entry.deletedAt + clamped * MILLIS_PER_DAY)
            recycleBinDao.insertEntry(updated)
        }
    }

    companion object {
        const val DEFAULT_RETENTION_DAYS = 30
        const val MIN_RETENTION_DAYS = 1
        const val MAX_RETENTION_DAYS = 30
        private const val MILLIS_PER_DAY = 86_400_000L
    }
}
