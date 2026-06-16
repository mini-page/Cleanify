package com.cleanify.ui.screens.settings.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.cleanify.R
import com.cleanify.data.repository.AppLocale
import com.cleanify.data.repository.DoubleTapAction
import com.cleanify.data.repository.DuplicateScanScope
import com.cleanify.data.repository.FolderNameLayout
import com.cleanify.data.repository.FolderSelectionMode
import com.cleanify.data.repository.SimilarityThresholdLevel
import com.cleanify.data.repository.SwipeDownAction
import com.cleanify.data.repository.SwipeSensitivity
import com.cleanify.data.repository.TapAction
import com.cleanify.ui.theme.AppTheme

@Composable
fun getAppLocaleDisplayName(locale: AppLocale): String = when (locale) {
    AppLocale.SYSTEM -> stringResource(R.string.language_system)
    AppLocale.ENGLISH -> stringResource(R.string.language_english)
    AppLocale.ITALIAN -> stringResource(R.string.language_italian)
}

@Composable
fun getSwipeSensitivityDisplayName(s: SwipeSensitivity): String = when (s) {
    SwipeSensitivity.LOW -> stringResource(R.string.sensitivity_low)
    SwipeSensitivity.MEDIUM -> stringResource(R.string.sensitivity_medium)
    SwipeSensitivity.HIGH -> stringResource(R.string.sensitivity_high)
}

@Composable
fun getSwipeDownActionDisplayName(a: SwipeDownAction): String = when (a) {
    SwipeDownAction.NONE -> stringResource(R.string.action_none)
    SwipeDownAction.MOVE_TO_EDIT -> stringResource(R.string.move_to_to_edit)
    SwipeDownAction.SKIP_ITEM -> stringResource(R.string.skip_item)
    SwipeDownAction.ADD_TARGET_FOLDER -> stringResource(R.string.add_target_folder)
    SwipeDownAction.SHARE -> stringResource(R.string.share)
    SwipeDownAction.OPEN_WITH -> stringResource(R.string.open_with)
}

@Composable
fun getTapActionDisplayName(a: TapAction): String = when (a) {
    TapAction.PLAY_PAUSE -> stringResource(R.string.tap_action_play_pause)
    TapAction.NONE -> stringResource(R.string.tap_action_none)
}

@Composable
fun getDoubleTapActionDisplayName(a: DoubleTapAction): String = when (a) {
    DoubleTapAction.FAVORITE -> stringResource(R.string.double_tap_favorite)
    DoubleTapAction.FULLSCREEN -> stringResource(R.string.double_tap_fullscreen)
    DoubleTapAction.NONE -> stringResource(R.string.action_none)
}

@Composable
fun getSimilarityLevelDisplayName(l: SimilarityThresholdLevel): String = when (l) {
    SimilarityThresholdLevel.STRICT -> stringResource(R.string.similarity_level_strict)
    SimilarityThresholdLevel.BALANCED -> stringResource(R.string.similarity_level_balanced)
    SimilarityThresholdLevel.LOOSE -> stringResource(R.string.similarity_level_loose)
}

@Composable
fun getSimilarityLevelDescriptionRes(l: SimilarityThresholdLevel): Int = when (l) {
    SimilarityThresholdLevel.STRICT -> R.string.similarity_level_strict_desc
    SimilarityThresholdLevel.BALANCED -> R.string.similarity_level_balanced_desc
    SimilarityThresholdLevel.LOOSE -> R.string.similarity_level_loose_desc
}

@Composable
fun getScanScopeDisplayName(s: DuplicateScanScope): String = when (s) {
    DuplicateScanScope.ALL_FILES -> stringResource(R.string.scan_scope_all)
    DuplicateScanScope.INCLUDE_LIST -> stringResource(R.string.scan_scope_include)
    DuplicateScanScope.EXCLUDE_LIST -> stringResource(R.string.scan_scope_exclude)
}

@Composable
fun getScanScopeDescription(s: DuplicateScanScope, inc: Set<String>, exc: Set<String>): String = when (s) {
    DuplicateScanScope.ALL_FILES -> stringResource(R.string.scan_scope_all_desc)
    DuplicateScanScope.INCLUDE_LIST -> pluralStringResource(R.plurals.scan_scope_include_desc, inc.size, inc.size)
    DuplicateScanScope.EXCLUDE_LIST -> pluralStringResource(R.plurals.scan_scope_exclude_desc, exc.size, exc.size)
}

@Composable
fun getFolderNameLayoutDisplayName(l: FolderNameLayout): String = when (l) {
    FolderNameLayout.ABOVE -> stringResource(R.string.folder_name_layout_above)
    FolderNameLayout.HIDDEN -> stringResource(R.string.folder_name_layout_hidden)
}

@Composable
fun getThemeDisplayName(t: AppTheme): String = when (t) {
    AppTheme.SYSTEM -> stringResource(R.string.theme_system_display)
    AppTheme.LIGHT -> stringResource(R.string.theme_light_display)
    AppTheme.DARK -> stringResource(R.string.theme_dark_display)
    AppTheme.DARKER -> stringResource(R.string.theme_darker_display)
    AppTheme.AMOLED -> stringResource(R.string.theme_amoled_display)
}

@Composable
fun getThemeDescriptionRes(t: AppTheme): Int = when (t) {
    AppTheme.SYSTEM -> R.string.theme_system_desc
    AppTheme.LIGHT -> R.string.theme_light_desc
    AppTheme.DARK -> R.string.theme_dark_desc
    AppTheme.DARKER -> R.string.theme_darker_desc
    AppTheme.AMOLED -> R.string.theme_amoled_desc
}

@Composable
fun getFolderSelectionModeDisplayName(m: FolderSelectionMode): String = when (m) {
    FolderSelectionMode.ALL -> stringResource(R.string.mode_all_folders)
    FolderSelectionMode.REMEMBER -> stringResource(R.string.mode_remember_previous)
    FolderSelectionMode.NONE -> stringResource(R.string.mode_none)
}

@Composable
fun getFolderSelectionModeDescriptionRes(m: FolderSelectionMode): Int = when (m) {
    FolderSelectionMode.ALL -> R.string.desc_mode_all
    FolderSelectionMode.REMEMBER -> R.string.desc_mode_remember
    FolderSelectionMode.NONE -> R.string.desc_mode_none
}
