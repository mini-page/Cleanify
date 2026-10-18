/*
 * Cleanify
 * Copyright (c) 2025 LoopOtto
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.cleanify.data.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

enum class FileCategory {
    Image, Video, Audio, Document, Other
}

@Parcelize
data class MediaItem(
    val id: String,
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val dateAdded: Long,
    val dateModified: Long,
    val size: Long,
    val bucketId: String,
    val bucketName: String,
    val category: FileCategory,
    val width: Int,
    val height: Int,
    val duration: Long = 0
) : Parcelable {
    val isVideo: Boolean
        get() = category == FileCategory.Video || mimeType.startsWith("video/")

    val isImage: Boolean
        get() = category == FileCategory.Image || mimeType.startsWith("image/")

    val extension: String
        get() = displayName.substringAfterLast('.', "").lowercase()

    val isPdf: Boolean
        get() = mimeType == "application/pdf" || extension == "pdf"

    val isAudio: Boolean
        get() = category == FileCategory.Audio || mimeType.startsWith("audio/") || extension in setOf("mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "opus", "mid", "midi")

    val isText: Boolean
        get() = mimeType.startsWith("text/") || extension in setOf("txt", "log", "json", "xml", "html", "css", "js", "ts", "kt", "java", "py", "c", "cpp", "md", "csv", "yaml", "yml", "sh", "bat", "ini", "conf", "rtf", "properties", "gradle", "sql")

    val isDocx: Boolean
        get() = extension in setOf("docx", "doc") || mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" || mimeType == "application/msword"

    val isPptx: Boolean
        get() = extension in setOf("pptx", "ppt") || mimeType == "application/vnd.openxmlformats-officedocument.presentationml.presentation" || mimeType == "application/vnd.ms-powerpoint"

    val isXlsx: Boolean
        get() = extension in setOf("xlsx", "xls") || mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" || mimeType == "application/vnd.ms-excel"

    val isArchive: Boolean
        get() = extension in setOf("zip", "apk", "jar", "tar", "gz", "rar", "7z", "bz2", "xz") || mimeType in setOf("application/zip", "application/vnd.android.package-archive", "application/x-tar", "application/gzip", "application/x-rar-compressed", "application/x-7z-compressed")

    val filePath: String
        get() = when (uri.scheme) {
            "file" -> uri.path ?: ""
            else -> ""
        }

    val file: File
        get() = if (filePath.isNotEmpty()) File(filePath) else File("")
}
