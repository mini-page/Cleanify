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

import android.app.Activity
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log

class ScreenshotDetector(
    private val activity: Activity,
    private val onScreenshotCaptured: () -> Unit
) {
    private var isListening = false
    private var lastCapturedTime = 0L

    // For Android 14+ (API 34+)
    private val screenCaptureCallback by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Activity.ScreenCaptureCallback {
                triggerIfDebounced()
            }
        } else {
            null
        }
    }

    // For Android < 14 fallback via ContentObserver
    private val mediaObserver by lazy {
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                if (uri != null) {
                    checkAndNotifyScreenshot(uri)
                }
            }
        }
    }

    private fun triggerIfDebounced() {
        val now = System.currentTimeMillis()
        if (now - lastCapturedTime >= 1500L) {
            lastCapturedTime = now
            activity.runOnUiThread {
                onScreenshotCaptured()
            }
        }
    }

    private fun checkAndNotifyScreenshot(uri: Uri) {
        try {
            val cursor = activity.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_ADDED),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val pathIndex = it.getColumnIndex(MediaStore.Images.Media.DATA)
                    val path = if (pathIndex >= 0) it.getString(pathIndex) ?: "" else ""
                    if (path.contains("screenshot", ignoreCase = true)) {
                        triggerIfDebounced()
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("ScreenshotDetector", "Could not check media uri: ${e.message}")
        }
    }

    fun start() {
        if (isListening) return
        isListening = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && screenCaptureCallback != null) {
            try {
                activity.registerScreenCaptureCallback(activity.mainExecutor, screenCaptureCallback!!)
            } catch (e: Exception) {
                Log.w("ScreenshotDetector", "Failed to register screen capture callback: ${e.message}")
            }
        } else {
            try {
                activity.contentResolver.registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    mediaObserver
                )
            } catch (e: Exception) {
                Log.w("ScreenshotDetector", "Failed to register MediaStore observer: ${e.message}")
            }
        }
    }

    fun stop() {
        if (!isListening) return
        isListening = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && screenCaptureCallback != null) {
            try {
                activity.unregisterScreenCaptureCallback(screenCaptureCallback!!)
            } catch (e: Exception) {
                Log.w("ScreenshotDetector", "Failed to unregister screen capture callback: ${e.message}")
            }
        } else {
            try {
                activity.contentResolver.unregisterContentObserver(mediaObserver)
            } catch (e: Exception) {
                Log.w("ScreenshotDetector", "Failed to unregister MediaStore observer: ${e.message}")
            }
        }
    }
}
