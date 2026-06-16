package com.cleanify.ui.screens.settings.sections

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.cleanify.R
import com.cleanify.ui.screens.settings.SettingsViewModel
import com.cleanify.util.UpdateChecker
import com.cleanify.util.UpdateCheckState
import com.cleanify.util.UpdateInfo
import kotlinx.coroutines.launch

@Composable
fun AboutSection(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToLibraries: () -> Unit = {},
    onShowFunding: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateState by remember { mutableStateOf<UpdateCheckState>(UpdateCheckState.Idle) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(16.dp))
            Text("Cleanify", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("v${viewModel.appVersion}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        HorizontalDivider()

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.features_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                val features = listOf(
                    R.string.feature_swipe,
                    R.string.feature_duplicates,
                    R.string.feature_cleaner,
                    R.string.feature_contacts,
                    R.string.feature_recycle,
                    R.string.feature_themes,
                    R.string.feature_language,
                    R.string.feature_offline,
                    R.string.feature_opensource
                )
                features.forEach { res ->
                    Row(modifier = Modifier.padding(vertical = 3.dp)) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(res),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        UpdateCard(
            state = updateState,
            appVersion = viewModel.appVersion,
            onCheck = {
                scope.launch {
                    updateState = UpdateCheckState.Checking
                    try {
                        val result = UpdateChecker.checkForUpdate(viewModel.appVersion)
                        updateState = if (result != null) {
                            UpdateCheckState.Available(result)
                        } else {
                            UpdateCheckState.UpToDate
                        }
                    } catch (e: Exception) {
                        updateState = UpdateCheckState.Error(e.message ?: "Update check failed")
                    }
                }
            },
            onDismiss = { updateState = UpdateCheckState.Idle }
        )

        when (val state = updateState) {
            is UpdateCheckState.Available -> UpdateAvailableDialog(
                info = state.info,
                onDownload = {
                    updateState = UpdateCheckState.Downloading(0f)
                    scope.launch {
                        try {
                            UpdateChecker.downloadApk(
                                context,
                                state.info.downloadUrl,
                                state.info.tagName
                            ).collect { progress ->
                                updateState = UpdateCheckState.Downloading(progress)
                            }
                            val file = java.io.File(context.cacheDir, "Cleanify-${state.info.tagName}.apk")
                            updateState = UpdateCheckState.Downloaded(file)
                        } catch (e: Exception) {
                            updateState = UpdateCheckState.Error(e.message ?: "Download failed")
                        }
                    }
                },
                onDismiss = { updateState = UpdateCheckState.Idle }
            )
            is UpdateCheckState.Downloading -> DownloadProgressDialog(
                progress = state.progress,
                tagName = (updateState as? UpdateCheckState.Available)?.info?.tagName ?: "",
                onDismiss = { updateState = UpdateCheckState.Idle }
            )
            is UpdateCheckState.Downloaded -> InstallDialog(
                onInstall = { UpdateChecker.installApk(context, state.file) },
                onDismiss = { updateState = UpdateCheckState.Idle }
            )
            else -> {}
        }

        Text(stringResource(R.string.social_section_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        val socialLinks = remember {
            listOf(
                Triple(Icons.Default.Star, "Star Repo", "https://github.com/mini-page/Cleanify"),
                Triple(Icons.Default.Code, "Developer", "https://mini-page.github.io/ugsoc"),
                Triple(Icons.Default.BusinessCenter, "LinkedIn", "https://www.linkedin.com/in/ug5711"),
                Triple(Icons.Default.CameraAlt, "Instagram", "https://www.instagram.com/ug_5711"),
                Triple(Icons.AutoMirrored.Filled.Send, "Telegram", "https://t.me/ug_5711"),
                Triple(Icons.Default.Mood, "Snapchat", "https://www.snapchat.com/add/rg_5711"),
                Triple(Icons.Default.Email, "Email", "mailto:raghavans5711+Support@gmail.com")
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(socialLinks.size) { index ->
                val (icon, label, url) = socialLinks[index]
                Surface(
                    onClick = { uriHandler.openUri(url) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(
                        modifier = Modifier.size(88.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon,
                                    contentDescription = label,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onShowFunding)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.support_development_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.support_development_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }

        SettingsItem(
            title = stringResource(R.string.privacy_policy_title),
            summary = stringResource(R.string.privacy_policy_desc),
            onClick = { uriHandler.openUri("https://home-cleanify.vercel.app/privacy.html") }
        )

        SettingsItem(
            title = stringResource(R.string.terms_of_use_title),
            summary = stringResource(R.string.terms_of_use_desc),
            onClick = { uriHandler.openUri("https://home-cleanify.vercel.app/terms.html") }
        )

        SettingsItem(stringResource(R.string.open_source_licenses_title), stringResource(R.string.open_source_licenses_desc), onClick = onNavigateToLibraries)

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun UpdateCard(
    state: UpdateCheckState,
    appVersion: String,
    onCheck: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Text("Updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            when (state) {
                is UpdateCheckState.Idle -> {
                    Text("Check for new versions of Cleanify.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onCheck, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Check for Updates")
                    }
                }
                is UpdateCheckState.Checking -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Checking for updates\u2026", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is UpdateCheckState.UpToDate -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Cleanify v$appVersion is up to date.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = onDismiss) { Text("Dismiss") }
                    }
                }
                is UpdateCheckState.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Update check failed", style = MaterialTheme.typography.bodyMedium)
                            Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = onDismiss) { Text("Dismiss") }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun UpdateAvailableDialog(
    info: UpdateInfo,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
        title = { Text("Update Available") },
        text = {
            Column {
                Text("v${info.versionName}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(info.tagName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (info.changelog.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    Text("What's new:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(info.changelog, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDownload) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        }
    )
}

@Composable
private fun DownloadProgressDialog(
    progress: Float,
    tagName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Downloading Update") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$tagName", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun InstallDialog(
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Download Complete") },
        text = { Text("The update has been downloaded. Install it now to get the latest features and fixes.") },
        confirmButton = {
            Button(onClick = onInstall) {
                Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Install")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        }
    )
}
