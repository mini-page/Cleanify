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

package com.cleanify.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cleanify.data.db.entity.FolderDetailsCache
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDetailsDao {
    @Query("SELECT * FROM folder_details_cache")
    fun getAll(): Flow<List<FolderDetailsCache>>

    @Query("SELECT * FROM folder_details_cache WHERE path = :path")
    suspend fun getFolderByPath(path: String): FolderDetailsCache?

    @Upsert
    suspend fun upsert(folder: FolderDetailsCache)

    @Upsert
    suspend fun upsertAll(folders: List<FolderDetailsCache>)

    @Query("DELETE FROM folder_details_cache WHERE path IN (:paths)")
    suspend fun deleteByPath(paths: List<String>)

    @Query("DELETE FROM folder_details_cache")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(folders: List<FolderDetailsCache>) {
        clear()
        upsertAll(folders)
    }
}
