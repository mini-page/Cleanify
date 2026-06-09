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

package com.cleanify.di

import android.content.Context
import androidx.room.Room
import com.cleanify.data.db.CleanifyDatabase
import com.cleanify.data.db.dao.FileSignatureDao
import com.cleanify.data.db.dao.FolderDetailsDao
import com.cleanify.data.db.dao.PHashDao
import com.cleanify.data.db.dao.RecycleBinDao
import com.cleanify.data.db.dao.ScanResultCacheDao
import com.cleanify.data.db.dao.SimilarityDenialDao
import com.cleanify.data.db.dao.UnreadableFileCacheDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CleanifyDatabase {
        return Room.databaseBuilder(
            context,
            CleanifyDatabase::class.java,
            CleanifyDatabase.DATABASE_NAME
        ).addMigrations(
            CleanifyDatabase.MIGRATION_1_2,
            CleanifyDatabase.MIGRATION_2_3,
            CleanifyDatabase.MIGRATION_3_4
        ).build()
    }

    @Provides
    @Singleton
    fun provideFileSignatureDao(database: CleanifyDatabase): FileSignatureDao {
        return database.fileSignatureDao()
    }

    @Provides
    @Singleton
    fun providePHashDao(database: CleanifyDatabase): PHashDao {
        return database.pHashDao()
    }

    @Provides
    @Singleton
    fun provideFolderDetailsDao(database: CleanifyDatabase): FolderDetailsDao {
        return database.folderDetailsDao()
    }

    @Provides
    @Singleton
    fun provideUnreadableFileCacheDao(database: CleanifyDatabase): UnreadableFileCacheDao {
        return database.unreadableFileCacheDao()
    }

    @Provides
    @Singleton
    fun provideScanResultCacheDao(database: CleanifyDatabase): ScanResultCacheDao {
        return database.scanResultCacheDao()
    }

    @Provides
    @Singleton
    fun provideSimilarityDenialDao(database: CleanifyDatabase): SimilarityDenialDao {
        return database.similarityDenialDao()
    }

    @Provides
    @Singleton
    fun provideRecycleBinDao(database: CleanifyDatabase): RecycleBinDao {
        return database.recycleBinDao()
    }
}
