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

package com.cleanify.ui.screens.swiper

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer as PdfBoxRenderer
import android.util.Log
import android.view.KeyEvent
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cleanify.R
import com.cleanify.data.model.FileCategory
import com.cleanify.data.model.MediaItem
import com.cleanify.data.repository.FolderBarLayout
import com.cleanify.data.repository.FolderNameLayout
import com.cleanify.data.repository.SwipeDownAction
import com.cleanify.data.repository.SwipeSensitivity
import com.cleanify.ui.components.AppDialog
import com.cleanify.ui.components.AppDropdownMenu
import com.cleanify.ui.components.AppMenuDivider
import com.cleanify.ui.components.FolderSearchDialog
import com.cleanify.ui.components.BackNavigationIcon
import com.cleanify.ui.components.MediaPreviewDialog
import com.cleanify.ui.components.RenameFolderDialog
import com.cleanify.ui.theme.AppTheme
import com.cleanify.ui.theme.LocalReducedAnimations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cleanify.util.Formatters
import java.io.File
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SwiperScreen(
    windowSizeClass: WindowSizeClass,
    bucketIds: List<String>,
    onNavigateUp: () -> Unit,
    onNavigateUpAndReset: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTools: () -> Unit = {},
    viewModel: SwiperViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val invertSwipe by viewModel.invertSwipe.collectAsStateWithLifecycle()
    val swipeSensitivity by viewModel.swipeSensitivity.collectAsStateWithLifecycle()
    val swipeDownAction by viewModel.swipeDownAction.collectAsStateWithLifecycle()
    val folderBarLayout by viewModel.folderBarLayout.collectAsStateWithLifecycle()
    val folderNameLayout by viewModel.folderNameLayout.collectAsStateWithLifecycle()
    val hidePreviewStrip by viewModel.hidePreviewStrip.collectAsStateWithLifecycle()
    val skipPartialExpansion by viewModel.skipPartialExpansion.collectAsStateWithLifecycle()
    val screenshotDeletesVideo by viewModel.screenshotDeletesVideo.collectAsStateWithLifecycle()
    val addFolderFocusTarget by viewModel.addFolderFocusTarget.collectAsStateWithLifecycle()
    val addFavoriteToTargetByDefault by viewModel.addFavoriteToTargetByDefault.collectAsStateWithLifecycle()
    val hintOnExistingFolderName by viewModel.hintOnExistingFolderName.collectAsStateWithLifecycle()
    val immersiveMode by viewModel.immersiveMode.collectAsStateWithLifecycle()
    val view = LocalView.current
    DisposableEffect(immersiveMode) {
        val window = (view.context as? android.app.Activity)?.window ?: return@DisposableEffect onDispose {}
        if (immersiveMode) {
            WindowCompat.getInsetsController(window, view).apply {
                hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            WindowCompat.getInsetsController(window, view).show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            WindowCompat.getInsetsController(window, view).show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }
    val appContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isExpandedScreen = windowSizeClass.widthSizeClass > WindowWidthSizeClass.Compact
    val folderSearchState by viewModel.folderSearchManager.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    var previewItem by remember { mutableStateOf<MediaItem?>(null) }
    previewItem?.let { item ->
        MediaPreviewDialog(
            uri = item.uri,
            displayName = item.displayName,
            fileSize = item.size,
            dateModified = item.dateModified,
            mimeType = item.mimeType,
            onDismiss = { previewItem = null }
        )
    }

    BackHandler {
        if (uiState.isSortingComplete && uiState.pendingChanges.isEmpty()) {
            onNavigateUpAndReset()
        } else {
            viewModel.onNavigateUp()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collectLatest { event ->
            when (event) {
                is NavigationEvent.NavigateUp -> onNavigateUp()
            }
        }
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(appContext).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }
    LaunchedEffect(uiState.videoPlaybackSpeed) {
        exoPlayer.setPlaybackSpeed(uiState.videoPlaybackSpeed)
    }
    LaunchedEffect(uiState.isVideoMuted) {
        exoPlayer.volume = if (uiState.isVideoMuted) 0f else 1f
    }
    uiState.showRenameDialogForPath?.let { path ->
        val folderName = uiState.targetFolders.find { it.first == path }?.second
        if (folderName != null) {
            RenameFolderDialog(
                currentFolderName = folderName,
                onConfirm = { newName ->
                    viewModel.renameTargetFolder(path, newName)
                },
                onDismiss = { viewModel.dismissRenameDialog() }
            )
        }
    }
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (exoPlayer.isPlaying) {
                        viewModel.saveVideoPlaybackPosition(exoPlayer.currentPosition)
                        exoPlayer.pause()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (uiState.currentItem?.isVideo == true) {
                        exoPlayer.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }
    LaunchedEffect(uiState.currentItem) {
        val currentItem = uiState.currentItem
        exoPlayer.playWhenReady = false
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        if (currentItem != null && (currentItem.isVideo || currentItem.category == FileCategory.Audio)) {
            val exoMediaItem = ExoMediaItem.fromUri(currentItem.uri)
            exoPlayer.setMediaItem(exoMediaItem)
            exoPlayer.prepare()

            if (uiState.videoPlaybackPosition > 0L && currentItem.isVideo) {
                exoPlayer.seekTo(uiState.videoPlaybackPosition)
            }
            exoPlayer.playWhenReady = true
        }
    }
    LaunchedEffect(bucketIds) {
        viewModel.initializeMedia(bucketIds)
    }
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
            viewModel.toastMessageShown()
        }
    }
    if (uiState.showConfirmExitDialog) {
        AppDialog(
            onDismissRequest = { viewModel.cancelExit() },
            title = { Text(stringResource(R.string.unsaved_changes_title)) },
            text = { Text(stringResource(R.string.unsaved_changes_message)) }
        ) {
            TextButton(onClick = { viewModel.cancelExitAndShowSummary() }) {
                Text(stringResource(R.string.review_changes))
            }
            Button(onClick = { viewModel.confirmExit() }) {
                Text(stringResource(R.string.cancel_all_changes))
            }
        }
    }

    Scaffold(
        topBar = {
            SwiperTopBar(
                currentItem = uiState.currentItem,
                currentIndex = uiState.currentIndex,
                totalCount = uiState.allMediaItems.size,
                onNavigateUp = { viewModel.onNavigateUp() },
                onOpenWith = { viewModel.openCurrentItem() },
                onShare = viewModel::shareCurrentItem,
                onInfoClick = viewModel::showItemInfoSheet,
                onNavigateToSettings = onNavigateToSettings,
                folderNameLayout = folderNameLayout,
                hidePreviewStrip = hidePreviewStrip,
                onToggleHidePreviewStrip = viewModel::toggleHidePreviewStrip
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        if (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                            if (uiState.isVideoMuted) {
                                val hasAudio = exoPlayer.currentTracks.isTypeSupported(C.TRACK_TYPE_AUDIO)
                                viewModel.toggleMute(hasAudio)
                            }
                            return@onKeyEvent true
                        }
                    }
                    false
                }
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.error != null -> ErrorMessage(message = uiState.error!!, onRetry = { viewModel.initializeMedia(bucketIds) })
                uiState.currentItem != null -> {
                    if (isExpandedScreen) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            MainContent(
                                modifier = Modifier
                                    .weight(1f)
                                    .zIndex(1f),
                                uiState = uiState,
                                exoPlayer = exoPlayer,
                                imageLoader = viewModel.imageLoader,
                                gifImageLoader = viewModel.gifImageLoader,
                                onSwipeLeft = viewModel::handleSwipeLeft,
                                onSwipeRight = viewModel::handleSwipeRight,
                                onSwipeDown = viewModel::handleSwipeDown,
                                onLongPress = viewModel::showMediaItemMenu,
                                invertSwipe = invertSwipe,
                                sensitivity = swipeSensitivity,
                                swipeDownAction = swipeDownAction,
                                onSetVideoPlaybackSpeed = viewModel::setPlaybackSpeed,
                                onToggleMute = {
                                    val hasAudio = exoPlayer.currentTracks.isTypeSupported(C.TRACK_TYPE_AUDIO)
                                    viewModel.toggleMute(hasAudio)
                                },
                                onTap = { item ->
                                    previewItem = item
                                },
                                isPendingConversion = uiState.isCurrentItemPendingConversion,
                                screenshotDeletesVideo = screenshotDeletesVideo,
                                folderNameLayout = folderNameLayout,
                                hidePreviewStrip = hidePreviewStrip,
                                onNavigateToIndex = viewModel::navigateToIndex,
                                onDelete = viewModel::handleSwipeLeft,
                                onKeep = viewModel::handleSwipeRight,
                                onInfoClick = viewModel::showItemInfoSheet
                            )
                            Box(modifier = Modifier
                                .fillMaxHeight()
                                .width(340.dp)) {
                                BottomFolderBar(
                                    targetFolders = uiState.targetFolders,
                                    compactFoldersView = uiState.compactFoldersView,
                                    isFolderBarExpanded = uiState.isFolderBarExpanded,
                                    onSetExpanded = viewModel::setFolderBarExpanded,
                                    currentTheme = uiState.currentTheme,
                                    useLegacyFolderIcons = uiState.useLegacyFolderIcons,
                                    pendingChangesCount = uiState.pendingChanges.size,
                                    currentItem = uiState.currentItem,
                                    targetFavorites = uiState.targetFavorites,
                                    isSkipButtonHidden = uiState.isSkipButtonHidden,
                                    onSelectFolder = viewModel::moveToFolder,
                                    onLongPressFolder = viewModel::showFolderMenu,
                                    onCreateNewAlbum = viewModel::showAddTargetFolderDialog,
                                    onToggleExpansion = viewModel::toggleFolderBarExpansion,
                                    onShowSummary = viewModel::showSummarySheet,
                                    onUndo = viewModel::revertLastChange,
                                    onSkip = viewModel::handleSkip,
                                    layout = FolderBarLayout.VERTICAL,
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            MainContent(
                                modifier = Modifier
                                    .weight(1f)
                                    .zIndex(1f),
                                uiState = uiState,
                                exoPlayer = exoPlayer,
                                imageLoader = viewModel.imageLoader,
                                gifImageLoader = viewModel.gifImageLoader,
                                onSwipeLeft = viewModel::handleSwipeLeft,
                                onSwipeRight = viewModel::handleSwipeRight,
                                onSwipeDown = viewModel::handleSwipeDown,
                                onLongPress = viewModel::showMediaItemMenu,
                                invertSwipe = invertSwipe,
                                sensitivity = swipeSensitivity,
                                swipeDownAction = swipeDownAction,
                                onSetVideoPlaybackSpeed = viewModel::setPlaybackSpeed,
                                onToggleMute = {
                                    val hasAudio = exoPlayer.currentTracks.isTypeSupported(C.TRACK_TYPE_AUDIO)
                                    viewModel.toggleMute(hasAudio)
                                },
                                onTap = { item ->
                                    previewItem = item
                                },
                                isPendingConversion = uiState.isCurrentItemPendingConversion,
                                screenshotDeletesVideo = screenshotDeletesVideo,
                                folderNameLayout = folderNameLayout,
                                hidePreviewStrip = hidePreviewStrip,
                                onNavigateToIndex = viewModel::navigateToIndex,
                                onDelete = viewModel::handleSwipeLeft,
                                onKeep = viewModel::handleSwipeRight,
                                onInfoClick = viewModel::showItemInfoSheet
                            )
                            BottomFolderBar(
                                targetFolders = uiState.targetFolders,
                                compactFoldersView = uiState.compactFoldersView,
                                isFolderBarExpanded = uiState.isFolderBarExpanded,
                                onSetExpanded = viewModel::setFolderBarExpanded,
                                currentTheme = uiState.currentTheme,
                                useLegacyFolderIcons = uiState.useLegacyFolderIcons,
                                pendingChangesCount = uiState.pendingChanges.size,
                                currentItem = uiState.currentItem,
                                targetFavorites = uiState.targetFavorites,
                                isSkipButtonHidden = uiState.isSkipButtonHidden,
                                onSelectFolder = viewModel::moveToFolder,
                                onLongPressFolder = viewModel::showFolderMenu,
                                onCreateNewAlbum = viewModel::showAddTargetFolderDialog,
                                onToggleExpansion = viewModel::toggleFolderBarExpansion,
                                onShowSummary = viewModel::showSummarySheet,
                                onUndo = viewModel::revertLastChange,
                                onSkip = viewModel::handleSkip,
                                layout = folderBarLayout
                            )
                        }
                    }
                }
                uiState.isSortingComplete && uiState.pendingChanges.isEmpty() -> {
                    val rememberProcessedMedia by viewModel.rememberProcessedMedia.collectAsStateWithLifecycle()
                    AlreadyOrganizedDialog(
                        onSelectNewFolders = onNavigateUpAndReset,
                        showResetHistoryButton = rememberProcessedMedia,
                        onResetHistory = viewModel::resetProcessedMedia,
                        onResetSingleFolderHistory = viewModel::showForgetMediaInFolderDialog,
                        skippedCount = uiState.sessionSkippedMediaIds.size,
                        onReviewSkipped = viewModel::reviewSkippedItems,
                        onClose = { (appContext as? Activity)?.finish() }
                    )
                }
                else -> {
                    NoMoreItemsMessage(
                        pendingChanges = uiState.pendingChanges,
                        onShowSummarySheet = viewModel::showSummarySheet
                    )
                }
            }
            if (uiState.showAddTargetFolderDialog) {
                AddTargetFolderDialog(
                    folderSearchState = folderSearchState,
                    addFolderFocusTarget = addFolderFocusTarget,
                    addFavoriteToTargetByDefault = addFavoriteToTargetByDefault,
                    hintOnExistingFolderName = hintOnExistingFolderName,
                    currentItemPath = uiState.currentItem?.id,
                    targetFavorites = uiState.targetFavorites,
                    onDismissRequest = viewModel::dismissAddTargetFolderDialog,
                    onSearchQueryChange = viewModel.folderSearchManager::updateSearchQuery,
                    onPathSelected = viewModel::onPathSelected,
                    onSearchFocusChanged = viewModel::onSearchFocusChanged,
                    onResetFolderSelection = viewModel::resetFolderSelectionToDefault,
                    onConfirm = viewModel::confirmFolderSelection
                )
            }
            if (uiState.showForgetMediaSearchDialog) {
                var showConfirmDialog by remember { mutableStateOf(false) }
                val folderToForget = folderSearchState.browsePath
                FolderSearchDialog(
                    state = folderSearchState,
                    title = stringResource(R.string.forget_sorted_media_title),
                    searchLabel = stringResource(R.string.search_folder_reset_label),
                    confirmButtonText = stringResource(R.string.forget_action),
                    autoConfirmOnSelection = false,
                    onDismiss = viewModel::dismissForgetMediaSearchDialog,
                    onQueryChanged = viewModel.folderSearchManager::updateSearchQuery,
                    onFolderSelected = viewModel::onPathSelected,
                    onConfirm = {
                        if (folderToForget != null) {
                            showConfirmDialog = true
                        }
                    }
                )
                if (showConfirmDialog && folderToForget != null) {
                    AppDialog(
                        onDismissRequest = { showConfirmDialog = false },
                        title = { Text(stringResource(R.string.forget_confirm_title)) },
                        text = { Text(stringResource(R.string.forget_confirm_body, File(folderToForget).name)) }
                    ) {
                        TextButton(onClick = { showConfirmDialog = false }) { Text(stringResource(R.string.cancel)) }
                        Button(onClick = {
                            viewModel.forgetSortedMediaInFolder(folderToForget)
                            showConfirmDialog = false
                            viewModel.dismissForgetMediaSearchDialog()
                        }) { Text(stringResource(R.string.confirm)) }
                    }
                }
            }
            FolderContextMenu(
                folderMenuState = uiState.folderMenuState,
                targetFavorites = uiState.targetFavorites,
                onDismiss = viewModel::dismissFolderMenu,
                onRename = viewModel::showRenameDialog,
                onToggleFavorite = viewModel::toggleTargetFavorite,
                onRemove = viewModel::removeTargetFolder
            )
            MediaItemContextMenu(
                isVisible = uiState.showMediaItemMenu,
                menuOffset = uiState.mediaItemMenuOffset,
                currentItem = uiState.currentItem,
                isPendingConversion = uiState.isCurrentItemPendingConversion,
                exoPlayer = exoPlayer,
                onDismiss = viewModel::dismissMediaItemMenu,
                onScreenshot = viewModel::addScreenshotChange,
                onMoveToEdit = viewModel::moveToEditFolder,
                onShare = viewModel::shareCurrentItem,
                onOpen = viewModel::openCurrentItem,
                onInfo = viewModel::showItemInfoSheet
            )
        }
        if (uiState.showSummarySheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartialExpansion)
            val summaryListState = rememberLazyListState()
            val isAmoled = uiState.currentTheme == AppTheme.AMOLED
            val containerColor = if (isAmoled) {
                MaterialTheme.colorScheme.surface
            } else {
                BottomSheetDefaults.ContainerColor
            }
            val tonalElevation = if (isAmoled) 0.dp else BottomSheetDefaults.Elevation

            if (uiState.pendingChanges.isEmpty()) {
                LaunchedEffect(Unit) {
                    viewModel.dismissSummarySheet()
                }
            }

            ModalBottomSheet(
                onDismissRequest = viewModel::dismissSummarySheet,
                sheetState = sheetState,
                containerColor = containerColor,
                tonalElevation = tonalElevation,
                modifier = Modifier.fillMaxWidth(),
                contentWindowInsets = { WindowInsets(0) }
            ) {
                SummarySheet(
                    pendingChanges = uiState.pendingChanges,
                    toDelete = uiState.toDelete,
                    toKeep = uiState.toKeep,
                    toConvert = uiState.toConvert,
                    groupedMoves = uiState.groupedMoves,
                    isApplyingChanges = uiState.isApplyingChanges,
                    folderIdNameMap = uiState.folderIdToNameMap,
                    onDismiss = viewModel::dismissSummarySheet,
                    onConfirm = { if (!uiState.isApplyingChanges) viewModel.applyChanges() },
                    onResetChanges = viewModel::resetPendingChanges,
                    onRevertChange = viewModel::revertChange,
                    viewMode = uiState.summaryViewMode,
                    onToggleViewMode = viewModel::toggleSummaryViewMode,
                    applyChangesButtonLabel = stringResource(R.string.apply_changes_button),
                    cancelChangesButtonLabel = stringResource(R.string.cancel_all_changes),
                    sheetScrollState = summaryListState,
                    isMaximized = uiState.useFullScreenSummarySheet,
                    onDynamicHeightChange = { shouldBeMaximized ->
                        scope.launch {
                            if (shouldBeMaximized) {
                                if (sheetState.currentValue != SheetValue.Expanded) {
                                    sheetState.expand()
                                }
                            } else {
                                if (sheetState.currentValue == SheetValue.Expanded) {
                                    sheetState.show()
                                }
                            }
                        }
                    }
                )
            }
        }
        uiState.currentItem?.let { sheetItem ->
            if (uiState.showItemInfoSheet) {
                val infoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = viewModel::dismissItemInfoSheet,
                    sheetState = infoSheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth(),
                    contentWindowInsets = { WindowInsets(0) }
                ) {
                    ItemInfoSheet(
                        item = sheetItem,
                        currentIndex = uiState.currentIndex,
                        totalCount = uiState.allMediaItems.size,
                        onRename = viewModel::renameCurrentItem,
                        onDropMetadata = viewModel::dropMetadataFromCurrentItem,
                        onDismiss = viewModel::dismissItemInfoSheet,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

/**
 * Top bar: [X]  📅 Bucket Name ▾  [♡ | ⋯]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwiperTopBar(
    currentItem: MediaItem?,
    currentIndex: Int,
    totalCount: Int,
    onNavigateUp: () -> Unit,
    onOpenWith: () -> Unit,
    onShare: () -> Unit,
    onInfoClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    folderNameLayout: FolderNameLayout = FolderNameLayout.ABOVE,
    hidePreviewStrip: Boolean = false,
    onToggleHidePreviewStrip: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: X close button
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Above,
                spacingBetweenTooltipAndAnchor = 4.dp
            ),
            tooltip = { PlainTooltip { Text("Close") } },
            state = rememberTooltipState()
        ) {
            Surface(
                onClick = onNavigateUp,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Center: folder name text
        if (folderNameLayout == FolderNameLayout.ABOVE) {
            Text(
                text = currentItem?.bucketName ?: "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )
        } else {
            Spacer(Modifier.weight(1f))
        }

        // N/N counter pill (next to overflow)
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${currentIndex + 1} / $totalCount",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.width(4.dp))

        // Overflow menu
        var showOverflow by remember { mutableStateOf(false) }
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Above,
                spacingBetweenTooltipAndAnchor = 4.dp
            ),
            tooltip = { PlainTooltip { Text("More") } },
            state = rememberTooltipState()
        ) {
            IconButton(
                onClick = { showOverflow = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        DropdownMenu(
            expanded = showOverflow,
            onDismissRequest = { showOverflow = false },
            shape = RoundedCornerShape(16.dp),
            offset = DpOffset((-220).dp, 40.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Open with") },
                onClick = { onOpenWith(); showOverflow = false },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open with") }
            )
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = { onShare(); showOverflow = false },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = "Share") }
            )
            DropdownMenuItem(
                text = { Text("Info") },
                onClick = { onInfoClick(); showOverflow = false },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = "Info") }
            )
            DropdownMenuItem(
                text = { Text(if (hidePreviewStrip) "Show Media Previews" else "Hide Media Previews") },
                onClick = { onToggleHidePreviewStrip(); showOverflow = false },
                leadingIcon = {
                    Icon(
                        if (hidePreviewStrip) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (hidePreviewStrip) "Show Media Previews" else "Hide Media Previews"
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Settings") },
                onClick = { onNavigateToSettings(); showOverflow = false },
                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }
            )
        }
    }
}

/**
 * Horizontal filmstrip of all items in the batch.
 * Selected item gets a primary-color border. Decided items get a checkmark overlay.
 */
@Composable
private fun ThumbnailStrip(
    items: List<MediaItem>,
    currentIndex: Int,
    decidedIds: Set<String>,
    onTap: (Int) -> Unit,
    imageLoader: ImageLoader
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        listState.animateScrollToItem(
            index = currentIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
            scrollOffset = -200
        )
    }
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(items.size) { index ->
            val item = items[index]
            val isActive = index == currentIndex
            val isDecided = item.id in decidedIds
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(MaterialTheme.shapes.small)
                    .then(
                        if (isActive) Modifier.border(
                            width = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small
                        ) else Modifier
                    )
                    .clickable { onTap(index) }
            ) {
                if (item.category == FileCategory.Document || item.category == FileCategory.Other) {
                    val docIcon = when {
                        item.mimeType.contains("pdf") || item.extension == "pdf" -> Icons.Default.PictureAsPdf
                        item.mimeType.contains("text") || item.extension in setOf("txt", "rtf") -> Icons.Default.Description
                        item.mimeType.contains("spreadsheet") || item.extension in setOf("xls", "xlsx") -> Icons.Default.TableChart
                        item.mimeType.contains("presentation") || item.extension in setOf("ppt", "pptx") -> Icons.Default.Slideshow
                        else -> Icons.AutoMirrored.Filled.InsertDriveFile
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1A1A2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = docIcon,
                            contentDescription = item.displayName,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.uri)
                            .size(128)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = item.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                // Decided overlay
                if (isDecided) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                // Video badge
                if (item.isVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(3.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoButton(onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(20.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        )
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Above,
                spacingBetweenTooltipAndAnchor = 4.dp
            ),
            tooltip = { PlainTooltip { Text("Item info") } },
            state = rememberTooltipState()
        ) {
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Bottom workflow toolbar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwiperBottomBar(
    hasPendingChanges: Boolean,
    isSkipButtonHidden: Boolean,
    onCreateNewAlbum: () -> Unit,
    onSkip: () -> Unit,
    onUndo: () -> Unit,
    onShowSummary: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Folder
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Above,
                spacingBetweenTooltipAndAnchor = 4.dp
            ),
            tooltip = { PlainTooltip { Text("Add target folder") } },
            state = rememberTooltipState()
        ) {
            IconButton(onClick = onCreateNewAlbum) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "Add folder")
            }
        }
        // Skip
        AnimatedVisibility(visible = !isSkipButtonHidden) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    positioning = TooltipAnchorPosition.Above,
                    spacingBetweenTooltipAndAnchor = 4.dp
                ),
                tooltip = { PlainTooltip { Text("Skip") } },
                state = rememberTooltipState()
            ) {
                IconButton(onClick = onSkip) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Skip")
                }
            }
        }
        // Undo
        AnimatedVisibility(visible = hasPendingChanges) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    positioning = TooltipAnchorPosition.Above,
                    spacingBetweenTooltipAndAnchor = 4.dp
                ),
                tooltip = { PlainTooltip { Text("Undo") } },
                state = rememberTooltipState()
            ) {
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onUndo() }) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                }
            }
        }
        Spacer(Modifier.weight(1f))
        // Proceed button
        Button(
            onClick = onShowSummary,
            enabled = hasPendingChanges,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text("Proceed")
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    modifier: Modifier = Modifier,
    uiState: SwiperUiState,
    exoPlayer: ExoPlayer,
    imageLoader: ImageLoader,
    gifImageLoader: ImageLoader,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeDown: () -> Unit,
    onLongPress: (offset: DpOffset) -> Unit,
    invertSwipe: Boolean,
    sensitivity: SwipeSensitivity,
    swipeDownAction: SwipeDownAction,
    onSetVideoPlaybackSpeed: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onTap: (MediaItem) -> Unit,
    isPendingConversion: Boolean,
    screenshotDeletesVideo: Boolean,
    folderNameLayout: FolderNameLayout,
    hidePreviewStrip: Boolean = false,
    onNavigateToIndex: (Int) -> Unit,
    onDelete: () -> Unit,
    onKeep: () -> Unit,
    onInfoClick: () -> Unit
) {
    val currentItem = uiState.currentItem ?: return
    val decidedIds = remember(uiState.pendingChanges) {
        uiState.pendingChanges.map { it.item.id }.toSet()
    }

    Column(modifier) {
        // Thumbnail strip
        if (!hidePreviewStrip) {
            ThumbnailStrip(
            items = uiState.allMediaItems,
            currentIndex = uiState.currentIndex,
            decidedIds = decidedIds,
            onTap = onNavigateToIndex,
            imageLoader = imageLoader
        )
        }
        // Media card + FABs
        Box(modifier = Modifier.weight(1f)) {
            key(currentItem.id) {
                MediaItemCard(
                    item = currentItem,
                    exoPlayer = exoPlayer,
                    imageLoader = imageLoader,
                    gifImageLoader = gifImageLoader,
                    onSwipeLeft = onSwipeLeft,
                    onSwipeRight = onSwipeRight,
                    onSwipeDown = onSwipeDown,
                    onLongPress = onLongPress,
                    modifier = Modifier.fillMaxSize(),
                    invertSwipe = invertSwipe,
                    sensitivity = sensitivity,
                    swipeDownAction = swipeDownAction,
                    videoPlaybackSpeed = uiState.videoPlaybackSpeed,
                    onSetVideoPlaybackSpeed = onSetVideoPlaybackSpeed,
                    isVideoMuted = uiState.isVideoMuted,
                    onToggleMute = onToggleMute,
                    onTap = onTap,
                    isPendingConversion = isPendingConversion,
                    screenshotDeletesVideo = screenshotDeletesVideo,
                    fullScreenSwipe = uiState.fullScreenSwipe
                )
            }
            // Delete / Keep round icon buttons with N/N counter between
            val leftIsDelete = !invertSwipe
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left FAB
                Surface(
                    onClick = if (leftIsDelete) onDelete else onKeep,
                    shape = CircleShape,
                    color = if (leftIsDelete) MaterialTheme.colorScheme.errorContainer else Color(0xFF1B5E20),
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (leftIsDelete) Icons.Default.Delete else Icons.Default.Check,
                            contentDescription = if (leftIsDelete) "Delete" else "Keep",
                            tint = if (leftIsDelete) MaterialTheme.colorScheme.onErrorContainer else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                // Right FAB
                Surface(
                    onClick = if (leftIsDelete) onKeep else onDelete,
                    shape = CircleShape,
                    color = if (leftIsDelete) Color(0xFF1B5E20) else MaterialTheme.colorScheme.errorContainer,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (leftIsDelete) Icons.Default.Check else Icons.Default.Delete,
                            contentDescription = if (leftIsDelete) "Keep" else "Delete",
                            tint = if (leftIsDelete) Color.White else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderContextMenu(
    folderMenuState: FolderMenuState,
    targetFavorites: Set<String>,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    if (folderMenuState is FolderMenuState.Visible) {
        val isFavorite = folderMenuState.folderPath in targetFavorites
        AppDropdownMenu(
            expanded = true,
            onDismissRequest = onDismiss,
            offset = folderMenuState.pressOffset
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rename)) },
                onClick = {
                    onRename(folderMenuState.folderPath)
                    onDismiss()
                },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit") }
            )
            DropdownMenuItem(
                text = { Text(if (isFavorite) stringResource(R.string.unfavorite) else stringResource(R.string.favorite)) },
                onClick = {
                    onToggleFavorite(folderMenuState.folderPath)
                    onDismiss()
                },
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = "Star") }
            )
            AppMenuDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove_from_bar), color = MaterialTheme.colorScheme.error) },
                onClick = {
                    onRemove(folderMenuState.folderPath)
                    onDismiss()
                },
                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
            )
        }
    }
}

@Composable
private fun MediaItemContextMenu(
    isVisible: Boolean,
    menuOffset: DpOffset,
    currentItem: MediaItem?,
    isPendingConversion: Boolean,
    exoPlayer: ExoPlayer,
    onDismiss: () -> Unit,
    onScreenshot: (Long) -> Unit,
    onMoveToEdit: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onInfo: () -> Unit
) {
    val appContext = LocalContext.current
    val copiedString = stringResource(R.string.filename_copied)

    if (isVisible && currentItem != null) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = onDismiss,
            offset = DpOffset((-16).dp, 0.dp),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Column {
                    // Info Section with truncated filename + Info button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Filename", currentItem.displayName)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(appContext, copiedString, Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                currentItem.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 180.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                stringResource(R.string.file_size_label, Formatters.fileSize(currentItem.size)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onInfo(); onDismiss() }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    // Actions
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.move_to_to_edit)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit") },
                        onClick = onMoveToEdit
                    )
                    if (currentItem.isVideo) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.screenshot)) },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = "Image") },
                            onClick = {
                                val timestampMicros = exoPlayer.currentPosition * 1000
                                onScreenshot(timestampMicros)
                            },
                            enabled = !isPendingConversion
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share)) },
                        onClick = onShare,
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = "Share") }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.open_with)) },
                        onClick = onOpen,
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open in new tab") }
                    )
            }
        }
    }
}

// ControlBar is now replaced by SwiperBottomBar (defined above in MainContent section).
// Kept as a thin wrapper so BottomFolderBar still compiles during the expand/collapse transition.
@Composable
private fun ControlBar(
    isExpanded: Boolean,
    showExpandButton: Boolean,
    hasPendingChanges: Boolean,
    isSkipButtonHidden: Boolean,
    onToggleExpansion: () -> Unit,
    onCreateNewAlbum: () -> Unit,
    onShowSummary: () -> Unit,
    onUndo: () -> Unit,
    onSkip: () -> Unit
) {
    Column {
        SwiperBottomBar(
            hasPendingChanges = hasPendingChanges,
            isSkipButtonHidden = isSkipButtonHidden,
            onCreateNewAlbum = onCreateNewAlbum,
            onSkip = onSkip,
            onUndo = onUndo,
            onShowSummary = onShowSummary
        )
        // Expand/collapse chevron for folder chip grid
        if (showExpandButton) {
            val rotationAngle by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                label = "expand_icon_rotation"
            )
            IconButton(
                onClick = onToggleExpansion,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                    modifier = Modifier.rotate(rotationAngle)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BottomFolderBar(
    targetFolders: List<Pair<String, String>>,
    compactFoldersView: Boolean,
    isFolderBarExpanded: Boolean,
    onSetExpanded: (Boolean) -> Unit,
    currentTheme: AppTheme,
    useLegacyFolderIcons: Boolean,
    pendingChangesCount: Int,
    currentItem: MediaItem?,
    targetFavorites: Set<String>,
    isSkipButtonHidden: Boolean,
    onSelectFolder: (String) -> Unit,
    onLongPressFolder: (String, DpOffset) -> Unit,
    onCreateNewAlbum: () -> Unit,
    onToggleExpansion: () -> Unit,
    onShowSummary: () -> Unit,
    onUndo: () -> Unit,
    onSkip: () -> Unit,
    layout: FolderBarLayout
) {
    BoxWithConstraints {
        val containerWidth = this.maxWidth
        val containerHeight = this.maxHeight
        val folderCount = targetFolders.size
        val chipWidth = if (compactFoldersView) 60.dp else 85.dp
        val chipHeight = if (compactFoldersView) 70.dp else 100.dp
        val chipSpacingHorizontal = 8.dp
        val chipSpacingVertical = 4.dp // From FlowRow verticalArrangement

        val gridHorizontalPadding = 16.dp * 2
        val availableWidthForGrid = containerWidth - gridHorizontalPadding
        val maxChipsPerLine = (availableWidthForGrid / (chipWidth + chipSpacingHorizontal)).toInt().coerceAtLeast(1)

        val singleRowHeight = chipHeight + (chipSpacingVertical * 2)
        val contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)

        val numChipsInOneFlowRowLine = (availableWidthForGrid / (chipWidth + chipSpacingHorizontal)).toInt().coerceAtLeast(1)
        val estimatedTotalRowsForFlowRow = (folderCount + numChipsInOneFlowRowLine - 1) / numChipsInOneFlowRowLine
        val estimatedFlowRowContentHeight = (chipHeight * estimatedTotalRowsForFlowRow) + (chipSpacingVertical * (estimatedTotalRowsForFlowRow - 1)) + (contentPadding.calculateTopPadding().value + contentPadding.calculateBottomPadding().value).dp
        val showExpandButton = folderCount > maxChipsPerLine

        LaunchedEffect(folderCount, maxChipsPerLine, layout, isFolderBarExpanded, showExpandButton) {
            if (isFolderBarExpanded && !showExpandButton) {
                onSetExpanded(false)
            }
        }

        val isAmoled = currentTheme == AppTheme.AMOLED
        val barColor = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        val barElevation = if (isAmoled) 0.dp else 2.dp

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = barColor,
            tonalElevation = barElevation
        ) {
            Column(
                modifier = Modifier
                    .animateContentSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                ControlBar(
                    isExpanded = isFolderBarExpanded,
                    showExpandButton = showExpandButton,
                    hasPendingChanges = pendingChangesCount > 0,
                    isSkipButtonHidden = isSkipButtonHidden,
                    onToggleExpansion = onToggleExpansion,
                    onCreateNewAlbum = onCreateNewAlbum,
                    onShowSummary = onShowSummary,
                    onUndo = onUndo,
                    onSkip = onSkip
                )

                if (targetFolders.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    val maxContainerHeight = containerHeight * 0.4f

                    when (layout) {
                        FolderBarLayout.HORIZONTAL -> {
                            if (!isFolderBarExpanded) {
                                LazyRow(
                                    modifier = Modifier
                                        .height(singleRowHeight)
                                        .fillMaxWidth(),
                                    contentPadding = contentPadding,
                                    horizontalArrangement = Arrangement.spacedBy(chipSpacingHorizontal, Alignment.CenterHorizontally)
                                ) {
                                    items(items = targetFolders, key = { it.first }) { (folderId, fName) ->
                                        FolderChipWrapper(currentItem, compactFoldersView, folderId in targetFavorites, useLegacyFolderIcons, folderId, fName, onSelectFolder, onLongPressFolder)
                                    }
                                }
                            } else {
                                val flowRowWouldOverflowMaxContainerHeight = estimatedFlowRowContentHeight > maxContainerHeight
                                if (!flowRowWouldOverflowMaxContainerHeight) {
                                    FlowRow(
                                        modifier = Modifier
                                            .heightIn(min = singleRowHeight, max = maxContainerHeight)
                                            .fillMaxWidth()
                                            .padding(contentPadding),
                                        horizontalArrangement = Arrangement.spacedBy(chipSpacingHorizontal, Alignment.CenterHorizontally),
                                        verticalArrangement = Arrangement.spacedBy(chipSpacingVertical)
                                    ) {
                                        targetFolders.forEach { (folderId, fName) ->
                                            FolderChipWrapper(currentItem, compactFoldersView, folderId in targetFavorites, useLegacyFolderIcons, folderId, fName, onSelectFolder, onLongPressFolder)
                                        }
                                    }
                                } else {
                                    val maxRowsInHorizontalGrid = (maxContainerHeight / (chipHeight + chipSpacingVertical)).toInt().coerceAtLeast(1)
                                    LazyHorizontalGrid(
                                        rows = GridCells.Fixed(maxRowsInHorizontalGrid),
                                        modifier = Modifier
                                            .heightIn(min = singleRowHeight, max = maxContainerHeight)
                                            .fillMaxWidth(),
                                        contentPadding = contentPadding,
                                        horizontalArrangement = Arrangement.spacedBy(chipSpacingHorizontal, Alignment.CenterHorizontally),
                                        verticalArrangement = Arrangement.spacedBy(chipSpacingVertical)
                                    ) {
                                        items(items = targetFolders, key = { it.first }) { (folderId, fName) ->
                                            FolderChipWrapper(currentItem, compactFoldersView, folderId in targetFavorites, useLegacyFolderIcons, folderId, fName, onSelectFolder, onLongPressFolder)
                                        }
                                    }
                                }
                            }
                        }
                        FolderBarLayout.VERTICAL -> {
                            if (!isFolderBarExpanded) {
                                Box(
                                    modifier = Modifier
                                        .height(singleRowHeight)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    FlowRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(contentPadding),
                                        horizontalArrangement = Arrangement.spacedBy(chipSpacingHorizontal, Alignment.CenterHorizontally),
                                        verticalArrangement = Arrangement.spacedBy(chipSpacingVertical)
                                    ) {
                                        targetFolders.forEach { (folderId, fName) ->
                                            FolderChipWrapper(currentItem, compactFoldersView, folderId in targetFavorites, useLegacyFolderIcons, folderId, fName, onSelectFolder, onLongPressFolder)
                                        }
                                    }
                                }
                            } else {
                                val minColumnsInExpandedGrid = maxChipsPerLine.coerceAtLeast(1)
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(minColumnsInExpandedGrid),
                                    modifier = Modifier
                                        .heightIn(min = singleRowHeight, max = maxContainerHeight)
                                        .fillMaxWidth(),
                                    contentPadding = contentPadding,
                                    horizontalArrangement = Arrangement.spacedBy(chipSpacingHorizontal, Alignment.CenterHorizontally),
                                    verticalArrangement = Arrangement.spacedBy(chipSpacingVertical)
                                ) {
                                    items(items = targetFolders, key = { it.first }) { (folderId, fName) ->
                                        FolderChipWrapper(currentItem, compactFoldersView, folderId in targetFavorites, useLegacyFolderIcons, folderId, fName, onSelectFolder, onLongPressFolder)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun FolderChipWrapper(
    currentItem: MediaItem?,
    compactFoldersView: Boolean,
    isFavorite: Boolean,
    useLegacyFolderIcons: Boolean,
    folderId: String,
    folderName: String,
    onSelectFolder: (String) -> Unit,
    onLongPressFolder: (String, DpOffset) -> Unit
) {
    val isEnabled = remember(currentItem?.id, folderId) {
        val currentItemPath = currentItem?.id ?: return@remember true
        val parentDirectory = try {
            File(currentItemPath).parent
        } catch (e: Exception) {
            null
        }
        parentDirectory != folderId
    }

    val chipHeight = if (compactFoldersView) 70.dp else 100.dp
    val chipWidth = if (compactFoldersView) 60.dp else 85.dp
    val iconSize = if (compactFoldersView) 28.dp else 40.dp
    val density = LocalDensity.current
    var globalPosition by remember { mutableStateOf(Offset.Zero) }
    FolderChip(
        modifier = Modifier.onGloballyPositioned {
            globalPosition = it.localToWindow(Offset.Zero)
        },
        name = folderName,
        isFavorite = isFavorite,
        onClick = { onSelectFolder(folderId) },
        onLongClick = { pressOffset ->
            val absolutePressOffset = globalPosition + pressOffset
            val dpOffset = with(density) {
                DpOffset(absolutePressOffset.x.toDp(), absolutePressOffset.y.toDp())
            }
            onLongPressFolder(folderId, dpOffset)
        },
        chipWidth = chipWidth,
        chipHeight = chipHeight,
        iconSize = iconSize,
        useLegacyIcon = useLegacyFolderIcons,
        isCompact = compactFoldersView,
        isEnabled = isEnabled
    )
}

@Composable
private fun FolderChip(
    modifier: Modifier = Modifier,
    name: String,
    onClick: () -> Unit,
    onLongClick: (Offset) -> Unit,
    isFavorite: Boolean,
    chipWidth: Dp,
    chipHeight: Dp,
    iconSize: Dp,
    useLegacyIcon: Boolean,
    isCompact: Boolean,
    isEnabled: Boolean = true
) {
    val alpha = if (isEnabled) 1f else 0.5f
    val color = MaterialTheme.colorScheme.secondaryContainer
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    val textStyle = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .width(chipWidth)
            .height(chipHeight)
            .graphicsLayer(alpha = alpha)
            .pointerInput(onClick, onLongClick, isEnabled) {
                detectTapGestures(
                    onLongPress = { offset -> onLongClick(offset) },
                    onTap = { if (isEnabled) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() } }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                if (useLegacyIcon) {
                    Text(
                        text = name
                            .take(1)
                            .uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = name,
                        tint = contentColor,
                        modifier = Modifier.size(iconSize * 0.6f)
                    )
                }
            }
            if (isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (4).dp, y = (4).dp)
                        .size(iconSize / 2.2f),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = Color.Black.copy(alpha = 0.4f))
                    }
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = stringResource(R.string.favorite),
                        tint = Color.White,
                        modifier = Modifier.size(iconSize / 3.5f)
                    )
                }
            }
        }
        Text(
            text = name,
            style = textStyle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaItemCard(
    item: MediaItem,
    exoPlayer: ExoPlayer,
    imageLoader: ImageLoader,
    gifImageLoader: ImageLoader,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeDown: () -> Unit,
    onLongPress: (offset: DpOffset) -> Unit,
    onTap: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    invertSwipe: Boolean = false,
    sensitivity: SwipeSensitivity,
    swipeDownAction: SwipeDownAction,
    videoPlaybackSpeed: Float,
    onSetVideoPlaybackSpeed: (Float) -> Unit,
    isVideoMuted: Boolean,
    onToggleMute: () -> Unit,
    isPendingConversion: Boolean,
    screenshotDeletesVideo: Boolean,
    fullScreenSwipe: Boolean
) {
    val swipeOffsetX = remember { Animatable(0f) }
    val swipeOffsetY = remember { Animatable(0f) }
    val density = LocalDensity.current
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var showVideoControls by remember { mutableStateOf(true) }
    val videoControlsScope = rememberCoroutineScope()
    val swipeScope = rememberCoroutineScope()

    val haptic = LocalHapticFeedback.current

    val swipeThreshold = when (sensitivity) {
        SwipeSensitivity.LOW -> with(density) { 60.dp.toPx() }
        SwipeSensitivity.MEDIUM -> with(density) { 80.dp.toPx() }
        SwipeSensitivity.HIGH -> with(density) { 140.dp.toPx() }
    }
    // Make swipe down slightly easier to trigger than horizontal swipes
    val swipeDownThreshold = swipeThreshold * 0.8f
    // Velocity threshold for fling detection (px/sec)
    val velocityThreshold = with(density) { 800.dp.toPx() }

    val reduceAnimations = LocalReducedAnimations.current
    val snapBackSpec = if (reduceAnimations) snap<Float>() else spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    val flyAwaySpec = if (reduceAnimations) snap<Float>() else tween<Float>(durationMillis = 200)
    val rotation = (swipeOffsetX.value / 30).coerceIn(-6f, 6f)
    val animatedScale by animateFloatAsState(targetValue = scale, label = "scale")
    val animatedPanOffset by animateOffsetAsState(targetValue = panOffset, label = "panOffset")

    val leftBorderAlpha = if (swipeOffsetX.value < 0) (abs(swipeOffsetX.value) / swipeThreshold).coerceIn(0f, 1f) else 0f
    val rightBorderAlpha = if (swipeOffsetX.value > 0) (swipeOffsetX.value / swipeThreshold).coerceIn(0f, 1f) else 0f
    val leftColor = if (invertSwipe) Color.Green else Color.Red
    val rightColor = if (invertSwipe) Color.Red else Color.Green
    var globalPosition by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(item.id) {
        scale = 1f
        panOffset = Offset.Zero
        swipeOffsetX.snapTo(0f)
        swipeOffsetY.snapTo(0f)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        val maxCardWidth = this.maxWidth * 0.95f
        val maxCardHeight = this.maxHeight * 0.9f
        val cardAspectRatio = if (item.height > 0 && item.width > 0) {
            item.width.toFloat() / item.height.toFloat()
        } else {
            1.0f
        }
        val widthByHeight = maxCardHeight * cardAspectRatio
        val heightByWidth = maxCardWidth / cardAspectRatio
        val (cardWidth, cardHeight) = if (widthByHeight <= maxCardWidth) widthByHeight to maxCardHeight else maxCardWidth to heightByWidth

        val gestureModifier = Modifier.pointerInput(item.id, swipeDownAction) {
            forEachGesture {
                coroutineScope {
                    awaitPointerEventScope {
                        var wasDragging = false
                        var wasTransforming = false
                        var longPressFired = false
                        val velocityTracker = VelocityTracker()

                        val down = awaitFirstDown(requireUnconsumed = true)
                        velocityTracker.addPosition(System.currentTimeMillis(), down.position)
                        val longPressJob = launch {
                            delay(viewConfiguration.longPressTimeoutMillis)
                            longPressFired = true
                            if (scale > 1f) {
                                scale = 1f
                                panOffset = Offset.Zero
                            } else {
                                val dpOffset = with(density) {
                                    DpOffset(globalPosition.x.toDp() + down.position.x.toDp(), globalPosition.y.toDp() + down.position.y.toDp())
                                }
                                onLongPress(dpOffset)
                            }
                        }

                        do {
                            val event = awaitPointerEvent()
                            val anyPressed = event.changes.any { it.pressed }

                            if (event.changes.size > 1) {
                                longPressJob.cancel()
                                wasTransforming = true
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val newScale = scale * zoom

                                if (newScale < 1f) {
                                    scale = 1f
                                    panOffset = Offset.Zero
                                } else {
                                    scale = newScale.coerceIn(1f, 5f)
                                    if (scale > 1f) {
                                        val xMax = (cardWidth.toPx() * (scale - 1)) / 2
                                        val yMax = (cardHeight.toPx() * (scale - 1)) / 2
                                        panOffset = Offset(
                                            x = (panOffset.x + pan.x * scale).coerceIn(-xMax, xMax),
                                            y = (panOffset.y + pan.y * scale).coerceIn(-yMax, yMax)
                                        )
                                    }
                                }
                                event.changes.forEach { it.consume() }
                            } else if (!wasTransforming) {
                                val change = event.changes.first()
                                val dragAmount = change.positionChange()

                                if (dragAmount.getDistance() > viewConfiguration.touchSlop) {
                                    longPressJob.cancel()
                                    wasDragging = true
                                    velocityTracker.addPosition(System.currentTimeMillis(), change.position)
                                    if (abs(dragAmount.x) > abs(dragAmount.y) && scale <= 1f) {
                                        swipeScope.launch { swipeOffsetX.snapTo(swipeOffsetX.value + dragAmount.x) }
                                    } else if (abs(dragAmount.y) > abs(dragAmount.x) && scale <= 1f) {
                                        if (swipeDownAction != SwipeDownAction.NONE) {
                                            val newY = (swipeOffsetY.value + dragAmount.y).coerceAtLeast(0f)
                                            swipeScope.launch { swipeOffsetY.snapTo(newY) }
                                        }
                                    }
                                    change.consume()
                                }
                            }
                        } while (anyPressed)

                        longPressJob.cancel()

                        if (wasDragging) {
                            val velocity = velocityTracker.calculateVelocity()
                            val vx = velocity.x
                            val vy = velocity.y
                            val isFlingX = abs(vx) > velocityThreshold
                            val isFlingY = abs(vy) > velocityThreshold

                            when {
                                swipeOffsetX.value < -swipeThreshold || (isFlingX && vx < 0) -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    swipeScope.launch {
                                        swipeOffsetX.animateTo(-cardWidth.toPx() * 1.5f, flyAwaySpec)
                                    }
                                    onSwipeLeft()
                                }
                                swipeOffsetX.value > swipeThreshold || (isFlingX && vx > 0) -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    swipeScope.launch {
                                        swipeOffsetX.animateTo(cardWidth.toPx() * 1.5f, flyAwaySpec)
                                    }
                                    onSwipeRight()
                                }
                                swipeOffsetY.value > swipeDownThreshold || (isFlingY && vy > 0) -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    swipeScope.launch {
                                        swipeOffsetY.animateTo(cardHeight.toPx() * 1.5f, flyAwaySpec)
                                    }
                                    onSwipeDown()
                                }
                                else -> {
                                    swipeScope.launch {
                                        launch { swipeOffsetX.animateTo(0f, snapBackSpec) }
                                        launch { swipeOffsetY.animateTo(0f, snapBackSpec) }
                                    }
                                }
                            }
                        } else if (!wasTransforming && !longPressFired) {
                            if (scale > 1f) {
                                scale = 1f
                                panOffset = Offset.Zero
                            } else {
                                if (item.isVideo) {
                                    showVideoControls = !showVideoControls
                                    if (showVideoControls) {
                                        videoControlsScope.launch {
                                            delay(3000)
                                            showVideoControls = false
                                        }
                                    }
                                }
                                onTap(item)
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (fullScreenSwipe) gestureModifier else Modifier)
        ) {
            Box(
                modifier = Modifier
                    .width(cardWidth)
                    .height(cardHeight)
                    .align(Alignment.Center)
                    .onGloballyPositioned {
                        globalPosition = it.localToWindow(Offset.Zero)
                    }
                    .then(if (!fullScreenSwipe) gestureModifier else Modifier)
                    .graphicsLayer {
                        translationX = if (animatedScale > 1f) animatedPanOffset.x else swipeOffsetX.value
                        translationY = if (animatedScale > 1f) animatedPanOffset.y else swipeOffsetY.value
                        scaleX = animatedScale
                        scaleY = animatedScale
                        rotationZ = if (animatedScale > 1f) 0f else rotation
                        clip = false
                    },
            ) {
                Card(modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center), shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f))) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .clip(MaterialTheme.shapes.medium)) {
                        when (item.category) {
                            FileCategory.Video -> {
                                VideoPlayer(
                                    exoPlayer = exoPlayer,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            FileCategory.Audio -> {
                                AudioPlayerCard(
                                    exoPlayer = exoPlayer,
                                    mediaItem = item,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            FileCategory.Image -> {
                                val loader = if (item.mimeType == "image/gif") gifImageLoader else imageLoader
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(item.uri).crossfade(true).build(),
                                    imageLoader = loader,
                                    contentDescription = item.displayName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                    alignment = Alignment.Center
                                )
                            }
                            else -> {
                                DocumentPreviewCard(
                                    mediaItem = item,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        if (leftBorderAlpha > 0f) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val intensity = (leftBorderAlpha * 0.9f).coerceAtMost(0.9f)
                                drawRect(brush = Brush.radialGradient(0.0f to leftColor.copy(alpha = intensity), 0.12f to leftColor.copy(alpha = intensity * 0.5f), 0.5f to leftColor.copy(alpha = intensity * 0.1f), 1.0f to Color.Transparent, center = Offset(0f, size.height), radius = size.maxDimension * (1.2f + leftBorderAlpha)))
                            }
                        }
                        if (rightBorderAlpha > 0f) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val intensity = (rightBorderAlpha * 0.9f).coerceAtMost(0.9f)
                                drawRect(brush = Brush.radialGradient(0.0f to rightColor.copy(alpha = intensity), 0.12f to rightColor.copy(alpha = intensity * 0.5f), 0.5f to rightColor.copy(alpha = intensity * 0.1f), 1.0f to Color.Transparent, center = Offset(size.width, size.height), radius = size.maxDimension * (1.2f + rightBorderAlpha)))
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Swipe direction tags (trailing edge)
                            if (leftBorderAlpha > 0.3f) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = leftColor.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 16.dp)
                                ) {
                                    Text(
                                        text = if (invertSwipe) "Keep" else "Delete",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                    )
                                }
                            }
                            if (rightBorderAlpha > 0.3f) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = rightColor.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 16.dp)
                                ) {
                                    Text(
                                        text = if (invertSwipe) "Delete" else "Keep",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                    )
                                }
                            }
                            if (item.category == FileCategory.Video) {
                                // Mute button
                                val muteDesc = if (isVideoMuted) stringResource(R.string.unmute_video) else stringResource(R.string.mute_video)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .size(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(onClick = onToggleMute) {
                                        Icon(
                                            imageVector = if (isVideoMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = muteDesc,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                // Speed button
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .size(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(onClick = {
                                        val next = when (videoPlaybackSpeed) {
                                            0.5f -> 1.0f
                                            1.0f -> 1.5f
                                            1.5f -> 2.0f
                                            else -> 0.5f
                                        }
                                        onSetVideoPlaybackSpeed(next)
                                    }) {
                                        Text(
                                            text = "${videoPlaybackSpeed}x",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                if (isPendingConversion && !screenshotDeletesVideo) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .padding(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Photo,
                                            contentDescription = stringResource(R.string.pending_conversion_desc),
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                // Video seek bar
                                Column(
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                ) {
                                    AnimatedVisibility(
                                        visible = showVideoControls,
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        VideoBottomBar(
                                            exoPlayer = exoPlayer,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }


                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun VideoPlayer(
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val textureView = remember { TextureView(context) }
    AndroidView(
        factory = {
            textureView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            textureView
        },
        update = { view ->
            exoPlayer.setVideoTextureView(view)
        },
        onRelease = { view ->
            exoPlayer.clearVideoTextureView(view)
        },
        modifier = modifier
    )
}

/**
 * Gradient bottom bar inside the video card:
 * [00:04] ─────●───── [00:15]
 */
@Composable
private fun VideoBottomBar(
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier
) {
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(1L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) duration = exoPlayer.duration.coerceAtLeast(1L)
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            if (!isSeeking) currentPosition = exoPlayer.currentPosition
            delay(250)
        }
    }

    val progress = if (isSeeking) seekValue else
        if (duration > 0) currentPosition.toFloat() / duration else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                )
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column {
            Slider(
                value = progress,
                onValueChange = { value ->
                    isSeeking = true
                    seekValue = value
                },
                onValueChangeFinished = {
                    exoPlayer.seekTo((seekValue * duration).toLong())
                    isSeeking = false
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Formatters.duration(currentPosition),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Text(
                    text = Formatters.duration(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun AudioPlayerCard(
    exoPlayer: ExoPlayer,
    mediaItem: MediaItem,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(1L) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(1L)
                }
            }
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                currentPosition = exoPlayer.currentPosition
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            delay(250)
        }
    }

    Box(
        modifier = modifier.background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = mediaItem.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = Formatters.duration(duration),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(24.dp))
            LinearProgressIndicator(
                progress = { if (duration > 0) (currentPosition.toFloat() / duration) else 0f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.2f),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Formatters.duration(currentPosition),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = Formatters.duration(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            Spacer(Modifier.height(16.dp))
            IconButton(
                onClick = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun FileInfoCard(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier
) {
    val icon = when {
        mediaItem.mimeType.contains("pdf") -> Icons.Default.PictureAsPdf
        mediaItem.mimeType.contains("text") || mediaItem.extension in setOf("txt", "rtf") -> Icons.Default.Description
        mediaItem.mimeType.contains("spreadsheet") || mediaItem.extension in setOf("xls", "xlsx") -> Icons.Default.TableChart
        mediaItem.mimeType.contains("presentation") || mediaItem.extension in setOf("ppt", "pptx") -> Icons.Default.Slideshow
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

    Box(
        modifier = modifier.background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = mediaItem.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = Formatters.fileSize(mediaItem.size),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = mediaItem.mimeType.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun DocumentPreviewCard(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier
) {
    val isPdf = mediaItem.mimeType == "application/pdf" || mediaItem.extension == "pdf"
    val isText = mediaItem.mimeType.startsWith("text/") || mediaItem.extension in setOf("txt", "rtf")
    when {
        isPdf -> PdfPreview(mediaItem = mediaItem, modifier = modifier)
        isText -> TextPreview(mediaItem = mediaItem, modifier = modifier)
        else -> FileInfoCard(mediaItem = mediaItem, modifier = modifier)
    }
}

@Composable
private fun PdfPreview(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mediaItem.id) {
        pages.forEach { it.recycle() }
        pages = emptyList()
        failed = false
        errorMessage = null
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                PDFBoxResourceLoader.init(context)
                val file = mediaItem.file
                PDDocument.load(file).use { document ->
                    val renderer = PdfBoxRenderer(document)
                    val dpi = 200f
                    val result = mutableListOf<Bitmap>()
                    for (i in 0 until document.numberOfPages) {
                        val bitmap = renderer.renderImage(i, dpi)
                        result.add(bitmap)
                    }
                    if (result.isNotEmpty()) pages = result
                }
            } catch (pdfBoxError: Exception) {
                // PDFBox failed — fall back to system PdfRenderer
                try {
                    var pfd: ParcelFileDescriptor? = null
                    val uri = mediaItem.uri
                    if (uri.scheme == "content") {
                        try { pfd = context.contentResolver.openFileDescriptor(uri, "r") } catch (_: Exception) { }
                    }
                    if (pfd == null && uri.scheme == "file") {
                        try { pfd = ParcelFileDescriptor.open(mediaItem.file, ParcelFileDescriptor.MODE_READ_ONLY) } catch (_: Exception) { }
                    }
                    if (pfd == null && uri.scheme == "file") {
                        try { pfd = context.contentResolver.openFileDescriptor(uri, "r") } catch (_: Exception) { }
                    }
                    if (pfd != null) {
                        pfd.use { fd ->
                            PdfRenderer(fd).use { renderer ->
                                val result = mutableListOf<Bitmap>()
                                for (i in 0 until renderer.pageCount) {
                                    val page = renderer.openPage(i)
                                    val bitmap = Bitmap.createBitmap(
                                        page.width.coerceAtLeast(1), page.height.coerceAtLeast(1),
                                        Bitmap.Config.ARGB_8888
                                    )
                                    page.render(bitmap, null, null, 0)
                                    page.close()
                                    result.add(bitmap)
                                }
                                if (result.isNotEmpty()) pages = result
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
            if (pages.isEmpty()) {
                failed = true
                errorMessage = "Could not render PDF"
            }
        }
        isLoading = false
    }

    DisposableEffect(mediaItem.id) {
        onDispose { pages.forEach { it.recycle() } }
    }

    Box(modifier = modifier.background(Color(0xFF1A1A2E))) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.6f))
                }
            }
            failed -> FileInfoCard(mediaItem = mediaItem, modifier = Modifier.fillMaxSize())
            else -> {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                ) {
                    pages.forEachIndexed { index, bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "${mediaItem.displayName} page ${index + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = if (index == 0) 0.dp else 2.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TextPreview(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(mediaItem.id) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val stream = if (mediaItem.uri.scheme == "file") {
                    File(mediaItem.uri.path!!).inputStream()
                } else {
                    context.contentResolver.openInputStream(mediaItem.uri)
                }
                stream?.use { s ->
                    content = s.bufferedReader().use {
                        val maxChars = 100_000
                        val sb = StringBuilder(maxChars)
                        val buf = CharArray(4096)
                        var total = 0
                        var read: Int
                        while (it.read(buf).also { read = it } != -1 && total < maxChars) {
                            val toAppend = minOf(read, maxChars - total)
                            sb.append(buf, 0, toAppend)
                            total += toAppend
                        }
                        sb.toString()
                    }
                }
            } catch (_: Exception) {
                content = null
            }
        }
        isLoading = false
    }

    Box(modifier = modifier.background(Color(0xFF1A1A2E))) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.6f))
                }
            }
            content != null -> {
                val scrollState = rememberScrollState()
                SelectionContainer {
                    Text(
                        text = content!!,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            else -> FileInfoCard(mediaItem = mediaItem, modifier = Modifier.fillMaxSize())
        }
    }
}

private val MediaItem.extension: String
    get() {
        val dot = displayName.lastIndexOf('.')
        return if (dot >= 0) displayName.substring(dot + 1).lowercase() else ""
    }

@Composable
private fun NoMoreItemsMessage(pendingChanges: List<PendingChange>, onShowSummarySheet: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.no_more_items))
        Spacer(modifier = Modifier.height(16.dp))
        if (pendingChanges.isNotEmpty()) {
            Button(onClick = onShowSummarySheet) { Text(stringResource(R.string.review_changes)) }
        }
    }
}

@Composable
private fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun AlreadyOrganizedDialog(
    onSelectNewFolders: () -> Unit,
    showResetHistoryButton: Boolean,
    onResetHistory: () -> Unit,
    onResetSingleFolderHistory: () -> Unit,
    skippedCount: Int,
    onReviewSkipped: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            stringResource(R.string.all_organized_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        if (skippedCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            val skippedText = pluralStringResource(R.plurals.skipped_session_items_plurals, skippedCount, skippedCount)
            Text(
                text = skippedText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        if (skippedCount > 0) {
            Button(
                onClick = onReviewSkipped,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.review_skipped_items))
            }
        }
        Button(
            onClick = onSelectNewFolders,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.select_different_folders))
        }
        Button(
            onClick = onResetSingleFolderHistory,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FolderOff,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.reset_single_folder))
        }
        if (showResetHistoryButton) {
            Button(
                onClick = onResetHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.reset_all_sorted_media))
            }
        }
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.close_app))
        }
    }
}

@Composable
private fun ItemInfoSheet(
    item: MediaItem,
    currentIndex: Int,
    totalCount: Int,
    onRename: (String) -> Unit,
    onDropMetadata: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember(item.id) { mutableStateOf(item.displayName) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 16.dp),
    ) {
        // Main Metadata Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {

                // Filename
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = { showRenameDialog = true },
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "Rename",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // File Path
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = item.id,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    val appContext = LocalContext.current
                    IconButton(
                        onClick = {
                            val parentFile = File(item.id).parentFile
                            if (parentFile != null) {
                                val uri = FileProvider.getUriForFile(
                                    appContext,
                                    "${appContext.packageName}.provider",
                                    parentFile
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setData(uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    appContext.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(appContext, "Cannot open location", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Open location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Media Type
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, label) = mediaTypeInfo(item.category, item.mimeType)
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Technical Metadata
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = Formatters.fileSize(item.size),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (item.width > 0 && item.height > 0) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "${item.width} × ${item.height}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (item.duration > 0) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = Formatters.duration(item.duration),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Date Section
                val datesMatch = item.dateAdded == item.dateModified
                if (datesMatch) {
                    Text(
                        text = "Created & Modified",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = formatTimestamp(item.dateAdded),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Added",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = formatTimestamp(item.dateAdded),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Modified",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = formatTimestamp(item.dateModified),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Drop Metadata
        Surface(
            onClick = onDropMetadata,
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Drop Metadata",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showRenameDialog) {
        AppDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("New name") }
                )
            }
        ) {
            TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            Button(onClick = {
                onRename(renameText)
                showRenameDialog = false
            }) { Text("Rename") }
        }
    }
}

private fun mediaTypeInfo(category: FileCategory, mimeType: String): Pair<androidx.compose.ui.graphics.vector.ImageVector, String> {
    val icon = when (category) {
        FileCategory.Image -> Icons.Default.Image
        FileCategory.Video -> Icons.Default.VideoFile
        FileCategory.Audio -> Icons.Default.AudioFile
        FileCategory.Document -> Icons.Default.Description
        FileCategory.Other -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
    val ext = when {
        mimeType.contains("/") -> mimeType.substringAfter("/").uppercase()
        else -> ""
    }
    val typeLabel = when (category) {
        FileCategory.Image -> "Image"
        FileCategory.Video -> "Video"
        FileCategory.Audio -> "Audio"
        FileCategory.Document -> "Document"
        FileCategory.Other -> "File"
    }
    return icon to "$typeLabel • $ext"
}

private fun formatTimestamp(millis: Long): String {
    if (millis <= 0) return "Unknown"
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(millis))
}
