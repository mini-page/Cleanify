package com.cleanify.ui.screens.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cleanify.R
import com.cleanify.data.repository.DoubleTapAction
import com.cleanify.data.repository.FolderNameLayout
import com.cleanify.data.repository.SwipeDownAction
import com.cleanify.data.repository.SwipeUpAction
import com.cleanify.data.repository.SwipeSensitivity
import com.cleanify.data.repository.TapAction
import com.cleanify.ui.screens.settings.DetailedIndexingStatus
import com.cleanify.ui.screens.settings.SettingsViewModel
import java.text.NumberFormat

@Composable
fun SortingSection(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val swipeSensitivity by viewModel.swipeSensitivity.collectAsStateWithLifecycle()
    val swipeDownAction by viewModel.swipeDownAction.collectAsStateWithLifecycle()
    val swipeUpAction by viewModel.swipeUpAction.collectAsStateWithLifecycle()
    val fullScreenSwipe by viewModel.fullScreenSwipe.collectAsStateWithLifecycle()
    val invertSwipe by viewModel.invertSwipe.collectAsStateWithLifecycle()
    val swiperBatchSize by viewModel.swiperBatchSize.collectAsStateWithLifecycle()
    val recycleBinEnabled by viewModel.recycleBinEnabled.collectAsStateWithLifecycle()
    val tapAction by viewModel.tapAction.collectAsStateWithLifecycle()
    val doubleTapAction by viewModel.doubleTapAction.collectAsStateWithLifecycle()
    val folderNameLayout by viewModel.folderNameLayout.collectAsStateWithLifecycle()
    val compactFolderView by viewModel.compactFolderView.collectAsStateWithLifecycle()
    val useLegacyFolderIcons by viewModel.useLegacyFolderIcons.collectAsStateWithLifecycle()
    val hideFilename by viewModel.hideFilename.collectAsStateWithLifecycle()
    val skipPartialExpansion by viewModel.skipPartialExpansion.collectAsStateWithLifecycle()
    val useFullScreenSummarySheet by viewModel.useFullScreenSummarySheet.collectAsStateWithLifecycle()

    // Additional Swiper & Album settings
    val hideSkipButton by viewModel.hideSkipButton.collectAsStateWithLifecycle()
    val hideActionButtons by viewModel.hideActionButtons.collectAsStateWithLifecycle()
    val addFavoriteToTargetByDefault by viewModel.addFavoriteToTargetByDefault.collectAsStateWithLifecycle()
    val unfavoriteRemovesFromBar by viewModel.unfavoriteRemovesFromBar.collectAsStateWithLifecycle()
    val hintOnExistingFolderName by viewModel.hintOnExistingFolderName.collectAsStateWithLifecycle()
    val defaultPath by viewModel.defaultAlbumCreationPath.collectAsStateWithLifecycle()
    val pathOptions = viewModel.standardAlbumDirectories
    val rememberProcessedMedia by viewModel.rememberProcessedMedia.collectAsStateWithLifecycle()

    // Media Playback & Indexing
    val defaultVideoSpeed by viewModel.defaultVideoSpeed.collectAsStateWithLifecycle()
    val startVideosUnmuted by viewModel.startVideosUnmuted.collectAsStateWithLifecycle()
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
        // ── Gestures & 4-Way Swiping ──
        FrostedSettingsGroup(titleRes = R.string.gestures_section_header) {
            // Left Swipe Action (Delete or Keep, interchangeable with Right)
            SettingsPickerItem(
                titleRes = R.string.left_swipe_action_title,
                description = stringResource(R.string.left_swipe_action_desc),
                options = listOf("Delete", "Keep"),
                selectedOption = if (invertSwipe) "Keep" else "Delete",
                onOptionSelected = { option -> viewModel.setInvertSwipe(option == "Keep") },
                getDisplayName = { it },
                settingId = "left_swipe_action"
            )

            // Right Swipe Action (Keep or Delete, interchangeable with Left)
            SettingsPickerItem(
                titleRes = R.string.right_swipe_action_title,
                description = stringResource(R.string.right_swipe_action_desc),
                options = listOf("Keep", "Delete"),
                selectedOption = if (invertSwipe) "Delete" else "Keep",
                onOptionSelected = { option -> viewModel.setInvertSwipe(option == "Delete") },
                getDisplayName = { it },
                settingId = "right_swipe_action"
            )

            // Swipe Up Action (None, Skip, Favorite)
            SettingsPickerItem(
                titleRes = R.string.swipe_up_action_title,
                descriptionRes = R.string.swipe_up_action_desc,
                options = SwipeUpAction.entries,
                selectedOption = swipeUpAction,
                onOptionSelected = { viewModel.setSwipeUpAction(it) },
                getDisplayName = ::getSwipeUpActionDisplayName,
                settingId = "swipe_up_action"
            )

            // Swipe Down Action (None, Move to edit folder, Skip)
            SettingsPickerItem(
                titleRes = R.string.swipe_down_action_title,
                descriptionRes = R.string.swipe_down_action_desc,
                options = listOf(SwipeDownAction.NONE, SwipeDownAction.MOVE_TO_EDIT, SwipeDownAction.SKIP_ITEM),
                selectedOption = swipeDownAction,
                onOptionSelected = { viewModel.setSwipeDownAction(it) },
                getDisplayName = ::getSwipeDownActionDisplayName,
                settingId = "swipe_down_action"
            )

            SettingsPickerItem(
                titleRes = R.string.swipe_sensitivity_title,
                descriptionRes = R.string.swipe_sensitivity_desc,
                options = SwipeSensitivity.entries,
                selectedOption = swipeSensitivity,
                onOptionSelected = { viewModel.setSwipeSensitivity(it) },
                getDisplayName = ::getSwipeSensitivityDisplayName,
                settingId = "swipe_sensitivity"
            )

            SettingSwitch(
                titleRes = R.string.full_screen_swipe_title,
                descriptionRes = R.string.full_screen_swipe_desc,
                checked = fullScreenSwipe,
                onCheckedChange = { viewModel.setFullScreenSwipe(it) },
                settingId = "fullscreen_swipe"
            )

            SettingsPickerItem(
                titleRes = R.string.tap_action_title,
                descriptionRes = R.string.tap_action_desc,
                options = TapAction.entries,
                selectedOption = tapAction,
                onOptionSelected = { viewModel.setTapAction(it) },
                getDisplayName = ::getTapActionDisplayName,
                settingId = "tap_action"
            )

            SettingsPickerItem(
                titleRes = R.string.double_tap_action_title,
                descriptionRes = R.string.double_tap_action_desc,
                options = DoubleTapAction.entries,
                selectedOption = doubleTapAction,
                onOptionSelected = { viewModel.setDoubleTapAction(it) },
                getDisplayName = ::getDoubleTapActionDisplayName,
                settingId = "double_tap_action"
            )

            SettingSwitch(
                titleRes = R.string.hide_skip_button_title,
                descriptionRes = R.string.hide_skip_button_desc,
                checked = hideSkipButton,
                onCheckedChange = { viewModel.setHideSkipButton(it) },
                settingId = "hide_skip_button"
            )

            SettingSwitch(
                titleRes = R.string.hide_action_buttons_title,
                descriptionRes = R.string.hide_action_buttons_desc,
                checked = hideActionButtons,
                onCheckedChange = { viewModel.setHideActionButtons(it) },
                settingId = "hide_action_buttons"
            )
        }

        // ── Batch Sizing & Review ──
        FrostedSettingsGroup(title = "Swiper Batching") {
            SettingsPickerItem(
                titleRes = R.string.swiper_batch_size_title,
                descriptionRes = R.string.swiper_batch_size_desc,
                options = listOf(10, 20, 30, 40, 50),
                selectedOption = swiperBatchSize,
                onOptionSelected = { viewModel.setSwiperBatchSize(it) },
                getDisplayName = { "$it items" },
                settingId = "swiper_batch_size"
            )
            SettingSwitch(
                titleRes = R.string.skip_partial_expansion_title,
                descriptionRes = R.string.skip_partial_expansion_desc,
                checked = skipPartialExpansion,
                onCheckedChange = { viewModel.onSkipPartialExpansionChanged(it) },
                settingId = "skip_partial_expansion"
            )
            SettingSwitch(
                titleRes = R.string.use_full_screen_summary_title,
                descriptionRes = R.string.use_full_screen_summary_desc,
                checked = useFullScreenSummarySheet,
                onCheckedChange = { viewModel.onUseFullScreenSummarySheetChanged(it) },
                settingId = "fullscreen_summary"
            )
        }

        // ── Deletion & Recycle Bin ──
        FrostedSettingsGroup(title = "Deletion & Recycle Bin") {
            SettingSwitch(
                titleRes = R.string.use_recycle_bin_title,
                description = stringResource(R.string.use_recycle_bin_desc),
                checked = recycleBinEnabled,
                onCheckedChange = { viewModel.setRecycleBinEnabled(it) },
                settingId = "recycle_bin_toggle"
            )
        }

        // ── Media Playback Engine ──
        FrostedSettingsGroup(titleRes = R.string.video_section_header) {
            SettingSwitch(R.string.start_videos_unmuted_title, R.string.start_videos_unmuted_desc, startVideosUnmuted, { viewModel.setStartVideosUnmuted(it) }, settingId = "start_videos_unmuted")
            SettingsPickerItem(R.string.default_video_speed_title, R.string.default_video_speed_desc, listOf(1.0f, 1.5f, 2.0f), defaultVideoSpeed, { viewModel.setDefaultVideoSpeed(it) }, { s -> "${s}x" }, settingId = "default_video_speed")
            SettingSwitch(R.string.screenshot_deletes_video_title, R.string.screenshot_deletes_video_desc, screenshotDeletesVideo, { viewModel.setScreenshotDeletesVideo(it) }, settingId = "screenshot_deletes_video")
            SettingsPickerItem(R.string.screenshot_quality_title, R.string.screenshot_quality_desc, listOf("90", "75"), screenshotJpegQuality, { viewModel.setScreenshotJpegQuality(it) }, { q -> when (q) { "90" -> stringResource(R.string.quality_high); "75" -> stringResource(R.string.quality_low); else -> q } }, settingId = "screenshot_quality")
        }

        // ── Albums & Organization ──
        FrostedSettingsGroup(titleRes = R.string.folder_behavior_section_header) {
            DefaultAlbumLocationSetting(viewModel, defaultPath, pathOptions, settingId = "default_album_location")
            RememberMediaSetting(viewModel, rememberProcessedMedia, settingId = "remember_sorted_media")
            ForgetSortedMediaSetting(viewModel, settingId = "forget_sorted_media")
            SettingSwitch(R.string.add_fav_by_default_title, R.string.add_fav_by_default_desc, addFavoriteToTargetByDefault, { viewModel.setAddFavoriteToTargetByDefault(it) }, settingId = "add_favorite_by_default")
            SettingSwitch(R.string.unfav_removes_from_bar_title, R.string.unfav_removes_from_bar_desc, unfavoriteRemovesFromBar, { viewModel.setUnfavoriteRemovesFromBar(it) }, settingId = "unfavorite_removes")
            SettingSwitch(R.string.hint_on_existing_folder_title, R.string.hint_on_existing_folder_desc, hintOnExistingFolderName, { viewModel.setHintOnExistingFolderName(it) }, settingId = "hint_existing_folder")
        }

        // ── Folder & Card Layout ──
        FrostedSettingsGroup(titleRes = R.string.layout_section_header) {
            SettingsPickerItem(R.string.folder_name_position_title, R.string.folder_name_position_desc, FolderNameLayout.entries, folderNameLayout, { viewModel.setFolderNameLayout(it) }, ::getFolderNameLayoutDisplayName, settingId = "folder_name_layout")
            SettingSwitch(R.string.compact_folder_view_title, R.string.compact_folder_view_desc, compactFolderView, { viewModel.setCompactFolderView(it) }, settingId = "compact_folder_view")
            SettingSwitch(R.string.legacy_folder_icons_title, R.string.legacy_folder_icons_desc, useLegacyFolderIcons, { viewModel.setUseLegacyFolderIcons(it) }, settingId = "legacy_folder_icons")
            SettingSwitch(R.string.hide_media_filename_title, R.string.hide_media_filename_desc, hideFilename, { viewModel.setHideFilename(it) }, settingId = "hide_filename")
        }

        // ── Media Scanning & Indexing ──
        FrostedSettingsGroup(titleRes = R.string.indexing_section_header) {
            MediaIndexingStatusItem(uiState.indexingStatus, uiState.isIndexingStatusLoading, uiState.isIndexing, viewModel::refreshIndexingStatus, viewModel::triggerFullScan, viewModel::showUnindexedFilesDialog, settingId = "media_indexing")
            SettingSwitch(R.string.scan_audio_title, R.string.scan_audio_desc, scanAudioEnabled, viewModel::setScanAudioEnabled, settingId = "scan_audio")
            SettingSwitch(R.string.scan_document_title, R.string.scan_document_desc, scanDocumentEnabled, viewModel::setScanDocumentEnabled, settingId = "scan_document")
            SettingSwitch(R.string.scan_video_title, R.string.scan_video_desc, scanVideoEnabled, viewModel::setScanVideoEnabled, settingId = "scan_video")
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun DefaultAlbumLocationSetting(viewModel: SettingsViewModel, defaultPath: String, pathOptions: List<Pair<String, String>>, settingId: String? = null) {
    val isHighlighted = LocalHighlightedSettingId.current != null && LocalHighlightedSettingId.current == settingId
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = if (isHighlighted) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
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
private fun RememberMediaSetting(viewModel: SettingsViewModel, rememberProcessedMedia: Boolean, settingId: String? = null) {
    val isHighlighted = LocalHighlightedSettingId.current != null && LocalHighlightedSettingId.current == settingId
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = if (isHighlighted) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
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
private fun ForgetSortedMediaSetting(viewModel: SettingsViewModel, settingId: String? = null) {
    val isHighlighted = LocalHighlightedSettingId.current != null && LocalHighlightedSettingId.current == settingId
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = if (isHighlighted) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
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

@Composable
private fun MediaIndexingStatusItem(status: DetailedIndexingStatus?, isStatusLoading: Boolean, isScanning: Boolean, onRefresh: () -> Unit, onScan: () -> Unit, onViewFiles: () -> Unit, settingId: String? = null) {
    val isHighlighted = LocalHighlightedSettingId.current != null && LocalHighlightedSettingId.current == settingId
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
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = if (isHighlighted) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
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
