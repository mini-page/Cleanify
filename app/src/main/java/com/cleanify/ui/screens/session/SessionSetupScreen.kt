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

package com.cleanify.ui.screens.session

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cleanify.R
import com.cleanify.domain.model.FolderDetails
import com.cleanify.ui.components.AppDialog
import com.cleanify.ui.components.AppDropdownMenu
import com.cleanify.ui.components.AppMenuDivider
import com.cleanify.ui.components.FastScrollbar
import com.cleanify.ui.components.FolderSearchDialog
import com.cleanify.ui.components.RenameFolderDialog
import com.cleanify.ui.theme.AppTheme
import com.cleanify.ui.theme.LocalAppTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cleanify.util.Formatters
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SessionSetupScreen(
    windowSizeClass: WindowSizeClass,
    onStartSession: (List<String>) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTools: () -> Unit,
    viewModel: SessionSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val folderSearchState by viewModel.folderSearchManager.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val searchAutofocusEnabled by viewModel.searchAutofocusEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isExpandedScreen = windowSizeClass.widthSizeClass > WindowWidthSizeClass.Compact
    val pullToRefreshState = rememberPullToRefreshState()
    val logTag ="SessionSetupScreen"
    var showSortMenu by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.isContextualSelectionMode) {
        viewModel.exitContextualSelectionMode()
    }

    BackHandler(enabled = uiState.searchQuery.isNotEmpty() && !uiState.isContextualSelectionMode) {
        viewModel.updateSearchQuery("")
    }

    // Handle toast messages
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.toastMessageShown()
        }
    }

    // Handle rename dialog
    uiState.showRenameDialogForPath?.let { path ->
        val folder = uiState.allFolderDetails.find { it.path == path }
        if (folder != null) {
            RenameFolderDialog(
                currentFolderName = folder.name,
                onConfirm = { newName ->
                    viewModel.renameFolder(path, newName)
                },
                onDismiss = { viewModel.dismissRenameDialog() }
            )
        }
    }

    // Handle "AddTargetFolder" dialog
    if (uiState.showMoveFolderDialogForPath != null) {
        FolderSearchDialog(
            state = folderSearchState,
            title = stringResource(R.string.move_to_ellipsis),
            searchLabel = stringResource(R.string.search_destination_label),
            confirmButtonText = stringResource(R.string.move_action),
            autoConfirmOnSelection = false, // Require explicit confirmation
            onDismiss = viewModel::dismissMoveFolderDialog,
            onQueryChanged = viewModel.folderSearchManager::updateSearchQuery,
            onFolderSelected = { path -> scope.launch { viewModel.folderSearchManager.selectPath(path) } },
            onConfirm = viewModel::confirmMoveFolderSelection,
            onSearch = { scope.launch { viewModel.folderSearchManager.selectSingleResultOrSelf() } },
            formatListItemTitle = Formatters::pathForDisplay
        )
    }

    // Handle "Mark Permanently as Sorted" confirmation dialog
    if (uiState.showMarkAsSortedConfirmation) {
        val foldersToMark = uiState.foldersToMarkAsSorted
        if (foldersToMark.isNotEmpty()) {
            val titleText: String
            val bodyText: String

            if (foldersToMark.size == 1) {
                val singleFolder = foldersToMark.first()
                val isRecursive = singleFolder.path in uiState.recursivelySelectedRoots
                titleText = stringResource(R.string.mark_sorted_title_single)
                bodyText = if (isRecursive) {
                    stringResource(R.string.mark_sorted_body_recursive, singleFolder.name)
                } else {
                    stringResource(R.string.mark_sorted_body_single, singleFolder.name)
                }
            } else {
                titleText = pluralStringResource(R.plurals.mark_sorted_title_multiple, foldersToMark.size, foldersToMark.size)
                bodyText = pluralStringResource(R.plurals.mark_sorted_body_multiple, foldersToMark.size, foldersToMark.size)
            }

            AppDialog(
                onDismissRequest = viewModel::dismissMarkAsSortedDialog,
                showDontAskAgain = true,
                dontAskAgainChecked = uiState.dontAskAgainMarkAsSorted,
                onDontAskAgainChanged = viewModel::onDontAskAgainMarkAsSortedChanged,
                title = { Text(text = titleText, style = MaterialTheme.typography.headlineSmall) },
                text = { Text(text = bodyText, style = MaterialTheme.typography.bodyMedium) },
                buttons = {
                    TextButton(onClick = viewModel::dismissMarkAsSortedDialog) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(onClick = viewModel::confirmMarkFolderAsSorted) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            )
        }
    }

    LaunchedEffect(searchAutofocusEnabled) {
        if (searchAutofocusEnabled) {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
            })
        },
        topBar = {
            if (uiState.isContextualSelectionMode) {
                ContextualTopAppBar(
                    selectionCount = uiState.contextSelectedFolderPaths.size,
                    canFavorite = uiState.canFavoriteContextualSelection,
                    onClose = viewModel::exitContextualSelectionMode,
                    onSelectAll = viewModel::contextualSelectAll,
                    onMarkAsSorted = viewModel::markSelectedFoldersAsSorted,
                    onToggleFavorite = viewModel::toggleFavoriteForSelectedFolders
                )
            } else {
                DefaultTopAppBar(
                    uiState = uiState,
                    onNavigateToTools = onNavigateToTools,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !uiState.isContextualSelectionMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (uiState.selectedBuckets.isNotEmpty()) {
                            Log.d(
                                logTag,
                                "Starting session with ${uiState.selectedBuckets.size} folders: ${uiState.selectedBuckets}"
                            )
                            viewModel.saveSelectedBucketsPreference()
                            onStartSession(uiState.selectedBuckets)
                        }
                    },
                    containerColor = if (uiState.selectedBuckets.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (uiState.selectedBuckets.isNotEmpty()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.start_session))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_hint),
                            modifier = Modifier.padding(start = 8.dp, end = 4.dp).size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (uiState.searchQuery.isEmpty()) {
                                Text(
                                    stringResource(R.string.search_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            BasicTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    }
                                )
                            )
                        }
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.updateSearchQuery("") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text(stringResource(R.string.sort)) } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort))
                        }
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        SortMenuItem(
                            icon = Icons.Default.TextFields,
                            label = stringResource(R.string.sort_name_az),
                            isSelected = uiState.currentSortOption == FolderSortOption.ALPHABETICAL_ASC,
                            onClick = {
                                viewModel.changeSortOption(FolderSortOption.ALPHABETICAL_ASC)
                                showSortMenu = false
                            }
                        )
                        SortMenuItem(
                            icon = Icons.Default.TextFields,
                            label = stringResource(R.string.sort_name_za),
                            isSelected = uiState.currentSortOption == FolderSortOption.ALPHABETICAL_DESC,
                            onClick = {
                                viewModel.changeSortOption(FolderSortOption.ALPHABETICAL_DESC)
                                showSortMenu = false
                            }
                        )
                        SortMenuItem(
                            icon = Icons.Default.Storage,
                            label = stringResource(R.string.sort_size_largest),
                            isSelected = uiState.currentSortOption == FolderSortOption.SIZE_DESC,
                            onClick = {
                                viewModel.changeSortOption(FolderSortOption.SIZE_DESC)
                                showSortMenu = false
                            }
                        )
                        SortMenuItem(
                            icon = Icons.Default.Storage,
                            label = stringResource(R.string.sort_size_smallest),
                            isSelected = uiState.currentSortOption == FolderSortOption.SIZE_ASC,
                            onClick = {
                                viewModel.changeSortOption(FolderSortOption.SIZE_ASC)
                                showSortMenu = false
                            }
                        )
                        SortMenuItem(
                            icon = Icons.Default.Folder,
                            label = stringResource(R.string.sort_count_most),
                            isSelected = uiState.currentSortOption == FolderSortOption.ITEM_COUNT_DESC,
                            onClick = {
                                viewModel.changeSortOption(FolderSortOption.ITEM_COUNT_DESC)
                                showSortMenu = false
                            }
                        )
                        SortMenuItem(
                            icon = Icons.Default.Folder,
                            label = stringResource(R.string.sort_count_fewest),
                            isSelected = uiState.currentSortOption == FolderSortOption.ITEM_COUNT_ASC,
                            onClick = {
                                viewModel.changeSortOption(FolderSortOption.ITEM_COUNT_ASC)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refreshFolders,
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = uiState.isRefreshing,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .then(
                                if (LocalAppTheme.current == AppTheme.AMOLED) {
                                    Modifier.padding(top = 16.dp)
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            ) {
                when {
                    // Case 1: Initial load, show the scanning message (true first launch/app's data deletion).
                    uiState.isInitialLoad && uiState.showScanningMessage -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.scanning_device_message),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Case 2: Initial load from cache. Show nothing.
                    uiState.isInitialLoad -> {
                        // Render a just the skeleton while the cache loads.
                    }

                    // Case 3: Load complete, but device has no media folders at all.
                    uiState.allFolderDetails.isEmpty() -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                EmptyStateMessage(modifier = Modifier.fillParentMaxSize())
                            }
                        }
                    }

                    // Case 4: Load complete, but the current search query filters them all out.
                    // Only show this if a search is not actively in progress.
                    uiState.folderCategories.isEmpty() && !uiState.isSearching -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                NoSearchResultsMessage(
                                    searchQuery = uiState.searchQuery,
                                    modifier = Modifier.fillParentMaxSize()
                                )
                            }
                        }
                    }

                    // Case 5: Load complete, display the folder list (or the old list while a new search is debouncing).
                    else -> {
                        val listState = rememberLazyListState()
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                    bottom = 96.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                uiState.folderCategories.forEach { category ->
                                    if (category.folders.isNotEmpty()) {
                                        item {
                                            val catFolders = category.folders
                                            val areAllCatSelected = catFolders.isNotEmpty() && catFolders.all { it.path in uiState.selectedBuckets }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp, bottom = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = category.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                TextButton(onClick = { if (areAllCatSelected) viewModel.unselectAllInCategory(category) else viewModel.selectAllInCategory(category) }) {
                                                    Text(
                                                        text = if (areAllCatSelected) stringResource(R.string.unselect_all) else stringResource(R.string.select_all),
                                                        style = MaterialTheme.typography.labelLarge
                                                    )
                                                }
                                            }
                                        }
                                        itemsIndexed(category.folders, key = { _, folder -> folder.path }) { index, folder ->
                                            val isSelectedForSession = folder.path in uiState.selectedBuckets
                                            val isSelectedForContext = folder.path in uiState.contextSelectedFolderPaths
                                            EnhancedFolderItem(
                                                folder = folder,
                                                colorIndex = index,
                                                isSelected = if (uiState.isContextualSelectionMode) isSelectedForContext else isSelectedForSession,
                                                isContextualMode = uiState.isContextualSelectionMode,
                                                isFavorite = folder.path in uiState.favoriteFolders,
                                                isRecursiveRoot = folder.path in uiState.recursivelySelectedRoots,
                                                onToggle = {
                                                    if (uiState.isContextualSelectionMode) {
                                                        viewModel.toggleContextualSelection(folder.path)
                                                    } else {
                                                        if (isSelectedForSession) {
                                                            viewModel.unselectBucket(folder.path)
                                                        } else {
                                                            viewModel.selectBucket(folder.path)
                                                        }
                                                    }
                                                },
                                                onLongPress = {
                                                    viewModel.enterContextualSelectionMode(folder.path)
                                                },
                                                onToggleFavorite = { viewModel.toggleFavorite(folder.path) },
                                                onSelectRecursively = { viewModel.selectFolderRecursively(folder.path) },
                                                onDeselectRecursively = { viewModel.deselectChildren(folder.path) },
                                                onRename = { viewModel.showRenameDialog(folder.path) },
                                                onMove = { viewModel.showMoveFolderDialog(folder.path) },
                                                onMarkAsSorted = { viewModel.markFolderAsSorted(folder) }
                                            )
                                        }
                                    }
                                }
                            }
                            FastScrollbar(
                                state = listState,
                                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 64.dp) // Offset from FABs
        ) {
            Icon(
                imageVector = Icons.Default.Collections,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = stringResource(R.string.no_media_folders_found),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.no_media_folders_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun NoSearchResultsMessage(searchQuery: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 64.dp) // Offset from FABs
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = stringResource(R.string.no_results_found),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.no_search_results_desc, searchQuery),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultTopAppBar(
    uiState: SessionSetupUiState,
    onNavigateToTools: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text("Tools") } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = onNavigateToTools) {
                            Icon(Icons.Default.Build, contentDescription = "Tools")
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    )
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text(stringResource(R.string.settings)) } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContextualTopAppBar(
    selectionCount: Int,
    canFavorite: Boolean,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onMarkAsSorted: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    TopAppBar(
        title = { Text(pluralStringResource(R.plurals.context_selected_count, selectionCount, selectionCount)) },
        navigationIcon = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(stringResource(R.string.close_selection_mode)) } },
                state = rememberTooltipState()
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_selection_mode))
                }
            }
        },
        actions = {
            if (canFavorite) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Above),
                    tooltip = { PlainTooltip { Text(stringResource(R.string.add_to_favorites)) } },
                    state = rememberTooltipState()
                ) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(Icons.Default.Star, contentDescription = stringResource(R.string.add_to_favorites))
                    }
                }
            }
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(stringResource(R.string.mark_as_sorted)) } },
                state = rememberTooltipState()
            ) {
                IconButton(onClick = onMarkAsSorted) {
                    Icon(Icons.Default.CheckCircleOutline, contentDescription = stringResource(R.string.mark_as_sorted))
                }
            }
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(stringResource(R.string.select_all)) } },
                state = rememberTooltipState()
            ) {
                IconButton(onClick = onSelectAll) {
                    Icon(Icons.Default.SelectAll, contentDescription = stringResource(R.string.select_all))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

private fun folderIcon(name: String) = when (name.uppercase()) {
    "DCIM", "CAMERA" -> Icons.Default.PhotoCamera
    "PICTURES", "SCREENSHOTS", "SCREENSHOT" -> Icons.Default.PhotoLibrary
    "DOWNLOADS" -> Icons.Default.Download
    "MUSIC", "AUDIO", "VOICE RECORDER", "NOTIFICATIONS", "RINGTONES", "ALARMS" -> Icons.Default.AudioFile
    "VIDEOS", "MOVIES", "RECORDINGS" -> Icons.Default.VideoFile
    "DOCUMENTS", "PDF" -> Icons.Default.Description
    "ANDROID", "DATA", "OBB" -> Icons.Default.Android
    "BLOB", "BLOBTORY" -> Icons.Default.Storage
    "TEMP", "CACHE" -> Icons.Default.Delete
    "BLUETOOTH" -> Icons.Default.Bluetooth
    "SYSTEM", "ETC", "PROC", "DEV", "SYS", "BIN", "BOOT" -> Icons.Default.Settings
    "TITANIUMBACKUP" -> Icons.Default.Backup
    else -> Icons.Default.Folder
}

private val folderIconTints = listOf(
    Color(0xFF5B9BD5), // Blue
    Color(0xFF70AD47), // Green
    Color(0xFFED7D31), // Orange
    Color(0xFF9B59B6), // Purple
    Color(0xFF00A2E8), // Cyan
    Color(0xFFE74C3C), // Red
    Color(0xFFF39C12), // Amber
    Color(0xFF1ABC9C), // Teal
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedFolderItem(
    folder: FolderDetails,
    colorIndex: Int,
    isSelected: Boolean,
    isContextualMode: Boolean,
    isFavorite: Boolean,
    isRecursiveRoot: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSelectRecursively: (String) -> Unit,
    onDeselectRecursively: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onMarkAsSorted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val tintColor = folderIconTints[colorIndex % folderIconTints.size]

    val cardColors = when {
        isSelected -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        LocalAppTheme.current == AppTheme.AMOLED -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        else -> CardDefaults.cardColors(containerColor = tintColor.copy(alpha = 0.08f))
    }

    val iconTint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else tintColor

    val secondaryTextColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        colors = cardColors,
        modifier = modifier
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onToggle,
                onLongClick = { if (!isContextualMode) onLongPress() }
            )
            .border(
                width = if (LocalAppTheme.current == AppTheme.AMOLED) 1.dp else 0.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = CardDefaults.shape
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Icon(
                    imageVector = folderIcon(folder.name),
                    contentDescription = "Folder",
                    modifier = Modifier.size(24.dp),
                    tint = iconTint
                )
                if (folder.isPrimarySystemFolder) {
                    Text(
                        text = "S",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-3).dp, y = 5.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(text = folder.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = pluralStringResource(R.plurals.folder_item_desc, folder.itemCount, folder.itemCount, Formatters.fileSize(folder.totalSize)),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor
                )
            }
            if (isFavorite) {
                Icon(imageVector = Icons.Default.Star, contentDescription = stringResource(R.string.favorite), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 4.dp))
            }
            if (isRecursiveRoot) {
                Icon(imageVector = Icons.Default.AccountTree, contentDescription = stringResource(R.string.includes_subfolders), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), modifier = Modifier.padding(horizontal = 4.dp))
            }

            if (!isContextualMode) {
                Box {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text(stringResource(R.string.more_options)) } },
                        state = rememberTooltipState()
                    ) {
                        IconButton(onClick = { showContextMenu = true }) {
                            Icon(Icons.Default.MoreVert, stringResource(R.string.more_options))
                        }
                    }
                    AppDropdownMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.rename)) },
                            onClick = {
                                onRename()
                                showContextMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.move_to_ellipsis)) },
                            onClick = {
                                onMove()
                                showContextMenu = false
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move") }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(if (isRecursiveRoot) stringResource(R.string.deselect_subfolders) else stringResource(R.string.select_folder_and_subfolders))
                            },
                            onClick = {
                                if (isRecursiveRoot) {
                                    onDeselectRecursively()
                                } else {
                                    onSelectRecursively(folder.path)
                                }
                                showContextMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.AccountTree, contentDescription = "Select subfolders") }
                        )
                        if (!folder.isSystemFolder) {
                            DropdownMenuItem(
                                text = { Text(if (isFavorite) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites)) },
                                onClick = {
                                    onToggleFavorite(folder.path)
                                    showContextMenu = false
                                },
                                leadingIcon = {
                                    val icon = if (isFavorite) Icons.Default.StarOutline else Icons.Default.Star
                                    Icon(icon, contentDescription = "Favorite")
                                }
                            )
                        }
                        AppMenuDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mark_permanently_as_sorted), color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onMarkAsSorted()
                                showContextMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.CheckCircleOutline, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
private fun SortMenuItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        },
        onClick = onClick,
        leadingIcon = {
            Icon(icon, contentDescription = label)
        },
        trailingIcon = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}
