package com.cleanify.data.cleaner

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

/**
 * Intelligent detector for superfluous/obsolete APK installer packages.
 *
 * Pattern inspired by SD Maid SE's SuperfluousApksFilter (GPLv3, Darken).
 * An APK file is only marked superfluous if:
 * 1. It is a valid Android package archive.
 * 2. An application with matching packageName is currently installed on the device.
 * 3. The installed version is greater than or equal to the archive's version.
 *
 * If the archive is an uninstalled application, or represents a newer pending update,
 * it is preserved and never flagged as junk.
 */
class SuperfluousApkDetector(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    data class ApkStatus(
        val packageName: String,
        val archiveVersionCode: Long,
        val installedVersionCode: Long?,
        val isSuperfluous: Boolean
    )

    fun inspectApk(file: File): ApkStatus? {
        if (!file.isFile || !file.name.endsWith(".apk", ignoreCase = true)) {
            return null
        }

        return try {
            val archiveInfo = packageManager.getPackageArchiveInfo(file.absolutePath, 0)
                ?: return null
            val packageName = archiveInfo.packageName ?: return null
            val archiveVersionCode = getVersionCode(archiveInfo)

            val installedInfo = try {
                packageManager.getPackageInfo(packageName, 0)
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }

            val installedVersionCode = installedInfo?.let { getVersionCode(it) }
            val isSuperfluous = installedVersionCode != null && installedVersionCode >= archiveVersionCode

            ApkStatus(
                packageName = packageName,
                archiveVersionCode = archiveVersionCode,
                installedVersionCode = installedVersionCode,
                isSuperfluous = isSuperfluous
            )
        } catch (_: Exception) {
            null
        }
    }

    fun isSuperfluous(file: File): Boolean {
        return inspectApk(file)?.isSuperfluous == true
    }

    private fun getVersionCode(packageInfo: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }
}
