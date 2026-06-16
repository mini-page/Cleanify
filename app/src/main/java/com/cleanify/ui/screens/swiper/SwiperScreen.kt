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
import com.cleanify.ui.components.EmptyState
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
import com.cleanify.ui.screens.swiper.components.AlreadyOrganizedDialog
import com.cleanify.ui.screens.swiper.components.BottomFolderBar
import com.cleanify.ui.screens.swiper.components.ErrorMessage
import com.cleanify.ui.screens.swiper.components.FolderContextMenu
import com.cleanify.ui.screens.swiper.components.ItemInfoSheet
import com.cleanify.ui.screens.swiper.components.MediaItemCard
import com.cleanify.ui.screens.swiper.components.MediaItemContextMenu
import com.cleanify.ui.screens.swiper.components.NoMoreItemsMessage
import com.cleanify.ui.screens.swiper.components.SwiperTopBar
import com.cleanify.ui.screens.swiper.components.ThumbnailStrip

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