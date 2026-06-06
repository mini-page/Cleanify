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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Caches the perceptual hash (pHash) and an optional color histogram
 * for a media file (image or video).
 */
@Entity(tableName = "phash_cache")
data class PHashCache(
    @PrimaryKey
    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long,

    val size: Long,

    @ColumnInfo(name = "p_hash")
    val pHash: String,

    /**
     * This is only generated for images to provide a second factor for similarity checks.
     * It is null for videos.
     */
    val histogram: String?
)
