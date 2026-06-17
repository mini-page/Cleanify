package com.cleanify.ui.screens.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.cleanify.R
import com.cleanify.ui.screens.settings.DetailedIndexingStatus
import com.cleanify.ui.screens.settings.SettingsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.NumberFormat

@Composable
fun MediaStorageSection(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val defaultVideoSpeed by viewModel.defaultVideoSpeed.collectAsStateWithLifecycle()
    val screenshotDeletesVideo by viewModel.screenshotDeletesVideo.collectAsStateWithLifecycle()
    val screenshotJpegQuality by viewModel.screenshotJpegQuality.collectAsStateWithLifecycle()
    val scanAudioEnabled by viewModel.scanAudioEnabled.collectAsStateWithLifecycle()
    val scanDocumentEnabled by viewModel.scanDocumentEnabled.collectAsStateWithLifecycle()
    val scanVideoEnabled by viewModel.scanVideoEnabled.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SectionHeader(R.string.indexing_section_header)
        MediaIndexingStatusItem(uiState.indexingStatus, uiState.isIndexingStatusLoading, uiState.isIndexing, viewModel::refreshIndexingStatus, viewModel::triggerFullScan, viewModel::showUnindexedFilesDialog)

        SectionHeader(R.string.scan_section_header)
        SettingSwitch(R.string.scan_audio_title, R.string.scan_audio_desc, scanAudioEnabled, viewModel::setScanAudioEnabled)
        SettingSwitch(R.string.scan_document_title, R.string.scan_document_desc, scanDocumentEnabled, viewModel::setScanDocumentEnabled)
        SettingSwitch(R.string.scan_video_title, R.string.scan_video_desc, scanVideoEnabled, viewModel::setScanVideoEnabled)

        SectionHeader(R.string.video_section_header)
        SettingsPickerItem(R.string.default_video_speed_title, R.string.default_video_speed_desc, listOf(1.0f, 1.5f, 2.0f), defaultVideoSpeed, { viewModel.setDefaultVideoSpeed(it) }, { s -> "${s}x" })
        SettingSwitch(R.string.screenshot_deletes_video_title, R.string.screenshot_deletes_video_desc, screenshotDeletesVideo, { viewModel.setScreenshotDeletesVideo(it) })
        SettingsPickerItem(R.string.screenshot_quality_title, R.string.screenshot_quality_desc, listOf("90", "75"), screenshotJpegQuality, { viewModel.setScreenshotJpegQuality(it) }, { q -> when (q) { "90" -> stringResource(R.string.quality_high); "75" -> stringResource(R.string.quality_low); else -> q } })

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun MediaIndexingStatusItem(status: DetailedIndexingStatus?, isStatusLoading: Boolean, isScanning: Boolean, onRefresh: () -> Unit, onScan: () -> Unit, onViewFiles: () -> Unit) {
    val statusText = when {
        isScanning -> stringResource(R.string.indexing_status_scanning)
        isStatusLoading -> stringResource(R.string.indexing_status_loading)
        status == null -> stringResource(R.string.indexing_status_initial)
        else -> {
            val pct = if (status.total > 0) (status.indexed.toDouble() / status.total * 100) else 100.0
            stringResource(R.string.indexing_status_format, NumberFormat.getInstance().format(status.indexed), NumberFormat.getInstance().format(status.total), String.format(java.util.Locale.US, "%.1f%%", pct))
        }
    }
    val supportingText = if (status != null && !isScanning && !isStatusLoading) {
        if (status.total > status.indexed) pluralStringResource(R.plurals.indexing_status_unindexed_count, status.unindexedUserFiles + status.unindexedHiddenFiles, status.unindexedUserFiles + status.unindexedHiddenFiles) + "\n" + stringResource(R.string.indexing_status_breakdown, status.unindexedUserFiles, status.unindexedHiddenFiles)
        else stringResource(R.string.indexing_status_all_indexed)
    } else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.media_indexing_status_title)) },
            supportingContent = {
                Column {
                    Text(statusText)
                    if (supportingText != null) {
                        val hasFiles = status != null && status.total > status.indexed
                        Spacer(Modifier.height(4.dp))
                        Text(supportingText, style = MaterialTheme.typography.bodySmall, color = if (hasFiles) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = if (hasFiles) Modifier.clickable { onViewFiles() } else Modifier)
                    }
                }
            },
            leadingContent = { if (isStatusLoading || isScanning) CircularProgressIndicator(Modifier.size(24.dp)) else Icon(Icons.Default.Info, contentDescription = null) },
            trailingContent = {
                IconButton(onClick = { if (status != null && status.total > status.indexed) onScan() else onRefresh() }, enabled = !isScanning && !isStatusLoading) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh_scan_icon_desc))
                }
            }
        )
    }
}
