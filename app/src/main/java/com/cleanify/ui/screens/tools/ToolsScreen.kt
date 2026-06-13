package com.cleanify.ui.screens.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ControlPointDuplicate
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cleanify.ui.components.BackNavigationIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToDuplicates: () -> Unit,
    onNavigateToEmptyCleaner: () -> Unit,
    onNavigateToRecycleBin: () -> Unit,
    onNavigateToContactCleaner: () -> Unit = {},
    onNavigateToStorageAnalysis: () -> Unit = {}
) {
    val context = LocalContext.current
    var showContactPermissionDialog by remember { mutableStateOf(false) }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            onNavigateToContactCleaner()
        } else {
            showContactPermissionDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tools") },
                navigationIcon = { BackNavigationIcon(onClick = onNavigateUp) }
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
            ToolCard(
                icon = { Icon(Icons.Default.ControlPointDuplicate, contentDescription = null, modifier = Modifier.size(32.dp)) },
                title = "Duplicate Finder",
                description = "Find and remove exact and similar duplicate files",
                onClick = onNavigateToDuplicates
            )
            ToolCard(
                icon = { Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(32.dp)) },
                title = "Empty Cleaner",
                description = "Scan and delete empty files and empty folders across your device",
                onClick = onNavigateToEmptyCleaner
            )
            ToolCard(
                icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(32.dp)) },
                title = "Recycle Bin",
                description = "Restore or permanently delete files you previously removed",
                onClick = onNavigateToRecycleBin
            )
            ToolCard(
                icon = { Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(32.dp)) },
                title = "Contact Cleaner",
                description = "Manage contacts, merge duplicates, and fix contact issues",
                onClick = {
                    val hasRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                    val hasWrite = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED
                    if (hasRead && hasWrite) {
                        onNavigateToContactCleaner()
                    } else {
                        contactPermissionLauncher.launch(
                            arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
                        )
                    }
                }
            )
            ToolCard(
                icon = { Icon(Icons.Outlined.Storage, contentDescription = null, modifier = Modifier.size(32.dp)) },
                title = "Storage Analysis",
                description = "View device storage usage and find the largest files",
                onClick = onNavigateToStorageAnalysis
            )
        }
    }

    if (showContactPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showContactPermissionDialog = false },
            title = { Text("Permission Required") },
            text = { Text("Contact Cleaner needs access to your contacts to find duplicates and fix issues. Please grant the permission in Settings.") },
            confirmButton = {
                TextButton(onClick = {
                    showContactPermissionDialog = false
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showContactPermissionDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ToolCard(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    icon()
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
