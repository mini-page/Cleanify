package com.cleanify.util

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageVolumeProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class VolumeInfo(
        val directory: File,
        val description: String,
        val isPrimary: Boolean,
        val isRemovable: Boolean,
        val volumeId: String?
    )

    fun getVolumes(): List<VolumeInfo> {
        val volumes = mutableListOf<VolumeInfo>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val storageVolumes = storageManager.storageVolumes

            for (sv in storageVolumes) {
                val dir = sv.directory ?: continue
                val desc = sv.getDescription(context)
                val isPrimary = sv.isPrimary
                val isRemovable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    !sv.isPrimary
                } else {
                    @Suppress("DEPRECATION")
                    sv.isRemovable
                }
                val volumeId = getVolumeId(sv)

                if (dir.exists() && dir.canRead()) {
                    volumes.add(VolumeInfo(dir, desc, isPrimary, isRemovable, volumeId))
                }
            }
        }

        val primaryFallback = Environment.getExternalStorageDirectory()
        if (volumes.none { it.isPrimary } && primaryFallback.exists() && primaryFallback.canRead()) {
            volumes.add(VolumeInfo(primaryFallback, "Internal storage", true, false, "primary"))
        }

        return volumes
    }

    fun getPrimaryVolume(): VolumeInfo? = getVolumes().firstOrNull { it.isPrimary }

    fun getSecondaryVolumes(): List<VolumeInfo> = getVolumes().filter { !it.isPrimary }

    fun getScanRoots(): List<File> = getVolumes().map { it.directory }

    private fun getVolumeId(volume: StorageVolume): String? {
        return volume.uuid ?: volume.directory?.absolutePath
    }
}
