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

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.cleanify.ui.theme.AppTheme
import com.cleanify.ui.theme.CleanifyTheme
import com.cleanify.data.cleaner.RecycleBinPurgeWorker
import com.cleanify.util.ProactiveIndexer
import com.cleanify.util.RecycleBinManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.graphics.drawable.toDrawable

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    @Inject
    lateinit var proactiveIndexer: ProactiveIndexer

    @Inject
    lateinit var recycleBinManager: RecycleBinManager

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the SplashScreen API before super.onCreate()
        val splashScreen = installSplashScreen()

        applyNoTransition()
        super.onCreate(savedInstanceState)

        // Hold the splash screen until the ViewModel has finished loading preferences (theme, locale).
        // This ensures the first frame rendered after the splash is the correct theme.
        splashScreen.setKeepOnScreenCondition { !mainViewModel.isReady.value }

        // Enable edge-to-edge drawing
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Set the window background color instantly to prevent flicker on activity recreation.
        window.setWindowAnimations(0)
        applyImmediateBackgroundColor()

        // Observe locale changes and apply them via AppCompatDelegate.
        lifecycleScope.launch {
            mainViewModel.appLocale.collectLatest { locale ->
                val appLocales = if (locale.tag != null) {
                    LocaleListCompat.forLanguageTags(locale.tag)
                } else {
                    LocaleListCompat.getEmptyLocaleList()
                }
                val currentLocales = AppCompatDelegate.getApplicationLocales()
                if (currentLocales.toLanguageTags() != appLocales.toLanguageTags()) {
                    AppCompatDelegate.setApplicationLocales(appLocales)
                }
            }
        }

        // Schedule the proactive indexing job on app startup.
        proactiveIndexer.scheduleGlobalIndex()

        // Schedule daily recycle bin purge and clean any already-expired entries
        RecycleBinPurgeWorker.scheduleDailyPurge(this)
        lifecycleScope.launch {
            recycleBinManager.purgeExpired()
        }

        setContent {
            val currentTheme by mainViewModel.currentTheme.collectAsStateWithLifecycle()
            val useDynamicColors by mainViewModel.useDynamicColors.collectAsStateWithLifecycle()
            val accentColorKey by mainViewModel.accentColorKey.collectAsStateWithLifecycle()
            val reduceAnimations by mainViewModel.reduceAnimations.collectAsStateWithLifecycle()
            val windowSizeClass = calculateWindowSizeClass(this)

            CleanifyTheme(
                theme = currentTheme,
                useDynamicColors = useDynamicColors,
                accentColorKey = accentColorKey,
                reduceAnimations = reduceAnimations
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(viewModel = mainViewModel, windowSizeClass = windowSizeClass)
                }
            }
        }
    }

    private fun applyImmediateBackgroundColor() {
        val isAmoled = mainViewModel.currentTheme.value == AppTheme.AMOLED
        val color = if (isAmoled) {
            Color.BLACK
        } else {
            val a = TypedValue()
            theme.resolveAttribute(android.R.attr.colorBackground, a, true)
            if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                a.data
            } else {
                Color.BLACK
            }
        }
        window.setBackgroundDrawable(color.toDrawable())
        window.decorView.setBackgroundColor(color)
    }

    private fun applyNoTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
