package com.cleanify.ui.screens.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.cleanify.R
import com.cleanify.data.repository.FolderSelectionMode
import com.cleanify.ui.screens.settings.SettingsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BehaviorSection(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val folderSelectionMode by viewModel.folderSelectionMode.collectAsStateWithLifecycle()
    val showFavoritesInSetup by viewModel.showFavoritesInSetup.collectAsStateWithLifecycle()
    val hideSkipButton by viewModel.hideSkipButton.collectAsStateWithLifecycle()
    val addFavoriteToTargetByDefault by viewModel.addFavoriteToTargetByDefault.collectAsStateWithLifecycle()
    val unfavoriteRemovesFromBar by viewModel.unfavoriteRemovesFromBar.collectAsStateWithLifecycle()
    val hintOnExistingFolderName by viewModel.hintOnExistingFolderName.collectAsStateWithLifecycle()
    val defaultPath by viewModel.defaultAlbumCreationPath.collectAsStateWithLifecycle()
    val pathOptions = viewModel.standardAlbumDirectories
    val rememberProcessedMedia by viewModel.rememberProcessedMedia.collectAsStateWithLifecycle()
    val searchAutofocusEnabled by viewModel.searchAutofocusEnabled.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SectionHeader(R.string.folder_behavior_section_header)

        SettingsPickerItem(R.string.folder_selection_mode_title, getFolderSelectionModeDescriptionRes(folderSelectionMode), FolderSelectionMode.entries, folderSelectionMode, { viewModel.setFolderSelectionMode(it) }, ::getFolderSelectionModeDisplayName)
        SettingSwitch(R.string.show_favorites_setup_title, R.string.show_favorites_setup_desc, showFavoritesInSetup, { viewModel.setShowFavoritesInSetup(it) })
        SettingSwitch(R.string.hide_skip_button_title, R.string.hide_skip_button_desc, hideSkipButton, { viewModel.setHideSkipButton(it) })
        SettingSwitch(R.string.add_fav_by_default_title, R.string.add_fav_by_default_desc, addFavoriteToTargetByDefault, { viewModel.setAddFavoriteToTargetByDefault(it) })
        SettingSwitch(R.string.unfav_removes_from_bar_title, R.string.unfav_removes_from_bar_desc, unfavoriteRemovesFromBar, { viewModel.setUnfavoriteRemovesFromBar(it) })
        SettingSwitch(R.string.hint_on_existing_folder_title, R.string.hint_on_existing_folder_desc, hintOnExistingFolderName, { viewModel.setHintOnExistingFolderName(it) })
        DefaultAlbumLocationSetting(viewModel, defaultPath, pathOptions)
        RememberMediaSetting(viewModel, rememberProcessedMedia)
        ForgetSortedMediaSetting(viewModel)

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        SectionHeader(R.string.search_section_header)

        SettingSwitch(R.string.search_autofocus_title, R.string.search_autofocus_desc, searchAutofocusEnabled, { viewModel.setSearchAutofocusEnabled(it) })

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun DefaultAlbumLocationSetting(viewModel: SettingsViewModel, defaultPath: String, pathOptions: List<Pair<String, String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(stringResource(R.string.default_album_location_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.default_album_location_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pathOptions.forEach { (name, path) -> FilterChip(defaultPath == path, { viewModel.onDefaultAlbumPathChanged(path) }, label = { Text(name, Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }, modifier = Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton({ viewModel.showDefaultPathSearchDialog() }, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Search, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.search_custom_folder))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (defaultPath.isNotBlank()) {
                    val std = pathOptions.find { it.second == defaultPath }
                    stringResource(R.string.current_path_prefix, if (std != null) std.first else ".../${defaultPath.takeLast(30)}")
                } else stringResource(R.string.no_default_folder_selected),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RememberMediaSetting(viewModel: SettingsViewModel, rememberProcessedMedia: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.remember_organized_media_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.remember_organized_media_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(rememberProcessedMedia, { viewModel.setRememberProcessedMedia(it) })
            }
            AnimatedVisibility(rememberProcessedMedia) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton({ viewModel.resetProcessedMediaIds() }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.reset_organized_media_history)) }
            }
        }
    }
}

@Composable
private fun ForgetSortedMediaSetting(viewModel: SettingsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(stringResource(R.string.forget_sorted_media_folder_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.forget_sorted_media_folder_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            OutlinedButton({ viewModel.showForgetMediaSearchDialog() }, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Search, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.select_folder_to_forget))
            }
        }
    }
}
