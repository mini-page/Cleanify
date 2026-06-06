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
 * Caches the aggregated details of a media folder to avoid re-scanning the
 * file system on every app launch.
 *
 * @param path The absolute path to the folder, serving as its unique ID.
 * @param name The display name of the folder.
 * @param itemCount The number of media items within the folder.
 * @param totalSize The total size of all media items in the folder, in bytes.
 * @param isSystemFolder A flag indicating if this is a primary system directory.
 */
@Entity(tableName = "folder_details_cache")
data class FolderDetailsCache(
    @PrimaryKey val path: String,
    val name: String,
    val itemCount: Int,
    val totalSize: Long,
    val isSystemFolder: Boolean
)
