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
import com.cleanify.domain.model.DuplicateGroup
import com.cleanify.domain.model.SimilarGroup
import com.cleanify.domain.repository.ScanScopeType

@Entity(tableName = "scan_result_groups")
data class ScanResultGroupCacheEntry(
    @PrimaryKey val uniqueId: String, // Unique ID for the group (e.g., pHash for similar, signature for exact)
    val groupType: String, // "EXACT" or "SIMILAR"
    @ColumnInfo(name = "unscannable_file_paths") val unscannableFilePaths: List<String> = emptyList(), // Store only for the summary entry
    val pHash: String?, // For SimilarGroup
    val signature: String?, // For DuplicateGroup
    val timestamp: Long, // When this result was saved
    @ColumnInfo(defaultValue = "FULL")
    val scopeType: String // "FULL" or "SCOPED"
)

// Helper functions to convert between domain models and cache entities
fun DuplicateGroup.toCacheEntry(timestamp: Long, scopeType: ScanScopeType): ScanResultGroupCacheEntry {
    return ScanResultGroupCacheEntry(
        uniqueId = this.uniqueId,
        groupType = "EXACT",
        pHash = null,
        signature = this.signature,
        timestamp = timestamp,
        scopeType = scopeType.name
    )
}

fun SimilarGroup.toCacheEntry(timestamp: Long, scopeType: ScanScopeType): ScanResultGroupCacheEntry {
    return ScanResultGroupCacheEntry(
        uniqueId = this.uniqueId,
        groupType = "SIMILAR",
        pHash = this.pHash,
        signature = null,
        timestamp = timestamp,
        scopeType = scopeType.name
    )
}

fun List<String>.toUnscannableFilesCacheEntry(timestamp: Long, scopeType: ScanScopeType): ScanResultGroupCacheEntry {
    return ScanResultGroupCacheEntry(
        uniqueId = "UNSCANNABLE_SUMMARY", // A special unique ID for the summary entry
        groupType = "SUMMARY_UNSCANNABLE",
        unscannableFilePaths = this,
        pHash = null,
        signature = null,
        timestamp = timestamp,
        scopeType = scopeType.name
    )
}
