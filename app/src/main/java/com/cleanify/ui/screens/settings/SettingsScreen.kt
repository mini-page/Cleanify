package com.cleanify.ui.screens.settings

import android.content.ClipData
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.cleanify.R
import com.cleanify.data.repository.AddFolderFocusTarget
import com.cleanify.data.repository.AppLocale
import com.cleanify.data.repository.DuplicateScanScope
import com.cleanify.data.repository.FolderBarLayout
import com.cleanify.data.repository.FolderNameLayout
import com.cleanify.data.repository.FolderSelectionMode
import com.cleanify.data.repository.SimilarityThresholdLevel
import com.cleanify.data.repository.SwipeDownAction
import com.cleanify.data.repository.SwipeSensitivity
import com.cleanify.data.repository.UnselectScanScope
import com.cleanify.ui.components.AppDialog
import com.cleanify.ui.components.BackNavigationIcon
import com.cleanify.ui.components.FolderSearchDialog
import com.cleanify.ui.theme.AppTheme
import com.cleanify.ui.theme.predefinedAccentColors
import com.cleanify.util.rememberIsUsingGestureNavigation
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import kotlin.math.roundToInt

private data class SettingsCategory(
    val id: String,
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector
)

private val categories = listOf(
    SettingsCategory("appearance", R.string.appearance_section_title, R.string.appearance_section_desc, Icons.Default.Palette),
    SettingsCategory("behavior", R.string.behavior_section_title, R.string.behavior_section_desc, Icons.Default.TouchApp),
    SettingsCategory("duplicates", R.string.duplicate_finder_section_title, R.string.duplicate_finder_section_desc, Icons.Default.ContentCopy),
    SettingsCategory("media_storage", R.string.media_storage_section_title, R.string.media_storage_section_desc, Icons.Default.Storage),
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
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val debouncedSearchQuery by viewModel.debouncedSearchQuery.collectAsState()
    val scope = rememberCoroutineScope()
    val folderSearchState by viewModel.folderSearchManager.state.collectAsState()
    val displayedUnindexedFiles by viewModel.displayedUnindexedFiles.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val currentLocale by viewModel.currentLocale.collectAsState()
    val useDynamicColors by viewModel.useDynamicColors.collectAsState()
    val accentColorKey by viewModel.accentColorKey.collectAsState()
    val compactFolderView by viewModel.compactFolderView.collectAsState()
    val hideFilename by viewModel.hideFilename.collectAsState()
    val invertSwipe by viewModel.invertSwipe.collectAsState()
    val fullScreenSwipe by viewModel.fullScreenSwipe.collectAsState()
    val folderSelectionMode by viewModel.folderSelectionMode.collectAsState()
    val rememberProcessedMedia by viewModel.rememberProcessedMedia.collectAsState()
    val unfavoriteRemovesFromBar by viewModel.unfavoriteRemovesFromBar.collectAsState()
    val hideSkipButton by viewModel.hideSkipButton.collectAsState()
    val defaultPath by viewModel.defaultAlbumCreationPath.collectAsState()
    val showFavoritesInSetup by viewModel.showFavoritesInSetup.collectAsState()
    val searchAutofocusEnabled by viewModel.searchAutofocusEnabled.collectAsState()
    val skipPartialExpansion by viewModel.skipPartialExpansion.collectAsState()
    val useFullScreenSummarySheet by viewModel.useFullScreenSummarySheet.collectAsState()
    val folderBarLayout by viewModel.folderBarLayout.collectAsState()
    val folderNameLayout by viewModel.folderNameLayout.collectAsState()
    val useLegacyFolderIcons by viewModel.useLegacyFolderIcons.collectAsState()
    val addFolderFocusTarget by viewModel.addFolderFocusTarget.collectAsState()
    val swipeSensitivity by viewModel.swipeSensitivity.collectAsState()
    val swipeDownAction by viewModel.swipeDownAction.collectAsState()
    val addFavoriteToTargetByDefault by viewModel.addFavoriteToTargetByDefault.collectAsState()
    val hintOnExistingFolderName by viewModel.hintOnExistingFolderName.collectAsState()
    val pathOptions = viewModel.standardAlbumDirectories
    val defaultVideoSpeed by viewModel.defaultVideoSpeed.collectAsState()
    val screenshotDeletesVideo by viewModel.screenshotDeletesVideo.collectAsState()
    val screenshotJpegQuality by viewModel.screenshotJpegQuality.collectAsState()
    val similarityThresholdLevel by viewModel.similarityThresholdLevel.collectAsState()
    val unselectAllInSearchScope by viewModel.unselectAllInSearchScope.collectAsState()
    val duplicateScanScope by viewModel.duplicateScanScope.collectAsState()
    val duplicateScanIncludeList by viewModel.duplicateScanIncludeList.collectAsState()
    val duplicateScanExcludeList by viewModel.duplicateScanExcludeList.collectAsState()

    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val configuration = LocalConfiguration.current
    val isGestureMode = rememberIsUsingGestureNavigation()
    val supportsDynamicColors = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    var currentPage by remember { mutableStateOf<String?>(initialPage) }

    LaunchedEffect(initialPage) {
        if (initialPage != null) {
            currentPage = initialPage
        }
    }

    BackHandler(enabled = currentPage != null) {
        if (initialPage != null) {
            onNavigateUp()
        } else {
            currentPage = null
        }
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

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

    var showAboutSortMediaDialog by remember { mutableStateOf(false) }
    var showFundingDialog by remember { mutableStateOf(false) }

    val pageTitle = currentPage?.let { id ->
        when (id) {
            "appearance" -> R.string.appearance_section_title
            "behavior" -> R.string.behavior_section_title
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
                    if (currentPage != null) {
                        Text(pageTitle ?: "")
                    } else {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            AnimatedVisibility(
                                visible = !uiState.isSearchActive,
                                exit = fadeOut(animationSpec = tween(200))
                            ) {
                                Text(stringResource(R.string.settings))
                            }
                            AnimatedVisibility(
                                visible = uiState.isSearchActive,
                                enter = slideInHorizontally(animationSpec = tween(350)) { -it } + fadeIn(animationSpec = tween(350)),
                                exit = slideOutHorizontally(animationSpec = tween(200)) { -it } + fadeOut(animationSpec = tween(200))
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 40.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 16.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (uiState.searchQuery.isEmpty()) {
                                        Text(
                                            stringResource(R.string.search_settings_placeholder),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    BasicTextField(
                                        value = uiState.searchQuery,
                                        onValueChange = viewModel::onSearchQueryChanged,
                                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    BackNavigationIcon(onClick = { if (currentPage != null) { if (initialPage != null) onNavigateUp() else currentPage = null } else onNavigateUp() }, contentDescription = stringResource(R.string.navigate_back))
                },
                actions = {
                    if (currentPage == null) {
                        IconButton(onClick = viewModel::toggleSearch) {
                            Icon(
                                imageVector = if (uiState.isSearchActive) Icons.Default.Clear else Icons.Default.Search,
                                contentDescription = if (uiState.isSearchActive) stringResource(R.string.close_search) else stringResource(R.string.search_settings_icon_desc)
                            )
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (currentPage) {
                null -> SettingsMainMenu(
                    categories = categories,
                    searchQuery = debouncedSearchQuery,
                    viewModel = viewModel,
                    onCategoryClick = { currentPage = it },
                    onCleanerClick = onNavigateToCleanerSettings
                )
                "appearance" -> AppearanceSubPage(
                    currentTheme, currentLocale, useDynamicColors, accentColorKey,
                    folderNameLayout, compactFolderView, useLegacyFolderIcons, hideFilename,
                    folderBarLayout, skipPartialExpansion, useFullScreenSummarySheet,
                    supportsDynamicColors, isGestureMode, viewModel
                )
                "behavior" -> BehaviorSubPage(
                    swipeSensitivity, swipeDownAction, fullScreenSwipe, invertSwipe,
                    folderSelectionMode, showFavoritesInSetup, hideSkipButton,
                    addFavoriteToTargetByDefault, unfavoriteRemovesFromBar,
                    hintOnExistingFolderName, addFolderFocusTarget, defaultPath, pathOptions,
                    rememberProcessedMedia, searchAutofocusEnabled, unselectAllInSearchScope,
                    viewModel
                )
                "duplicates" -> DuplicateFinderSubPage(
                    similarityThresholdLevel, duplicateScanScope,
                    duplicateScanIncludeList, duplicateScanExcludeList, viewModel
                )
                "media_storage" -> MediaStorageSubPage(
                    defaultVideoSpeed, screenshotDeletesVideo, screenshotJpegQuality,
                    uiState, viewModel
                )
                "help" -> HelpSupportSubPage(
                    onNavigateToLibraries, exportFavoritesLauncher, importFavoritesLauncher,
                    defaultExportFilename, viewModel
                )
                "about" -> AboutSubPage(
                    showFundingDialog, showAboutSortMediaDialog, viewModel,
                    onNavigateToLibraries,
                    onShowFunding = { showFundingDialog = true },
                    onShowAbout = { showAboutSortMediaDialog = true }
                )
            }
        }
    }

    if (showFundingDialog) FundingDialog(onDismiss = { showFundingDialog = false })
    if (showAboutSortMediaDialog) {
        AppDialog(
            onDismissRequest = { showAboutSortMediaDialog = false },
            title = { Text(stringResource(R.string.about_cleanify_title), style = MaterialTheme.typography.headlineSmall) },
            text = { Text(stringResource(R.string.version_title) + ": ${viewModel.appVersion}", style = MaterialTheme.typography.bodyLarge) },
            buttons = {
                TextButton(onClick = { showAboutSortMediaDialog = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

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
            formatListItemTitle = ::formatPathForDisplay
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
            formatListItemTitle = ::formatPathForDisplay
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
            formatListItemTitle = ::formatPathForDisplay
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
    onCleanerClick: () -> Unit = {}
) {
    val resources = LocalResources.current
    val filteredCategories = if (searchQuery.isBlank()) {
        categories
    } else {
        categories.filter { cat ->
            val title = resources.getString(cat.titleRes)
            val desc = resources.getString(cat.descRes)
            title.contains(searchQuery, ignoreCase = true) ||
                desc.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (filteredCategories.isEmpty()) {
            Text(
                text = stringResource(R.string.no_settings_found, searchQuery),
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        filteredCategories.forEach { category ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable {
                    if (category.id == "cleaner") onCleanerClick() else onCategoryClick(category.id)
                },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = category.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(category.titleRes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(text = stringResource(category.descRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun AppearanceSubPage(
    currentTheme: AppTheme, currentLocale: AppLocale, useDynamicColors: Boolean,
    accentColorKey: String, folderNameLayout: FolderNameLayout, compactFolderView: Boolean,
    useLegacyFolderIcons: Boolean, hideFilename: Boolean, folderBarLayout: FolderBarLayout,
    skipPartialExpansion: Boolean, useFullScreenSummarySheet: Boolean,
    supportsDynamicColors: Boolean, isGestureMode: Boolean, viewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
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
        SectionHeader(R.string.layout_section_header)

        SettingsPickerItem(R.string.folder_name_position_title, R.string.folder_name_position_desc, FolderNameLayout.entries, folderNameLayout, { viewModel.setFolderNameLayout(it) }, ::getFolderNameLayoutDisplayName)
        SettingSwitch(R.string.compact_folder_view_title, R.string.compact_folder_view_desc, compactFolderView, { viewModel.setCompactFolderView(it) })
        SettingSwitch(R.string.legacy_folder_icons_title, R.string.legacy_folder_icons_desc, useLegacyFolderIcons, { viewModel.setUseLegacyFolderIcons(it) })
        SettingSwitch(R.string.hide_media_filename_title, R.string.hide_media_filename_desc, hideFilename, { viewModel.setHideFilename(it) })
        SettingsPickerItem(R.string.folder_bar_layout_title, R.string.folder_bar_layout_desc, FolderBarLayout.entries, folderBarLayout, { viewModel.setFolderBarLayout(it) }, { l -> when (l) { FolderBarLayout.HORIZONTAL -> stringResource(R.string.layout_horizontal); FolderBarLayout.VERTICAL -> stringResource(R.string.layout_vertical) } })
        SettingSwitch(R.string.skip_partial_expansion_title, R.string.skip_partial_expansion_desc, skipPartialExpansion, { viewModel.onSkipPartialExpansionChanged(it) })
        SettingSwitch(R.string.use_full_screen_summary_title, R.string.use_full_screen_summary_desc, useFullScreenSummarySheet, { viewModel.onUseFullScreenSummarySheetChanged(it) })

        Spacer(Modifier.height(if (isGestureMode) 0.dp else 32.dp))
    }
}

@Composable
private fun BehaviorSubPage(
    swipeSensitivity: SwipeSensitivity, swipeDownAction: SwipeDownAction,
    fullScreenSwipe: Boolean, invertSwipe: Boolean,
    folderSelectionMode: FolderSelectionMode, showFavoritesInSetup: Boolean,
    hideSkipButton: Boolean, addFavoriteToTargetByDefault: Boolean,
    unfavoriteRemovesFromBar: Boolean, hintOnExistingFolderName: Boolean,
    addFolderFocusTarget: AddFolderFocusTarget, defaultPath: String,
    pathOptions: List<Pair<String, String>>, rememberProcessedMedia: Boolean,
    searchAutofocusEnabled: Boolean, unselectAllInSearchScope: UnselectScanScope,
    viewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SectionHeader(R.string.gestures_section_header)
        SettingsPickerItem(R.string.swipe_sensitivity_title, R.string.swipe_sensitivity_desc, SwipeSensitivity.entries, swipeSensitivity, { viewModel.setSwipeSensitivity(it) }, ::getSwipeSensitivityDisplayName)
        SettingsPickerItem(R.string.swipe_down_action_title, R.string.swipe_down_action_desc, SwipeDownAction.entries, swipeDownAction, { viewModel.setSwipeDownAction(it) }, ::getSwipeDownActionDisplayName)
        SettingSwitch(R.string.full_screen_swipe_title, R.string.full_screen_swipe_desc, fullScreenSwipe, { viewModel.setFullScreenSwipe(it) })
        SettingSwitch(R.string.invert_swipe_title, R.string.invert_swipe_desc, invertSwipe, { viewModel.setInvertSwipe(it) })

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        SectionHeader(R.string.folder_behavior_section_header)

        SettingsPickerItem(R.string.folder_selection_mode_title, getFolderSelectionModeDescriptionRes(folderSelectionMode), FolderSelectionMode.entries, folderSelectionMode, { viewModel.setFolderSelectionMode(it) }, ::getFolderSelectionModeDisplayName)
        SettingSwitch(R.string.show_favorites_setup_title, R.string.show_favorites_setup_desc, showFavoritesInSetup, { viewModel.setShowFavoritesInSetup(it) })
        SettingSwitch(R.string.hide_skip_button_title, R.string.hide_skip_button_desc, hideSkipButton, { viewModel.setHideSkipButton(it) })
        SettingSwitch(R.string.add_fav_by_default_title, R.string.add_fav_by_default_desc, addFavoriteToTargetByDefault, { viewModel.setAddFavoriteToTargetByDefault(it) })
        SettingSwitch(R.string.unfav_removes_from_bar_title, R.string.unfav_removes_from_bar_desc, unfavoriteRemovesFromBar, { viewModel.setUnfavoriteRemovesFromBar(it) })
        SettingSwitch(R.string.hint_on_existing_folder_title, R.string.hint_on_existing_folder_desc, hintOnExistingFolderName, { viewModel.setHintOnExistingFolderName(it) })
        SettingsPickerItem(R.string.initial_dialog_focus_title, R.string.initial_dialog_focus_desc, AddFolderFocusTarget.entries, addFolderFocusTarget, { viewModel.setAddFolderFocusTarget(it) }, ::getAddFolderFocusTargetDisplayName)
        DefaultAlbumLocationSetting(viewModel, defaultPath, pathOptions)
        RememberMediaSetting(viewModel, rememberProcessedMedia)
        ForgetSortedMediaSetting(viewModel)

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        SectionHeader(R.string.search_section_header)

        SettingSwitch(R.string.search_autofocus_title, R.string.search_autofocus_desc, searchAutofocusEnabled, { viewModel.setSearchAutofocusEnabled(it) })
        SettingsPickerItem(R.string.unselect_all_behavior_title, getUnselectAllScopeDescriptionRes(unselectAllInSearchScope), UnselectScanScope.entries, unselectAllInSearchScope, { viewModel.setUnselectAllInSearchScope(it) }, ::getUnselectAllScopeDisplayName)

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun DuplicateFinderSubPage(
    similarityThresholdLevel: SimilarityThresholdLevel,
    duplicateScanScope: DuplicateScanScope,
    duplicateScanIncludeList: Set<String>,
    duplicateScanExcludeList: Set<String>,
    viewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
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
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun MediaStorageSubPage(
    defaultVideoSpeed: Float, screenshotDeletesVideo: Boolean,
    screenshotJpegQuality: String, uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SectionHeader(R.string.indexing_section_header)
        MediaIndexingStatusItem(uiState.indexingStatus, uiState.isIndexingStatusLoading, uiState.isIndexing, viewModel::refreshIndexingStatus, viewModel::triggerFullScan, viewModel::showUnindexedFilesDialog)

        SectionHeader(R.string.video_section_header)
        SettingsPickerItem(R.string.default_video_speed_title, R.string.default_video_speed_desc, listOf(1.0f, 1.5f, 2.0f), defaultVideoSpeed, { viewModel.setDefaultVideoSpeed(it) }, { s -> "${s}x" })
        SettingSwitch(R.string.screenshot_deletes_video_title, R.string.screenshot_deletes_video_desc, screenshotDeletesVideo, { viewModel.setScreenshotDeletesVideo(it) })
        SettingsPickerItem(R.string.screenshot_quality_title, R.string.screenshot_quality_desc, listOf("95", "90", "85", "75"), screenshotJpegQuality, { viewModel.setScreenshotJpegQuality(it) }, { q -> when (q) { "95" -> stringResource(R.string.quality_high); "90" -> stringResource(R.string.quality_good); "85" -> stringResource(R.string.quality_balanced); "75" -> stringResource(R.string.quality_low); else -> q } })

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun HelpSupportSubPage(
    onNavigateToLibraries: () -> Unit,
    exportFavoritesLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    importFavoritesLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    defaultExportFilename: String,
    viewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SectionHeader(R.string.onboarding_section_header)
        Column {
            Text(stringResource(R.string.onboarding_tutorial_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.onboarding_tutorial_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { viewModel.resetOnboarding() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.replay_tutorial_button)) }
        }

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        SectionHeader(R.string.data_section_header)

        SettingsItem(stringResource(R.string.export_target_favorites_title), stringResource(R.string.export_target_favorites_desc), onClick = { exportFavoritesLauncher.launch(defaultExportFilename) })
        SettingsItem(stringResource(R.string.import_target_favorites_title), stringResource(R.string.import_target_favorites_desc), onClick = { importFavoritesLauncher.launch(arrayOf("application/json", "text/plain")) })

        Card(
            modifier = Modifier.fillMaxWidth().clickable { viewModel.resetDialogWarnings() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.reset_dialog_warnings_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.reset_dialog_warnings_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun AboutSubPage(
    showFundingDialog: Boolean, showAboutSortMediaDialog: Boolean,
    viewModel: SettingsViewModel,
    onNavigateToLibraries: () -> Unit,
    onShowFunding: () -> Unit, onShowAbout: () -> Unit
) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.version_title) + " " + viewModel.appVersion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        HorizontalDivider()

        SettingsItem(stringResource(R.string.support_development_title), stringResource(R.string.support_development_desc), onClick = onShowFunding)

        val versionString = viewModel.appVersion
        val copyMessage = stringResource(R.string.app_version_copied, versionString)
        SettingsItem(stringResource(R.string.version_title), versionString, onLongClick = {
            scope.launch {
                val clipData = ClipData.newPlainText("label", versionString)
                val clipEntry = ClipEntry(clipData)
                clipboard.setClipEntry(clipEntry)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                    android.widget.Toast.makeText(context, copyMessage, android.widget.Toast.LENGTH_SHORT).show()
            }
        })

        SettingsItem(stringResource(R.string.about_cleanify_title), stringResource(R.string.about_cleanify_desc), onClick = onShowAbout)

        val githubUrl = stringResource(R.string.github_summary)
        SettingsItem(stringResource(R.string.github_title), githubUrl, onClick = { uriHandler.openUri("https://$githubUrl") })

        val gitlabSummary = stringResource(R.string.gitlab_summary)
        SettingsItem(stringResource(R.string.gitlab_title), gitlabSummary, onClick = { uriHandler.openUri("https://${gitlabSummary.substringBefore(" ")}") })

        SettingsItem(stringResource(R.string.open_source_licenses_title), stringResource(R.string.open_source_licenses_desc), onClick = onNavigateToLibraries)

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionHeader(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsItem(title: String, summary: String, onClick: (() -> Unit)? = null, onLongClick: (() -> Unit)? = null) {
    val modifier = if (onClick != null || onLongClick != null)
        Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onClick?.invoke() }, onLongPress = { onLongClick?.invoke() }) }
    else Modifier
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(text = summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatPathForDisplay(path: String): Pair<String, String> {
    val file = File(path)
    val name = file.name
    val parentPath = file.parent?.replace("/storage/emulated/0", "") ?: ""
    val displayParent = if (parentPath.length > 30) "...${parentPath.takeLast(27)}" else parentPath
    return Pair(name, displayParent)
}

@Composable
private fun DuplicateScanScopeManagementDialog(title: String, folderList: List<String>, onDismiss: () -> Unit, onAddFolder: () -> Unit, onRemoveFolder: (String) -> Unit) {
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

@Composable
private fun AccentColorSetting(currentAccentKey: String, onClick: () -> Unit) {
    val accent = predefinedAccentColors.find { it.key == currentAccentKey } ?: predefinedAccentColors.first()
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.accent_color_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(accent.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
        }
    }
}

@Composable
private fun AccentColorDialog(currentAccentKey: String, onDismiss: () -> Unit, onColorSelected: (String) -> Unit) {
    var localKey by remember { mutableStateOf(currentAccentKey) }
    val isDark = isSystemInDarkTheme()
    val selAccent = predefinedAccentColors.find { it.key == localKey } ?: predefinedAccentColors.first()
    fun Color.toHex() = String.format("#%06X", 0xFFFFFF and this.toArgb())
    val hexColor = if (isDark) selAccent.darkColor else selAccent.lightColor

    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.customize_colors_title), style = MaterialTheme.typography.headlineSmall) },
        text = {
            val colors = remember(isDark) { predefinedAccentColors.map { if (isDark) it.darkColor else it.lightColor } }
            Column {
                androidx.compose.foundation.Canvas(
                    Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(20.dp))
                        .pointerInput(Unit) { detectTapGestures { o -> localKey = predefinedAccentColors[((o.x / size.width).coerceIn(0f, 1f) * (colors.size - 1)).roundToInt()].key } }
                ) {
                    drawRoundRect(brush = Brush.linearGradient(colors = colors), cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()))
                    val idx = predefinedAccentColors.indexOfFirst { it.key == localKey }
                    if (idx != -1) {
                        val px = (size.width * idx.toFloat() / (colors.size - 1).toFloat()).coerceIn(12.dp.toPx(), size.width - 12.dp.toPx())
                        drawCircle(Color.White, 12.dp.toPx(), Offset(px, size.height / 2))
                        drawCircle(colors[idx], 8.dp.toPx(), Offset(px, size.height / 2))
                        drawCircle(Color.Black.copy(alpha = 0.2f), 12.dp.toPx(), Offset(px, size.height / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(hexColor.toHex(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        buttons = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onColorSelected(localKey) }) { Text(stringResource(R.string.ok)) }
        }
    )
}

private val settingsPickerAccents = mapOf(
    R.string.language_title to Color(0xFF9B59B6),
    R.string.theme_title to Color(0xFF9B59B6),
    R.string.folder_name_position_title to Color(0xFF9B59B6),
    R.string.folder_bar_layout_title to Color(0xFF9B59B6),
    R.string.swipe_sensitivity_title to Color(0xFF00BCD4),
    R.string.swipe_down_action_title to Color(0xFF00BCD4),
    R.string.folder_selection_mode_title to Color(0xFF00BCD4),
    R.string.initial_dialog_focus_title to Color(0xFF00BCD4),
    R.string.unselect_all_behavior_title to Color(0xFF00BCD4),
    R.string.similarity_level_title to Color(0xFFE74C3C),
    R.string.scan_scope_title to Color(0xFFE74C3C),
    R.string.default_video_speed_title to Color(0xFF4CAF50),
    R.string.screenshot_quality_title to Color(0xFF4CAF50),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingsPickerItem(titleRes: Int, descriptionRes: Int, options: List<T>, selectedOption: T, onOptionSelected: (T) -> Unit, getDisplayName: @Composable (T) -> String) {
    SettingsPickerItem(
        titleRes = titleRes,
        description = stringResource(descriptionRes),
        options = options,
        selectedOption = selectedOption,
        onOptionSelected = onOptionSelected,
        getDisplayName = getDisplayName
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingsPickerItem(titleRes: Int, description: String, options: List<T>, selectedOption: T, onOptionSelected: (T) -> Unit, getDisplayName: @Composable (T) -> String) {
    val accentColor = settingsPickerAccents[titleRes] ?: Color(0xFF7F8C8D)
    var showSheet by remember { mutableStateOf(false) }
    val displayValue = getDisplayName(selectedOption)
    Card(
        modifier = Modifier.fillMaxWidth().clickable { showSheet = true },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(displayValue, style = MaterialTheme.typography.bodyLarge, color = accentColor, modifier = Modifier.weight(1f))
                    Icon(imageVector = Icons.Default.UnfoldMore, contentDescription = null, tint = accentColor.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = null
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)))
                    }
                    Box(
                        modifier = Modifier.padding(end = 8.dp).size(32.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)).clickable { showSheet = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close, contentDescription = "Close",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(titleRes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                options.forEach { option ->
                    val isSelected = option == selectedOption
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onOptionSelected(option); showSheet = false }.padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) accentColor.copy(alpha = 0.1f) else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.5.dp, accentColor) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(getDisplayName(option), style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Spacer(Modifier.width(8.dp))
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(titleRes: Int, descriptionRes: Int, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(stringResource(descriptionRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked, onCheckedChange, enabled = enabled)
        }
    }
}

@Composable
private fun SettingSwitch(titleRes: Int, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked, onCheckedChange, enabled = enabled)
        }
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

@Composable
private fun getAppLocaleDisplayName(locale: AppLocale): String = when (locale) {
    AppLocale.SYSTEM -> stringResource(R.string.language_system)
    AppLocale.ENGLISH -> stringResource(R.string.language_english)
    AppLocale.ITALIAN -> stringResource(R.string.language_italian)
}

@Composable
private fun getSwipeSensitivityDisplayName(s: SwipeSensitivity): String = when (s) {
    SwipeSensitivity.LOW -> stringResource(R.string.sensitivity_low)
    SwipeSensitivity.MEDIUM -> stringResource(R.string.sensitivity_medium)
    SwipeSensitivity.HIGH -> stringResource(R.string.sensitivity_high)
}

@Composable
private fun getSwipeDownActionDisplayName(a: SwipeDownAction): String = when (a) {
    SwipeDownAction.NONE -> stringResource(R.string.action_none)
    SwipeDownAction.MOVE_TO_EDIT -> stringResource(R.string.move_to_to_edit)
    SwipeDownAction.SKIP_ITEM -> stringResource(R.string.skip_item)
    SwipeDownAction.ADD_TARGET_FOLDER -> stringResource(R.string.add_target_folder)
    SwipeDownAction.SHARE -> stringResource(R.string.share)
    SwipeDownAction.OPEN_WITH -> stringResource(R.string.open_with)
}

@Composable
private fun getSimilarityLevelDisplayName(l: SimilarityThresholdLevel): String = when (l) {
    SimilarityThresholdLevel.STRICT -> stringResource(R.string.similarity_level_strict)
    SimilarityThresholdLevel.BALANCED -> stringResource(R.string.similarity_level_balanced)
    SimilarityThresholdLevel.LOOSE -> stringResource(R.string.similarity_level_loose)
}

@Composable
private fun getSimilarityLevelDescriptionRes(l: SimilarityThresholdLevel): Int = when (l) {
    SimilarityThresholdLevel.STRICT -> R.string.similarity_level_strict_desc
    SimilarityThresholdLevel.BALANCED -> R.string.similarity_level_balanced_desc
    SimilarityThresholdLevel.LOOSE -> R.string.similarity_level_loose_desc
}

@Composable
private fun getScanScopeDisplayName(s: DuplicateScanScope): String = when (s) {
    DuplicateScanScope.ALL_FILES -> stringResource(R.string.scan_scope_all)
    DuplicateScanScope.INCLUDE_LIST -> stringResource(R.string.scan_scope_include)
    DuplicateScanScope.EXCLUDE_LIST -> stringResource(R.string.scan_scope_exclude)
}

@Composable
private fun getScanScopeDescription(s: DuplicateScanScope, inc: Set<String>, exc: Set<String>): String = when (s) {
    DuplicateScanScope.ALL_FILES -> stringResource(R.string.scan_scope_all_desc)
    DuplicateScanScope.INCLUDE_LIST -> pluralStringResource(R.plurals.scan_scope_include_desc, inc.size, inc.size)
    DuplicateScanScope.EXCLUDE_LIST -> pluralStringResource(R.plurals.scan_scope_exclude_desc, exc.size, exc.size)
}

@Composable
private fun getFolderNameLayoutDisplayName(l: FolderNameLayout): String = when (l) {
    FolderNameLayout.ABOVE -> stringResource(R.string.folder_name_layout_above)
    FolderNameLayout.BELOW -> stringResource(R.string.folder_name_layout_below)
    FolderNameLayout.HIDDEN -> stringResource(R.string.folder_name_layout_hidden)
}

@Composable
private fun getThemeDisplayName(t: AppTheme): String = when (t) {
    AppTheme.SYSTEM -> stringResource(R.string.theme_system_display)
    AppTheme.LIGHT -> stringResource(R.string.theme_light_display)
    AppTheme.DARK -> stringResource(R.string.theme_dark_display)
    AppTheme.DARKER -> stringResource(R.string.theme_darker_display)
    AppTheme.AMOLED -> stringResource(R.string.theme_amoled_display)
}

@Composable
private fun getThemeDescriptionRes(t: AppTheme): Int = when (t) {
    AppTheme.SYSTEM -> R.string.theme_system_desc
    AppTheme.LIGHT -> R.string.theme_light_desc
    AppTheme.DARK -> R.string.theme_dark_desc
    AppTheme.DARKER -> R.string.theme_darker_desc
    AppTheme.AMOLED -> R.string.theme_amoled_desc
}

@Composable
private fun getFolderSelectionModeDisplayName(m: FolderSelectionMode): String = when (m) {
    FolderSelectionMode.ALL -> stringResource(R.string.mode_all_folders)
    FolderSelectionMode.REMEMBER -> stringResource(R.string.mode_remember_previous)
    FolderSelectionMode.NONE -> stringResource(R.string.mode_none)
}

@Composable
private fun getFolderSelectionModeDescriptionRes(m: FolderSelectionMode): Int = when (m) {
    FolderSelectionMode.ALL -> R.string.desc_mode_all
    FolderSelectionMode.REMEMBER -> R.string.desc_mode_remember
    FolderSelectionMode.NONE -> R.string.desc_mode_none
}

@Composable
private fun getAddFolderFocusTargetDisplayName(t: AddFolderFocusTarget): String = when (t) {
    AddFolderFocusTarget.SEARCH_PATH -> stringResource(R.string.focus_search_path)
    AddFolderFocusTarget.FOLDER_NAME -> stringResource(R.string.focus_folder_name)
    AddFolderFocusTarget.NONE -> stringResource(R.string.action_none)
}

@Composable
private fun getUnselectAllScopeDisplayName(s: UnselectScanScope): String = when (s) {
    UnselectScanScope.GLOBAL -> stringResource(R.string.unselect_everything)
    UnselectScanScope.VISIBLE_ONLY -> stringResource(R.string.unselect_visible_only)
}

@Composable
private fun getUnselectAllScopeDescriptionRes(s: UnselectScanScope): Int = when (s) {
    UnselectScanScope.GLOBAL -> R.string.desc_unselect_global
    UnselectScanScope.VISIBLE_ONLY -> R.string.desc_unselect_visible
}

@Composable
private fun FundingDialog(onDismiss: () -> Unit) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    data class Opt(val name: String, val network: String, val address: String)

    val addressCopiedMsg = stringResource(R.string.address_copied)
    val options = remember {
        listOf(
            Opt("Bitcoin", "BTC", "bc1qmzse7fuatzjws5a0n4evm9pjnj9sqmy0y6epu6"),
            Opt("Ethereum", "ETH (ERC20)", "0xb4e7a72a06b606fecb1deb965573da07fcd86107"),
            Opt("Solana", "SOL", "4sXJt424WjL3zcazC7mAccZAzeEpNreg2cpQQ8r7wMWr"),
            Opt("BNB", "BSC (BEP20)", "0xb4e7a72a06b606fecb1deb965573da07fcd86107"),
            Opt("USDT", "ETH/BSC", "0xb4e7a72a06b606fecb1deb965573da07fcd86107"),
            Opt("XRP", "Ripple", "rp6jyrwvSrkKghyqXznZZuqM9TedrKiKEb"),
            Opt("Litecoin", "LTC", "ltc1q8ey9y66frmlqg800m266vucjjux2672f6v2ffl"),
            Opt("Tron", "TRC20", "TFSu7BfSjaiV1CioQgLaVAJsmNtPq4Cv1J"),
            Opt("Monero", "XMR", "42Bh2WEy8LG6D4US4wCJ9aRKtuaKWgUM3Y35xBeCDgmvhWYhoJG9MbPPkW7o3GAGF1MNskK3jwXcGjByFDNrugZhEkqBrMT")
        )
    }

    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.funding_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.funding_dialog_body), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(options) { opt ->
                        Row(Modifier.fillMaxWidth().clickable { scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("label", opt.address))); android.widget.Toast.makeText(context, addressCopiedMsg, android.widget.Toast.LENGTH_SHORT).show() } }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(opt.name, style = MaterialTheme.typography.titleSmall)
                                Text(opt.network, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(opt.address, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy_address_icon_desc), tint = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider()
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.funding_dialog_footer), style = MaterialTheme.typography.bodySmall)
            }
        },
        buttons = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}
