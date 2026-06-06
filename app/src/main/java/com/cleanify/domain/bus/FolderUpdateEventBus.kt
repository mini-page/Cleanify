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

package com.cleanify.domain.bus

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A data class representing the change in item count and total size for a single folder.
 * This is used for batch updates.
 */
data class FolderDelta(
    val itemCountChange: Int,
    val sizeChange: Long
)

/**
 * Defines the specific events related to folder content changes.
 */
sealed class FolderUpdateEvent {
    /**
     * Fired when one or more folders have their contents changed.
     * This single event carries a map of all changes, allowing for efficient,
     * atomic UI updates and preventing flickering from multiple individual events.
     *
     * @param updates A map where the key is the folder path and the value is the delta of changes.
     */
    data class FolderBatchUpdate(val updates: Map<String, FolderDelta>) : FolderUpdateEvent()

    /**
     * Fired when a new folder is created and should be added to the list.
     */
    data class FolderAdded(val path: String, val name: String) : FolderUpdateEvent()

    /**
     * A system-wide signal that the folder list is fundamentally out of date
     * and requires a complete, forced refresh from the data source.
     */
    data object FullRefreshRequired : FolderUpdateEvent()
}


/**
 * A dedicated singleton event bus for real-time folder updates.
 * This allows for instantaneous UI feedback in SessionSetupScreen when changes
 * are made in the SwiperScreen, bypassing slower, full-scan repository methods.
 */
@Singleton
class FolderUpdateEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<FolderUpdateEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    suspend fun post(event: FolderUpdateEvent) {
        _events.emit(event)
    }
}
