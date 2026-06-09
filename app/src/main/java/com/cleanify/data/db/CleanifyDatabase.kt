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

package com.cleanify.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cleanify.data.db.converter.Converters
import com.cleanify.data.db.dao.FileSignatureDao
import com.cleanify.data.db.dao.FolderDetailsDao
import com.cleanify.data.db.dao.PHashDao
import com.cleanify.data.db.dao.RecycleBinDao
import com.cleanify.data.db.dao.ScanResultCacheDao
import com.cleanify.data.db.dao.SimilarityDenialDao
import com.cleanify.data.db.dao.UnreadableFileCacheDao
import com.cleanify.data.db.entity.FileSignatureCache
import com.cleanify.data.db.entity.FolderDetailsCache
import com.cleanify.data.db.entity.MediaItemRefCacheEntry
import com.cleanify.data.db.entity.PHashCache
import com.cleanify.data.db.entity.RecycleBinEntry
import com.cleanify.data.db.entity.ScanResultGroupCacheEntry
import com.cleanify.data.db.entity.SimilarityDenial
import com.cleanify.data.db.entity.UnreadableFileCache

@Database(
    entities = [
        FileSignatureCache::class,
        FolderDetailsCache::class,
        PHashCache::class,
        ScanResultGroupCacheEntry::class,
        MediaItemRefCacheEntry::class,
        UnreadableFileCache::class,
        SimilarityDenial::class,
        RecycleBinEntry::class
    ],
    version = 4,
    autoMigrations = [],
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class CleanifyDatabase : RoomDatabase() {
    abstract fun fileSignatureDao(): FileSignatureDao
    abstract fun folderDetailsDao(): FolderDetailsDao
    abstract fun pHashDao(): PHashDao
    abstract fun scanResultCacheDao(): ScanResultCacheDao
    abstract fun unreadableFileCacheDao(): UnreadableFileCacheDao
    abstract fun similarityDenialDao(): SimilarityDenialDao
    abstract fun recycleBinDao(): RecycleBinDao

    companion object {
        const val DATABASE_NAME = "Cleanify_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scan_result_groups ADD COLUMN scopeType TEXT NOT NULL DEFAULT 'FULL'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS similarity_denials (
                        pairKey TEXT NOT NULL,
                        pathA TEXT NOT NULL,
                        pathB TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        PRIMARY KEY(pairKey)
                    )
                """.trimIndent())
                // Purge the old similar_groups table if it exists to reclaim space
                db.execSQL("DROP TABLE IF EXISTS similar_groups")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS recycle_bin (
                        id TEXT NOT NULL PRIMARY KEY,
                        originalPath TEXT NOT NULL,
                        recycledPath TEXT NOT NULL,
                        fileName TEXT NOT NULL,
                        fileSize INTEGER NOT NULL,
                        fileCategory TEXT NOT NULL,
                        mimeType TEXT NOT NULL,
                        deletedAt INTEGER NOT NULL,
                        expiresAt INTEGER NOT NULL,
                        sourceTool TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
