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

package com.cleanify.data.repository

import android.content.Context
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cleanify.ui.theme.AppTheme
import com.cleanify.ui.theme.predefinedAccentColors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class FolderSelectionMode {
    ALL,
    REMEMBER,
    NONE
}

enum class SummaryViewMode {
    LIST,
    GRID,
    COMPACT
}

enum class FolderBarLayout {
    HORIZONTAL,
    VERTICAL
}

enum class AddFolderFocusTarget {
    SEARCH_PATH,
    FOLDER_NAME,
    NONE
}

enum class SwipeSensitivity {
    LOW,
    MEDIUM,
    HIGH
}

enum class FolderNameLayout {
    ABOVE,
    HIDDEN
}

enum class SimilarityThresholdLevel {
    STRICT,
    BALANCED,
    LOOSE
}

enum class DuplicateScanScope {
    ALL_FILES,
    INCLUDE_LIST,
    EXCLUDE_LIST
}

enum class SwipeDownAction {
    NONE,
    MOVE_TO_EDIT,
    SKIP_ITEM,
    ADD_TARGET_FOLDER,
    SHARE,
    OPEN_WITH
}

enum class TapAction {
    PLAY_PAUSE,
    NONE
}

enum class DoubleTapAction {
    FAVORITE,
    FULLSCREEN,
    NONE
}

enum class UnselectScanScope {
    GLOBAL,
    VISIBLE_ONLY
}

enum class AppLocale(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    ITALIAN("it")
}


@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val DEFAULT_ALBUM_CREATION_PATH =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath
        private val KEY_DEFAULT_ALBUM_PATH = stringPreferencesKey("default_album_creation_path")
    }

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val APP_LOCALE = stringPreferencesKey("app_locale")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val COMPACT_FOLDER_VIEW = booleanPreferencesKey("compact_folder_view")
        val HIDE_FILENAME = booleanPreferencesKey("hide_filename")
        val REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
        val HIDE_FROM_GALLERY = booleanPreferencesKey("hide_from_gallery")
        val INVERT_SWIPE = booleanPreferencesKey("invert_swipe")
        val FULL_SCREEN_SWIPE = booleanPreferencesKey("full_screen_swipe")
        val FOLDER_SELECTION_MODE = stringPreferencesKey("folder_selection_mode")
        val PREVIOUS_BUCKETS = stringPreferencesKey("previous_buckets")
        val SUMMARY_VIEW_MODE = stringPreferencesKey("summary_view_mode")
        val PROCESSED_MEDIA_PATHS = stringPreferencesKey("processed_media_paths")
        val REMEMBER_PROCESSED_MEDIA = booleanPreferencesKey("remember_processed_media")
        val SOURCE_FAVORITE_FOLDERS = stringPreferencesKey("source_favorite_folders")
        val TARGET_FAVORITE_FOLDERS = stringPreferencesKey("target_favorite_folders")
        val SHOW_FAVORITES_FIRST_IN_SETUP = booleanPreferencesKey("show_favorites_first_in_setup")
        val SEARCH_AUTOFOCUS_ENABLED = booleanPreferencesKey("search_autofocus_enabled")
        val SKIP_PARTIAL_EXPANSION = booleanPreferencesKey("expand_summary_sheet") // Renamed, key kept for migration
        val USE_FULL_SCREEN_SUMMARY_SHEET = booleanPreferencesKey("use_full_screen_summary_sheet")
        val FOLDER_BAR_LAYOUT = stringPreferencesKey("folder_bar_layout")
        val FOLDER_NAME_LAYOUT = stringPreferencesKey("folder_name_layout")
        val USE_LEGACY_FOLDER_ICONS = booleanPreferencesKey("use_legacy_folder_icons")
        val ADD_FOLDER_FOCUS_TARGET = stringPreferencesKey("add_folder_focus_target")
        val SWIPE_SENSITIVITY = stringPreferencesKey("swipe_sensitivity")
        val PERMANENTLY_SORTED_FOLDERS = stringPreferencesKey("permanently_sorted_folders")
        val ADD_FAVORITE_TO_TARGET_BY_DEFAULT = booleanPreferencesKey("add_favorite_to_target_by_default")
        val HINT_ON_EXISTING_FOLDER_NAME = booleanPreferencesKey("hint_on_existing_folder_name")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
        val HIDE_PREVIEW_STRIP = booleanPreferencesKey("hide_preview_strip")
        val ACCENT_COLOR_KEY = stringPreferencesKey("accent_color_key")
        val BOTTOM_BAR_EXPANDED = booleanPreferencesKey("bottom_bar_expanded")
        val UNFAVORITE_REMOVES_FROM_BAR = booleanPreferencesKey("unfavorite_removes_from_bar")
        val HAS_RUN_DUPLICATE_SCAN_ONCE = booleanPreferencesKey("has_run_duplicate_scan_once")
        val SCREENSHOT_DELETES_VIDEO = booleanPreferencesKey("screenshot_deletes_video")
        val SCREENSHOT_JPEG_QUALITY = stringPreferencesKey("screenshot_jpeg_quality")
        val SIMILARITY_THRESHOLD_LEVEL = stringPreferencesKey("similarity_threshold_level")
        val HIDE_SWIPER_SKIP_BUTTON = booleanPreferencesKey("hide_swiper_skip_button")
        val DUPLICATE_SCAN_SCOPE = stringPreferencesKey("duplicate_scan_scope")
        val DUPLICATE_SCAN_INCLUDE_LIST = stringPreferencesKey("duplicate_scan_include_list")
        val DUPLICATE_SCAN_EXCLUDE_LIST = stringPreferencesKey("duplicate_scan_exclude_list")
        val SWIPE_DOWN_ACTION = stringPreferencesKey("swipe_down_action")
        val FAVORITE_MEDIA_ITEMS = stringPreferencesKey("favorite_media_items")
        val TAP_ACTION = stringPreferencesKey("tap_action")
        val DOUBLE_TAP_ACTION = stringPreferencesKey("double_tap_action")

        val DEFAULT_VIDEO_SPEED = floatPreferencesKey("default_video_speed")

        // Dialog Confirmation Preferences
        val SHOW_CONFIRM_MARK_AS_SORTED = booleanPreferencesKey("show_confirm_mark_as_sorted")
        val SHOW_CONFIRM_RESET_ALL_HISTORY = booleanPreferencesKey("show_confirm_reset_all_history")
        val SHOW_CONFIRM_FORGET_FOLDER = booleanPreferencesKey("show_confirm_forget_folder")
        val SHOW_CONFIRM_RESET_SOURCE_FAVS = booleanPreferencesKey("show_confirm_reset_source_favs")
        val SHOW_CONFIRM_RESET_TARGET_FAVS = booleanPreferencesKey("show_confirm_reset_target_favs")
        val SHOW_CONFIRM_DELETE_ALL_EXACT = booleanPreferencesKey("show_confirm_delete_all_exact")
        val UNSELECT_ALL_IN_SEARCH_SCOPE = stringPreferencesKey("unselect_all_in_search_scope")
        val SCAN_AUDIO_ENABLED = booleanPreferencesKey("scan_audio_enabled")
        val SCAN_DOCUMENT_ENABLED = booleanPreferencesKey("scan_document_enabled")
        val RECYCLE_BIN_ENABLED = booleanPreferencesKey("recycle_bin_enabled")
        val RECYCLE_BIN_RETENTION_DAYS = intPreferencesKey("recycle_bin_retention_days")
        val RECYCLE_BIN_STORAGE_LOCATION = stringPreferencesKey("recycle_bin_storage_location")
        val RECYCLE_BIN_SKIP_TRASH_JUNK = booleanPreferencesKey("recycle_bin_skip_trash_junk")
    }

    // ── Boolean Preference instances ─────────────────────────────────────────
    // Each replaces ~10 lines of flow+setter boilerplate with 1 line.

    private val useDynamicColorsPref = Preference(context.dataStore, PreferencesKeys.USE_DYNAMIC_COLORS, true)
    private val isOnboardingCompletedPref = Preference(context.dataStore, PreferencesKeys.ONBOARDING_COMPLETED, false)
    private val compactFolderViewPref = Preference(context.dataStore, PreferencesKeys.COMPACT_FOLDER_VIEW, false)
    private val hideFilenamePref = Preference(context.dataStore, PreferencesKeys.HIDE_FILENAME, false)
    private val invertSwipePref = Preference(context.dataStore, PreferencesKeys.INVERT_SWIPE, false)
    private val reduceAnimationsPref = Preference(context.dataStore, PreferencesKeys.REDUCE_ANIMATIONS, false)
    private val hideFromGalleryPref = Preference(context.dataStore, PreferencesKeys.HIDE_FROM_GALLERY, true)
    private val fullScreenSwipePref = Preference(context.dataStore, PreferencesKeys.FULL_SCREEN_SWIPE, false)
    private val rememberProcessedMediaPref = Preference(context.dataStore, PreferencesKeys.REMEMBER_PROCESSED_MEDIA, true)
    private val unfavoriteRemovesFromBarPref = Preference(context.dataStore, PreferencesKeys.UNFAVORITE_REMOVES_FROM_BAR, false)
    private val hideSkipButtonPref = Preference(context.dataStore, PreferencesKeys.HIDE_SWIPER_SKIP_BUTTON, false)
    private val hidePreviewStripPref = Preference(context.dataStore, PreferencesKeys.HIDE_PREVIEW_STRIP, false)
    private val hasRunDuplicateScanOncePref = Preference(context.dataStore, PreferencesKeys.HAS_RUN_DUPLICATE_SCAN_ONCE, false)
    private val addFavoriteToTargetByDefaultPref = Preference(context.dataStore, PreferencesKeys.ADD_FAVORITE_TO_TARGET_BY_DEFAULT, false)
    private val scanAudioEnabledPref = Preference(context.dataStore, PreferencesKeys.SCAN_AUDIO_ENABLED, true)
    private val scanDocumentEnabledPref = Preference(context.dataStore, PreferencesKeys.SCAN_DOCUMENT_ENABLED, true)
    private val hintOnExistingFolderNamePref = Preference(context.dataStore, PreferencesKeys.HINT_ON_EXISTING_FOLDER_NAME, true)
    private val showFavoritesFirstInSetupPref = Preference(context.dataStore, PreferencesKeys.SHOW_FAVORITES_FIRST_IN_SETUP, true)
    private val searchAutofocusEnabledPref = Preference(context.dataStore, PreferencesKeys.SEARCH_AUTOFOCUS_ENABLED, false)
    private val skipPartialExpansionPref = Preference(context.dataStore, PreferencesKeys.SKIP_PARTIAL_EXPANSION, true)
    private val useFullScreenSummarySheetPref = Preference(context.dataStore, PreferencesKeys.USE_FULL_SCREEN_SUMMARY_SHEET, false)
    private val useLegacyFolderIconsPref = Preference(context.dataStore, PreferencesKeys.USE_LEGACY_FOLDER_ICONS, false)
    private val bottomBarExpandedPref = Preference(context.dataStore, PreferencesKeys.BOTTOM_BAR_EXPANDED, false)
    private val screenshotDeletesVideoPref = Preference(context.dataStore, PreferencesKeys.SCREENSHOT_DELETES_VIDEO, false)
    private val showConfirmMarkAsSortedPref = Preference(context.dataStore, PreferencesKeys.SHOW_CONFIRM_MARK_AS_SORTED, true)
    private val showConfirmResetAllHistoryPref = Preference(context.dataStore, PreferencesKeys.SHOW_CONFIRM_RESET_ALL_HISTORY, true)
    private val showConfirmForgetFolderPref = Preference(context.dataStore, PreferencesKeys.SHOW_CONFIRM_FORGET_FOLDER, true)
    private val showConfirmResetSourceFavsPref = Preference(context.dataStore, PreferencesKeys.SHOW_CONFIRM_RESET_SOURCE_FAVS, true)
    private val showConfirmResetTargetFavsPref = Preference(context.dataStore, PreferencesKeys.SHOW_CONFIRM_RESET_TARGET_FAVS, true)
    private val showConfirmDeleteAllExactPref = Preference(context.dataStore, PreferencesKeys.SHOW_CONFIRM_DELETE_ALL_EXACT, true)
    private val recycleBinEnabledPref = Preference(context.dataStore, PreferencesKeys.RECYCLE_BIN_ENABLED, true)
    private val recycleBinSkipTrashJunkPref = Preference(context.dataStore, PreferencesKeys.RECYCLE_BIN_SKIP_TRASH_JUNK, false)

    // ── Enum Preference instances ────────────────────────────────────────────
    // Each replaces ~10 lines of flow+setter boilerplate with 1 line.

    private val themePref = EnumPreference(context.dataStore, PreferencesKeys.THEME, AppTheme.SYSTEM, AppTheme::class.java)
    private val appLocalePref = EnumPreference(context.dataStore, PreferencesKeys.APP_LOCALE, AppLocale.SYSTEM, AppLocale::class.java)
    private val folderSelectionModePref = EnumPreference(context.dataStore, PreferencesKeys.FOLDER_SELECTION_MODE, FolderSelectionMode.REMEMBER, FolderSelectionMode::class.java)
    private val summaryViewModePref = EnumPreference(context.dataStore, PreferencesKeys.SUMMARY_VIEW_MODE, SummaryViewMode.LIST, SummaryViewMode::class.java)
    private val folderBarLayoutPref = EnumPreference(context.dataStore, PreferencesKeys.FOLDER_BAR_LAYOUT, FolderBarLayout.HORIZONTAL, FolderBarLayout::class.java)
    private val folderNameLayoutPref = EnumPreference(context.dataStore, PreferencesKeys.FOLDER_NAME_LAYOUT, FolderNameLayout.ABOVE, FolderNameLayout::class.java)
    private val addFolderFocusTargetPref = EnumPreference(context.dataStore, PreferencesKeys.ADD_FOLDER_FOCUS_TARGET, AddFolderFocusTarget.SEARCH_PATH, AddFolderFocusTarget::class.java)
    private val swipeSensitivityPref = EnumPreference(context.dataStore, PreferencesKeys.SWIPE_SENSITIVITY, SwipeSensitivity.MEDIUM, SwipeSensitivity::class.java)
    private val swipeDownActionPref = EnumPreference(context.dataStore, PreferencesKeys.SWIPE_DOWN_ACTION, SwipeDownAction.NONE, SwipeDownAction::class.java)
    private val tapActionPref = EnumPreference(context.dataStore, PreferencesKeys.TAP_ACTION, TapAction.PLAY_PAUSE, TapAction::class.java)
    private val doubleTapActionPref = EnumPreference(context.dataStore, PreferencesKeys.DOUBLE_TAP_ACTION, DoubleTapAction.FAVORITE, DoubleTapAction::class.java)
    private val similarityThresholdLevelPref = EnumPreference(context.dataStore, PreferencesKeys.SIMILARITY_THRESHOLD_LEVEL, SimilarityThresholdLevel.BALANCED, SimilarityThresholdLevel::class.java)
    private val duplicateScanScopePref = EnumPreference(context.dataStore, PreferencesKeys.DUPLICATE_SCAN_SCOPE, DuplicateScanScope.ALL_FILES, DuplicateScanScope::class.java)
    private val unselectAllInSearchScopePref = EnumPreference(context.dataStore, PreferencesKeys.UNSELECT_ALL_IN_SEARCH_SCOPE, UnselectScanScope.GLOBAL, UnselectScanScope::class.java)

    // ── Non-boolean flows (unchanged) ────────────────────────────────────────

    val themeFlow: Flow<AppTheme> = themePref.flow

    val appLocaleFlow: Flow<AppLocale> = appLocalePref.flow

    val accentColorKeyFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ACCENT_COLOR_KEY] ?: predefinedAccentColors.first().key
        }

    val folderSelectionModeFlow: Flow<FolderSelectionMode> = folderSelectionModePref.flow

    val previouslySelectedBucketsFlow: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            val bucketString = preferences[PreferencesKeys.PREVIOUS_BUCKETS] ?: ""
            if (bucketString.isEmpty()) emptyList() else bucketString.split(",")
        }

    val summaryViewModeFlow: Flow<SummaryViewMode> = summaryViewModePref.flow

    val processedMediaPathsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val pathsString = preferences[PreferencesKeys.PROCESSED_MEDIA_PATHS] ?: ""
            if (pathsString.isEmpty()) {
                emptySet()
            } else {
                pathsString.split(",").filter { it.isNotBlank() }.toSet()
            }
        }

    val sourceFavoriteFoldersFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val favoritesString = preferences[PreferencesKeys.SOURCE_FAVORITE_FOLDERS] ?: ""
            if (favoritesString.isEmpty()) {
                emptySet()
            } else {
                favoritesString.split(",").filter { it.isNotBlank() }.toSet()
            }
        }

    val targetFavoriteFoldersFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val favoritesString = preferences[PreferencesKeys.TARGET_FAVORITE_FOLDERS] ?: ""
            if (favoritesString.isEmpty()) {
                emptySet()
            } else {
                favoritesString.split(",").filter { it.isNotBlank() }.toSet()
            }
        }

    val permanentlySortedFoldersFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val pathsString = preferences[PreferencesKeys.PERMANENTLY_SORTED_FOLDERS] ?: ""
            if (pathsString.isEmpty()) {
                emptySet()
            } else {
                pathsString.split(",").filter { it.isNotBlank() }.toSet()
            }
        }

    val favoriteMediaItemsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val string = preferences[PreferencesKeys.FAVORITE_MEDIA_ITEMS] ?: ""
            if (string.isEmpty()) {
                emptySet()
            } else {
                string.split(",").filter { it.isNotBlank() }.toSet()
            }
        }

    val defaultAlbumCreationPathFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_DEFAULT_ALBUM_PATH] ?: DEFAULT_ALBUM_CREATION_PATH
        }

    val folderBarLayoutFlow: Flow<FolderBarLayout> = folderBarLayoutPref.flow

    val folderNameLayoutFlow: Flow<FolderNameLayout> = context.dataStore.data
        .map { preferences ->
            val layoutName =
                preferences[PreferencesKeys.FOLDER_NAME_LAYOUT] ?: FolderNameLayout.ABOVE.name
            try {
                if (layoutName == "BELOW") FolderNameLayout.HIDDEN else FolderNameLayout.valueOf(layoutName)
            } catch (e: IllegalArgumentException) {
                FolderNameLayout.ABOVE
            }
        }

    val addFolderFocusTargetFlow: Flow<AddFolderFocusTarget> = addFolderFocusTargetPref.flow

    val swipeSensitivityFlow: Flow<SwipeSensitivity> = swipeSensitivityPref.flow

    val swipeDownActionFlow: Flow<SwipeDownAction> = swipeDownActionPref.flow

    val tapActionFlow: Flow<TapAction> = tapActionPref.flow

    val doubleTapActionFlow: Flow<DoubleTapAction> = doubleTapActionPref.flow

    val defaultVideoSpeedFlow: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DEFAULT_VIDEO_SPEED] ?: 1.0f
        }

    val screenshotJpegQualityFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SCREENSHOT_JPEG_QUALITY] ?: "90"
        }

    val similarityThresholdLevelFlow: Flow<SimilarityThresholdLevel> = similarityThresholdLevelPref.flow

    val duplicateScanScopeFlow: Flow<DuplicateScanScope> = duplicateScanScopePref.flow

    val duplicateScanIncludeListFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val listString = preferences[PreferencesKeys.DUPLICATE_SCAN_INCLUDE_LIST] ?: ""
            if (listString.isEmpty()) emptySet() else listString.split(",").filter { it.isNotBlank() }.toSet()
        }

    val duplicateScanExcludeListFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val listString = preferences[PreferencesKeys.DUPLICATE_SCAN_EXCLUDE_LIST] ?: ""
            if (listString.isEmpty()) emptySet() else listString.split(",").filter { it.isNotBlank() }.toSet()
        }

    val unselectAllInSearchScopeFlow: Flow<UnselectScanScope> = unselectAllInSearchScopePref.flow

    val recycleBinRetentionDaysFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.RECYCLE_BIN_RETENTION_DAYS] ?: 30 }

    val recycleBinStorageLocationFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.RECYCLE_BIN_STORAGE_LOCATION] ?: "external" }

    // ── Boolean flows: delegated to Preference instances ──────────────────────

    val useDynamicColorsFlow: Flow<Boolean> = useDynamicColorsPref.flow
    val isOnboardingCompletedFlow: Flow<Boolean?> = isOnboardingCompletedPref.flow
    val compactFolderViewFlow: Flow<Boolean> = compactFolderViewPref.flow
    val hideFilenameFlow: Flow<Boolean> = hideFilenamePref.flow
    val invertSwipeFlow: Flow<Boolean> = invertSwipePref.flow
    val reduceAnimationsFlow: Flow<Boolean> = reduceAnimationsPref.flow
    val hideFromGalleryFlow: Flow<Boolean> = hideFromGalleryPref.flow
    val fullScreenSwipeFlow: Flow<Boolean> = fullScreenSwipePref.flow
    val rememberProcessedMediaFlow: Flow<Boolean> = rememberProcessedMediaPref.flow
    val unfavoriteRemovesFromBarFlow: Flow<Boolean> = unfavoriteRemovesFromBarPref.flow
    val hideSkipButtonFlow: Flow<Boolean> = hideSkipButtonPref.flow
    val hidePreviewStripFlow: Flow<Boolean> = hidePreviewStripPref.flow
    val hasRunDuplicateScanOnceFlow: Flow<Boolean> = hasRunDuplicateScanOncePref.flow
    val addFavoriteToTargetByDefaultFlow: Flow<Boolean> = addFavoriteToTargetByDefaultPref.flow
    val scanAudioEnabledFlow: Flow<Boolean> = scanAudioEnabledPref.flow
    val scanDocumentEnabledFlow: Flow<Boolean> = scanDocumentEnabledPref.flow
    val hintOnExistingFolderNameFlow: Flow<Boolean> = hintOnExistingFolderNamePref.flow
    val showFavoritesFirstInSetupFlow: Flow<Boolean> = showFavoritesFirstInSetupPref.flow
    val searchAutofocusEnabledFlow: Flow<Boolean> = searchAutofocusEnabledPref.flow
    val skipPartialExpansionFlow: Flow<Boolean> = skipPartialExpansionPref.flow
    val useFullScreenSummarySheetFlow: Flow<Boolean> = useFullScreenSummarySheetPref.flow
    val useLegacyFolderIconsFlow: Flow<Boolean> = useLegacyFolderIconsPref.flow
    val bottomBarExpandedFlow: Flow<Boolean> = bottomBarExpandedPref.flow
    val screenshotDeletesVideoFlow: Flow<Boolean> = screenshotDeletesVideoPref.flow
    val showConfirmMarkAsSortedFlow: Flow<Boolean> = showConfirmMarkAsSortedPref.flow
    val showConfirmResetAllHistoryFlow: Flow<Boolean> = showConfirmResetAllHistoryPref.flow
    val showConfirmForgetFolderFlow: Flow<Boolean> = showConfirmForgetFolderPref.flow
    val showConfirmResetSourceFavsFlow: Flow<Boolean> = showConfirmResetSourceFavsPref.flow
    val showConfirmResetTargetFavsFlow: Flow<Boolean> = showConfirmResetTargetFavsPref.flow
    val showConfirmDeleteAllExactFlow: Flow<Boolean> = showConfirmDeleteAllExactPref.flow
    val recycleBinEnabledFlow: Flow<Boolean> = recycleBinEnabledPref.flow
    val recycleBinSkipTrashJunkFlow: Flow<Boolean> = recycleBinSkipTrashJunkPref.flow

    // ── Non-boolean setters (unchanged) ──────────────────────────────────────

    suspend fun setTheme(theme: AppTheme) = themePref.set(theme)

    suspend fun setAppLocale(locale: AppLocale) = appLocalePref.set(locale)

    suspend fun setAccentColorKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCENT_COLOR_KEY] = key
        }
    }

    suspend fun setOnboardingCompleted() {
        isOnboardingCompletedPref.set(true)
    }

    suspend fun setFolderSelectionMode(mode: FolderSelectionMode) = folderSelectionModePref.set(mode)

    suspend fun savePreviouslySelectedBuckets(bucketIds: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREVIOUS_BUCKETS] = bucketIds.joinToString(",")
        }
    }

    suspend fun setSummaryViewMode(mode: SummaryViewMode) = summaryViewModePref.set(mode)

    suspend fun addProcessedMediaPaths(mediaPaths: Set<String>) {
        if (mediaPaths.isEmpty()) return
        context.dataStore.edit { preferences ->
            val currentPaths = preferences[PreferencesKeys.PROCESSED_MEDIA_PATHS] ?: ""
            val pathsSet = if (currentPaths.isEmpty()) {
                mediaPaths
            } else {
                currentPaths.split(",")
                    .filter { it.isNotBlank() }
                    .toMutableSet()
                    .apply { addAll(mediaPaths) }
            }
            preferences[PreferencesKeys.PROCESSED_MEDIA_PATHS] = pathsSet.joinToString(",")
        }
    }

    suspend fun clearProcessedMediaPaths() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.PROCESSED_MEDIA_PATHS)
        }
    }

    suspend fun removeProcessedMediaPathsInFolder(folderPath: String) {
        context.dataStore.edit { preferences ->
            val currentPaths = preferences[PreferencesKeys.PROCESSED_MEDIA_PATHS] ?: ""
            if (currentPaths.isNotEmpty()) {
                val newPaths = currentPaths.split(',')
                    .filter { path ->
                        if (path.isBlank()) return@filter false
                        val parent = try { File(path).parent } catch (e: Exception) { null }
                        parent != folderPath
                    }
                    .joinToString(",")
                preferences[PreferencesKeys.PROCESSED_MEDIA_PATHS] = newPaths
            }
        }
    }

    suspend fun resetOnboarding() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.ONBOARDING_COMPLETED)
        }
    }

    suspend fun addSourceFavoriteFolder(folderPath: String) {
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[PreferencesKeys.SOURCE_FAVORITE_FOLDERS] ?: ""
            val favoritesSet = if (currentFavorites.isEmpty()) {
                setOf(folderPath)
            } else {
                currentFavorites.split(",").filter { it.isNotBlank() }.toMutableSet().apply {
                    add(folderPath)
                }
            }
            preferences[PreferencesKeys.SOURCE_FAVORITE_FOLDERS] = favoritesSet.joinToString(",")
        }
    }

    suspend fun addTargetFavoriteFolder(folderPath: String) {
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[PreferencesKeys.TARGET_FAVORITE_FOLDERS] ?: ""
            val favoritesSet = if (currentFavorites.isEmpty()) {
                setOf(folderPath)
            } else {
                currentFavorites.split(",").filter { it.isNotBlank() }.toMutableSet().apply {
                    add(folderPath)
                }
            }
            preferences[PreferencesKeys.TARGET_FAVORITE_FOLDERS] = favoritesSet.joinToString(",")
        }
    }

    suspend fun addTargetFavoriteFolders(folderPaths: Set<String>) {
        if (folderPaths.isEmpty()) return
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[PreferencesKeys.TARGET_FAVORITE_FOLDERS] ?: ""
            val favoritesSet = if (currentFavorites.isEmpty()) {
                folderPaths
            } else {
                currentFavorites.split(",").filter { it.isNotBlank() }.toMutableSet().apply {
                    addAll(folderPaths)
                }
            }
            preferences[PreferencesKeys.TARGET_FAVORITE_FOLDERS] = favoritesSet.joinToString(",")
        }
    }

    suspend fun removeSourceFavoriteFolder(folderPath: String) {
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[PreferencesKeys.SOURCE_FAVORITE_FOLDERS] ?: ""
            if (currentFavorites.isNotEmpty()) {
                val favoritesSet = currentFavorites.split(",")
                    .filter { it.isNotBlank() && it != folderPath }
                    .toSet()
                preferences[PreferencesKeys.SOURCE_FAVORITE_FOLDERS] = favoritesSet.joinToString(",")
            }
        }
    }

    suspend fun removeTargetFavoriteFolder(folderPath: String) {
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[PreferencesKeys.TARGET_FAVORITE_FOLDERS] ?: ""
            if (currentFavorites.isNotEmpty()) {
                val favoritesSet = currentFavorites.split(",")
                    .filter { it.isNotBlank() && it != folderPath }
                    .toSet()
                preferences[PreferencesKeys.TARGET_FAVORITE_FOLDERS] = favoritesSet.joinToString(",")
            }
        }
    }

    suspend fun addFavoriteMediaItem(id: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.FAVORITE_MEDIA_ITEMS] ?: ""
            val set = if (current.isEmpty()) {
                setOf(id)
            } else {
                current.split(",").filter { it.isNotBlank() }.toMutableSet().apply { add(id) }
            }
            preferences[PreferencesKeys.FAVORITE_MEDIA_ITEMS] = set.joinToString(",")
        }
    }

    suspend fun removeFavoriteMediaItem(id: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.FAVORITE_MEDIA_ITEMS] ?: ""
            if (current.isNotEmpty()) {
                val set = current.split(",").filter { it.isNotBlank() && it != id }.toSet()
                preferences[PreferencesKeys.FAVORITE_MEDIA_ITEMS] = set.joinToString(",")
            }
        }
    }

    suspend fun clearAllSourceFavorites() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.SOURCE_FAVORITE_FOLDERS)
        }
    }

    suspend fun clearAllTargetFavorites() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.TARGET_FAVORITE_FOLDERS)
        }
    }

    suspend fun setDefaultAlbumCreationPath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_ALBUM_PATH] = path
        }
    }

    suspend fun setFolderBarLayout(layout: FolderBarLayout) = folderBarLayoutPref.set(layout)

    suspend fun setFolderNameLayout(layout: FolderNameLayout) = folderNameLayoutPref.set(layout)

    suspend fun setAddFolderFocusTarget(target: AddFolderFocusTarget) = addFolderFocusTargetPref.set(target)

    suspend fun setSwipeSensitivity(sensitivity: SwipeSensitivity) = swipeSensitivityPref.set(sensitivity)

    suspend fun setSwipeDownAction(action: SwipeDownAction) = swipeDownActionPref.set(action)

    suspend fun setTapAction(action: TapAction) = tapActionPref.set(action)

    suspend fun setDoubleTapAction(action: DoubleTapAction) = doubleTapActionPref.set(action)

    suspend fun setDefaultVideoSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_VIDEO_SPEED] = speed
        }
    }

    suspend fun setScreenshotJpegQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCREENSHOT_JPEG_QUALITY] = quality
        }
    }

    suspend fun setSimilarityThresholdLevel(level: SimilarityThresholdLevel) = similarityThresholdLevelPref.set(level)

    suspend fun setDuplicateScanScope(scope: DuplicateScanScope) = duplicateScanScopePref.set(scope)

    suspend fun setDuplicateScanIncludeList(folders: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DUPLICATE_SCAN_INCLUDE_LIST] = folders.joinToString(",")
        }
    }

    suspend fun setDuplicateScanExcludeList(folders: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DUPLICATE_SCAN_EXCLUDE_LIST] = folders.joinToString(",")
        }
    }

    suspend fun setHasRunDuplicateScanOnce() {
        hasRunDuplicateScanOncePref.set(true)
    }

    suspend fun updateFolderPath(oldPath: String, newPath: String) {
        context.dataStore.edit { preferences ->
            val keysToUpdate = listOf(
                PreferencesKeys.SOURCE_FAVORITE_FOLDERS,
                PreferencesKeys.TARGET_FAVORITE_FOLDERS,
                PreferencesKeys.PREVIOUS_BUCKETS,
                KEY_DEFAULT_ALBUM_PATH,
                PreferencesKeys.PERMANENTLY_SORTED_FOLDERS,
                PreferencesKeys.DUPLICATE_SCAN_INCLUDE_LIST,
                PreferencesKeys.DUPLICATE_SCAN_EXCLUDE_LIST
            )

            keysToUpdate.forEach { key ->
                val currentValue = preferences[key]
                if (currentValue != null && currentValue.contains(oldPath)) {
                    // Use a more robust replacement to avoid partial matches
                    val pathSet = currentValue.split(',').map {
                        if (it == oldPath) newPath else it
                    }.toSet()
                    preferences[key] = pathSet.joinToString(",")
                }
            }
        }
    }

    suspend fun addPermanentlySortedFolder(folderPath: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.PERMANENTLY_SORTED_FOLDERS] ?: ""
            val set = current.split(",").filter { it.isNotBlank() }.toMutableSet()
            set.add(folderPath)
            preferences[PreferencesKeys.PERMANENTLY_SORTED_FOLDERS] = set.joinToString(",")
        }
    }

    suspend fun removePermanentlySortedFolder(folderPath: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.PERMANENTLY_SORTED_FOLDERS] ?: ""
            if (current.isNotEmpty()) {
                val set = current.split(",").filter { it.isNotBlank() && it != folderPath }.toSet()
                preferences[PreferencesKeys.PERMANENTLY_SORTED_FOLDERS] = set.joinToString(",")
            }
        }
    }

    suspend fun clearPermanentlySortedFolders() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.PERMANENTLY_SORTED_FOLDERS)
        }
    }

    suspend fun setUnselectAllInSearchScope(scope: UnselectScanScope) = unselectAllInSearchScopePref.set(scope)

    suspend fun setRecycleBinRetentionDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RECYCLE_BIN_RETENTION_DAYS] = days.coerceIn(1, 30)
        }
    }

    suspend fun setRecycleBinStorageLocation(location: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RECYCLE_BIN_STORAGE_LOCATION] = location
        }
    }

    // ── Boolean setters: delegated to Preference instances ────────────────────

    suspend fun setUseDynamicColors(enabled: Boolean) = useDynamicColorsPref.set(enabled)
    suspend fun setCompactFolderView(enabled: Boolean) = compactFolderViewPref.set(enabled)
    suspend fun setHideFilename(enabled: Boolean) = hideFilenamePref.set(enabled)
    suspend fun setInvertSwipe(enabled: Boolean) = invertSwipePref.set(enabled)
    suspend fun setReduceAnimations(enabled: Boolean) = reduceAnimationsPref.set(enabled)
    suspend fun setHideFromGallery(enabled: Boolean) = hideFromGalleryPref.set(enabled)
    suspend fun setFullScreenSwipe(enabled: Boolean) = fullScreenSwipePref.set(enabled)
    suspend fun setRememberProcessedMedia(enabled: Boolean) = rememberProcessedMediaPref.set(enabled)
    suspend fun setUnfavoriteRemovesFromBar(enabled: Boolean) = unfavoriteRemovesFromBarPref.set(enabled)
    suspend fun setHideSkipButton(enabled: Boolean) = hideSkipButtonPref.set(enabled)
    suspend fun setHidePreviewStrip(enabled: Boolean) = hidePreviewStripPref.set(enabled)
    suspend fun setAddFavoriteToTargetByDefault(enabled: Boolean) = addFavoriteToTargetByDefaultPref.set(enabled)
    suspend fun setHintOnExistingFolderName(enabled: Boolean) = hintOnExistingFolderNamePref.set(enabled)
    suspend fun setShowFavoritesFirstInSetup(enabled: Boolean) = showFavoritesFirstInSetupPref.set(enabled)
    suspend fun setSearchAutofocusEnabled(enabled: Boolean) = searchAutofocusEnabledPref.set(enabled)
    suspend fun setSkipPartialExpansion(enabled: Boolean) = skipPartialExpansionPref.set(enabled)
    suspend fun setUseFullScreenSummarySheet(useFullScreen: Boolean) = useFullScreenSummarySheetPref.set(useFullScreen)
    suspend fun setUseLegacyFolderIcons(enabled: Boolean) = useLegacyFolderIconsPref.set(enabled)
    suspend fun setBottomBarExpanded(isExpanded: Boolean) = bottomBarExpandedPref.set(isExpanded)
    suspend fun setScreenshotDeletesVideo(enabled: Boolean) = screenshotDeletesVideoPref.set(enabled)
    suspend fun setScanAudioEnabled(enabled: Boolean) = scanAudioEnabledPref.set(enabled)
    suspend fun setScanDocumentEnabled(enabled: Boolean) = scanDocumentEnabledPref.set(enabled)
    suspend fun setRecycleBinEnabled(enabled: Boolean) = recycleBinEnabledPref.set(enabled)
    suspend fun setRecycleBinSkipTrashJunk(enabled: Boolean) = recycleBinSkipTrashJunkPref.set(enabled)

    // ── Dialog confirmation setters ──────────────────────────────────────────

    suspend fun setShowConfirmMarkAsSorted(enabled: Boolean) = showConfirmMarkAsSortedPref.set(enabled)
    suspend fun setShowConfirmResetAllHistory(enabled: Boolean) = showConfirmResetAllHistoryPref.set(enabled)
    suspend fun setShowConfirmForgetFolder(enabled: Boolean) = showConfirmForgetFolderPref.set(enabled)
    suspend fun setShowConfirmResetSourceFavs(enabled: Boolean) = showConfirmResetSourceFavsPref.set(enabled)
    suspend fun setShowConfirmResetTargetFavs(enabled: Boolean) = showConfirmResetTargetFavsPref.set(enabled)
    suspend fun setShowConfirmDeleteAllExact(enabled: Boolean) = showConfirmDeleteAllExactPref.set(enabled)

    suspend fun resetDialogConfirmations() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_CONFIRM_MARK_AS_SORTED] = true
            preferences[PreferencesKeys.SHOW_CONFIRM_RESET_ALL_HISTORY] = true
            preferences[PreferencesKeys.SHOW_CONFIRM_FORGET_FOLDER] = true
            preferences[PreferencesKeys.SHOW_CONFIRM_RESET_SOURCE_FAVS] = true
            preferences[PreferencesKeys.SHOW_CONFIRM_RESET_TARGET_FAVS] = true
            preferences[PreferencesKeys.SHOW_CONFIRM_DELETE_ALL_EXACT] = true
        }
    }
}
