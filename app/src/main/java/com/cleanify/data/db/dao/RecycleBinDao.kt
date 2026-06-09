package com.cleanify.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cleanify.data.db.entity.FileCategory
import com.cleanify.data.db.entity.RecycleBinEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface RecycleBinDao {
    @Query("SELECT * FROM recycle_bin ORDER BY deletedAt DESC")
    fun getAllEntries(): Flow<List<RecycleBinEntry>>

    @Query("SELECT * FROM recycle_bin WHERE fileCategory = :category ORDER BY deletedAt DESC")
    fun getEntriesByCategory(category: FileCategory): Flow<List<RecycleBinEntry>>

    @Query("SELECT * FROM recycle_bin WHERE id = :id")
    suspend fun getEntryById(id: String): RecycleBinEntry?

    @Query("SELECT * FROM recycle_bin WHERE originalPath = :path LIMIT 1")
    suspend fun getEntryByOriginalPath(path: String): RecycleBinEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: RecycleBinEntry)

    @Delete
    suspend fun deleteEntry(entry: RecycleBinEntry)

    @Query("DELETE FROM recycle_bin WHERE id IN (:ids)")
    suspend fun deleteEntriesByIds(ids: List<String>)

    @Query("DELETE FROM recycle_bin WHERE expiresAt < :now")
    suspend fun deleteExpired(now: Long)

    @Query("DELETE FROM recycle_bin")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM recycle_bin")
    fun observeCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM recycle_bin")
    fun observeTotalSize(): Flow<Long>

    @Query("SELECT COUNT(*) FROM recycle_bin WHERE expiresAt < :now")
    suspend fun getExpiredCount(now: Long): Int

    @Query("SELECT * FROM recycle_bin WHERE expiresAt < :now")
    suspend fun getExpiredEntries(now: Long): List<RecycleBinEntry>
}
