package com.cleanify.ui.screens.settings.sections

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.cleanify.R
import com.cleanify.ui.screens.settings.SettingsViewModel
import com.cleanify.util.PermissionManager

@Composable
fun HelpSection(
    viewModel: SettingsViewModel = hiltViewModel(),
    exportFavoritesLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    importFavoritesLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    defaultExportFilename: String
) {
    val context = LocalContext.current
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> /* re-composition handles refreshed state */ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SectionHeader(R.string.onboarding_section_header)
        Column {
            Text(stringResource(R.string.onboarding_tutorial_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.onboarding_tutorial_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { viewModel.resetOnboarding() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.replay_tutorial_button)) }
        }

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        SectionHeader(R.string.data_section_header)

        SettingsItem(stringResource(R.string.export_target_favorites_title), stringResource(R.string.export_target_favorites_desc), onClick = { exportFavoritesLauncher.launch(defaultExportFilename) })
        SettingsItem(stringResource(R.string.import_target_favorites_title), stringResource(R.string.import_target_favorites_desc), onClick = { importFavoritesLauncher.launch(arrayOf("application/json", "text/plain")) })

        Card(
            modifier = Modifier.fillMaxWidth().clickable { viewModel.resetDialogWarnings() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.reset_dialog_warnings_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.reset_dialog_warnings_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        SectionHeader(R.string.permission_manager_header)

        val permissionEntries = remember {
            listOf(
                PermissionEntry(
                    name = "All Files Access",
                    desc = "Read and manage files on device storage",
                    icon = Icons.Default.Storage,
                    isGranted = PermissionManager.hasAllFilesAccess(),
                    settingsAction = PermissionManager::createAllFilesAccessIntent
                ),
                PermissionEntry(
                    name = "Notifications",
                    desc = "Show scan progress notifications",
                    icon = Icons.Default.Notifications,
                    isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    else true,
                    requestPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        Manifest.permission.POST_NOTIFICATIONS else null
                ),
                PermissionEntry(
                    name = "Contacts (Read)",
                    desc = "Scan and clean duplicate contacts",
                    icon = Icons.Default.Contacts,
                    isGranted = checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED,
                    requestPermission = Manifest.permission.READ_CONTACTS
                ),
                PermissionEntry(
                    name = "Contacts (Write)",
                    desc = "Merge and update cleaned contacts",
                    icon = Icons.Default.Edit,
                    isGranted = checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED,
                    requestPermission = Manifest.permission.WRITE_CONTACTS
                ),
                PermissionEntry(
                    name = "Foreground Service",
                    desc = "Run duplicate scans in the background",
                    icon = Icons.Default.History,
                    isGranted = true,
                    grantedNote = "Auto-granted"
                ),
                PermissionEntry(
                    name = "Boot Completed",
                    desc = "Run cleaner automatically after device restart",
                    icon = Icons.Default.RestartAlt,
                    isGranted = true,
                    grantedNote = "Auto-granted"
                ),
                PermissionEntry(
                    name = "Query All Packages",
                    desc = "Detect orphaned folders from uninstalled apps",
                    icon = Icons.Default.Apps,
                    isGranted = true,
                    grantedNote = "Auto-granted"
                )
            )
        }

        permissionEntries.forEach { entry ->
            PermissionRow(
                entry = entry,
                context = context,
                onRequest = { perm -> requestPermissionLauncher.launch(perm) },
                onOpenSettings = { intent -> intent?.let { context.startActivity(it) } }
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PermissionRow(
    entry: PermissionEntry,
    context: Context,
    onRequest: (String) -> Unit,
    onOpenSettings: (Intent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (entry.isGranted) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (entry.isGranted) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                Text(entry.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (entry.isGranted) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = if (entry.isGranted) (entry.grantedNote ?: "Granted") else "Denied",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.isGranted) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            if (!entry.isGranted) {
                if (entry.settingsAction != null) {
                    FilledTonalButton(
                        onClick = { entry.settingsAction(context)?.let(onOpenSettings) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Settings", style = MaterialTheme.typography.labelMedium)
                    }
                } else if (entry.requestPermission != null) {
                    FilledTonalButton(
                        onClick = { onRequest(entry.requestPermission) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Grant", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

data class PermissionEntry(
    val name: String,
    val desc: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isGranted: Boolean,
    val settingsAction: ((Context) -> Intent?)? = null,
    val requestPermission: String? = null,
    val grantedNote: String? = null
)

private fun checkSelfPermission(context: Context, permission: String): Int {
    return androidx.core.content.ContextCompat.checkSelfPermission(context, permission)
}
