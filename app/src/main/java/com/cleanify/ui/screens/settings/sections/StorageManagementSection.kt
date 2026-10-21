package com.cleanify.ui.screens.settings.sections

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cleanify.ui.screens.settings.SettingsViewModel

/**
 * Settings section for File & Storage Management.
 * Adapted from Quarry App (Apache 2.0, Tanvir).
 */
@Composable
fun StorageManagementSection(
    viewModel: SettingsViewModel,
    onNavigateToCleaner: () -> Unit = {}
) {
    val context = LocalContext.current
    val storageLargeFileThresholdMb by viewModel.storageLargeFileThresholdMb.collectAsStateWithLifecycle()
    val storageOldDownloadsDays by viewModel.storageOldDownloadsDays.collectAsStateWithLifecycle()
    val storageScanHiddenFiles by viewModel.storageScanHiddenFiles.collectAsStateWithLifecycle()
    val storageHapticsEnabled by viewModel.storageHapticsEnabled.collectAsStateWithLifecycle()

    val volumes = remember(context) {
        val list = mutableListOf<StorageVolumeInfo>()
        val sm = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
        sm?.storageVolumes?.forEach { sv ->
            val dir = sv.directory
            if (dir != null && dir.isDirectory && dir.canRead()) {
                val stat = StatFs(dir.absolutePath)
                val total = stat.blockSizeLong * stat.blockCountLong
                val free = stat.blockSizeLong * stat.availableBlocksLong
                list.add(
                    StorageVolumeInfo(
                        name = sv.getDescription(context),
                        isPrimary = sv.isPrimary,
                        totalBytes = total,
                        usedBytes = total - free
                    )
                )
            }
        }
        if (list.none { it.isPrimary }) {
            val primaryDir = Environment.getExternalStorageDirectory()
            if (primaryDir.isDirectory && primaryDir.canRead()) {
                val stat = StatFs(primaryDir.absolutePath)
                val total = stat.blockSizeLong * stat.blockCountLong
                val free = stat.blockSizeLong * stat.availableBlocksLong
                list.add(
                    StorageVolumeInfo(
                        name = "Internal Storage",
                        isPrimary = true,
                        totalBytes = total,
                        usedBytes = total - free
                    )
                )
            }
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Detected Storage Volumes
        FrostedSettingsGroup(title = "Detected Storage Volumes") {
            volumes.forEach { vol ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (vol.isPrimary) Icons.Default.Devices else Icons.Default.SdCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(vol.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            val fraction = if (vol.totalBytes > 0) vol.usedBytes.toFloat() / vol.totalBytes else 0f
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth().height(6.dp).padding(vertical = 2.dp)
                            )
                            Text(
                                "${Formatter.formatShortFileSize(context, vol.usedBytes)} used of ${Formatter.formatShortFileSize(context, vol.totalBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Storage Candidate Thresholds
        FrostedSettingsGroup(title = "Storage Candidate Thresholds") {
            SettingsPickerItem(
                title = "Large File Threshold",
                description = "Minimum file size to be flagged as a Large File candidate",
                options = listOf(25L, 50L, 100L, 250L, 500L),
                selectedOption = storageLargeFileThresholdMb,
                onOptionSelected = { viewModel.setStorageLargeFileThresholdMb(it) },
                getDisplayName = { "${it} MB" }
            )

            SettingsPickerItem(
                title = "Old Downloads Age",
                description = "Files in Download folder older than this will be flagged",
                options = listOf(14, 30, 60, 90, 180),
                selectedOption = storageOldDownloadsDays,
                onOptionSelected = { viewModel.setStorageOldDownloadsDays(it) },
                getDisplayName = { "${it} days" }
            )
        }

        // 3. Scan & Safety
        FrostedSettingsGroup(title = "Filesystem & Haptics") {
            SettingSwitch(
                title = "Scan Hidden Files",
                description = "Include dotfiles and hidden folders in storage cleanup scans",
                checked = storageScanHiddenFiles,
                onCheckedChange = { viewModel.setStorageScanHiddenFiles(it) }
            )

            SettingSwitch(
                title = "Tactile Haptic Feedback",
                description = "Provide gentle vibration on cleanup actions and gesture triggers",
                checked = storageHapticsEnabled,
                onCheckedChange = { viewModel.setStorageHapticsEnabled(it) }
            )
        }

        // 4. Quick Shortcuts
        FrostedSettingsGroup(title = "Related Cleanup Tools") {
            OutlinedButton(
                onClick = onNavigateToCleaner,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Empty Cleaner Schedule & Settings")
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

private data class StorageVolumeInfo(
    val name: String,
    val isPrimary: Boolean,
    val totalBytes: Long,
    val usedBytes: Long
)
