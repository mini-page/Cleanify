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
 * A singleton event bus for broadcasting file modification events across the application.
 * This ensures that different parts of the app can react to file deletions or moves
 * without being tightly coupled.
 */
@Singleton
class FileModificationEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<FileModificationEvent>()
    val events = _events.asSharedFlow()

    suspend fun postEvent(event: FileModificationEvent) {
        _events.emit(event)
    }
}

/**
 * Defines the types of file modification events that can be broadcast.
 */
sealed class FileModificationEvent {
    /**
     * Represents an event where one or more files have been deleted.
     * @property paths A list of the unique IDs (file paths) of the deleted media.
     */
    data class FilesDeleted(val paths: List<String>) : FileModificationEvent()
}
