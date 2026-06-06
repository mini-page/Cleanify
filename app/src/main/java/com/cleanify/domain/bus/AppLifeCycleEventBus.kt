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
 * A Singleton event bus for broadcasting app-wide lifecycle events.
 * This is used to communicate between ViewModels without creating direct dependencies,
 * which is prohibited by Hilt.
 */
@Singleton
class AppLifecycleEventBus @Inject constructor() {

    private val _appResumeEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val appResumeEvent = _appResumeEvent.asSharedFlow()

    fun postAppResumed() {
        _appResumeEvent.tryEmit(Unit)
    }
}
