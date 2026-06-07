package com.cleanify.ui.screens.tools

import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cleanify.data.cleaner.CleanerPreferences
import com.cleanify.ui.components.BackNavigationIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistEditorScreen(onNavigateUp: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { CleanerPreferences(ctx) }
    var whitelist by remember { mutableStateOf(prefs.whitelist.toList().sorted()) }
    var whitelistOn by remember { mutableStateOf(prefs.whitelistOn) }
    var showManualEntryDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    fun save() {
        prefs.whitelist = whitelist.toSet()
        prefs.whitelistOn = whitelistOn
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val path = getPathFromUri(uri)
            if (path != null && path !in whitelist) {
                whitelist = (whitelist + path).sorted()
                whitelistOn = whitelistOn + path
                save()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Whitelist Editor") },
                navigationIcon = { BackNavigationIcon(onClick = onNavigateUp) },
                actions = {
                    IconButton(onClick = { folderPickerLauncher.launch(null) }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Browse folder")
                    }
                    IconButton(onClick = { showManualEntryDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Manual entry")
                    }
                }
            )
        }
    ) { padding ->
        if (whitelist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No whitelist paths",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Tap folder icon to browse or + to type a path",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(whitelist) { index, path ->
                    val enabled = path in whitelistOn
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (enabled) MaterialTheme.colorScheme.surfaceContainerHigh
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = path,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Switch(
                                checked = enabled,
                                onCheckedChange = { v ->
                                    whitelistOn = if (v) whitelistOn + path else whitelistOn - path
                                    save()
                                }
                            )
                            IconButton(onClick = { showDeleteConfirm = path }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManualEntryDialog) {
        var newPath by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showManualEntryDialog = false },
            title = { Text("Add Whitelist Path") },
            text = {
                OutlinedTextField(
                    value = newPath,
                    onValueChange = { newPath = it },
                    singleLine = true,
                    label = { Text("Path to whitelist") },
                    placeholder = { Text("e.g. /storage/emulated/0/Important") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPath.isNotBlank()) {
                        whitelist = (whitelist + newPath).sorted()
                        whitelistOn = whitelistOn + newPath
                        save()
                        showManualEntryDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showManualEntryDialog = false }) { Text("Cancel") }
            }
        )
    }

    showDeleteConfirm?.let { path ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Remove Path") },
            text = { Text("Remove this whitelist path?\n\n$path") },
            confirmButton = {
                TextButton(onClick = {
                    whitelist = whitelist - path
                    whitelistOn = whitelistOn - path
                    save()
                    showDeleteConfirm = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
            }
        )
    }
}

private fun getPathFromUri(uri: android.net.Uri): String? {
    return try {
        val docId = DocumentsContract.getTreeDocumentId(uri)
        // Format: "primary:DCIM/Camera" or "XXXX-XXXX:path"
        val parts = docId.split(":")
        val path = if (parts.size >= 2) parts[1] else ""
        when {
            parts[0].equals("primary", ignoreCase = true) -> "/storage/emulated/0/$path"
            parts[0].startsWith("home", ignoreCase = true) -> "/storage/emulated/0/$path"
            else -> "/storage/$docId"
        }
    } catch (e: Exception) {
        uri.path
    }
}
