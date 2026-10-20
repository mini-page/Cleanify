package com.cleanify.ui.screens.recyclebin

import android.net.Uri
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cleanify.R
import com.cleanify.data.db.entity.FileCategory
import com.cleanify.data.db.entity.RecycleBinEntry
import com.cleanify.ui.components.BackNavigationIcon
import com.cleanify.ui.components.EmptyState
import com.cleanify.ui.components.MediaPreviewDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onNavigateUp: () -> Unit = {},
    embedded: Boolean = false,
    viewModel: RecycleBinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val previewEntry by viewModel.previewEntry.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEmptyBinConfirmDialog by remember { mutableStateOf(false) }

    val totalRecycledBytes = remember(uiState.entries) {
        uiState.entries.sumOf { it.fileSize }
    }
    val selectedBytes = remember(uiState.selectedIds, uiState.entries) {
        uiState.entries.filter { it.id in uiState.selectedIds }.sumOf { it.fileSize }
    }

    Scaffold(
        topBar = {
            if (!embedded) {
                TopAppBar(
                    title = { Text("Recycle Bin", fontWeight = FontWeight.Bold) },
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
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showEmptyBinConfirmDialog = true
                                }
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Empty bin")
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = uiState.selectedIds.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Top info row: count + recoverable size badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${uiState.selectedIds.size} file(s) selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = Formatter.formatShortFileSize(context, selectedBytes),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Bottom action row: 50/50 equal-width buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.restoreSelected()
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Restore", maxLines = 1)
                            }
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showDeleteConfirmDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Delete", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        val hasAnyCategories = uiState.availableCategories.isNotEmpty()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Overview & Summary Banner (when files exist in bin)
            if (uiState.entries.isNotEmpty() || hasAnyCategories) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.RestoreFromTrash,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${uiState.entries.size} files in bin (${Formatter.formatShortFileSize(context, totalRecycledBytes)})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Items are automatically purged after 30 days retention.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Category Tabs / Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.selectedCategory == null,
                        onClick = { viewModel.setCategory(null) },
                        label = { Text("All (${uiState.entries.size})") },
                        leadingIcon = {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        shape = RoundedCornerShape(18.dp)
                    )
                    uiState.availableCategories.forEach { category ->
                        val count = uiState.entries.count { it.fileCategory == category }
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = { viewModel.setCategory(category) },
                            label = { Text("${category.displayName} ($count)") },
                            leadingIcon = {
                                Icon(categoryIcon(category), contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            shape = RoundedCornerShape(18.dp)
                        )
                    }
                }
            }

            // Body States: Empty vs Filter Empty vs List
            when {
                uiState.entries.isEmpty() && !hasAnyCategories -> {
                    // Completely empty bin state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.size(88.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            Text(
                                text = stringResource(R.string.empty_recycle_bin_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.empty_recycle_bin_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                uiState.entries.isEmpty() && uiState.selectedCategory != null -> {
                    // Filter resulted in 0 items
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.FolderOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "No ${uiState.selectedCategory?.displayName} files",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "No items matching this category currently exist in your recycle bin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = { viewModel.setCategory(null) },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Show All Files")
                            }
                        }
                    }
                }
                else -> {
                    // Loaded state
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null) },
            title = { Text("Delete permanently?") },
            text = {
                Text(
                    "This action cannot be undone. ${uiState.selectedIds.size} file(s) will be permanently deleted from device storage."
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
            text = { Text("All files in the recycle bin will be permanently deleted. This cannot be undone.") },
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
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
            Spacer(Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = categoryIcon(entry.fileCategory),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = entry.originalPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedSize,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            daysRemaining <= 2 -> MaterialTheme.colorScheme.errorContainer
                            daysRemaining <= 7 -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = if (daysRemaining > 0) "$daysRemaining days left" else "Purging today",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                daysRemaining <= 2 -> MaterialTheme.colorScheme.onErrorContainer
                                daysRemaining <= 7 -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
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
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(entry.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Original path: ${entry.originalPath}", style = MaterialTheme.typography.bodySmall)
                    Text("Category: ${entry.fileCategory.displayName}", style = MaterialTheme.typography.bodySmall)
                    Text("MIME type: ${entry.mimeType}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Recycled on: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(entry.deletedAt))}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        )
    }
}
