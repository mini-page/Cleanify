package com.cleanify.ui.screens.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.cleanify.R
import com.cleanify.data.repository.DoubleTapAction
import com.cleanify.data.repository.FolderNameLayout
import com.cleanify.data.repository.SwipeDownAction
import com.cleanify.data.repository.SwipeSensitivity
import com.cleanify.data.repository.TapAction
import com.cleanify.ui.screens.settings.SettingsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SortingSection(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val swipeSensitivity by viewModel.swipeSensitivity.collectAsStateWithLifecycle()
    val swipeDownAction by viewModel.swipeDownAction.collectAsStateWithLifecycle()
    val fullScreenSwipe by viewModel.fullScreenSwipe.collectAsStateWithLifecycle()
    val invertSwipe by viewModel.invertSwipe.collectAsStateWithLifecycle()
    val tapAction by viewModel.tapAction.collectAsStateWithLifecycle()
    val doubleTapAction by viewModel.doubleTapAction.collectAsStateWithLifecycle()
    val folderNameLayout by viewModel.folderNameLayout.collectAsStateWithLifecycle()
    val compactFolderView by viewModel.compactFolderView.collectAsStateWithLifecycle()
    val useLegacyFolderIcons by viewModel.useLegacyFolderIcons.collectAsStateWithLifecycle()
    val hideFilename by viewModel.hideFilename.collectAsStateWithLifecycle()
    val skipPartialExpansion by viewModel.skipPartialExpansion.collectAsStateWithLifecycle()
    val useFullScreenSummarySheet by viewModel.useFullScreenSummarySheet.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SectionHeader(R.string.gestures_section_header)
        SettingsPickerItem(R.string.swipe_sensitivity_title, R.string.swipe_sensitivity_desc, SwipeSensitivity.entries, swipeSensitivity, { viewModel.setSwipeSensitivity(it) }, ::getSwipeSensitivityDisplayName)
        SettingsPickerItem(R.string.swipe_down_action_title, R.string.swipe_down_action_desc, SwipeDownAction.entries, swipeDownAction, { viewModel.setSwipeDownAction(it) }, ::getSwipeDownActionDisplayName)
        SettingSwitch(R.string.full_screen_swipe_title, R.string.full_screen_swipe_desc, fullScreenSwipe, { viewModel.setFullScreenSwipe(it) })
        SettingSwitch(R.string.invert_swipe_title, R.string.invert_swipe_desc, invertSwipe, { viewModel.setInvertSwipe(it) })

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        SectionHeader(R.string.tap_section_header)
        SettingsPickerItem(R.string.tap_action_title, R.string.tap_action_desc, TapAction.entries, tapAction, { viewModel.setTapAction(it) }, ::getTapActionDisplayName)
        SettingsPickerItem(R.string.double_tap_action_title, R.string.double_tap_action_desc, DoubleTapAction.entries, doubleTapAction, { viewModel.setDoubleTapAction(it) }, ::getDoubleTapActionDisplayName)

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        SectionHeader(R.string.layout_section_header)
        SettingsPickerItem(R.string.folder_name_position_title, R.string.folder_name_position_desc, FolderNameLayout.entries, folderNameLayout, { viewModel.setFolderNameLayout(it) }, ::getFolderNameLayoutDisplayName)
        SettingSwitch(R.string.compact_folder_view_title, R.string.compact_folder_view_desc, compactFolderView, { viewModel.setCompactFolderView(it) })
        SettingSwitch(R.string.legacy_folder_icons_title, R.string.legacy_folder_icons_desc, useLegacyFolderIcons, { viewModel.setUseLegacyFolderIcons(it) })
        SettingSwitch(R.string.hide_media_filename_title, R.string.hide_media_filename_desc, hideFilename, { viewModel.setHideFilename(it) })
        SettingSwitch(R.string.skip_partial_expansion_title, R.string.skip_partial_expansion_desc, skipPartialExpansion, { viewModel.onSkipPartialExpansionChanged(it) })
        SettingSwitch(R.string.use_full_screen_summary_title, R.string.use_full_screen_summary_desc, useFullScreenSummarySheet, { viewModel.onUseFullScreenSummarySheetChanged(it) })

        Spacer(Modifier.height(32.dp))
    }
}
