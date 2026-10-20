/*
 * Cleanify
 * Copyright (c) 2025 LoopOtto
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.cleanify.ui.screens.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import com.cleanify.R
import com.cleanify.data.repository.UpdateCheckInterval
import com.cleanify.ui.components.FeedbackDialog
import com.cleanify.ui.components.MarkdownViewer
import com.cleanify.ui.screens.settings.SettingsViewModel
import com.cleanify.util.UpdateChecker
import com.cleanify.util.UpdateCheckState
import com.cleanify.util.UpdateInfo
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch

data class InstalledPaymentApp(
    val name: String,
    val packageName: String,
    val iconBitmap: ImageBitmap?
)

data class SocialBrandItem(
    val icon: ImageVector,
    val label: String,
    val url: String,
    val brandColor: Color
)

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
    var showFeedbackDialog by remember { mutableStateOf(false) }
    val currentInterval by viewModel.updateCheckInterval.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // App Identity Header Card
        FrostedSettingsGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(88.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.mipmap.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Cleanify",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Version ${viewModel.appVersion}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Smart, Private, Offline Storage Decluttering",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 2. Support Development Card directly below App Card
        val clipboard = androidx.compose.ui.platform.LocalClipboard.current
        val installedUpiApps = remember {
            val upiIntent = Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay"))
            try {
                val pm = context.packageManager
                pm.queryIntentActivities(upiIntent, PackageManager.MATCH_DEFAULT_ONLY).mapNotNull { resolveInfo ->
                    val pkg = resolveInfo.activityInfo.packageName
                    val name = resolveInfo.loadLabel(pm).toString()
                    val iconBitmap = try {
                        val drawable = resolveInfo.loadIcon(pm)
                        drawable.toBitmap(width = 96, height = 96).asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                    InstalledPaymentApp(name = name, packageName = pkg, iconBitmap = iconBitmap)
                }.distinctBy { it.packageName }
            } catch (e: Exception) {
                emptyList()
            }
        }

        val launchUpi: (String?) -> Unit = { targetPackage ->
            val uri = Uri.parse("upi://pay?pa=raghavans5711@okicici&pn=Raghav%20S&mc=&tid=&tr=&tn=Support%20Cleanify%20Development&cu=INR")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (targetPackage != null) {
                intent.setPackage(targetPackage)
            }
            try {
                context.startActivity(if (targetPackage != null) intent else Intent.createChooser(intent, "Pay via UPI"))
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Could not open payment app: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        FrostedSettingsGroup(title = "Support Development") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.support_development_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Support continuous open-source development",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Compact QR Code Container (without outer padding or header text)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    shadowElevation = 2.dp
                ) {
                    Image(
                        painter = painterResource(R.drawable.payment_qr),
                        contentDescription = "Payment QR",
                        modifier = Modifier
                            .size(190.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Understated Name & UPI ID Container with Dedicated Copy Button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Raghav S",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "raghavans5711@okicici",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = {
                                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                cm.setPrimaryClip(android.content.ClipData.newPlainText("UPI ID", "raghavans5711@okicici"))
                                android.widget.Toast.makeText(context, "UPI ID copied: raghavans5711@okicici", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Copy", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Installed Payment Apps as clean consistent rows
                if (installedUpiApps.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Pay with Installed App",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.height(6.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        installedUpiApps.forEach { app ->
                            Surface(
                                onClick = { launchUpi(app.packageName) },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (app.iconBitmap != null) {
                                        Image(
                                            bitmap = app.iconBitmap,
                                            contentDescription = app.name,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Payment,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Text(
                                        text = app.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Open ${app.name}",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Updates Card with Manual Check & Frequency Selector
        UpdateCard(
            state = updateState,
            appVersion = viewModel.appVersion,
            currentInterval = currentInterval,
            onIntervalChanged = viewModel::setUpdateCheckInterval,
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

        // Features & Highlights Card
        FrostedSettingsGroup(title = "Features & Capabilities") {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.features_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(14.dp))
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(res),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Community & Social Links with brand styling
        Column {
            Text(
                text = stringResource(R.string.social_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            val socialLinks = remember {
                listOf(
                    SocialBrandItem(Icons.Default.Star, "GitHub", "https://github.com/mini-page/Cleanify", Color(0xFF24292E)),
                    SocialBrandItem(Icons.Default.Code, "Developer", "https://mini-page.github.io/ugsoc", Color(0xFF10B981)),
                    SocialBrandItem(Icons.AutoMirrored.Filled.Send, "Telegram", "https://t.me/ug_5711", Color(0xFF0088CC)),
                    SocialBrandItem(Icons.Default.BusinessCenter, "LinkedIn", "https://www.linkedin.com/in/ug5711", Color(0xFF0A66C2)),
                    SocialBrandItem(Icons.Default.CameraAlt, "Instagram", "https://www.instagram.com/ug_5711", Color(0xFFE1306C)),
                    SocialBrandItem(Icons.Default.Email, "Feedback", "feedback_action", Color(0xFF6366F1))
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(socialLinks.size) { index ->
                    val item = socialLinks[index]
                    Surface(
                        onClick = {
                            if (item.url == "feedback_action") {
                                showFeedbackDialog = true
                            } else {
                                uriHandler.openUri(item.url)
                            }
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(
                            modifier = Modifier
                                .size(width = 86.dp, height = 82.dp)
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = item.brandColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(20.dp),
                                        tint = item.brandColor
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Legal & Transparency Grouped Card
        FrostedSettingsGroup(title = "Legal & Open Source") {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                SettingsItemRow(
                    icon = Icons.Default.Gavel,
                    title = stringResource(R.string.open_source_licenses_title),
                    summary = stringResource(R.string.open_source_licenses_desc),
                    onClick = onNavigateToLibraries
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsItemRow(
                    icon = Icons.Default.PrivacyTip,
                    title = stringResource(R.string.privacy_policy_title),
                    summary = stringResource(R.string.privacy_policy_desc),
                    onClick = { uriHandler.openUri("https://home-cleanify.vercel.app/privacy.html") }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsItemRow(
                    icon = Icons.Default.Description,
                    title = stringResource(R.string.terms_of_use_title),
                    summary = stringResource(R.string.terms_of_use_desc),
                    onClick = { uriHandler.openUri("https://home-cleanify.vercel.app/terms.html") }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // Dialogs for update lifecycle
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

    if (showFeedbackDialog) {
        FeedbackDialog(
            appVersion = viewModel.appVersion,
            onDismiss = { showFeedbackDialog = false }
        )
    }
}

@Composable
private fun SettingsItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun UpdateCard(
    state: UpdateCheckState,
    appVersion: String,
    currentInterval: UpdateCheckInterval,
    onIntervalChanged: (UpdateCheckInterval) -> Unit,
    onCheck: () -> Unit,
    onDismiss: () -> Unit
) {
    FrostedSettingsGroup(title = "Updates") {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Latest release & background checking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(14.dp))

            when (state) {
                is UpdateCheckState.Idle -> {
                    OutlinedButton(
                        onClick = onCheck,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Check for Updates Now")
                    }
                }
                is UpdateCheckState.Checking -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Checking GitHub for updates\u2026", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is UpdateCheckState.UpToDate -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Cleanify v$appVersion is up to date.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = onDismiss) { Text("Dismiss") }
                    }
                }
                is UpdateCheckState.Error -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Update check failed", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = onDismiss) { Text("Dismiss") }
                    }
                }
                else -> {}
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(Modifier.height(12.dp))

            // Auto-check interval slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.auto_update_check_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            stringResource(R.string.auto_update_check_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (currentInterval.isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = currentInterval.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (currentInterval.isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                var sliderPosition by remember(currentInterval) {
                    mutableFloatStateOf(UpdateCheckInterval.toSliderValue(currentInterval))
                }

                Slider(
                    value = sliderPosition,
                    onValueChange = { pos ->
                        sliderPosition = pos
                        val selected = UpdateCheckInterval.fromSliderValue(pos)
                        if (selected != currentInterval) {
                            onIntervalChanged(selected)
                        }
                    },
                    valueRange = 0f..(UpdateCheckInterval.entries.size - 1).toFloat(),
                    steps = UpdateCheckInterval.entries.size - 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    UpdateCheckInterval.entries.forEach { interval ->
                        Text(
                            text = interval.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (interval == currentInterval) FontWeight.Bold else FontWeight.Normal,
                            color = if (interval == currentInterval) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
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
        icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Update Available", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("v${info.versionName}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "New Release",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                if (info.changelog.isNotBlank()) {
                    Text("What's New:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(12.dp)
                    ) {
                        MarkdownViewer(
                            markdown = info.changelog,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Text("A new version of Cleanify is available for download.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDownload) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Download & Install")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
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
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Downloading Cleanify $tagName") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
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
        text = { Text("The update has been downloaded. Tap Install to proceed.") },
        confirmButton = {
            Button(onClick = {
                onInstall()
                onDismiss()
            }) {
                Text("Install")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        }
    )
}
