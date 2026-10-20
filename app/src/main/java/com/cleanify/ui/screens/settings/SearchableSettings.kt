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

package com.cleanify.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.cleanify.R

sealed interface SettingItemType {
    data class SwitchItem(
        val isChecked: (SettingsViewModel) -> Boolean,
        val onToggle: (SettingsViewModel, Boolean) -> Unit
    ) : SettingItemType

    data class ActionItem(
        val onClick: (SettingsViewModel) -> Unit
    ) : SettingItemType

    object NavigationItem : SettingItemType
}

data class SearchableSettingItem(
    val id: String,
    val titleRes: Int,
    val descRes: Int? = null,
    val customDesc: String? = null,
    val sectionId: String,
    val sectionTitleRes: Int,
    val icon: ImageVector,
    val itemType: SettingItemType
)

val allSearchableSettings = listOf(
    // Appearance
    SearchableSettingItem(
        id = "theme",
        titleRes = R.string.theme_title,
        customDesc = "Select light, dark, or system theme",
        sectionId = "appearance",
        sectionTitleRes = R.string.appearance_section_title,
        icon = Icons.Default.Palette,
        itemType = SettingItemType.NavigationItem
    ),
    SearchableSettingItem(
        id = "language",
        titleRes = R.string.language_title,
        descRes = R.string.language_desc,
        sectionId = "appearance",
        sectionTitleRes = R.string.appearance_section_title,
        icon = Icons.Default.Language,
        itemType = SettingItemType.NavigationItem
    ),
    SearchableSettingItem(
        id = "accent_color",
        titleRes = R.string.accent_color_title,
        customDesc = "Customize UI accent colors",
        sectionId = "appearance",
        sectionTitleRes = R.string.appearance_section_title,
        icon = Icons.Default.ColorLens,
        itemType = SettingItemType.ActionItem { vm -> vm.showAccentColorDialog() }
    ),
    SearchableSettingItem(
        id = "dynamic_colors",
        titleRes = R.string.dynamic_colors_title,
        descRes = R.string.dynamic_colors_desc,
        sectionId = "appearance",
        sectionTitleRes = R.string.appearance_section_title,
        icon = Icons.Default.AutoAwesome,
        itemType = SettingItemType.SwitchItem(
            isChecked = { it.useDynamicColors.value },
            onToggle = { vm, checked -> vm.setUseDynamicColors(checked) }
        )
    ),
    SearchableSettingItem(
        id = "reduce_animations",
        titleRes = R.string.reduce_animations_title,
        descRes = R.string.reduce_animations_desc,
        sectionId = "appearance",
        sectionTitleRes = R.string.appearance_section_title,
        icon = Icons.Default.SlowMotionVideo,
        itemType = SettingItemType.SwitchItem(
            isChecked = { it.reduceAnimations.value },
            onToggle = { vm, checked -> vm.setReduceAnimations(checked) }
        )
    ),
    SearchableSettingItem(
        id = "search_autofocus",
        titleRes = R.string.search_autofocus_title,
        descRes = R.string.search_autofocus_desc,
        sectionId = "appearance",
        sectionTitleRes = R.string.appearance_section_title,
        icon = Icons.Default.Search,
        itemType = SettingItemType.SwitchItem(
            isChecked = { it.searchAutofocusEnabled.value },
            onToggle = { vm, checked -> vm.setSearchAutofocusEnabled(checked) }
        )
    ),

    // Sorting & Behavior
    SearchableSettingItem(
        id = "invert_swipe",
        titleRes = R.string.invert_swipe_title,
        descRes = R.string.invert_swipe_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.Swipe,
        itemType = SettingItemType.SwitchItem(
            isChecked = { it.invertSwipe.value },
            onToggle = { vm, checked -> vm.setInvertSwipe(checked) }
        )
    ),
    SearchableSettingItem(
        id = "fullscreen_swipe",
        titleRes = R.string.full_screen_swipe_title,
        descRes = R.string.full_screen_swipe_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.Fullscreen,
        itemType = SettingItemType.SwitchItem(
            isChecked = { it.fullScreenSwipe.value },
            onToggle = { vm, checked -> vm.setFullScreenSwipe(checked) }
        )
    ),
    SearchableSettingItem(
        id = "swipe_up_action",
        titleRes = R.string.swipe_up_action_title,
        descRes = R.string.swipe_up_action_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.ArrowUpward,
        itemType = SettingItemType.NavigationItem
    ),
    SearchableSettingItem(
        id = "swiper_batch_size",
        titleRes = R.string.swiper_batch_size_title,
        descRes = R.string.swiper_batch_size_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.Queue,
        itemType = SettingItemType.NavigationItem
    ),
    SearchableSettingItem(
        id = "recycle_bin_toggle",
        titleRes = R.string.use_recycle_bin_title,
        descRes = R.string.use_recycle_bin_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.DeleteSweep,
        itemType = SettingItemType.SwitchItem(
            isChecked = { it.recycleBinEnabled.value },
            onToggle = { vm, checked -> vm.setRecycleBinEnabled(checked) }
        )
    ),
    SearchableSettingItem(
        id = "swipe_sensitivity",
        titleRes = R.string.swipe_sensitivity_title,
        descRes = R.string.swipe_sensitivity_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.Speed,
        itemType = SettingItemType.NavigationItem
    ),
    SearchableSettingItem(
        id = "swipe_down_action",
        titleRes = R.string.swipe_down_action_title,
        descRes = R.string.swipe_down_action_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.ArrowDownward,
        itemType = SettingItemType.NavigationItem
    ),
    SearchableSettingItem(
        id = "tap_action",
        titleRes = R.string.tap_action_title,
        descRes = R.string.tap_action_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.TouchApp,
        itemType = SettingItemType.NavigationItem
    ),
    SearchableSettingItem(
        id = "double_tap_action",
        titleRes = R.string.double_tap_action_title,
        descRes = R.string.double_tap_action_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.Favorite,
        itemType = SettingItemType.NavigationItem
    ),
    SearchableSettingItem(
        id = "compact_folder_view",
        titleRes = R.string.compact_folder_view_title,
        descRes = R.string.compact_folder_view_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.ViewCompact,
        itemType = SettingItemType.SwitchItem(
            isChecked = { it.compactFolderView.value },
            onToggle = { vm, checked -> vm.setCompactFolderView(checked) }
        )
    ),
    SearchableSettingItem(
        id = "hide_filename",
        titleRes = R.string.hide_media_filename_title,
        descRes = R.string.hide_media_filename_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.VisibilityOff,
        itemType = SettingItemType.SwitchItem(
            isChecked = { it.hideFilename.value },
            onToggle = { vm, checked -> vm.setHideFilename(checked) }
        )
    ),
    SearchableSettingItem(
        id = "hide_skip_button",
        titleRes = R.string.hide_skip_button_title,
        descRes = R.string.hide_skip_button_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.SkipNext,
        itemType = SettingItemType.SwitchItem(
            isChecked = { it.hideSkipButton.value },
            onToggle = { vm, checked -> vm.setHideSkipButton(checked) }
        )
    ),
    SearchableSettingItem(
        id = "hide_action_buttons",
        titleRes = R.string.hide_action_buttons_title,
        descRes = R.string.hide_action_buttons_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.SmartButton,
        itemType = SettingItemType.SwitchItem(
            isChecked = { it.hideActionButtons.value },
            onToggle = { vm, checked -> vm.setHideActionButtons(checked) }
        )
    ),
    SearchableSettingItem(
        id = "folder_selection_mode",
        titleRes = R.string.folder_selection_mode_title,
        customDesc = "Select how folders are chosen by default",
        sectionId = "appearance",
        sectionTitleRes = R.string.appearance_section_title,
        icon = Icons.Default.Folder,
        itemType = SettingItemType.NavigationItem
    ),
    SearchableSettingItem(
        id = "default_album_location",
        titleRes = R.string.default_album_location_title,
        descRes = R.string.default_album_location_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.FolderOpen,
        itemType = SettingItemType.ActionItem { vm -> vm.showDefaultPathSearchDialog() }
    ),
    SearchableSettingItem(
        id = "remember_sorted_media",
        titleRes = R.string.remember_organized_media_title,
        descRes = R.string.remember_organized_media_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.History,
        itemType = SettingItemType.SwitchItem(
            isChecked = { it.rememberProcessedMedia.value },
            onToggle = { vm, checked -> vm.setRememberProcessedMedia(checked) }
        )
    ),
    SearchableSettingItem(
        id = "forget_sorted_media",
        titleRes = R.string.forget_sorted_media_folder_title,
        descRes = R.string.forget_sorted_media_folder_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.DeleteSweep,
        itemType = SettingItemType.ActionItem { vm -> vm.showForgetMediaSearchDialog() }
    ),
    SearchableSettingItem(
        id = "start_videos_unmuted",
        titleRes = R.string.start_videos_unmuted_title,
        descRes = R.string.start_videos_unmuted_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.VolumeUp,
        itemType = SettingItemType.SwitchItem(
            isChecked = { it.startVideosUnmuted.value },
            onToggle = { vm, checked -> vm.setStartVideosUnmuted(checked) }
        )
    ),
    SearchableSettingItem(
        id = "default_video_speed",
        titleRes = R.string.default_video_speed_title,
        descRes = R.string.default_video_speed_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.SlowMotionVideo,
        itemType = SettingItemType.NavigationItem
    ),
    SearchableSettingItem(
        id = "immersive_mode",
        titleRes = R.string.immersive_mode_title,
        descRes = R.string.immersive_mode_desc,
        sectionId = "appearance",
        sectionTitleRes = R.string.appearance_section_title,
        icon = Icons.Default.Fullscreen,
        itemType = SettingItemType.SwitchItem(
            isChecked = { it.immersiveMode.value },
            onToggle = { vm, checked -> vm.setImmersiveMode(checked) }
        )
    ),
    SearchableSettingItem(
        id = "scan_audio",
        titleRes = R.string.scan_audio_title,
        descRes = R.string.scan_audio_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.Audiotrack,
        itemType = SettingItemType.SwitchItem(
            isChecked = { it.scanAudioEnabled.value },
            onToggle = { vm, checked -> vm.setScanAudioEnabled(checked) }
        )
    ),
    SearchableSettingItem(
        id = "scan_video",
        titleRes = R.string.scan_video_title,
        descRes = R.string.scan_video_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.Videocam,
        itemType = SettingItemType.SwitchItem(
            isChecked = { it.scanVideoEnabled.value },
            onToggle = { vm, checked -> vm.setScanVideoEnabled(checked) }
        )
    ),
    SearchableSettingItem(
        id = "scan_document",
        titleRes = R.string.scan_document_title,
        descRes = R.string.scan_document_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.Description,
        itemType = SettingItemType.SwitchItem(
            isChecked = { it.scanDocumentEnabled.value },
            onToggle = { vm, checked -> vm.setScanDocumentEnabled(checked) }
        )
    ),
    SearchableSettingItem(
        id = "screenshot_deletes_video",
        titleRes = R.string.screenshot_deletes_video_title,
        descRes = R.string.screenshot_deletes_video_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.CameraAlt,
        itemType = SettingItemType.SwitchItem(
            isChecked = { it.screenshotDeletesVideo.value },
            onToggle = { vm, checked -> vm.setScreenshotDeletesVideo(checked) }
        )
    ),
    SearchableSettingItem(
        id = "screenshot_quality",
        titleRes = R.string.screenshot_quality_title,
        descRes = R.string.screenshot_quality_desc,
        sectionId = "sorting",
        sectionTitleRes = R.string.gestures_section_title,
        icon = Icons.Default.HighQuality,
        itemType = SettingItemType.NavigationItem
    ),

    // Duplicates
    SearchableSettingItem(
        id = "similarity_level",
        titleRes = R.string.similarity_level_title,
        customDesc = "Adjust sensitivity for detecting similar files",
        sectionId = "duplicates",
        sectionTitleRes = R.string.duplicate_finder_section_title,
        icon = Icons.Default.Tune,
        itemType = SettingItemType.NavigationItem
    ),
    SearchableSettingItem(
        id = "scan_scope",
        titleRes = R.string.scan_scope_title,
        customDesc = "Control which folders are scanned for duplicates",
        sectionId = "duplicates",
        sectionTitleRes = R.string.duplicate_finder_section_title,
        icon = Icons.Default.FolderSpecial,
        itemType = SettingItemType.NavigationItem
    ),

    // Help & Support
    SearchableSettingItem(
        id = "shake_to_feedback",
        titleRes = R.string.feedback_dialog_title,
        customDesc = "Shake your phone anytime to open feedback",
        sectionId = "help",
        sectionTitleRes = R.string.help_support_section_title,
        icon = Icons.Default.Vibration,
        itemType = SettingItemType.SwitchItem(
            isChecked = { it.shakeToFeedbackEnabled.value },
            onToggle = { vm, checked -> vm.setShakeToFeedbackEnabled(checked) }
        )
    ),
    SearchableSettingItem(
        id = "screenshot_to_feedback",
        titleRes = R.string.feedback_dialog_title,
        customDesc = "Taking a screenshot prompts for feedback",
        sectionId = "help",
        sectionTitleRes = R.string.help_support_section_title,
        icon = Icons.Default.Screenshot,
        itemType = SettingItemType.SwitchItem(
            isChecked = { it.screenshotToFeedbackEnabled.value },
            onToggle = { vm, checked -> vm.setScreenshotToFeedbackEnabled(checked) }
        )
    ),
    SearchableSettingItem(
        id = "onboarding_tutorial",
        titleRes = R.string.onboarding_tutorial_title,
        descRes = R.string.onboarding_tutorial_desc,
        sectionId = "help",
        sectionTitleRes = R.string.help_support_section_title,
        icon = Icons.Default.School,
        itemType = SettingItemType.ActionItem { vm -> vm.resetOnboarding() }
    ),
    SearchableSettingItem(
        id = "reset_warnings",
        titleRes = R.string.reset_dialog_warnings_title,
        descRes = R.string.reset_dialog_warnings_desc,
        sectionId = "help",
        sectionTitleRes = R.string.help_support_section_title,
        icon = Icons.Default.RestartAlt,
        itemType = SettingItemType.ActionItem { vm -> vm.resetDialogWarnings() }
    ),

    // About
    SearchableSettingItem(
        id = "check_updates",
        titleRes = R.string.auto_update_check_title,
        descRes = R.string.auto_update_check_desc,
        sectionId = "about",
        sectionTitleRes = R.string.about_section_title,
        icon = Icons.Default.SystemUpdate,
        itemType = SettingItemType.NavigationItem
    ),
    SearchableSettingItem(
        id = "support_development",
        titleRes = R.string.support_development_title,
        descRes = R.string.support_development_desc,
        sectionId = "about",
        sectionTitleRes = R.string.about_section_title,
        icon = Icons.Default.VolunteerActivism,
        itemType = SettingItemType.NavigationItem
    ),
    SearchableSettingItem(
        id = "libraries",
        titleRes = R.string.open_source_licenses_title,
        descRes = R.string.open_source_licenses_desc,
        sectionId = "about",
        sectionTitleRes = R.string.about_section_title,
        icon = Icons.Default.Code,
        itemType = SettingItemType.NavigationItem
    )
)
