package com.cleanify.ui.screens.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.cleanify.R
import com.cleanify.data.repository.AppLocale
import com.cleanify.ui.screens.settings.SettingsViewModel
import com.cleanify.ui.theme.AppTheme
import com.cleanify.util.rememberIsUsingGestureNavigation
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AppearanceSection(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit
) {
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val currentLocale by viewModel.currentLocale.collectAsStateWithLifecycle()
    val useDynamicColors by viewModel.useDynamicColors.collectAsStateWithLifecycle()
    val accentColorKey by viewModel.accentColorKey.collectAsStateWithLifecycle()
    val immersiveMode by viewModel.immersiveMode.collectAsStateWithLifecycle()
    val hideFromGallery by viewModel.hideFromGallery.collectAsStateWithLifecycle()
    val supportsDynamicColors = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val isGestureMode = rememberIsUsingGestureNavigation()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SectionHeader(R.string.theme_section_header)
        SettingsPickerItem(R.string.language_title, R.string.language_desc, AppLocale.entries, currentLocale, { viewModel.setAppLocale(it) }, ::getAppLocaleDisplayName)
        SettingsPickerItem(R.string.theme_title, getThemeDescriptionRes(currentTheme), AppTheme.entries, currentTheme, { viewModel.setTheme(it) }, ::getThemeDisplayName)
        SettingSwitch(R.string.dynamic_colors_title,
            if (supportsDynamicColors) stringResource(R.string.dynamic_colors_desc) else stringResource(R.string.dynamic_colors_req_android_12),
            useDynamicColors, { viewModel.setUseDynamicColors(it) }, supportsDynamicColors)
        AnimatedVisibility(visible = !useDynamicColors || !supportsDynamicColors) {
            AccentColorSetting(currentAccentKey = accentColorKey, onClick = viewModel::showAccentColorDialog)
        }

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        SectionHeader(R.string.accessibility_section_header)

        SettingSwitch(
            titleRes = R.string.immersive_mode_title,
            description = stringResource(R.string.immersive_mode_desc),
            checked = immersiveMode,
            onCheckedChange = { viewModel.setImmersiveMode(it) }
        )
        SettingSwitch(R.string.hide_from_gallery_title, R.string.hide_from_gallery_desc, hideFromGallery, { viewModel.setHideFromGallery(it) })

        Spacer(Modifier.height(if (isGestureMode) 0.dp else 32.dp))
    }
}
