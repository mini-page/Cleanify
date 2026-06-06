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

package com.cleanify.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Caches a calculated signature of a file to avoid re-calculating it on subsequent scans.
 * A signature represents the file's content (e.g., from a partial hash or pixel hash)
 * but is not guaranteed to be a full, byte-for-byte content hash.
 *
 * @param filePath The absolute path to the file, serving as its unique ID.
 * @param lastModified The last modification timestamp of the file when it was hashed.
 * @param size The size of the file in bytes when it was hashed.
 * @param signature The pre-calculated signature of the file.
 */
@Entity(tableName = "file_signature_cache")
data class FileSignatureCache(
    @PrimaryKey val filePath: String,
    val lastModified: Long,
    val size: Long,
    val hash: String
)
