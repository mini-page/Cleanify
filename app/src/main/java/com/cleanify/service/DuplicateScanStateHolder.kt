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

package com.cleanify.service

import com.cleanify.domain.model.ScanResultGroup
import com.cleanify.domain.repository.ScanScopeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

enum class BackgroundScanState {
    Idle,
    Scanning,
    Complete,
    Cancelled,
    Error
}

data class DuplicateScanState(
    val scanState: BackgroundScanState = BackgroundScanState.Idle,
    val progress: Float = 0f,
    val progressPhase: String? = null,
    val shouldShowResultsDuringScan: Boolean = false,
    val results: List<ScanResultGroup> = emptyList(),
    val unscannableFiles: List<String> = emptyList(),
    val errorMessage: String? = null,
    // Metadata for the completed scan
    val timestamp: Long? = null,
    val scanScopeType: ScanScopeType? = null
)

@Singleton
class DuplicateScanStateHolder @Inject constructor() {

    private val _state = MutableStateFlow(DuplicateScanState())
    val state = _state.asStateFlow()

    fun setScanning(initialPhase: String, shouldShowResultsDuringScan: Boolean) {
        _state.value = DuplicateScanState(
            scanState = BackgroundScanState.Scanning,
            progressPhase = initialPhase,
            shouldShowResultsDuringScan = shouldShowResultsDuringScan
        )
    }

    fun setProgress(progress: Float, phase: String) {
        _state.update {
            it.copy(
                progress = progress,
                progressPhase = phase
            )
        }
    }

    fun setComplete(
        results: List<ScanResultGroup>,
        unscannableFiles: List<String>,
        timestamp: Long,
        scanScopeType: ScanScopeType
    ) {
        _state.value = DuplicateScanState(
            scanState = BackgroundScanState.Complete,
            progress = 1f,
            progressPhase = "Complete",
            results = results,
            unscannableFiles = unscannableFiles,
            timestamp = timestamp,
            scanScopeType = scanScopeType
        )
    }

    fun setCancelled() {
        _state.update {
            it.copy(
                scanState = BackgroundScanState.Cancelled,
                progress = 0f,
                progressPhase = null,
                unscannableFiles = emptyList()
            )
        }
    }

    fun setError(message: String) {
        _state.value = DuplicateScanState(
            scanState = BackgroundScanState.Error,
            errorMessage = message,
            progressPhase = "Error"
        )
    }

    fun reset() {
        _state.value = DuplicateScanState()
    }
}
