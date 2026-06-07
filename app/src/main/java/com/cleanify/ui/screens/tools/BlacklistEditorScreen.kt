package com.cleanify.ui.screens.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cleanify.data.cleaner.CleanerPreferences
import com.cleanify.ui.components.BackNavigationIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlacklistEditorScreen(onNavigateUp: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { CleanerPreferences(ctx) }
    var blacklist by remember { mutableStateOf(prefs.blacklist.toList().sorted()) }
    var blacklistOn by remember { mutableStateOf(prefs.blacklistOn) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    fun save() {
        prefs.blacklist = blacklist.toSet()
        prefs.blacklistOn = blacklistOn
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blacklist Editor") },
                navigationIcon = { BackNavigationIcon(onClick = onNavigateUp) },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add pattern")
                    }
                }
            )
        }
    ) { padding ->
        if (blacklist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No blacklist patterns",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                itemsIndexed(blacklist) { index, pattern ->
                    val enabled = pattern in blacklistOn
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
                                    text = pattern,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Switch(
                                checked = enabled,
                                onCheckedChange = { v ->
                                    blacklistOn = if (v) blacklistOn + pattern else blacklistOn - pattern
                                    save()
                                }
                            )
                            IconButton(onClick = { showDeleteConfirm = pattern }) {
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

    if (showAddDialog) {
        var newPattern by remember { mutableStateOf("") }
        val isDangerous = remember(newPattern) {
            newPattern.contains("Android/data", ignoreCase = true) ||
            newPattern.contains("DCIM/Camera", ignoreCase = true)
        }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Blacklist Pattern") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPattern,
                        onValueChange = { newPattern = it },
                        singleLine = true,
                        label = { Text("Regex pattern or path") },
                        placeholder = { Text("e.g. .*\\.log$") }
                    )
                    if (isDangerous) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "Warning: This pattern matches a critical system path. Adding it to the blacklist may cause apps or the system to behave incorrectly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPattern.isNotBlank()) {
                        blacklist = (blacklist + newPattern).sorted()
                        blacklistOn = blacklistOn + newPattern
                        save()
                        showAddDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    showDeleteConfirm?.let { pattern ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Remove Pattern") },
            text = { Text("Remove this blacklist pattern?\n\n$pattern") },
            confirmButton = {
                TextButton(onClick = {
                    blacklist = blacklist - pattern
                    blacklistOn = blacklistOn - pattern
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
