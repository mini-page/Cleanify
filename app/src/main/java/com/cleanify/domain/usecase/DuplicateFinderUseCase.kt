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

package com.cleanify.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import com.cleanify.data.db.dao.FileSignatureDao
import com.cleanify.data.db.entity.FileSignatureCache
import com.cleanify.data.model.MediaItem
import com.cleanify.domain.model.DuplicateGroup
import com.cleanify.util.NativeHasher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

class DuplicateFinderUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileSignatureDao: FileSignatureDao
) {
    private val TAG = "DuplicateFinderUseCase"

    data class DuplicateScanResult(
        val groups: List<DuplicateGroup>,
        val skippedFilePaths: List<String>
    )

    suspend fun findDuplicates(
        allMediaItems: List<MediaItem>,
        onProgress: (itemsProcessed: Int) -> Unit
    ): DuplicateScanResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "DEBUG: Starting exact duplicate scan using hybrid hashing.")

        if (allMediaItems.isEmpty()) {
            onProgress(0)
            return@withContext DuplicateScanResult(emptyList(), emptyList())
        }

        // --- Phase 1: Group by size ---
        val filesGroupedBySize = allMediaItems
            .filter { it.size > 0 }
            .groupBy { it.size }

        val potentialDuplicates = filesGroupedBySize.values.filter { it.size > 1 }.flatten()
        val nonPotentialsCount = allMediaItems.size - potentialDuplicates.size
        if (nonPotentialsCount > 0) {
            onProgress(nonPotentialsCount) // Report non-potential duplicates as "processed"
        }
        Log.d(TAG, "DEBUG: Phase 1 (size check) complete. Found ${potentialDuplicates.size} potential duplicates.")

        // --- Phase 2: Hashing ---
        val cache = fileSignatureDao.getAllHashes().associateBy { it.filePath }
        val finalHashes = mutableMapOf<String, MutableList<MediaItem>>()
        val hashesToUpsert = mutableListOf<FileSignatureCache>()
        val allScannedPaths = mutableSetOf<String>()
        val skippedPaths = mutableListOf<String>()

        if (potentialDuplicates.isNotEmpty()) {
            val CHUNK_SIZE = 100 // Process in chunks to report progress periodically
            potentialDuplicates.chunked(CHUNK_SIZE).forEach { chunk ->
                coroutineContext.ensureActive()
                chunk.forEach { mediaItem ->
                    allScannedPaths.add(mediaItem.id)
                    val cached = cache[mediaItem.id]

                    val hash = if (cached != null && cached.lastModified == mediaItem.dateModified && cached.size == mediaItem.size) {
                        cached.hash
                    } else {
                        try {
                            val newHash = when {
                                mediaItem.isVideo -> generatePartialFileHash(mediaItem)
                                mediaItem.isImage -> generatePixelHash(mediaItem)
                                else -> generateFullFileHash(mediaItem)
                            }

                            if (newHash.isNotEmpty()) {
                                hashesToUpsert.add(FileSignatureCache(mediaItem.id, mediaItem.dateModified, mediaItem.size, newHash))
                            }
                            newHash
                        } catch (e: Exception) {
                            Log.e(TAG, "DEBUG: Error hashing file ${mediaItem.displayName}: ${e.message}")
                            skippedPaths.add(mediaItem.id)
                            "" // Skip file on error
                        }
                    }

                    if (hash.isNotEmpty()) {
                        finalHashes.getOrPut(hash) { mutableListOf() }.add(mediaItem)
                    } else if (cached == null) {
                        // If hashing resulted in an empty string and it wasn't a cache hit, it means hashing failed.
                        skippedPaths.add(mediaItem.id)
                    }
                }
                onProgress(chunk.size) // Report progress after each chunk
            }
        }
        Log.d(TAG, "DEBUG: Phase 2 (hybrid hashing) complete.")

        // --- Phase 3: DB Cleanup & Finalizing ---
        if (hashesToUpsert.isNotEmpty()) {
            fileSignatureDao.upsertHashes(hashesToUpsert)
        }
        val deletedPaths = cache.keys - allScannedPaths
        if (deletedPaths.isNotEmpty()) {
            fileSignatureDao.deleteHashesByPath(deletedPaths.toList())
        }

        val resultGroups = finalHashes.values
            .filter { it.size > 1 }
            .map { items ->
                DuplicateGroup(
                    signature = finalHashes.entries.first { it.value === items }.key,
                    items = items.sortedBy { it.dateModified },
                    sizePerFile = items.first().size
                )
            }
            .sortedByDescending { it.items.size * it.sizePerFile }

        Log.d(TAG, "DEBUG: Exact duplicate scan finished. Found ${resultGroups.size} groups, skipped ${skippedPaths.size} files.")
        return@withContext DuplicateScanResult(resultGroups, skippedPaths)
    }

    private suspend fun generatePartialFileHash(mediaItem: MediaItem, bytesToRead: Int = 256 * 1024): String {
        return try {
            val hash = NativeHasher.hashFilePartial(context, mediaItem.uri, bytesToRead)
            if (!hash.isNullOrEmpty()) "video-partial-$hash" else ""
        } catch (e: Exception) {
            Log.w(TAG, "Native partial hashing failed for ${mediaItem.id}: ${e.message}")
            ""
        }
    }

    private suspend fun generateFullFileHash(mediaItem: MediaItem): String {
        return try {
            val hash = NativeHasher.hashFile(context, mediaItem.uri)
            if (!hash.isNullOrEmpty()) "full-$hash" else ""
        } catch (e: Exception) {
            Log.w(TAG, "Native full hashing failed for ${mediaItem.id}: ${e.message}")
            ""
        }
    }

    private fun getThumbnail(mediaItem: MediaItem, size: Int): Bitmap? {
        return try {
            context.contentResolver.loadThumbnail(mediaItem.uri, Size(size, size), null)
        } catch (e: Exception) {
            Log.w(TAG, "DEBUG: Thumbnail generation failed for ${mediaItem.id}. Message: ${e.message}")
            null
        }
    }

    private suspend fun generatePixelHash(mediaItem: MediaItem): String {
        return try {
            val bitmap = getThumbnail(mediaItem, 256)
                ?: return ""

            coroutineContext.ensureActive()

            val buffer = ByteBuffer.allocate(bitmap.byteCount)
            bitmap.copyPixelsToBuffer(buffer)
            bitmap.recycle()

            coroutineContext.ensureActive()
            buffer.flip()

            val pixels = ByteArray(buffer.remaining())
            buffer.get(pixels)
            val hash = NativeHasher.hashBytes(pixels)

            if (!hash.isNullOrEmpty()) "image-pixel-$hash" else ""
        } catch (e: Exception) {
            Log.w(TAG, "DEBUG: Pixel hashing failed for ${mediaItem.id}. Message: ${e.message}")
            ""
        }
    }
}
