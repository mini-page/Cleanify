package com.cleanify.data.cleaner

import android.app.ActivityManager
import android.content.ClipData
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class CleanerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = CleanerPreferences(applicationContext)
        val scanner = FileScanner(
            Environment.getExternalStorageDirectory(),
            applicationContext
        )
        scanner.delete = true
        scanner.emptyFile = prefs.cleanEmptyFile
        scanner.emptyDir = prefs.cleanEmptyFolder
        scanner.corpse = prefs.cleanCorpse
        scanner.setFilters(generic = prefs.cleanGeneric, apk = prefs.cleanApk)
        scanner.start()

        if (prefs.cleanClipboard) {
            clearClipboard()
        }

        if (prefs.stopBackgroundApps) {
            stopBackgroundApps()
        }

        return Result.success()
    }

    private fun clearClipboard() {
        try {
            val cm = applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cm.clearPrimaryClip()
            } else {
                cm.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        } catch (_: Exception) {}
    }

    private fun stopBackgroundApps() {
        val am = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm = applicationContext.packageManager
        @Suppress("DEPRECATION")
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in apps) {
            if (app.packageName != applicationContext.packageName) {
                try {
                    @Suppress("DEPRECATION")
                    am.killBackgroundProcesses(app.packageName)
                } catch (_: Exception) {}
            }
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "hourly_clean"

        fun scheduleHourlyClean(context: Context, intervalHours: Int) {
            if (intervalHours <= 0) {
                cancelHourlyClean(context)
                return
            }
            val workRequest = PeriodicWorkRequestBuilder<CleanerWorker>(intervalHours.toLong(), TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiresDeviceIdle(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                workRequest
            )
        }

        fun cancelHourlyClean(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
