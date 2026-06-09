package com.cleanify.ui.screens.recyclebin

import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cleanify.data.db.entity.FileCategory
import com.cleanify.data.db.entity.RecycleBinEntry
import com.cleanify.ui.components.BackNavigationIcon
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onNavigateUp: () -> Unit,
    viewModel: RecycleBinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycle Bin") },
                navigationIcon = { BackNavigationIcon(onClick = onNavigateUp) },
                actions = {
                    if (uiState.entries.isNotEmpty()) {
                        IconButton(onClick = { viewModel.emptyBin() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Empty bin")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.selectedIds.isNotEmpty()) {
                BottomAppBar(
                    tonalElevation = 3.dp
                ) {
                    Text(
                        text = "${uiState.selectedIds.size} selected",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { viewModel.restoreSelected() }) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Restore")
                    }
                    TextButton(onClick = { viewModel.permanentlyDeleteSelected() }) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.entries.isEmpty()) {
                EmptyState()
            } else {
                CategoryTabs(
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = { viewModel.setCategory(it) }
                )

                val context = LocalContext.current
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(uiState.entries, key = { it.id }) { entry ->
                        RecycleBinItemRow(
                            entry = entry,
                            isSelected = entry.id in uiState.selectedIds,
                            onToggle = { viewModel.toggleSelection(entry.id) },
                            formattedSize = Formatter.formatShortFileSize(context, entry.fileSize),
                            daysRemaining = ((entry.expiresAt - System.currentTimeMillis()) / 86_400_000L).toInt().coerceAtLeast(0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Recycle bin is empty",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Deleted files will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

private val categoryTabs = listOf<FileCategory?>(
    null, FileCategory.Image, FileCategory.Video,
    FileCategory.Audio, FileCategory.Document, FileCategory.Archive, FileCategory.Other
)

private fun categoryIcon(category: FileCategory?): ImageVector = when (category) {
    null -> Icons.Default.DeleteSweep
    FileCategory.Image -> Icons.Default.Image
    FileCategory.Video -> Icons.Default.VideoFile
    FileCategory.Audio -> Icons.Default.MusicNote
    FileCategory.Document -> Icons.Default.Description
    FileCategory.Archive -> Icons.Default.FolderZip
    FileCategory.Other -> Icons.Default.InsertDriveFile
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryTabs(
    selectedCategory: FileCategory?,
    onCategorySelected: (FileCategory?) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = categoryTabs.indexOf(selectedCategory).coerceAtLeast(0),
        edgePadding = 0.dp,
        divider = {}
    ) {
        categoryTabs.forEach { category ->
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

@Composable
private fun RecycleBinItemRow(
    entry: RecycleBinEntry,
    isSelected: Boolean,
    onToggle: () -> Unit,
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
        )
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
