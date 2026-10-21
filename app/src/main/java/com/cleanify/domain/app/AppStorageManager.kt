package com.cleanify.domain.app

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class AppStorageItem(
    val packageName: String,
    val appName: String,
    val apkBytes: Long,
    val dataBytes: Long?,
    val cacheBytes: Long?,
    val isSystemApp: Boolean
) {
    val totalBytes: Long
        get() = apkBytes + (dataBytes ?: 0L) + (cacheBytes ?: 0L)
}

/**
 * Discovers and inspects storage used by installed applications.
 *
 * Adapted from Quarry App (Apache 2.0, Tanvir).
 * - Queries launchable applications safely without requiring broad QUERY_ALL_PACKAGES permission.
 * - Resolves APK size directly from disk.
 * - Resolves App Data and Cache sizes via StorageStatsManager when Usage Access is available.
 */
class AppStorageManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    fun hasUsageAccess(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getInstalledAppsStorage(includeSystem: Boolean = false): List<AppStorageItem> =
        withContext(Dispatchers.IO) {
            val launchablePackages = queryLaunchablePackages()
            val result = mutableListOf<AppStorageItem>()

            for (packageName in launchablePackages) {
                val item = buildAppStorageItem(packageName) ?: continue
                if (!includeSystem && item.isSystemApp) continue
                result.add(item)
            }

            result.sortedByDescending { it.totalBytes }
        }

    private fun queryLaunchablePackages(): List<String> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = try {
            packageManager.queryIntentActivities(intent, 0)
        } catch (_: Exception) {
            emptyList()
        }
        return resolveInfos.mapNotNull { it.activityInfo?.packageName }.distinct()
    }

    private fun buildAppStorageItem(packageName: String): AppStorageItem? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            val apkBytes = try {
                File(appInfo.sourceDir).length()
            } catch (_: Exception) {
                0L
            }

            var dataBytes: Long? = null
            var cacheBytes: Long? = null

            if (hasUsageAccess()) {
                try {
                    val statsManager =
                        context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
                    if (statsManager != null) {
                        val stats = statsManager.queryStatsForUid(StorageManager.UUID_DEFAULT, appInfo.uid)
                        dataBytes = stats.dataBytes
                        cacheBytes = stats.cacheBytes
                    }
                } catch (_: Exception) {
                    // Fallback to apk size only if storage stats query fails
                }
            }

            AppStorageItem(
                packageName = packageName,
                appName = appName,
                apkBytes = apkBytes,
                dataBytes = dataBytes,
                cacheBytes = cacheBytes,
                isSystemApp = isSystemApp
            )
        } catch (_: Exception) {
            null
        }
    }

    fun openAppDetails(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openUsageAccessSettings() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppDetails(context.packageName)
        }
    }

    fun requestUninstall(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppDetails(packageName)
        }
    }

    fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) {
            null
        }
    }
}
