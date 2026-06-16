package com.cleanify.ui.screens.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    val accentColorKey by viewModel.accentColorKey.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val isGestureMode = com.cleanify.util.rememberIsUsingGestureNavigation()

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
                "appearance" -> AppearanceSection(
                    viewModel = viewModel,
                    onNavigateUp = { if (initialPage != null) onNavigateUp() else currentPage = null }
                )
                "sorting" -> SortingSection(viewModel = viewModel)
                "behavior" -> BehaviorSection(viewModel = viewModel)
                "duplicates" -> DuplicatesSection(viewModel = viewModel)
                "media_storage" -> MediaStorageSection(viewModel = viewModel)
                "contact_cleaner" -> ContactCleanerSection(viewModel = viewModel)
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
                        Row(Modifier.fillMaxWidth().clickable { scope.launch { clipboard.setClipEntry(androidx.compose.ui.platform.ClipEntry(android.content.ClipData.newPlainText("label", opt.address))); android.widget.Toast.makeText(context, addressCopiedMsg, android.widget.Toast.LENGTH_SHORT).show() } }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(opt.name, style = MaterialTheme.typography.titleSmall)
                                Text(opt.network, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(opt.address, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
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
