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

package com.cleanify.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cleanify.data.repository.AppLocale
import com.cleanify.data.repository.PreferencesRepository
import com.cleanify.domain.bus.AppLifecycleEventBus
import com.cleanify.domain.repository.MediaRepository
import com.cleanify.ui.theme.AppTheme
import com.cleanify.ui.theme.predefinedAccentColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val mediaRepository: MediaRepository,
    private val appLifecycleEventBus: AppLifecycleEventBus
) : ViewModel() {

    val isReady: StateFlow<Boolean> = combine(
        preferencesRepository.themeFlow,
        preferencesRepository.appLocaleFlow,
        preferencesRepository.isOnboardingCompletedFlow
    ) { _, _, _ -> true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val currentTheme: StateFlow<AppTheme> = preferencesRepository.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM
        )

    val appLocale: StateFlow<AppLocale> = preferencesRepository.appLocaleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppLocale.SYSTEM
        )

    val useDynamicColors: StateFlow<Boolean> = preferencesRepository.useDynamicColorsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val reduceAnimations: StateFlow<Boolean> = preferencesRepository.reduceAnimationsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val accentColorKey: StateFlow<String> = preferencesRepository.accentColorKeyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = predefinedAccentColors.first().key
        )

    val updateCheckInterval: StateFlow<com.cleanify.data.repository.UpdateCheckInterval> = preferencesRepository.updateCheckIntervalFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.cleanify.data.repository.UpdateCheckInterval.EVERY_1_DAY
        )

    val lastSeenVersionCode: StateFlow<Int> = preferencesRepository.lastSeenVersionCodeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0
        )

    val isOnboardingCompleted: StateFlow<Boolean?> = preferencesRepository.isOnboardingCompletedFlow
        .onStart { emit(null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val shakeToFeedbackEnabled: StateFlow<Boolean> = preferencesRepository.shakeToFeedbackFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    val screenshotToFeedbackEnabled: StateFlow<Boolean> = preferencesRepository.screenshotToFeedbackFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    val showFeedbackDialog = kotlinx.coroutines.flow.MutableStateFlow(false)
    val showChangelogDialog = kotlinx.coroutines.flow.MutableStateFlow(false)

    private var lastFeedbackOpenTime = 0L

    init {
        viewModelScope.launch {
            mediaRepository.cleanupGhostFolders()
        }
    }

    fun openFeedbackDialog() {
        val now = System.currentTimeMillis()
        if (now - lastFeedbackOpenTime < 1500L) return
        lastFeedbackOpenTime = now
        showFeedbackDialog.value = true
    }

    fun closeFeedbackDialog() {
        showFeedbackDialog.value = false
    }

    fun openChangelogDialog() {
        showChangelogDialog.value = true
    }

    fun closeChangelogDialog(currentVersionCode: Int) {
        showChangelogDialog.value = false
        viewModelScope.launch {
            preferencesRepository.setLastSeenVersionCode(currentVersionCode)
        }
    }

    /**
     * Called when the app is brought to the foreground. Checks for underlying
     * file system changes and, only if changes are found, invalidates caches
     * and broadcasts a resume event.
     */
    fun onAppResumed() {
        viewModelScope.launch {
            val wasInvalidated = mediaRepository.checkForChangesAndInvalidate()
            if (wasInvalidated) {
                // Only post the event if a change was actually detected.
                appLifecycleEventBus.postAppResumed()
            }
        }
    }
}
