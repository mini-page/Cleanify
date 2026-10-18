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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cleanify.BuildConfig
import com.cleanify.R
import com.cleanify.data.repository.UpdateCheckInterval
import com.cleanify.ui.MainActivity
import java.util.concurrent.TimeUnit

class AppUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting periodic app update check...")
        return try {
            val updateInfo = UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
            if (updateInfo != null) {
                Log.d(TAG, "New update available: ${updateInfo.tagName}")
                showUpdateNotification(context, updateInfo)
            } else {
                Log.d(TAG, "App is up to date.")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates: ${e.message}", e)
            Result.retry()
        }
    }

    private fun showUpdateNotification(context: Context, info: UpdateInfo) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when a new version of Cleanify is available"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "about")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_quick_clean)
            .setContentTitle("Cleanify Update Available: v${info.versionName}")
            .setContentText("A new version of Cleanify is ready. Tap to view release notes and update.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    if (info.changelog.isNotBlank()) "Cleanify v${info.versionName} is now available!\n\n${info.changelog.take(200)}..."
                    else "Cleanify v${info.versionName} is now available with new features and fixes. Tap to download."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "AppUpdateWorker"
        const val CHANNEL_ID = "app_updates_channel"
        private const val NOTIFICATION_ID = 2001
        private const val UNIQUE_WORK_NAME = "periodic_app_update_check"

        fun scheduleOrCancel(context: Context, interval: UpdateCheckInterval) {
            val workManager = WorkManager.getInstance(context)
            if (interval == UpdateCheckInterval.NEVER || interval.days <= 0L) {
                Log.d(TAG, "Cancelling periodic update check")
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            } else {
                Log.d(TAG, "Scheduling periodic update check every ${interval.days} days")
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = PeriodicWorkRequestBuilder<AppUpdateWorker>(interval.days, TimeUnit.DAYS)
                    .setConstraints(constraints)
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            }
        }
    }
}
