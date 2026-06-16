package com.cleanify.ui.screens.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.cleanify.R
import com.cleanify.data.repository.DuplicateScanScope
import com.cleanify.data.repository.SimilarityThresholdLevel
import com.cleanify.ui.components.AppDialog
import com.cleanify.ui.screens.settings.SettingsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DuplicatesSection(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val similarityThresholdLevel by viewModel.similarityThresholdLevel.collectAsStateWithLifecycle()
    val duplicateScanScope by viewModel.duplicateScanScope.collectAsStateWithLifecycle()
    val duplicateScanIncludeList by viewModel.duplicateScanIncludeList.collectAsStateWithLifecycle()
    val duplicateScanExcludeList by viewModel.duplicateScanExcludeList.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsPickerItem(R.string.similarity_level_title, getSimilarityLevelDescriptionRes(similarityThresholdLevel), SimilarityThresholdLevel.entries, similarityThresholdLevel, { viewModel.setSimilarityThresholdLevel(it) }, ::getSimilarityLevelDisplayName)
        SettingsPickerItem(R.string.scan_scope_title, getScanScopeDescription(duplicateScanScope, duplicateScanIncludeList, duplicateScanExcludeList), DuplicateScanScope.entries, duplicateScanScope, { viewModel.setDuplicateScanScope(it) }, ::getScanScopeDisplayName)
        AnimatedVisibility(visible = duplicateScanScope != DuplicateScanScope.ALL_FILES, enter = fadeIn(), exit = fadeOut()) {
            val title = if (duplicateScanScope == DuplicateScanScope.INCLUDE_LIST) stringResource(R.string.manage_include_list) else stringResource(R.string.manage_exclude_list)
            val listSize = if (duplicateScanScope == DuplicateScanScope.INCLUDE_LIST) duplicateScanIncludeList.size else duplicateScanExcludeList.size
            Column {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { viewModel.showDuplicateScanScopeDialog() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.manage_list_format, title, listSize)) }
            }
        }
        Spacer(Modifier.height(16.dp))

        HorizontalDivider()

        ToolAboutCard(
            icon = Icons.Default.ContentCopy,
            title = "Duplicate Finder",
            description = "Finds duplicate files across your device using content-based similarity checks. " +
                "Supports adjustable similarity thresholds, include/exclude folder scoping, " +
                "and batch management of duplicate groups.",
            warning = "Review duplicates carefully before deleting. Some files may share the same content " +
                "but be needed by different apps. Use the preview and selective deletion features."
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun DuplicateScanScopeManagementDialog(title: String, folderList: List<String>, onDismiss: () -> Unit, onAddFolder: () -> Unit, onRemoveFolder: (String) -> Unit) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (folderList.isEmpty()) {
                Text(stringResource(R.string.no_folders_added_to_list))
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                    items(folderList) { path ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(text = ".../${path.takeLast(35)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onRemoveFolder(path) }) { Icon(Icons.Default.Delete, contentDescription = "Remove folder") }
                        }
                    }
                }
            }
        },
        buttons = {
            OutlinedButton(onClick = onAddFolder) { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.add_folder)) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}
