package com.cleanify.ui.screens.tools

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cleanify.ui.components.BackNavigationIcon
import com.cleanify.data.cleaner.CleanerPreferences
import com.cleanify.ui.screens.settings.sections.FrostedSettingsGroup
import com.cleanify.ui.screens.settings.sections.ToolAboutCard
import kotlin.math.roundToInt
import org.json.JSONObject
import org.json.JSONTokener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerSettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToBlacklist: () -> Unit,
    onNavigateToWhitelist: () -> Unit
) {
    val ctx = LocalContext.current
    val prefs = remember { CleanerPreferences(ctx) }

    var cleanEmptyFile by remember { mutableStateOf(prefs.cleanEmptyFile) }
    var cleanEmptyFolder by remember { mutableStateOf(prefs.cleanEmptyFolder) }
    var cleanGeneric by remember { mutableStateOf(prefs.cleanGeneric) }
    var cleanApk by remember { mutableStateOf(prefs.cleanApk) }
    var cleanCorpse by remember { mutableStateOf(prefs.cleanCorpse) }
    var cleanClipboard by remember { mutableStateOf(prefs.cleanClipboard) }
    var cleanOnBoot by remember { mutableStateOf(prefs.cleanOnBoot) }
    var cleanEvery by remember { mutableStateOf(prefs.cleanEvery) }
    var stopBackgroundApps by remember { mutableStateOf(prefs.stopBackgroundApps) }
    var multiRun by remember { mutableStateOf(prefs.multiRun) }
    var autoWhite by remember { mutableStateOf(prefs.autoWhite) }

    var exportResult by remember { mutableStateOf<String?>(null) }
    var importResult by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            exportCleanerSettings(ctx, prefs, uri)
            exportResult = "Settings exported"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val result = importCleanerSettings(ctx, prefs, uri)
            importResult = result
            cleanEmptyFile = prefs.cleanEmptyFile
            cleanEmptyFolder = prefs.cleanEmptyFolder
            cleanGeneric = prefs.cleanGeneric
            cleanApk = prefs.cleanApk
            cleanCorpse = prefs.cleanCorpse
            cleanClipboard = prefs.cleanClipboard
            cleanOnBoot = prefs.cleanOnBoot
            cleanEvery = prefs.cleanEvery
            stopBackgroundApps = prefs.stopBackgroundApps
            multiRun = prefs.multiRun
            autoWhite = prefs.autoWhite
        }
    }

    LaunchedEffect(exportResult) {
        exportResult?.let { toast ->
            android.widget.Toast.makeText(ctx, toast, android.widget.Toast.LENGTH_SHORT).show()
            exportResult = null
        }
    }

    LaunchedEffect(importResult) {
        importResult?.let { toast ->
            android.widget.Toast.makeText(ctx, toast, android.widget.Toast.LENGTH_SHORT).show()
            importResult = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cleaner Settings") },
                navigationIcon = { BackNavigationIcon(onClick = onNavigateUp) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Scan Filters ──
            FrostedSettingsGroup(title = "Scan Filters") {
                CleanerSwitchRow(
                    title = "Empty Files (0 bytes)",
                    subtitle = "Delete files with size 0",
                    checked = cleanEmptyFile,
                    onCheckedChange = { v -> cleanEmptyFile = v; prefs.cleanEmptyFile = v }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
                CleanerSwitchRow(
                    title = "Empty Folders",
                    subtitle = "Delete directories containing no files",
                    checked = cleanEmptyFolder,
                    onCheckedChange = { v -> cleanEmptyFolder = v; prefs.cleanEmptyFolder = v }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
                CleanerSwitchRow(
                    title = "Generic Junk",
                    subtitle = "Delete .tmp, .log files and temporary folders",
                    checked = cleanGeneric,
                    onCheckedChange = { v -> cleanGeneric = v; prefs.cleanGeneric = v }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
                CleanerSwitchRow(
                    title = "APK Packages",
                    subtitle = "Delete cached .apk, .apks, .apkm, .aab files",
                    checked = cleanApk,
                    onCheckedChange = { v -> cleanApk = v; prefs.cleanApk = v }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
                CleanerSwitchRow(
                    title = "Corpse Files",
                    subtitle = "Delete orphaned app data folders in Android/data",
                    checked = cleanCorpse,
                    onCheckedChange = { v -> cleanCorpse = v; prefs.cleanCorpse = v }
                )
            }

            // ── Scan Behavior & Automation ──
            FrostedSettingsGroup(title = "Scan Behavior & Automation") {
                CleanerSliderRow(
                    title = "Double-checker (multi-run)",
                    subtitle = "Repeat scan passes: $multiRun time${if (multiRun != 1) "s" else ""}",
                    value = multiRun.toFloat(),
                    onValueChange = { v ->
                        val stepped = (v / 1f).roundToInt().coerceIn(1, 10)
                        multiRun = stepped
                        prefs.multiRun = stepped
                    },
                    valueRange = 1f..10f,
                    steps = 8,
                    startLabel = "1x",
                    endLabel = "10x"
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
                CleanerSwitchRow(
                    title = "Auto-whitelist",
                    subtitle = "Auto-whitelist paths containing 'backup', 'important', etc.",
                    checked = autoWhite,
                    onCheckedChange = { v -> autoWhite = v; prefs.autoWhite = v }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
                CleanerSwitchRow(
                    title = "Clear Clipboard",
                    subtitle = "Clear system clipboard after each clean",
                    checked = cleanClipboard,
                    onCheckedChange = { v -> cleanClipboard = v; prefs.cleanClipboard = v }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
                CleanerSwitchRow(
                    title = "Clean on Boot",
                    subtitle = "Run a full clean automatically after device restart",
                    checked = cleanOnBoot,
                    onCheckedChange = { v -> cleanOnBoot = v; prefs.cleanOnBoot = v }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
                CleanerSliderRow(
                    title = "Scheduled Clean",
                    subtitle = if (cleanEvery <= 0) "Disabled" else "Every $cleanEvery hour${if (cleanEvery != 1) "s" else ""}",
                    value = if (cleanEvery <= 0) 0f else cleanEvery.toFloat(),
                    onValueChange = { v ->
                        val stepped = v.roundToInt().coerceIn(0, 24)
                        cleanEvery = stepped
                        prefs.cleanEvery = stepped
                        if (stepped > 0) {
                            com.cleanify.data.cleaner.CleanerWorker.scheduleHourlyClean(ctx, stepped)
                        } else {
                            com.cleanify.data.cleaner.CleanerWorker.cancelHourlyClean(ctx)
                        }
                    },
                    valueRange = 0f..24f,
                    steps = 23,
                    startLabel = "Off",
                    endLabel = "24h"
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
                CleanerSwitchRow(
                    title = "Stop Background Apps",
                    subtitle = "Kill background processes of non-system apps after clean",
                    checked = stopBackgroundApps,
                    onCheckedChange = { v -> stopBackgroundApps = v; prefs.stopBackgroundApps = v }
                )
            }

            // ── Filter Lists ──
            FrostedSettingsGroup(title = "Filter Lists") {
                CleanerNavigationRow(
                    title = "Blacklist",
                    subtitle = "Paths and patterns excluded from cleaning",
                    icon = Icons.Default.Block,
                    onClick = onNavigateToBlacklist
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
                CleanerNavigationRow(
                    title = "Whitelist",
                    subtitle = "Paths protected from cleaning",
                    icon = Icons.Default.CheckCircle,
                    onClick = onNavigateToWhitelist
                )
            }

            // ── Import / Export ──
            FrostedSettingsGroup(title = "Import / Export") {
                CleanerNavigationRow(
                    title = "Export Cleaner Settings",
                    subtitle = "Save all cleaner preferences to a JSON file",
                    icon = Icons.Default.FileUpload,
                    onClick = { exportLauncher.launch("cleaner_settings.json") }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )
                CleanerNavigationRow(
                    title = "Import Cleaner Settings",
                    subtitle = "Restore cleaner preferences from a JSON file",
                    icon = Icons.Default.FileDownload,
                    onClick = { importLauncher.launch(arrayOf("application/json")) }
                )
            }

            // ── About Tool ──
            FrostedSettingsGroup(title = "About Empty Cleaner") {
                ToolAboutCard(
                    icon = Icons.Default.CleaningServices,
                    title = "Empty Cleaner",
                    description = "The Empty File Cleaner scans your device for junk files — empty files, empty folders, temporary files, APKs, and orphaned app data. It supports scheduled cleanups, auto-whitelisting, blacklist/whitelist filters, and can stop background apps after cleaning.",
                    warning = "Files deleted during cleaning cannot be recovered. Be sure to check your whitelist before running deep scans."
                )
            }
        }
    }
}

private fun exportCleanerSettings(ctx: android.content.Context, prefs: CleanerPreferences, uri: Uri) {
    try {
        val json = JSONObject().apply {
            put("cleanEmptyFile", prefs.cleanEmptyFile)
            put("cleanEmptyFolder", prefs.cleanEmptyFolder)
            put("cleanGeneric", prefs.cleanGeneric)
            put("cleanApk", prefs.cleanApk)
            put("cleanCorpse", prefs.cleanCorpse)
            put("cleanClipboard", prefs.cleanClipboard)
            put("cleanOnBoot", prefs.cleanOnBoot)
            put("cleanEvery", prefs.cleanEvery)
            put("stopBackgroundApps", prefs.stopBackgroundApps)
            put("multiRun", prefs.multiRun)
            put("autoWhite", prefs.autoWhite)
            put("whitelist", prefs.whitelistOn.toList())
            put("blacklist", prefs.blacklistOn.toList())
        }
        ctx.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(json.toString(2).toByteArray())
        }
    } catch (_: Exception) {}
}

private fun importCleanerSettings(ctx: android.content.Context, prefs: CleanerPreferences, uri: Uri): String {
    return try {
        val jsonStr = ctx.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().readText()
        } ?: return "Import failed: empty file"

        val json = JSONTokener(jsonStr).nextValue() as? JSONObject ?: return "Import failed: invalid JSON"

        if (json.has("cleanEmptyFile")) prefs.cleanEmptyFile = json.getBoolean("cleanEmptyFile")
        if (json.has("cleanEmptyFolder")) prefs.cleanEmptyFolder = json.getBoolean("cleanEmptyFolder")
        if (json.has("cleanGeneric")) prefs.cleanGeneric = json.getBoolean("cleanGeneric")
        if (json.has("cleanApk")) prefs.cleanApk = json.getBoolean("cleanApk")
        if (json.has("cleanCorpse")) prefs.cleanCorpse = json.getBoolean("cleanCorpse")
        if (json.has("cleanClipboard")) prefs.cleanClipboard = json.getBoolean("cleanClipboard")
        if (json.has("cleanOnBoot")) prefs.cleanOnBoot = json.getBoolean("cleanOnBoot")
        if (json.has("cleanEvery")) prefs.cleanEvery = json.getInt("cleanEvery")
        if (json.has("stopBackgroundApps")) prefs.stopBackgroundApps = json.getBoolean("stopBackgroundApps")
        if (json.has("multiRun")) prefs.multiRun = json.getInt("multiRun")
        if (json.has("autoWhite")) prefs.autoWhite = json.getBoolean("autoWhite")
        if (json.has("whitelist")) {
            val list = json.getJSONArray("whitelist")
            val set = mutableSetOf<String>()
            for (i in 0 until list.length()) set.add(list.getString(i))
            prefs.whitelistOn = set
            prefs.whitelist = set
        }
        if (json.has("blacklist")) {
            val list = json.getJSONArray("blacklist")
            val set = mutableSetOf<String>()
            for (i in 0 until list.length()) set.add(list.getString(i))
            prefs.blacklistOn = set
            prefs.blacklist = set
        }

        "Settings imported"
    } catch (e: Exception) {
        "Import failed: ${e.message}"
    }
}

@Composable
private fun CleanerSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CleanerNavigationRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun CleanerSliderRow(
    title: String,
    subtitle: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    startLabel: String,
    endLabel: String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(startLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(endLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
