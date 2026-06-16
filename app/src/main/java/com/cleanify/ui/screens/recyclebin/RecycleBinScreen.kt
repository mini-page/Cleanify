package com.cleanify.ui.screens.recyclebin

import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cleanify.data.db.entity.FileCategory
import com.cleanify.data.db.entity.RecycleBinEntry
import com.cleanify.ui.components.BackNavigationIcon
import com.cleanify.ui.components.EmptyState
import com.cleanify.ui.components.MediaPreviewDialog
import com.cleanify.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onNavigateUp: () -> Unit,
    viewModel: RecycleBinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val previewEntry by viewModel.previewEntry.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEmptyBinConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycle Bin") },
                navigationIcon = { BackNavigationIcon(onClick = onNavigateUp) },
                actions = {
                    if (uiState.entries.isNotEmpty()) {
                        val hasAllSelected = uiState.entries.isNotEmpty() &&
                            uiState.selectedIds.size == uiState.entries.size
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(
                                imageVector = if (hasAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = if (hasAllSelected) "Deselect all" else "Select all"
                            )
                        }
                        IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showEmptyBinConfirmDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Empty bin")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.selectedIds.isNotEmpty()) {
                BottomAppBar(tonalElevation = 3.dp) {
                    Text(
                        text = "${uiState.selectedIds.size} selected",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.restoreSelected() }) {
                        Icon(Icons.Default.Restore, contentDescription = "Restore", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Restore")
                    }
                    TextButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showDeleteConfirmDialog = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    ) { padding ->
        val hasAnyEntries = uiState.availableCategories.isNotEmpty()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasAnyEntries) {
                CategoryTabs(
                    selectedCategory = uiState.selectedCategory,
                    availableCategories = uiState.availableCategories,
                    onCategorySelected = { viewModel.setCategory(it) }
                )
            }

            if (uiState.entries.isEmpty()) {
                EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    icon = Icons.Default.DeleteSweep,
                    titleRes = R.string.empty_recycle_bin_title,
                    descriptionRes = R.string.empty_recycle_bin_desc
                )
            } else {
                val context = LocalContext.current
                if (uiState.selectedCategory != null && uiState.entries.isEmpty()) {
                    EmptyState(
                        modifier = Modifier.fillMaxSize(),
                        icon = Icons.Default.FolderOff,
                        titleRes = R.string.empty_generic_title,
                        descriptionRes = R.string.empty_generic_desc
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(uiState.entries, key = { it.id }) { entry ->
                            RecycleBinItemRow(
                                entry = entry,
                                isSelected = entry.id in uiState.selectedIds,
                                onToggle = { viewModel.toggleSelection(entry.id) },
                                onTap = { viewModel.showPreview(entry) },
                                formattedSize = Formatter.formatShortFileSize(context, entry.fileSize),
                                daysRemaining = ((entry.expiresAt - System.currentTimeMillis()) / 86_400_000L).toInt().coerceAtLeast(0)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Delete permanently?") },
            text = {
                Text(
                    "This action cannot be undone. ${uiState.selectedIds.size} file(s) will be permanently deleted."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.permanentlyDeleteEntries(uiState.selectedIds)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEmptyBinConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyBinConfirmDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
            title = { Text("Empty recycle bin?") },
            text = { Text("All files in the recycle bin will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.emptyBin()
                        showEmptyBinConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Empty")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyBinConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    previewEntry?.let { entry ->
        PreviewDialog(
            entry = entry,
            onDismiss = { viewModel.dismissPreview() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryTabs(
    selectedCategory: FileCategory?,
    availableCategories: List<FileCategory>,
    onCategorySelected: (FileCategory?) -> Unit
) {
    val tabs = listOf<FileCategory?>(null) + availableCategories

    PrimaryScrollableTabRow(
        selectedTabIndex = tabs.indexOf(selectedCategory).coerceAtLeast(0),
        edgePadding = 0.dp,
        divider = {}
    ) {
        tabs.forEach { category ->
            Tab(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                text = {
                    Text(
                        text = category?.displayName ?: "All",
                        maxLines = 1,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                icon = {
                    Icon(
                        imageVector = categoryIcon(category),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

private fun categoryIcon(category: FileCategory?): ImageVector = when (category) {
    null -> Icons.Default.DeleteSweep
    FileCategory.Image -> Icons.Default.Image
    FileCategory.Video -> Icons.Default.VideoFile
    FileCategory.Audio -> Icons.Default.MusicNote
    FileCategory.Document -> Icons.Default.Description
    FileCategory.Archive -> Icons.Default.FolderZip
    FileCategory.Other -> Icons.AutoMirrored.Filled.InsertDriveFile
}

@Composable
private fun RecycleBinItemRow(
    entry: RecycleBinEntry,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onTap: () -> Unit,
    formattedSize: String,
    daysRemaining: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        onClick = onTap
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = categoryIcon(entry.fileCategory),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.originalPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = formattedSize,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = if (daysRemaining > 0) "$daysRemaining days left" else "Expires today",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (daysRemaining <= 1)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewDialog(
    entry: RecycleBinEntry,
    onDismiss: () -> Unit
) {
    val isMedia = entry.mimeType.startsWith("image/") || entry.mimeType.startsWith("video/")
    if (isMedia) {
        MediaPreviewDialog(
            uri = Uri.fromFile(File(entry.recycledPath)),
            displayName = entry.fileName,
            fileSize = entry.fileSize,
            dateModified = entry.deletedAt,
            mimeType = entry.mimeType,
            onDismiss = onDismiss
        )
    } else {
        val context = LocalContext.current
        val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = categoryIcon(entry.fileCategory),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text(entry.fileName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("Category", entry.fileCategory.displayName)
                    DetailRow("Size", Formatter.formatShortFileSize(context, entry.fileSize))
                    DetailRow("Deleted", dateFormat.format(Date(entry.deletedAt)))
                    DetailRow("Expires", dateFormat.format(Date(entry.expiresAt)))
                    HorizontalDivider()
                    Text(
                        text = entry.originalPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
