package com.cleanify.data.cleaner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = CleanerPreferences(context)
            if (prefs.cleanOnBoot) {
                val workRequest = OneTimeWorkRequestBuilder<CleanerWorker>()
                    .setInitialDelay(1, TimeUnit.MINUTES)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiresBatteryNotLow(true)
                            .setRequiresDeviceIdle(true)
                            .build()
                    )
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "boot_cleanup",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            }
        }
    }
}
