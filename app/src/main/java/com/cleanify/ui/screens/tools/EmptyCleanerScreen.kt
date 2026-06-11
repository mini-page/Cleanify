package com.cleanify.ui.screens.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cleanify.ui.components.BackNavigationIcon
import java.io.File
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyCleanerScreen(
    onNavigateUp: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: CleanerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Empty Cleaner") },
                navigationIcon = { BackNavigationIcon(onClick = onNavigateUp) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!uiState.scanComplete && !uiState.isScanning && !uiState.deleteComplete) {
                OptionsSection(
                    scanEmptyFile = uiState.scanEmptyFile,
                    scanEmptyFolder = uiState.scanEmptyFolder,
                    scanGeneric = uiState.scanGeneric,
                    scanApk = uiState.scanApk,
                    scanCorpse = uiState.scanCorpse,
                    onToggleEmptyFile = { viewModel.updateEmptyFile(it) },
                    onToggleEmptyFolder = { viewModel.updateEmptyFolder(it) },
                    onToggleGeneric = { viewModel.updateGeneric(it) },
                    onToggleApk = { viewModel.updateApk(it) },
                    onToggleCorpse = { viewModel.updateCorpse(it) },
                    onStartScan = { viewModel.startScan() },
                    onQuickClean = { viewModel.quickClean() }
                )
            }

            if (uiState.isScanning) {
                ScanningSection(progress = uiState.progress)
            }

            if (uiState.scanComplete && !uiState.isScanning) {
                ResultsSection(
                    foundFiles = uiState.foundFiles,
                    totalSize = uiState.totalSize,
                    onClean = { viewModel.executeClean() },
                    onRescan = { viewModel.startScan() }
                )
            }

            if (uiState.deleteComplete && !uiState.isScanning) {
                DoneSection(
                    filesFailed = uiState.filesFailed,
                    ramFreed = uiState.ramFreed,
                    onRescan = { viewModel.startScan() }
                )
            }

            if (uiState.log.isNotEmpty() && !uiState.isScanning) {
                Text(
                    text = "Log (${uiState.log.size} entries)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    val logScrollState = rememberScrollState()
                    LazyColumn(
                        modifier = Modifier.padding(8.dp).horizontalScroll(logScrollState),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(uiState.log.takeLast(100)) { entry ->
                            Text(
                                text = entry,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionsSection(
    scanEmptyFile: Boolean,
    scanEmptyFolder: Boolean,
    scanGeneric: Boolean,
    scanApk: Boolean,
    scanCorpse: Boolean,
    onToggleEmptyFile: (Boolean) -> Unit,
    onToggleEmptyFolder: (Boolean) -> Unit,
    onToggleGeneric: (Boolean) -> Unit,
    onToggleApk: (Boolean) -> Unit,
    onToggleCorpse: (Boolean) -> Unit,
    onStartScan: () -> Unit,
    onQuickClean: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Scan Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Empty Files (0 bytes)")
                Switch(checked = scanEmptyFile, onCheckedChange = onToggleEmptyFile)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Empty Folders")
                Switch(checked = scanEmptyFolder, onCheckedChange = onToggleEmptyFolder)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Generic Junk")
                    Text(
                        ".tmp, .log, temp folders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = scanGeneric, onCheckedChange = onToggleGeneric)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("APK Files")
                    Text(
                        ".apk, .apks, .apkm, .aab",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = scanApk, onCheckedChange = onToggleApk)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Corpse Files")
                    Text(
                        "Orphaned app data folders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = scanCorpse, onCheckedChange = onToggleCorpse)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onStartScan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Device")
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onQuickClean,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quick Clean")
            }
        }
    }
}

@Composable
private fun ScanningSection(progress: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Scanning...", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { (progress / 100.0).toFloat() }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${progress.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResultsSection(
    foundFiles: List<File>,
    totalSize: Long,
    onClean: () -> Unit,
    onRescan: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val format = DecimalFormat("#.##")
    val kib: Long = 1024
    val mib: Long = 1048576
    val sizeStr = when {
        totalSize > mib -> "${format.format(totalSize / mib)} MB"
        totalSize > kib -> "${format.format(totalSize / kib)} KB"
        else -> "$totalSize B"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Scan Complete", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Found: ${foundFiles.size} items")
            Text("Total size: $sizeStr")
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClean() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete All")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onRescan) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rescan")
                }
            }
        }
    }
}

@Composable
private fun DoneSection(
    filesFailed: Int = 0,
    ramFreed: Long = 0,
    onRescan: () -> Unit
) {
    val format = DecimalFormat("#.##")
    val kib: Long = 1024
    val mib: Long = 1048576
    val ramStr = when {
        ramFreed > mib -> "${format.format(ramFreed / mib)} MB"
        ramFreed > kib -> "${format.format(ramFreed / kib)} KB"
        ramFreed > 0 -> "$ramFreed B"
        else -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CleaningServices,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Cleanup Complete!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (filesFailed > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "$filesFailed item${if (filesFailed != 1) "s" else ""} could not be deleted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (ramStr != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "RAM freed: $ramStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onRescan) {
                Text("Scan Again")
            }
        }
    }
}
