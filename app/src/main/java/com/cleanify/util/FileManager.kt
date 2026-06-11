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

package com.cleanify.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

object FileManager {

    /**
     * Moves a file from a source path to a destination folder.
     * This is the NEW way. Beautifully simple.
     * @return The new File object on success, null on failure.
     */
    fun moveFile(sourcePath: String, destinationFolderPath: String): File? {
        return try {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) return null

            val destinationFolder = File(destinationFolderPath)
            destinationFolder.mkdirs()

            val destinationFile = File(destinationFolder, sourceFile.name)

            if (sourceFile.renameTo(destinationFile)) {
                destinationFile
            } else {
                // renameTo fails across mount points (e.g. SD card → internal).
                // Fall back to copy + delete.
                sourceFile.copyTo(destinationFile, overwrite = true)
                if (destinationFile.exists()) {
                    sourceFile.delete()
                    destinationFile
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Creates a new directory (Album).
     */
    fun createAlbum(path: String, name: String): File? {
        return try {
            val newAlbumDir = File(path, name)
            if (newAlbumDir.mkdirs()) {
                newAlbumDir
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Deletes a file.
     * @return true if the file was deleted successfully, false otherwise.
     */
    fun deleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Gets the actual file path from a URI content://... if possible.
     * For files on external storage, this converts to direct file paths.
     */
    fun getFilePathFromUri(uri: android.net.Uri): String? {
        return try {
            val uriString = uri.toString()
            when {
                uriString.startsWith("file://") -> {
                    uri.path
                }

                uriString.startsWith("content://media/external/") -> {
                    // For MediaStore URIs, we'll need to query for the actual path
                    // This is a simplified version - in practice you'd need ContentResolver
                    null
                }

                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Converts a Storage Access Framework tree URI (content://) to a raw file path.
     * Returns null if the path cannot be resolved (e.g., from an SD card or cloud provider).
     * This implementation is robust for primary storage, using the official DocumentsContract API.
     */
    fun getPathFromTreeUri(context: Context, treeUri: Uri): String? {
        val treeDocumentId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (e: IllegalArgumentException) {
            return null
        }

        if (DocumentsContract.isDocumentUri(context, treeUri)) {
            val documentId = DocumentsContract.getDocumentId(treeUri)
            val split = documentId.split(":")
            if (split.size > 1) {
                val type = split[0]
                val path = split[1]

                val resolved = resolveVolumePath(context, type, path)
                if (resolved != null) return resolved
            }
        }

        val split = treeDocumentId.split(":")
        if (split.size > 1) {
            val type = split[0]
            val path = split[1]

            val resolved = resolveVolumePath(context, type, path)
            if (resolved != null) return resolved
        }

        return null
    }

    private fun resolveVolumePath(context: Context, volumeType: String, path: String): String? {
        if ("primary".equals(volumeType, ignoreCase = true)) {
            return "${Environment.getExternalStorageDirectory().absolutePath}/$path"
        }

        // Try to match secondary volumes (SD cards, USB OTG) by their UUID / volume type.
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? android.os.storage.StorageManager
        if (storageManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            for (sv in storageManager.storageVolumes) {
                val dir = sv.directory ?: continue
                @Suppress("DEPRECATION")
                val uuid = sv.getUuid()
                if (uuid != null && uuid.equals(volumeType, ignoreCase = true)) {
                    return "${dir.absolutePath}/$path"
                }
                if (!sv.isPrimary && dir.name.equals(volumeType, ignoreCase = true)) {
                    return "${dir.absolutePath}/$path"
                }
            }
        }

        return null
    }

    /**
     * Checks if a file exists.
     */
    fun fileExists(filePath: String): Boolean {
        return try {
            File(filePath).exists()
        } catch (e: Exception) {
            false
        }
    }
}