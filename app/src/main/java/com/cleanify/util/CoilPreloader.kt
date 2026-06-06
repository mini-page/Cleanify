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

package com.cleanify.util

import android.content.Context
import coil.ImageLoader
import coil.request.ImageRequest
import com.cleanify.data.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A utility class for pre-warming Coil's memory cache with image thumbnails.
 *
 * This singleton can be injected anywhere in the app to proactively load images
 * into memory, ensuring they are available instantly when the UI needs to display them.
 * This is particularly useful for improving the perceived performance of lists or grids
 * of images that are about to be displayed.
 */
@Singleton
class CoilPreloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader
) {

    /**
     * Enqueues low-priority image requests for a list of media items.
     * The results of these requests are discarded; the sole purpose is to populate
     * Coil's memory cache.
     *
     * @param mediaItems A list of [MediaItem] objects whose thumbnails should be preloaded.
     */
    fun preload(mediaItems: List<MediaItem>) {
        if (mediaItems.isEmpty()) return

        mediaItems.forEach { item ->
            val request = ImageRequest.Builder(context)
                .data(item.uri)
                // By not specifying a target, we tell Coil to just load the image
                // into the cache and then discard the result.
                .target(null)
                .build()
            imageLoader.enqueue(request)
        }
    }
}
