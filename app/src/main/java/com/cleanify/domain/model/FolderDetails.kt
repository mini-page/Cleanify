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

package com.cleanify.domain.model

/**
 * Represents the detailed information about a media-containing folder on the device.
 * This is the canonical domain model used throughout the application.
 *
 * @param path The absolute file path, which serves as the unique ID.
 * @param name The display name of the folder.
 * @param itemCount The number of media items in the folder that have not yet been processed.
 * @param totalSize The total byte size of the unprocessed media items in the folder.
 * @param isSystemFolder True if the folder is considered a standard system directory (e.g., DCIM, Pictures).
 * @param isPrimarySystemFolder True if the folder is a canonical, top-level system directory (e.g., DCIM/Camera, Pictures/Screenshots), used for sorting priority.
 */
data class FolderDetails(
    val path: String,
    val name: String,
    val itemCount: Int,
    val totalSize: Long,
    val isSystemFolder: Boolean,
    val isPrimarySystemFolder: Boolean = false
)