package com.cleanify.ui.screens.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cleanify.ui.components.AppDialog
import com.cleanify.ui.components.BackNavigationIcon
import java.io.File
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyCleanerScreen(
    onNavigateUp: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    embedded: Boolean = false,
    viewModel: CleanerViewModel = viewModel()
) {
    if (embedded) {
        EmptyCleanerBody(viewModel = viewModel, modifier = Modifier.padding(16.dp))
    } else {
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
            EmptyCleanerBody(viewModel = viewModel, modifier = Modifier.padding(padding).padding(16.dp))
        }
    }
}

@Composable
fun EmptyCleanerBody(
    viewModel: CleanerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val volumes = viewModel.storageVolumes
    var selectedVolumeIndex by remember { mutableIntStateOf(-1) }
    val hasMultipleVolumes = volumes.size > 1

    Column(
        modifier = modifier.fillMaxSize(),
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
                    volumes = if (hasMultipleVolumes) volumes else emptyList(),
                    selectedVolumeIndex = selectedVolumeIndex,
                    onSelectVolume = { selectedVolumeIndex = it },
                    onStartScan = { viewModel.startScan(selectedVolumeIndex) },
                    onQuickClean = { viewModel.quickClean(selectedVolumeIndex) }
                )
            }

            if (uiState.isScanning) {
                ScanningSection(progress = uiState.progress)
            }

            if (uiState.scanComplete && !uiState.isScanning) {
                ResultsSection(
                    foundFiles = uiState.foundFiles,
                    totalSize = uiState.totalSize,
                    onCleanSelected = { files -> viewModel.executeCleanFiles(files) },
                    onRescan = { viewModel.startScan(selectedVolumeIndex) }
                )
            }

            if (uiState.deleteComplete && !uiState.isScanning) {
                DoneSection(
                    filesFailed = uiState.filesFailed,
                    ramFreed = uiState.ramFreed,
                    onRescan = { viewModel.startScan(selectedVolumeIndex) }
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
    volumes: List<StorageVolumeEntry> = emptyList(),
    selectedVolumeIndex: Int = -1,
    onSelectVolume: (Int) -> Unit = {},
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

            if (volumes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Storage", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedVolumeIndex == -1,
                        onClick = { onSelectVolume(-1) },
                        label = { Text("All") },
                        leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    volumes.forEachIndexed { index, volume ->
                        val icon: ImageVector = if (volume.isPrimary) Icons.Default.Devices else Icons.Default.SdCard
                        FilterChip(
                            selected = selectedVolumeIndex == index,
                            onClick = { onSelectVolume(index) },
                            label = { Text(volume.description) },
                            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            val scanLabel = when {
                volumes.isEmpty() -> "Scan Device"
                selectedVolumeIndex == -1 -> "Scan All Storage"
                selectedVolumeIndex in volumes.indices -> "Scan ${volumes[selectedVolumeIndex].description}"
                else -> "Scan Device"
            }
            Button(
                onClick = onStartScan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(scanLabel)
            }
            Spacer(modifier = Modifier.height(4.dp))
            val cleanLabel = when {
                volumes.isEmpty() -> "Quick Clean"
                selectedVolumeIndex == -1 -> "Quick Clean All"
                selectedVolumeIndex in volumes.indices -> "Quick Clean ${volumes[selectedVolumeIndex].description}"
                else -> "Quick Clean"
            }
            OutlinedButton(
                onClick = onQuickClean,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(cleanLabel)
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
    onCleanSelected: (List<File>) -> Unit,
    onRescan: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPaths by remember(foundFiles) { mutableStateOf(foundFiles.map { it.absolutePath }.toSet()) }

    val format = DecimalFormat("#.##")
    val kib: Long = 1024
    val mib: Long = 1048576
    val sizeStr = when {
        totalSize > mib -> "${format.format(totalSize / mib)} MB"
        totalSize > kib -> "${format.format(totalSize / kib)} KB"
        else -> "$totalSize B"
    }

    val filteredFiles = remember(foundFiles, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) foundFiles
        else foundFiles.filter { it.name.lowercase().contains(q) || it.absolutePath.lowercase().contains(q) }
    }

    val selectedFiles = remember(selectedPaths, foundFiles) {
        foundFiles.filter { it.absolutePath in selectedPaths }
    }

    if (showDeleteConfirm) {
        AppDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete selected items?") },
            text = { Text("This will permanently delete ${selectedFiles.size} empty file(s)/folder(s). Unchecked items will be kept.") }
        ) {
            TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            Button(
                onClick = {
                    showDeleteConfirm = false
                    onCleanSelected(selectedFiles)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete (${selectedFiles.size})")
            }
        }
    }

    if (foundFiles.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Text("Your device is clean!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("No empty files or folders found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onRescan) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rescan")
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Action Card (Quarry style)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Scan Complete",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${foundFiles.size} items found • $sizeStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val allFilteredSelected = filteredFiles.isNotEmpty() && filteredFiles.all { it.absolutePath in selectedPaths }
                        TextButton(
                            onClick = {
                                val filteredPaths = filteredFiles.map { it.absolutePath }.toSet()
                                selectedPaths = if (allFilteredSelected) {
                                    selectedPaths - filteredPaths
                                } else {
                                    selectedPaths + filteredPaths
                                }
                            }
                        ) {
                            Text(if (allFilteredSelected) "Deselect All" else "Select All")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showDeleteConfirm = true
                            },
                            enabled = selectedFiles.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete (${selectedFiles.size})")
                        }

                        OutlinedButton(onClick = onRescan) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rescan")
                        }
                    }
                }
            }

            // Accessible Search Bar for filtering candidate folders
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search empty folders or path...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )

            // Candidates list header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "Empty Candidates (Uncheck to Keep)"
                    else "Matching Candidates (${filteredFiles.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${selectedFiles.size} selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No empty folders match \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredFiles, key = { it.absolutePath }) { file ->
                        val isChecked = file.absolutePath in selectedPaths
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) MaterialTheme.colorScheme.surfaceContainerHigh
                                else MaterialTheme.colorScheme.surface
                            ),
                            onClick = {
                                selectedPaths = if (isChecked) selectedPaths - file.absolutePath
                                else selectedPaths + file.absolutePath
                            }
                        ) {
                            Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                                contentDescription = null,
                                tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = file.parent ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedPaths = if (checked) selectedPaths + file.absolutePath
                                    else selectedPaths - file.absolutePath
                                }
                            )
                        }
                    }
                }
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
