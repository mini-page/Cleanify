package com.cleanify.ui.screens.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.cleanify.R
import com.cleanify.data.repository.DuplicateScanScope
import com.cleanify.ui.components.AppDialog
import com.cleanify.ui.components.BackNavigationIcon
import com.cleanify.ui.components.FolderSearchDialog
import com.cleanify.ui.screens.settings.sections.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.cleanify.ui.components.CleanifySearchBar
import com.cleanify.util.Formatters
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.io.File

private data class SettingsCategory(
    val id: String,
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector
)

private val categories = listOf(
    SettingsCategory("appearance", R.string.appearance_section_title, R.string.appearance_section_desc, Icons.Default.Palette),
    SettingsCategory("sorting", R.string.gestures_section_title, R.string.gestures_section_desc, Icons.AutoMirrored.Filled.Sort),
    SettingsCategory("storage_management", R.string.storage_management_title, R.string.storage_management_desc, Icons.Default.SdCard),
    SettingsCategory("duplicates", R.string.duplicate_finder_section_title, R.string.duplicate_finder_section_desc, Icons.Default.ContentCopy),
    SettingsCategory("cleaner", R.string.cleaner_section_title, R.string.cleaner_section_desc, Icons.Default.CleaningServices),
    SettingsCategory("help", R.string.help_support_section_title, R.string.help_support_section_desc, Icons.AutoMirrored.Filled.HelpOutline),
    SettingsCategory("about", R.string.about_section_title, R.string.about_section_desc, Icons.Default.Info)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToLibraries: () -> Unit,
    onNavigateToCleanerSettings: () -> Unit = {},
    initialPage: String? = null,
    initialHighlight: String? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val debouncedSearchQuery by viewModel.debouncedSearchQuery.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val folderSearchState by viewModel.folderSearchManager.state.collectAsStateWithLifecycle()
    val displayedUnindexedFiles by viewModel.displayedUnindexedFiles.collectAsStateWithLifecycle()
    val accentColorKey by viewModel.accentColorKey.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val isGestureMode = com.cleanify.util.rememberIsUsingGestureNavigation()

    val mainMenuScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(initial = 0) }
    var targetHighlightItem by remember { mutableStateOf<String?>(initialHighlight) }
    var currentPage by rememberSaveable { mutableStateOf<String?>(initialPage) }

    LaunchedEffect(initialPage) {
        if (initialPage == "cleaner") {
            onNavigateToCleanerSettings()
            onNavigateUp()
        } else if (initialPage != null) {
            currentPage = initialPage
        }
    }

    LaunchedEffect(initialHighlight) {
        if (initialHighlight != null) {
            targetHighlightItem = initialHighlight
            kotlinx.coroutines.delay(2500)
            targetHighlightItem = null
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    BackHandler(enabled = uiState.isSearchActive || uiState.searchQuery.isNotEmpty()) {
        viewModel.toggleSearch()
        keyboardController?.hide()
    }

    BackHandler(enabled = currentPage != null && !uiState.isSearchActive && uiState.searchQuery.isEmpty()) {
        if (initialPage != null) {
            onNavigateUp()
        } else {
            currentPage = null
        }
    }

    LaunchedEffect(uiState.isSearchActive) {
        if (uiState.isSearchActive) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            viewModel.toastMessageShown()
        }
    }

    val defaultExportFilename = stringResource(R.string.export_filename_default)
    val exportFavoritesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportTargetFavorites(it) } }
    val importFavoritesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importTargetFavorites(it) } }

    var showFundingDialog by remember { mutableStateOf(false) }

    val pageTitle = currentPage?.let { id ->
        when (id) {
            "appearance" -> R.string.appearance_section_title
            "sorting" -> R.string.gestures_section_title
            "behavior" -> R.string.behavior_section_title
            "storage_management" -> R.string.storage_management_title
            "duplicates" -> R.string.duplicate_finder_section_title
            "media_storage" -> R.string.media_storage_section_title
            "help" -> R.string.help_support_section_title
            "about" -> R.string.about_section_title
            else -> null
        }?.let { stringResource(it) }
    }

    Scaffold(
                topBar = {
            TopAppBar(
                title = {
                    Text(if (currentPage != null) (pageTitle ?: "") else stringResource(R.string.settings))
                },
                navigationIcon = {
                    BackNavigationIcon(
                        onClick = { if (currentPage != null) { if (initialPage != null) onNavigateUp() else currentPage = null } else onNavigateUp() },
                        contentDescription = stringResource(R.string.navigate_back)
                    )
                },
                actions = {
                    val isSearching = uiState.isSearchActive || uiState.searchQuery.isNotEmpty()
                    IconButton(onClick = viewModel::toggleSearch) {
                        Icon(
                            imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.Search,
                            contentDescription = if (isSearching) stringResource(R.string.close_search) else stringResource(R.string.search_settings_icon_desc),
                            tint = if (isSearching) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AnimatedVisibility(
                visible = uiState.isSearchActive || uiState.searchQuery.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                CleanifySearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    placeholder = stringResource(R.string.search_settings_placeholder),
                    height = 48.dp,
                    focusRequester = focusRequester,
                    onSearchAction = { keyboardController?.hide() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                CompositionLocalProvider(LocalHighlightedSettingId provides targetHighlightItem) {
                    if (debouncedSearchQuery.isNotBlank()) {
                        SettingsMainMenu(
                            categories = categories,
                            searchQuery = debouncedSearchQuery,
                            viewModel = viewModel,
                            onCategoryClick = { catId ->
                                keyboardController?.hide()
                                viewModel.toggleSearch()
                                currentPage = catId
                            },
                            onCleanerClick = {
                                keyboardController?.hide()
                                viewModel.toggleSearch()
                                onNavigateToCleanerSettings()
                            },
                            onSettingClick = { item ->
                                keyboardController?.hide()
                                viewModel.toggleSearch()
                                if (item.sectionId == "cleaner") {
                                    onNavigateToCleanerSettings()
                                } else {
                                    currentPage = item.sectionId
                                    targetHighlightItem = item.id
                                    scope.launch {
                                        kotlinx.coroutines.delay(2500)
                                        targetHighlightItem = null
                                    }
                                }
                            },
                            scrollState = rememberScrollState()
                        )
                    } else {
                        AnimatedContent(
                            targetState = currentPage,
                            transitionSpec = {
                                if (targetState != null && initialState == null) {
                                    (slideInHorizontally(animationSpec = tween(220)) { width -> width / 4 } + fadeIn(animationSpec = tween(220)))
                                        .togetherWith(slideOutHorizontally(animationSpec = tween(180)) { width -> -width / 4 } + fadeOut(animationSpec = tween(180)))
                                } else if (targetState == null && initialState != null) {
                                    (slideInHorizontally(animationSpec = tween(220)) { width -> -width / 4 } + fadeIn(animationSpec = tween(220)))
                                        .togetherWith(slideOutHorizontally(animationSpec = tween(180)) { width -> width / 4 } + fadeOut(animationSpec = tween(180)))
                                } else {
                                    fadeIn(animationSpec = tween(200)).togetherWith(fadeOut(animationSpec = tween(200)))
                                }
                            },
                            label = "SettingsPageTransition"
                        ) { page ->
                            when (page) {
                                "appearance" -> AppearanceSection(
                                    viewModel = viewModel,
                                    onNavigateUp = { if (initialPage != null) onNavigateUp() else currentPage = null }
                                )
                                "sorting", "behavior", "media_storage" -> SortingSection(viewModel = viewModel)
                                "storage_management" -> StorageManagementSection(
                                    viewModel = viewModel,
                                    onNavigateToCleaner = onNavigateToCleanerSettings
                                )
                                "duplicates" -> DuplicatesSection(viewModel = viewModel)
                                "help" -> HelpSection(
                                    viewModel = viewModel,
                                    exportFavoritesLauncher = exportFavoritesLauncher,
                                    importFavoritesLauncher = importFavoritesLauncher,
                                    defaultExportFilename = defaultExportFilename
                                )
                                "about" -> AboutSection(
                                    viewModel = viewModel,
                                    onNavigateToLibraries = onNavigateToLibraries,
                                    onShowFunding = { showFundingDialog = true }
                                )
                                else -> SettingsMainMenu(
                                    categories = categories,
                                    searchQuery = "",
                                    viewModel = viewModel,
                                    onCategoryClick = { currentPage = it },
                                    onCleanerClick = onNavigateToCleanerSettings,
                                    onSettingClick = { item ->
                                        if (item.sectionId == "cleaner") {
                                            onNavigateToCleanerSettings()
                                        } else {
                                            currentPage = item.sectionId
                                            targetHighlightItem = item.id
                                            scope.launch {
                                                kotlinx.coroutines.delay(2500)
                                                targetHighlightItem = null
                                            }
                                        }
                                    },
                                    scrollState = mainMenuScrollState
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFundingDialog) FundingDialog(onDismiss = { showFundingDialog = false })

    if (uiState.showUnindexedFilesDialog) {
        UnindexedFilesDialog(
            filePaths = displayedUnindexedFiles,
            totalUnindexedCount = uiState.unindexedFilePaths.size,
            showHidden = uiState.showHiddenUnindexedFiles,
            onToggleShowHidden = viewModel::toggleShowHiddenUnindexedFiles,
            onDismiss = viewModel::dismissUnindexedFilesDialog
        )
    }
    if (uiState.showAccentColorDialog) {
        AccentColorDialog(
            currentAccentKey = accentColorKey,
            onDismiss = viewModel::dismissAccentColorDialog,
            onColorSelected = viewModel::setAccentColor
        )
    }
    if (uiState.showDuplicateScanScopeDialog) {
        val duplicateScanScope = viewModel.duplicateScanScope.collectAsStateWithLifecycle().value
        val duplicateScanIncludeList = viewModel.duplicateScanIncludeList.collectAsStateWithLifecycle().value
        val duplicateScanExcludeList = viewModel.duplicateScanExcludeList.collectAsStateWithLifecycle().value
        val title = if (duplicateScanScope == DuplicateScanScope.INCLUDE_LIST) stringResource(R.string.manage_include_list) else stringResource(R.string.manage_exclude_list)
        val folderList = if (duplicateScanScope == DuplicateScanScope.INCLUDE_LIST) duplicateScanIncludeList else duplicateScanExcludeList
        DuplicateScanScopeManagementDialog(
            title = title,
            folderList = folderList.toList(),
            onDismiss = viewModel::dismissDuplicateScanScopeDialog,
            onAddFolder = { viewModel.showDuplicateScanScopeFolderSearch(duplicateScanScope == DuplicateScanScope.INCLUDE_LIST) },
            onRemoveFolder = viewModel::removeFolderFromScanScopeList
        )
    }
    if (uiState.showDuplicateScanScopeFolderSearch) {
        FolderSearchDialog(
            state = folderSearchState,
            title = stringResource(R.string.add_folder),
            searchLabel = stringResource(R.string.search_hint) + "\u2026",
            confirmButtonText = stringResource(R.string.confirm),
            autoConfirmOnSelection = false,
            onDismiss = viewModel::dismissFolderSearchDialog,
            onQueryChanged = viewModel.folderSearchManager::updateSearchQuery,
            onFolderSelected = viewModel::onPathSelected,
            onConfirm = {
                val selectedPath = folderSearchState.browsePath
                if (selectedPath != null) viewModel.addFolderToScanScopeList(selectedPath)
            },
            onSearch = { scope.launch { viewModel.folderSearchManager.selectSingleResultOrSelf() } },
            formatListItemTitle = Formatters::pathForDisplay
        )
    }
    if (uiState.showDefaultPathSearchDialog) {
        FolderSearchDialog(
            state = folderSearchState,
            title = stringResource(R.string.default_album_location_title),
            searchLabel = stringResource(R.string.search_hint) + "\u2026",
            confirmButtonText = stringResource(R.string.confirm),
            autoConfirmOnSelection = false,
            onDismiss = viewModel::dismissFolderSearchDialog,
            onQueryChanged = viewModel.folderSearchManager::updateSearchQuery,
            onFolderSelected = viewModel::onPathSelected,
            onConfirm = viewModel::confirmDefaultPathSelection,
            onSearch = { scope.launch { viewModel.folderSearchManager.selectSingleResultOrSelf() } },
            formatListItemTitle = Formatters::pathForDisplay
        )
    }
    if (uiState.showForgetMediaSearchDialog) {
        FolderSearchDialog(
            state = folderSearchState,
            title = stringResource(R.string.forget_sorted_media_title),
            searchLabel = stringResource(R.string.search_hint) + "\u2026",
            confirmButtonText = stringResource(R.string.forget_action),
            autoConfirmOnSelection = true,
            onDismiss = viewModel::dismissFolderSearchDialog,
            onQueryChanged = viewModel.folderSearchManager::updateSearchQuery,
            onFolderSelected = { path -> viewModel.forgetSortedMediaInFolder(path) },
            onConfirm = {},
            onSearch = { scope.launch { viewModel.folderSearchManager.selectSingleResultOrSelf() } },
            formatListItemTitle = Formatters::pathForDisplay
        )
    }
    if (uiState.showConfirmForgetFolderDialog) {
        AppDialog(
            onDismissRequest = { viewModel.dismissDialog("forgetFolder") },
            showDontAskAgain = true,
            dontAskAgainChecked = uiState.dontAskAgainForgetFolder,
            onDontAskAgainChanged = { viewModel.onDontAskAgainChanged("forgetFolder", it) },
            title = { Text(stringResource(R.string.forget_confirm_title)) },
            text = { Text(stringResource(R.string.forget_confirm_body, File(uiState.folderToForget ?: "").name)) },
            buttons = {
                TextButton(onClick = { viewModel.dismissDialog("forgetFolder") }) { Text(stringResource(R.string.cancel)) }
                Button(onClick = viewModel::confirmForgetSortedMediaInFolder) { Text(stringResource(R.string.confirm)) }
            }
        )
    }
    if (uiState.showResetDialogsConfirmation) {
        AppDialog(
            onDismissRequest = { viewModel.dismissDialog("resetWarnings") },
            title = { Text(stringResource(R.string.reset_all_warnings_title)) },
            text = { Text(stringResource(R.string.reset_all_warnings_body)) },
            buttons = {
                TextButton(onClick = { viewModel.dismissDialog("resetWarnings") }) { Text(stringResource(R.string.cancel)) }
                Button(onClick = viewModel::confirmResetDialogWarnings) { Text(stringResource(R.string.reset)) }
            }
        )
    }
    if (uiState.showResetHistoryConfirmation) {
        AppDialog(
            onDismissRequest = { viewModel.dismissDialog("resetHistory") },
            showDontAskAgain = true,
            dontAskAgainChecked = uiState.dontAskAgainResetHistory,
            onDontAskAgainChanged = { viewModel.onDontAskAgainChanged("resetHistory", it) },
            title = { Text(stringResource(R.string.reset_history_title)) },
            text = { Text(stringResource(R.string.reset_history_body)) },
            buttons = {
                TextButton(onClick = { viewModel.dismissDialog("resetHistory") }) { Text(stringResource(R.string.cancel)) }
                Button(onClick = viewModel::confirmResetHistory) { Text(stringResource(R.string.reset)) }
            }
        )
    }
    if (uiState.showResetSourceFavoritesConfirmation) {
        AppDialog(
            onDismissRequest = { viewModel.dismissDialog("resetSource") },
            showDontAskAgain = true,
            dontAskAgainChecked = uiState.dontAskAgainResetSourceFavorites,
            onDontAskAgainChanged = { viewModel.onDontAskAgainChanged("resetSource", it) },
            title = { Text(stringResource(R.string.reset_source_favs_title)) },
            text = { Text(stringResource(R.string.reset_source_favs_body)) },
            buttons = {
                TextButton(onClick = { viewModel.dismissDialog("resetSource") }) { Text(stringResource(R.string.cancel)) }
                Button(onClick = viewModel::confirmClearSourceFavorites) { Text(stringResource(R.string.reset)) }
            }
        )
    }
    if (uiState.showResetTargetFavoritesConfirmation) {
        AppDialog(
            onDismissRequest = { viewModel.dismissDialog("resetTarget") },
            showDontAskAgain = true,
            dontAskAgainChecked = uiState.dontAskAgainResetTargetFavorites,
            onDontAskAgainChanged = { viewModel.onDontAskAgainChanged("resetTarget", it) },
            title = { Text(stringResource(R.string.reset_target_favs_title)) },
            text = { Text(stringResource(R.string.reset_target_favs_body)) },
            buttons = {
                TextButton(onClick = { viewModel.dismissDialog("resetTarget") }) { Text(stringResource(R.string.cancel)) }
                Button(onClick = viewModel::confirmClearTargetFavorites) { Text(stringResource(R.string.reset)) }
            }
        )
    }

    val missingFolders = uiState.missingImportedFolders
    if (missingFolders != null) {
        AppDialog(
            onDismissRequest = { viewModel.dismissMissingFoldersDialog() },
            title = { Text(stringResource(R.string.some_folders_not_found_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.some_folders_not_found_body))
                    Spacer(Modifier.height(8.dp))
                    Column(modifier = Modifier.heightIn(max = 150.dp).verticalScroll(rememberScrollState())) {
                        missingFolders.forEach { path ->
                            Text(".../${path.takeLast(35)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            buttons = {
                TextButton(onClick = { viewModel.dismissMissingFoldersDialog() }) { Text(stringResource(R.string.skip)) }
                Button(onClick = { viewModel.createAndImportMissingFolders() }) { Text(stringResource(R.string.create)) }
            }
        )
    }
}

@Composable
private fun SettingsMainMenu(
    categories: List<SettingsCategory>,
    searchQuery: String,
    viewModel: SettingsViewModel,
    onCategoryClick: (String) -> Unit,
    onCleanerClick: () -> Unit = {},
    onSettingClick: (SearchableSettingItem) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState()
) {
    val resources = LocalResources.current
    val isSearching = searchQuery.isNotBlank()

    val filteredSettings = remember(searchQuery) {
        if (!isSearching) emptyList() else {
            allSearchableSettings.filter { item ->
                val title = resources.getString(item.titleRes)
                val desc = item.descRes?.let { resources.getString(it) } ?: item.customDesc ?: ""
                title.contains(searchQuery, ignoreCase = true) ||
                    desc.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredCategories = remember(searchQuery) {
        if (!isSearching) categories else {
            categories.filter { cat ->
                val title = resources.getString(cat.titleRes)
                val desc = resources.getString(cat.descRes)
                title.contains(searchQuery, ignoreCase = true) ||
                    desc.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isSearching && filteredSettings.isEmpty() && filteredCategories.isEmpty()) {
            Text(
                text = stringResource(R.string.no_settings_found, searchQuery),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        // 1. Search Results
        if (isSearching) {
            if (filteredSettings.isNotEmpty()) {
                FrostedSettingsGroup(title = "Settings (${filteredSettings.size})") {
                    filteredSettings.forEachIndexed { index, setting ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSettingClick(setting) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = setting.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${stringResource(setting.sectionTitleRes)} ›",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(1.dp))
                                Text(
                                    text = stringResource(setting.titleRes),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                val desc = setting.descRes?.let { stringResource(it) } ?: setting.customDesc
                                if (!desc.isNullOrBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        if (index < filteredSettings.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }

            if (filteredCategories.isNotEmpty()) {
                FrostedSettingsGroup(title = "Sections (${filteredCategories.size})") {
                    filteredCategories.forEachIndexed { index, category ->
                        SettingsCategoryRow(
                            category = category,
                            onClick = {
                                if (category.id == "cleaner") onCleanerClick() else onCategoryClick(category.id)
                            }
                        )
                        if (index < filteredCategories.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }
        } else {
            // 2. Normal grouped categories
            val preferenceCategories = remember(filteredCategories) {
                filteredCategories.filter { it.id in listOf("appearance", "sorting") }
            }
            val toolCategories = remember(filteredCategories) {
                filteredCategories.filter { it.id in listOf("storage_management", "duplicates", "cleaner") }
            }
            val supportCategories = remember(filteredCategories) {
                filteredCategories.filter { it.id in listOf("help", "about") }
            }

            if (preferenceCategories.isNotEmpty()) {
                FrostedSettingsGroup(title = "Preferences") {
                    preferenceCategories.forEachIndexed { index, category ->
                        SettingsCategoryRow(
                            category = category,
                            onClick = { onCategoryClick(category.id) }
                        )
                        if (index < preferenceCategories.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }

            if (toolCategories.isNotEmpty()) {
                FrostedSettingsGroup(title = "Tools") {
                    toolCategories.forEachIndexed { index, category ->
                        SettingsCategoryRow(
                            category = category,
                            onClick = {
                                if (category.id == "cleaner") onCleanerClick() else onCategoryClick(category.id)
                            }
                        )
                        if (index < toolCategories.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }

            if (supportCategories.isNotEmpty()) {
                FrostedSettingsGroup(title = "Support & About") {
                    supportCategories.forEachIndexed { index, category ->
                        SettingsCategoryRow(
                            category = category,
                            onClick = { onCategoryClick(category.id) }
                        )
                        if (index < supportCategories.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryRow(
    category: SettingsCategory,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(category.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(category.descRes),
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
private fun UnindexedFilesDialog(filePaths: List<String>, totalUnindexedCount: Int, showHidden: Boolean, onToggleShowHidden: () -> Unit, onDismiss: () -> Unit) {
    val groupedFiles = remember(filePaths) { filePaths.groupBy { File(it).parent ?: "Unknown Location" } }
    val unknownLocation = stringResource(R.string.unknown_location)
    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.unindexed_files_dialog_title)) },
        text = {
            Column {
                if (filePaths.isEmpty() && totalUnindexedCount > 0) Text(stringResource(R.string.unindexed_files_ideal_state), style = MaterialTheme.typography.bodyMedium)
                else if (filePaths.isEmpty()) Text(stringResource(R.string.no_unindexed_files_found), style = MaterialTheme.typography.bodyMedium)
                if (filePaths.isNotEmpty()) {
                    LazyColumn(Modifier.weight(1f, false).fillMaxWidth().heightIn(max = 350.dp)) {
                        item {
                            val desc = if (showHidden) stringResource(R.string.unindexed_files_system_desc) else stringResource(R.string.unindexed_files_user_desc)
                            Text(desc, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        groupedFiles.forEach { (directory, files) ->
                            item {
                                Text(if (directory == "Unknown Location") unknownLocation else directory, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                HorizontalDivider()
                            }
                            items(files) { fp -> Text("${File(fp).parentFile?.name ?: "..."}/${File(fp).name}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth().clickable(onClick = onToggleShowHidden).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showHidden, onCheckedChange = { onToggleShowHidden() })
                    Text(stringResource(R.string.show_hidden_temp_files))
                }
            }
        },
        buttons = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}

@Composable
private fun FundingDialog(onDismiss: () -> Unit) {
    val clipboard = androidx.compose.ui.platform.LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val upiId = "raghavans5711@okicici"

    // Detect installed UPI payment apps
    val installedUpiApps = remember {
        val upiIntent = Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay"))
        try {
            val pm = context.packageManager
            pm.queryIntentActivities(upiIntent, PackageManager.MATCH_DEFAULT_ONLY).mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                val name = resolveInfo.loadLabel(pm).toString()
                val iconBitmap = try {
                    val drawable = resolveInfo.loadIcon(pm)
                    drawable.toBitmap(width = 96, height = 96).asImageBitmap()
                } catch (e: Exception) {
                    null
                }
                InstalledPaymentApp(name = name, packageName = pkg, iconBitmap = iconBitmap)
            }.distinctBy { it.packageName }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val launchUpi: (String?) -> Unit = { targetPackage ->
        val uri = Uri.parse("upi://pay?pa=$upiId&pn=Raghav%20S&mc=&tid=&tr=&tn=Support%20Cleanify%20Development&cu=INR")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (targetPackage != null) {
            intent.setPackage(targetPackage)
        }
        try {
            context.startActivity(if (targetPackage != null) intent else Intent.createChooser(intent, "Pay via UPI"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Could not open UPI app: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    AppDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.funding_dialog_title), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.funding_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))

                // Compact QR Code Container (without outer padding or header text)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    shadowElevation = 2.dp
                ) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.payment_qr),
                        contentDescription = "Payment QR",
                        modifier = Modifier
                            .size(190.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Understated Name & UPI ID Container with Dedicated Copy Button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Raghav S",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = upiId,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = {
                                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                cm.setPrimaryClip(android.content.ClipData.newPlainText("UPI ID", upiId))
                                android.widget.Toast.makeText(context, "UPI ID copied: $upiId", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Copy", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Installed Payment Apps as clean consistent rows
                if (installedUpiApps.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Pay with Installed App",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.height(6.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        installedUpiApps.forEach { app ->
                            Surface(
                                onClick = { launchUpi(app.packageName) },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (app.iconBitmap != null) {
                                        Image(
                                            bitmap = app.iconBitmap,
                                            contentDescription = app.name,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Payment,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Text(
                                        text = app.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Open ${app.name}",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = { launchUpi(null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open UPI App")
                }
            }
        },
        buttons = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}
