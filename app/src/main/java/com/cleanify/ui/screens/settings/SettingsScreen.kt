package com.cleanify.ui.screens.settings

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.ui.unit.DpOffset
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import com.cleanify.data.repository.DoubleTapAction
import com.cleanify.data.repository.SwipeDownAction
import com.cleanify.data.repository.SwipeSensitivity
import com.cleanify.data.repository.TapAction
import com.cleanify.data.repository.UnselectScanScope
import com.cleanify.ui.components.AppDialog
import com.cleanify.ui.components.BackNavigationIcon
import com.cleanify.ui.components.FolderSearchDialog
import com.cleanify.ui.theme.AppTheme
import com.cleanify.ui.theme.predefinedAccentColors
import com.cleanify.util.PermissionManager
import com.cleanify.util.UpdateChecker
import com.cleanify.util.UpdateCheckState
import com.cleanify.util.UpdateInfo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cleanify.util.Formatters
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
    SettingsCategory("sorting", R.string.gestures_section_title, R.string.gestures_section_desc, Icons.AutoMirrored.Filled.Sort),
    SettingsCategory("behavior", R.string.behavior_section_title, R.string.behavior_section_desc, Icons.Default.TouchApp),
    SettingsCategory("duplicates", R.string.duplicate_finder_section_title, R.string.duplicate_finder_section_desc, Icons.Default.ContentCopy),
    SettingsCategory("media_storage", R.string.media_storage_section_title, R.string.media_storage_section_desc, Icons.Default.Storage),
    SettingsCategory("cleaner", R.string.cleaner_section_title, R.string.cleaner_section_desc, Icons.Default.CleaningServices),
    SettingsCategory("contact_cleaner", R.string.contact_cleaner_section_title, R.string.contact_cleaner_section_desc, Icons.Default.Contacts),
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val debouncedSearchQuery by viewModel.debouncedSearchQuery.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val folderSearchState by viewModel.folderSearchManager.state.collectAsStateWithLifecycle()
    val displayedUnindexedFiles by viewModel.displayedUnindexedFiles.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val currentLocale by viewModel.currentLocale.collectAsStateWithLifecycle()
    val useDynamicColors by viewModel.useDynamicColors.collectAsStateWithLifecycle()
    val accentColorKey by viewModel.accentColorKey.collectAsStateWithLifecycle()
    val compactFolderView by viewModel.compactFolderView.collectAsStateWithLifecycle()
    val hideFilename by viewModel.hideFilename.collectAsStateWithLifecycle()
    val invertSwipe by viewModel.invertSwipe.collectAsStateWithLifecycle()
    val fullScreenSwipe by viewModel.fullScreenSwipe.collectAsStateWithLifecycle()
    val folderSelectionMode by viewModel.folderSelectionMode.collectAsStateWithLifecycle()
    val rememberProcessedMedia by viewModel.rememberProcessedMedia.collectAsStateWithLifecycle()
    val unfavoriteRemovesFromBar by viewModel.unfavoriteRemovesFromBar.collectAsStateWithLifecycle()
    val hideSkipButton by viewModel.hideSkipButton.collectAsStateWithLifecycle()
    val defaultPath by viewModel.defaultAlbumCreationPath.collectAsStateWithLifecycle()
    val showFavoritesInSetup by viewModel.showFavoritesInSetup.collectAsStateWithLifecycle()
    val searchAutofocusEnabled by viewModel.searchAutofocusEnabled.collectAsStateWithLifecycle()
    val skipPartialExpansion by viewModel.skipPartialExpansion.collectAsStateWithLifecycle()
    val useFullScreenSummarySheet by viewModel.useFullScreenSummarySheet.collectAsStateWithLifecycle()
    val reduceAnimations by viewModel.reduceAnimations.collectAsStateWithLifecycle()
    val immersiveMode by viewModel.immersiveMode.collectAsStateWithLifecycle()
    val hideFromGallery by viewModel.hideFromGallery.collectAsStateWithLifecycle()
    val folderBarLayout by viewModel.folderBarLayout.collectAsStateWithLifecycle()
    val folderNameLayout by viewModel.folderNameLayout.collectAsStateWithLifecycle()
    val useLegacyFolderIcons by viewModel.useLegacyFolderIcons.collectAsStateWithLifecycle()
    val addFolderFocusTarget by viewModel.addFolderFocusTarget.collectAsStateWithLifecycle()
    val swipeSensitivity by viewModel.swipeSensitivity.collectAsStateWithLifecycle()
    val swipeDownAction by viewModel.swipeDownAction.collectAsStateWithLifecycle()
    val tapAction by viewModel.tapAction.collectAsStateWithLifecycle()
    val doubleTapAction by viewModel.doubleTapAction.collectAsStateWithLifecycle()
    val addFavoriteToTargetByDefault by viewModel.addFavoriteToTargetByDefault.collectAsStateWithLifecycle()
    val hintOnExistingFolderName by viewModel.hintOnExistingFolderName.collectAsStateWithLifecycle()
    val pathOptions = viewModel.standardAlbumDirectories
    val defaultVideoSpeed by viewModel.defaultVideoSpeed.collectAsStateWithLifecycle()
    val screenshotDeletesVideo by viewModel.screenshotDeletesVideo.collectAsStateWithLifecycle()
    val screenshotJpegQuality by viewModel.screenshotJpegQuality.collectAsStateWithLifecycle()
    val similarityThresholdLevel by viewModel.similarityThresholdLevel.collectAsStateWithLifecycle()
    val unselectAllInSearchScope by viewModel.unselectAllInSearchScope.collectAsStateWithLifecycle()
    val duplicateScanScope by viewModel.duplicateScanScope.collectAsStateWithLifecycle()
    val duplicateScanIncludeList by viewModel.duplicateScanIncludeList.collectAsStateWithLifecycle()
    val duplicateScanExcludeList by viewModel.duplicateScanExcludeList.collectAsStateWithLifecycle()
    val scanAudioEnabled by viewModel.scanAudioEnabled.collectAsStateWithLifecycle()
    val scanDocumentEnabled by viewModel.scanDocumentEnabled.collectAsStateWithLifecycle()

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

    var showFundingDialog by remember { mutableStateOf(false) }

    val pageTitle = currentPage?.let { id ->
        when (id) {
            "appearance" -> R.string.appearance_section_title
            "sorting" -> R.string.gestures_section_title
            "behavior" -> R.string.behavior_section_title
            "duplicates" -> R.string.duplicate_finder_section_title
            "media_storage" -> R.string.media_storage_section_title
            "contact_cleaner" -> R.string.contact_cleaner_section_title
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
                    supportsDynamicColors, isGestureMode, reduceAnimations, immersiveMode, hideFromGallery,
                    viewModel
                )
                "sorting" -> SortingSubPage(
                    swipeSensitivity, swipeDownAction, fullScreenSwipe, invertSwipe,
                    tapAction, doubleTapAction,
                    folderNameLayout, compactFolderView, useLegacyFolderIcons, hideFilename,
                    folderBarLayout, skipPartialExpansion, useFullScreenSummarySheet,
                    viewModel
                )
            "behavior" -> BehaviorSubPage(
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
                    scanAudioEnabled, scanDocumentEnabled,
                    uiState, viewModel
                )
                "contact_cleaner" -> ContactCleanerSubPage(viewModel)
                "help" -> HelpSupportSubPage(
                    onNavigateToLibraries, exportFavoritesLauncher, importFavoritesLauncher,
                    defaultExportFilename, viewModel
                )
                "about" -> AboutSubPage(
                    showFundingDialog, viewModel,
                    onNavigateToLibraries,
                    onShowFunding = { showFundingDialog = true }
                )
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
    accentColorKey: String,
    supportsDynamicColors: Boolean, isGestureMode: Boolean, reduceAnimations: Boolean,
    immersiveMode: Boolean,
    hideFromGallery: Boolean, viewModel: SettingsViewModel
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
        SectionHeader(R.string.accessibility_section_header)

        SettingSwitch(R.string.reduce_animations_title, R.string.reduce_animations_desc, reduceAnimations, { viewModel.setReduceAnimations(it) })
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

@Composable
private fun SortingSubPage(
    swipeSensitivity: SwipeSensitivity,
    swipeDownAction: SwipeDownAction,
    fullScreenSwipe: Boolean,
    invertSwipe: Boolean,
    tapAction: TapAction,
    doubleTapAction: DoubleTapAction,
    folderNameLayout: FolderNameLayout,
    compactFolderView: Boolean,
    useLegacyFolderIcons: Boolean,
    hideFilename: Boolean,
    folderBarLayout: FolderBarLayout,
    skipPartialExpansion: Boolean,
    useFullScreenSummarySheet: Boolean,
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
        SectionHeader(R.string.tap_section_header)
        SettingsPickerItem(R.string.tap_action_title, R.string.tap_action_desc, TapAction.entries, tapAction, { viewModel.setTapAction(it) }, ::getTapActionDisplayName)
        SettingsPickerItem(R.string.double_tap_action_title, R.string.double_tap_action_desc, DoubleTapAction.entries, doubleTapAction, { viewModel.setDoubleTapAction(it) }, ::getDoubleTapActionDisplayName)

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        SectionHeader(R.string.layout_section_header)
        SettingsPickerItem(R.string.folder_name_position_title, R.string.folder_name_position_desc, FolderNameLayout.entries, folderNameLayout, { viewModel.setFolderNameLayout(it) }, ::getFolderNameLayoutDisplayName)
        SettingSwitch(R.string.compact_folder_view_title, R.string.compact_folder_view_desc, compactFolderView, { viewModel.setCompactFolderView(it) })
        SettingSwitch(R.string.legacy_folder_icons_title, R.string.legacy_folder_icons_desc, useLegacyFolderIcons, { viewModel.setUseLegacyFolderIcons(it) })
        SettingSwitch(R.string.hide_media_filename_title, R.string.hide_media_filename_desc, hideFilename, { viewModel.setHideFilename(it) })
        SettingsPickerItem(R.string.folder_bar_layout_title, R.string.folder_bar_layout_desc, FolderBarLayout.entries, folderBarLayout, { viewModel.setFolderBarLayout(it) }, { l -> when (l) { FolderBarLayout.HORIZONTAL -> stringResource(R.string.layout_horizontal); FolderBarLayout.VERTICAL -> stringResource(R.string.layout_vertical) } })
        SettingSwitch(R.string.skip_partial_expansion_title, R.string.skip_partial_expansion_desc, skipPartialExpansion, { viewModel.onSkipPartialExpansionChanged(it) })
        SettingSwitch(R.string.use_full_screen_summary_title, R.string.use_full_screen_summary_desc, useFullScreenSummarySheet, { viewModel.onUseFullScreenSummarySheetChanged(it) })

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun BehaviorSubPage(
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
private fun MediaStorageSubPage(
    defaultVideoSpeed: Float, screenshotDeletesVideo: Boolean,
    screenshotJpegQuality: String,
    scanAudioEnabled: Boolean, scanDocumentEnabled: Boolean,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SectionHeader(R.string.indexing_section_header)
        MediaIndexingStatusItem(uiState.indexingStatus, uiState.isIndexingStatusLoading, uiState.isIndexing, viewModel::refreshIndexingStatus, viewModel::triggerFullScan, viewModel::showUnindexedFilesDialog)

        SectionHeader(R.string.scan_section_header)
        SettingSwitch(R.string.scan_audio_title, R.string.scan_audio_desc, scanAudioEnabled, viewModel::setScanAudioEnabled)
        SettingSwitch(R.string.scan_document_title, R.string.scan_document_desc, scanDocumentEnabled, viewModel::setScanDocumentEnabled)

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
    val context = LocalContext.current
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> /* re-composition handles refreshed state */ }

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

        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        SectionHeader(R.string.permission_manager_header)

        val permissionEntries = remember {
            listOf(
                PermissionEntry(
                    name = "All Files Access",
                    desc = "Read and manage files on device storage",
                    icon = Icons.Default.Storage,
                    isGranted = PermissionManager.hasAllFilesAccess(),
                    settingsAction = PermissionManager::createAllFilesAccessIntent
                ),
                PermissionEntry(
                    name = "Notifications",
                    desc = "Show scan progress notifications",
                    icon = Icons.Default.Notifications,
                    isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    else true,
                    requestPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        Manifest.permission.POST_NOTIFICATIONS else null
                ),
                PermissionEntry(
                    name = "Contacts (Read)",
                    desc = "Scan and clean duplicate contacts",
                    icon = Icons.Default.Contacts,
                    isGranted = checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED,
                    requestPermission = Manifest.permission.READ_CONTACTS
                ),
                PermissionEntry(
                    name = "Contacts (Write)",
                    desc = "Merge and update cleaned contacts",
                    icon = Icons.Default.Edit,
                    isGranted = checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED,
                    requestPermission = Manifest.permission.WRITE_CONTACTS
                ),
                PermissionEntry(
                    name = "Foreground Service",
                    desc = "Run duplicate scans in the background",
                    icon = Icons.Default.History,
                    isGranted = true,
                    grantedNote = "Auto-granted"
                ),
                PermissionEntry(
                    name = "Boot Completed",
                    desc = "Run cleaner automatically after device restart",
                    icon = Icons.Default.RestartAlt,
                    isGranted = true,
                    grantedNote = "Auto-granted"
                ),
                PermissionEntry(
                    name = "Query All Packages",
                    desc = "Detect orphaned folders from uninstalled apps",
                    icon = Icons.Default.Apps,
                    isGranted = true,
                    grantedNote = "Auto-granted"
                )
            )
        }

        permissionEntries.forEach { entry ->
            PermissionRow(
                entry = entry,
                context = context,
                onRequest = { perm -> requestPermissionLauncher.launch(perm) },
                onOpenSettings = { intent -> intent?.let { context.startActivity(it) } }
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PermissionRow(
    entry: PermissionEntry,
    context: Context,
    onRequest: (String) -> Unit,
    onOpenSettings: (Intent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (entry.isGranted) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (entry.isGranted) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                Text(entry.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (entry.isGranted) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = if (entry.isGranted) (entry.grantedNote ?: "Granted") else "Denied",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.isGranted) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            if (!entry.isGranted) {
                if (entry.settingsAction != null) {
                    FilledTonalButton(
                        onClick = { entry.settingsAction(context)?.let(onOpenSettings) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Settings", style = MaterialTheme.typography.labelMedium)
                    }
                } else if (entry.requestPermission != null) {
                    FilledTonalButton(
                        onClick = { onRequest(entry.requestPermission) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Grant", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

private data class PermissionEntry(
    val name: String,
    val desc: String,
    val icon: ImageVector,
    val isGranted: Boolean,
    val settingsAction: ((Context) -> Intent?)? = null,
    val requestPermission: String? = null,
    val grantedNote: String? = null
)

private fun checkSelfPermission(context: Context, permission: String): Int {
    return androidx.core.content.ContextCompat.checkSelfPermission(context, permission)
}

@Composable
private fun AboutSubPage(
    showFundingDialog: Boolean,
    viewModel: SettingsViewModel,
    onNavigateToLibraries: () -> Unit,
    onShowFunding: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateState by remember { mutableStateOf<UpdateCheckState>(UpdateCheckState.Idle) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(16.dp))
            Text("Cleanify", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("v${viewModel.appVersion}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        HorizontalDivider()

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.features_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                val features = listOf(
                    R.string.feature_swipe,
                    R.string.feature_duplicates,
                    R.string.feature_cleaner,
                    R.string.feature_contacts,
                    R.string.feature_recycle,
                    R.string.feature_themes,
                    R.string.feature_language,
                    R.string.feature_offline,
                    R.string.feature_opensource
                )
                features.forEach { res ->
                    Row(modifier = Modifier.padding(vertical = 3.dp)) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(res),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        UpdateCard(
            state = updateState,
            appVersion = viewModel.appVersion,
            onCheck = {
                scope.launch {
                    updateState = UpdateCheckState.Checking
                    try {
                        val result = UpdateChecker.checkForUpdate(viewModel.appVersion)
                        updateState = if (result != null) {
                            UpdateCheckState.Available(result)
                        } else {
                            UpdateCheckState.UpToDate
                        }
                    } catch (e: Exception) {
                        updateState = UpdateCheckState.Error(e.message ?: "Update check failed")
                    }
                }
            },
            onDismiss = { updateState = UpdateCheckState.Idle }
        )

        when (val state = updateState) {
            is UpdateCheckState.Available -> UpdateAvailableDialog(
                info = state.info,
                onDownload = {
                    updateState = UpdateCheckState.Downloading(0f)
                    scope.launch {
                        try {
                            UpdateChecker.downloadApk(
                                context,
                                state.info.downloadUrl,
                                state.info.tagName
                            ).collect { progress ->
                                updateState = UpdateCheckState.Downloading(progress)
                            }
                            val file = java.io.File(context.cacheDir, "Cleanify-${state.info.tagName}.apk")
                            updateState = UpdateCheckState.Downloaded(file)
                        } catch (e: Exception) {
                            updateState = UpdateCheckState.Error(e.message ?: "Download failed")
                        }
                    }
                },
                onDismiss = { updateState = UpdateCheckState.Idle }
            )
            is UpdateCheckState.Downloading -> DownloadProgressDialog(
                progress = state.progress,
                tagName = (updateState as? UpdateCheckState.Available)?.info?.tagName ?: "",
                onDismiss = { updateState = UpdateCheckState.Idle }
            )
            is UpdateCheckState.Downloaded -> InstallDialog(
                onInstall = { UpdateChecker.installApk(context, state.file) },
                onDismiss = { updateState = UpdateCheckState.Idle }
            )
            else -> {}
        }

        Text(stringResource(R.string.social_section_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        val socialLinks = remember {
            listOf(
                Triple(Icons.Default.Star, "Star Repo", "https://github.com/mini-page/Cleanify"),
                Triple(Icons.Default.Code, "Developer", "https://mini-page.github.io/ugsoc"),
                Triple(Icons.Default.BusinessCenter, "LinkedIn", "https://www.linkedin.com/in/ug5711"),
                Triple(Icons.Default.CameraAlt, "Instagram", "https://www.instagram.com/ug_5711"),
                Triple(Icons.AutoMirrored.Filled.Send, "Telegram", "https://t.me/ug_5711"),
                Triple(Icons.Default.Mood, "Snapchat", "https://www.snapchat.com/add/rg_5711"),
                Triple(Icons.Default.Email, "Email", "mailto:raghavans5711+Support@gmail.com")
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(socialLinks.size) { index ->
                val (icon, label, url) = socialLinks[index]
                Surface(
                    onClick = { uriHandler.openUri(url) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(
                        modifier = Modifier.size(88.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon,
                                    contentDescription = label,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onShowFunding)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.support_development_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.support_development_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }

        SettingsItem(
            title = stringResource(R.string.privacy_policy_title),
            summary = stringResource(R.string.privacy_policy_desc),
            onClick = { uriHandler.openUri("https://home-cleanify.vercel.app/privacy.html") }
        )

        SettingsItem(
            title = stringResource(R.string.terms_of_use_title),
            summary = stringResource(R.string.terms_of_use_desc),
            onClick = { uriHandler.openUri("https://home-cleanify.vercel.app/terms.html") }
        )

        SettingsItem(stringResource(R.string.open_source_licenses_title), stringResource(R.string.open_source_licenses_desc), onClick = onNavigateToLibraries)

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ContactCleanerSubPage(
    viewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.contact_cleaner_section_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.contact_cleaner_section_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }

        HorizontalDivider()

        ToolAboutCard(
            icon = Icons.Default.Contacts,
            title = "Contact Cleaner",
            description = "Contact Cleaner helps you find and fix issues in your contacts list. " +
                "It detects duplicate contacts (by phone number or name), " +
                "and flags contacts with missing names or phone numbers so you can clean them up.",
            warning = "Deleting contacts permanently removes them from your device and synced accounts. " +
                "Merging combines duplicate entries using Android's aggregation mechanism — " +
                "the first contact in each group becomes the primary. These actions cannot be undone."
        )

        Spacer(Modifier.height(16.dp))
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

@Composable
private fun <T> SettingsPickerItem(titleRes: Int, description: String, options: List<T>, selectedOption: T, onOptionSelected: (T) -> Unit, getDisplayName: @Composable (T) -> String) {
    val accentColor = settingsPickerAccents[titleRes] ?: Color(0xFF7F8C8D)
    var showDropdown by remember { mutableStateOf(false) }
    val displayValue = getDisplayName(selectedOption)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = accentColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable { showDropdown = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(displayValue, style = MaterialTheme.typography.bodyMedium, color = accentColor)
                    Spacer(Modifier.width(6.dp))
                    Icon(imageVector = Icons.Default.UnfoldMore, contentDescription = null, tint = accentColor.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }
        }
        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
            shape = RoundedCornerShape(16.dp),
            offset = DpOffset(x = (-280).dp, y = 8.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                getDisplayName(option),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    onClick = { onOptionSelected(option); showDropdown = false }
                )
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
private fun getTapActionDisplayName(a: TapAction): String = when (a) {
    TapAction.PLAY_PAUSE -> stringResource(R.string.tap_action_play_pause)
    TapAction.NONE -> stringResource(R.string.tap_action_none)
}

@Composable
private fun getDoubleTapActionDisplayName(a: DoubleTapAction): String = when (a) {
    DoubleTapAction.FAVORITE -> stringResource(R.string.double_tap_favorite)
    DoubleTapAction.FULLSCREEN -> stringResource(R.string.double_tap_fullscreen)
    DoubleTapAction.NONE -> stringResource(R.string.action_none)
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
private fun ToolAboutCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    warning: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text("About this tool", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (warning != null) {
                Spacer(Modifier.height(8.dp))
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)) {
                    Row(modifier = Modifier.padding(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(6.dp))
                        Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateCard(
    state: UpdateCheckState,
    appVersion: String,
    onCheck: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Text("Updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            when (state) {
                is UpdateCheckState.Idle -> {
                    Text("Check for new versions of Cleanify.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onCheck, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Check for Updates")
                    }
                }
                is UpdateCheckState.Checking -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Checking for updates\u2026", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is UpdateCheckState.UpToDate -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Cleanify v$appVersion is up to date.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = onDismiss) { Text("Dismiss") }
                    }
                }
                is UpdateCheckState.Error -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Update check failed", style = MaterialTheme.typography.bodyMedium)
                            Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = onDismiss) { Text("Dismiss") }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun UpdateAvailableDialog(
    info: UpdateInfo,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
        title = { Text("Update Available") },
        text = {
            Column {
                Text("v${info.versionName}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(info.tagName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (info.changelog.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    Text("What's new:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(info.changelog, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDownload) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        }
    )
}

@Composable
private fun DownloadProgressDialog(
    progress: Float,
    tagName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Downloading Update") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$tagName", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun InstallDialog(
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Download Complete") },
        text = { Text("The update has been downloaded. Install it now to get the latest features and fixes.") },
        confirmButton = {
            Button(onClick = onInstall) {
                Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Install")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        }
    )
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
