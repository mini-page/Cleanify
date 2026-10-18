package com.cleanify.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.cleanify.R
import com.cleanify.ui.screens.swiper.components.AudioAnimatedWaves
import com.cleanify.util.DocumentParsers
import com.cleanify.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun MediaPreviewDialog(
    uri: Uri,
    displayName: String,
    fileSize: Long,
    dateModified: Long,
    mimeType: String,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val extension = displayName.substringAfterLast('.', "").lowercase()

    val isVideo = mimeType.startsWith("video/")
    val isAudio = mimeType.startsWith("audio/") || extension in setOf("mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "opus", "mid")
    val isPdf = mimeType == "application/pdf" || extension == "pdf"
    val isDocx = extension in setOf("docx", "doc") || mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" || mimeType == "application/msword"
    val isPptx = extension in setOf("pptx", "ppt") || mimeType == "application/vnd.openxmlformats-officedocument.presentationml.presentation" || mimeType == "application/vnd.ms-powerpoint"
    val isXlsx = extension in setOf("xlsx", "xls") || mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" || mimeType == "application/vnd.ms-excel"
    val isArchive = extension in setOf("zip", "apk", "jar", "tar", "gz", "rar", "7z") || mimeType in setOf("application/zip", "application/vnd.android.package-archive", "application/x-tar", "application/gzip")
    val isText = mimeType.startsWith("text/") || extension in setOf("txt", "log", "json", "xml", "html", "css", "js", "ts", "kt", "java", "py", "c", "cpp", "md", "csv", "yaml", "yml", "sh", "bat", "ini", "conf", "rtf", "properties", "gradle", "sql")
    val isImage = mimeType.startsWith("image/")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F17))
        ) {
            when {
                isVideo -> {
                    VideoPlayerInAppContent(
                        uri = uri,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                isAudio -> {
                    AudioPlayerInAppContent(
                        uri = uri,
                        displayName = displayName,
                        fileSize = fileSize,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                isPdf -> {
                    PdfInAppViewer(
                        uri = uri,
                        displayName = displayName,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                isDocx -> {
                    DocxInAppViewer(
                        uri = uri,
                        displayName = displayName,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                isPptx -> {
                    PptxInAppViewer(
                        uri = uri,
                        displayName = displayName,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                isXlsx -> {
                    XlsxInAppViewer(
                        uri = uri,
                        displayName = displayName,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                isArchive -> {
                    ArchiveInAppViewer(
                        uri = uri,
                        displayName = displayName,
                        fileSize = fileSize,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                isText -> {
                    TextInAppViewer(
                        uri = uri,
                        displayName = displayName,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                isImage -> {
                    ImageInAppViewer(
                        uri = uri,
                        displayName = displayName,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    GenericFileInAppViewer(
                        uri = uri,
                        displayName = displayName,
                        fileSize = fileSize,
                        dateModified = dateModified,
                        mimeType = mimeType,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Top action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.65f), shape = CircleShape)
                        .size(42.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = mimeType.ifEmpty { "*/*" }
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, displayName))
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.65f), shape = CircleShape)
                            .size(42.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share), tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, mimeType.ifEmpty { "*/*" })
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(Intent.createChooser(intent, displayName))
                            } catch (_: Exception) {
                                Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.65f), shape = CircleShape)
                            .size(42.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = stringResource(R.string.open_with), tint = Color.White)
                    }

                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.65f), shape = CircleShape)
                                .size(42.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/* =========================================================================
 * 1. VIDEO VIEWER
 * ========================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoPlayerInAppContent(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }
    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }
    var controlsVisible by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isLooping by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    val d = exoPlayer.duration
                    if (d > 0) duration = d
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            if (!isUserScrubbing) {
                currentPosition = exoPlayer.currentPosition
                val d = exoPlayer.duration
                if (d > 0) duration = d
            }
            delay(200)
        }
    }

    // Auto-hide controls after 3.5s
    LaunchedEffect(controlsVisible, isPlaying, isUserScrubbing) {
        if (controlsVisible && isPlaying && !isUserScrubbing && !showSettings) {
            delay(3500)
            controlsVisible = false
        }
    }

    val displayPos = if (isUserScrubbing) (scrubPosition * duration).toLong() else currentPosition
    val progress = if (duration > 0) (displayPos.toFloat() / duration).coerceIn(0f, 1f) else 0f

    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    LaunchedEffect(resizeMode) {
        playerViewRef?.resizeMode = resizeMode
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                controlsVisible = !controlsVisible
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    playerViewRef = this
                }
            },
            update = { pv ->
                pv.resizeMode = resizeMode
                playerViewRef = pv
            },
            modifier = Modifier.fillMaxSize()
        )

        // Center 5-button controls
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                // 1. Skip Previous
                IconButton(
                    onClick = { exoPlayer.seekTo(0) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Restart / Previous",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // 2. Rewind 5s
                IconButton(
                    onClick = {
                        val newPos = (exoPlayer.currentPosition - 5000L).coerceAtLeast(0L)
                        exoPlayer.seekTo(newPos)
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Replay5,
                        contentDescription = "Rewind 5s",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // 3. Play / Pause (Large Center Circle)
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(64.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                // 4. Forward 10s
                IconButton(
                    onClick = {
                        val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(duration)
                        exoPlayer.seekTo(newPos)
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // 5. Skip Next
                IconButton(
                    onClick = {
                        exoPlayer.seekTo(duration)
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Skip Next",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Bottom scrubber bar + timestamps + Settings Gear
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Time & Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${Formatters.duration(displayPos)} · ${Formatters.duration(duration)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Video Settings",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Slider(
                    value = progress,
                    onValueChange = {
                        isUserScrubbing = true
                        scrubPosition = it
                    },
                    onValueChangeFinished = {
                        val target = (scrubPosition * duration).toLong()
                        exoPlayer.seekTo(target)
                        isUserScrubbing = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Settings Sheet with Rounded Corner Shape (24.dp)
        if (showSettings) {
            VideoPlayerSettingsSheet(
                playbackSpeed = playbackSpeed,
                onSpeedChange = { speed ->
                    playbackSpeed = speed
                    exoPlayer.playbackParameters = PlaybackParameters(speed)
                },
                resizeMode = resizeMode,
                onResizeModeChange = { mode ->
                    resizeMode = mode
                },
                isLooping = isLooping,
                onLoopChange = { loop ->
                    isLooping = loop
                    exoPlayer.repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                },
                isMuted = isMuted,
                onMuteChange = { muted ->
                    isMuted = muted
                    exoPlayer.volume = if (muted) 0f else 1f
                },
                onDismiss = { showSettings = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoPlayerSettingsSheet(
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    resizeMode: Int,
    onResizeModeChange: (Int) -> Unit,
    isLooping: Boolean,
    onLoopChange: (Boolean) -> Unit,
    isMuted: Boolean,
    onMuteChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color(0xFF1E1E2E),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Player Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Playback Speed
            Text(
                text = "Playback Speed",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                speeds.forEach { speed ->
                    val isSelected = (playbackSpeed == speed)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSpeedChange(speed) }
                    ) {
                        Text(
                            text = "${speed}x",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Screen Fit
            Text(
                text = "Screen Fit",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = listOf(
                    "Fit" to AspectRatioFrameLayout.RESIZE_MODE_FIT,
                    "Zoom / Fill" to AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                    "Stretch" to AspectRatioFrameLayout.RESIZE_MODE_FILL
                )
                modes.forEach { (label, mode) ->
                    val isSelected = (resizeMode == mode)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onResizeModeChange(mode) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Loop Video Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Loop Video",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Switch(
                    checked = isLooping,
                    onCheckedChange = onLoopChange
                )
            }

            Spacer(Modifier.height(12.dp))

            // Mute Audio Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mute Audio",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Switch(
                    checked = isMuted,
                    onCheckedChange = onMuteChange
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/* =========================================================================
 * 2. AUDIO VIEWER
 * ========================================================================= */
@Composable
private fun AudioPlayerInAppContent(
    uri: Uri,
    displayName: String,
    fileSize: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }
    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }
    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var repeatOne by remember { mutableStateOf(false) }
    var metadata by remember { mutableStateOf<DocumentParsers.AudioMetadata?>(null) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            val meta = DocumentParsers.extractAudioMetadata(context, uri)
            metadata = meta
            if (meta.durationMs > 0) duration = meta.durationMs
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    val d = exoPlayer.duration
                    if (d > 0) duration = d
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            if (!isUserScrubbing) {
                currentPosition = exoPlayer.currentPosition
                val d = exoPlayer.duration
                if (d > 0) duration = d
            }
            delay(200)
        }
    }

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(Color(0xFF1E1E2E), Color(0xFF0F0F17)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            // Large Album Art or Animated Disc
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF2A2A3E)),
                contentAlignment = Alignment.Center
            ) {
                val albumArt = metadata?.albumArt
                if (albumArt != null) {
                    Image(
                        bitmap = albumArt.asImageBitmap(),
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    AudioAnimatedWaves(isPlaying = isPlaying, modifier = Modifier.size(100.dp))
                }
            }

            Spacer(Modifier.height(28.dp))

            // Title & Artist
            Text(
                text = metadata?.title ?: displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = metadata?.artist ?: "Audio Track",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            if (metadata?.album != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = metadata?.album!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(Modifier.height(20.dp))

            // Track Specs Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "${Formatters.fileSize(fileSize)}${metadata?.bitrate?.let { " • $it" } ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Scrubber Slider
            val progressFraction = if (duration > 0) {
                if (isUserScrubbing) scrubPosition else (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Slider(
                value = progressFraction,
                onValueChange = {
                    isUserScrubbing = true
                    scrubPosition = it
                },
                onValueChangeFinished = {
                    val targetMs = (scrubPosition * duration).toLong()
                    exoPlayer.seekTo(targetMs)
                    currentPosition = targetMs
                    isUserScrubbing = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Timeline Timers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val displayPos = if (isUserScrubbing) (scrubPosition * duration).toLong() else currentPosition
                Text(
                    text = Formatters.duration(displayPos),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = Formatters.duration(duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.height(20.dp))

            // Player Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playback speed
                IconButton(onClick = {
                    val nextSpeed = when (playbackSpeed) {
                        0.5f -> 1.0f
                        1.0f -> 1.25f
                        1.25f -> 1.5f
                        1.5f -> 2.0f
                        else -> 0.5f
                    }
                    playbackSpeed = nextSpeed
                    exoPlayer.setPlaybackSpeed(nextSpeed)
                }) {
                    Text(
                        text = "${playbackSpeed}x",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Rewind 10s
                IconButton(
                    onClick = {
                        val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                        exoPlayer.seekTo(newPos)
                        currentPosition = newPos
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                }

                // Play / Pause FAB
                Surface(
                    onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(68.dp),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                // Forward 10s
                IconButton(
                    onClick = {
                        val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(duration)
                        exoPlayer.seekTo(newPos)
                        currentPosition = newPos
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                }

                // Loop Toggle
                IconButton(onClick = {
                    repeatOne = !repeatOne
                    exoPlayer.repeatMode = if (repeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_ALL
                }) {
                    Icon(
                        imageVector = if (repeatOne) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Loop",
                        tint = if (repeatOne) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/* =========================================================================
 * 3. PDF VIEWER
 * ========================================================================= */
@Composable
private fun PdfInAppViewer(
    uri: Uri,
    displayName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val listState = rememberLazyListState()
    val currentPageIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex.coerceAtLeast(0) }
    }

    LaunchedEffect(uri) {
        isLoading = true
        errorMessage = null
        withContext(Dispatchers.IO) {
            try {
                val descriptor = if (uri.scheme == "file") {
                    val f = File(uri.path ?: "")
                    if (f.exists()) ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
                    else context.contentResolver.openFileDescriptor(uri, "r")
                } else {
                    context.contentResolver.openFileDescriptor(uri, "r")
                }
                if (descriptor != null) {
                    pfd = descriptor
                    val renderer = PdfRenderer(descriptor)
                    pdfRenderer = renderer
                    pageCount = renderer.pageCount
                } else {
                    errorMessage = "Unable to open document"
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to render PDF"
            } finally {
                isLoading = false
            }
        }
    }

    DisposableEffect(uri) {
        onDispose {
            try { pdfRenderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF121212))
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 4f)
                    offsetX = if (newScale > 1f) (offsetX + pan.x).coerceIn(-1000f, 1000f) else 0f
                    offsetY = if (newScale > 1f) (offsetY + pan.y).coerceIn(-1000f, 1000f) else 0f
                    scale = newScale
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                })
            }
    ) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            errorMessage != null || pdfRenderer == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "Could not render PDF",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            else -> {
                val renderer = pdfRenderer!!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 72.dp, bottom = 80.dp, start = 12.dp, end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(pageCount) { pageIndex ->
                            PdfPageCard(
                                renderer = renderer,
                                pageIndex = pageIndex,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (pageCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        contentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.pdf_page_n_of_total, (currentPageIndex + 1).coerceAtMost(pageCount), pageCount),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPageCard(
    renderer: PdfRenderer,
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var aspectRatio by remember { mutableFloatStateOf(1f / 1.414f) }
    var isPageLoading by remember { mutableStateOf(true) }

    LaunchedEffect(pageIndex) {
        isPageLoading = true
        withContext(Dispatchers.IO) {
            try {
                synchronized(renderer) {
                    if (pageIndex < renderer.pageCount) {
                        val page = renderer.openPage(pageIndex)
                        try {
                            val w = page.width.coerceAtLeast(1)
                            val h = page.height.coerceAtLeast(1)
                            aspectRatio = w.toFloat() / h.toFloat()

                            val screenWidth = context.resources.displayMetrics.widthPixels
                            val targetWidth = (screenWidth * 1.2f).toInt().coerceAtLeast(600)
                            val scaleFactor = targetWidth.toFloat() / w.toFloat()
                            val targetHeight = (h.toFloat() * scaleFactor).toInt().coerceAtLeast(100)

                            val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bmp)
                            canvas.drawColor(android.graphics.Color.WHITE)

                            val matrix = Matrix()
                            matrix.postScale(scaleFactor, scaleFactor)
                            page.render(bmp, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmap = bmp
                        } finally {
                            page.close()
                        }
                    }
                }
            } catch (_: Exception) {}
            isPageLoading = false
        }
    }

    DisposableEffect(pageIndex) {
        onDispose {
            try { bitmap?.recycle() } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else if (isPageLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
        }
    }
}

/* =========================================================================
 * 4. DOCX VIEWER
 * ========================================================================= */
@Composable
private fun DocxInAppViewer(
    uri: Uri,
    displayName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var paragraphs by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(uri) {
        isLoading = true
        withContext(Dispatchers.IO) {
            paragraphs = DocumentParsers.extractDocxText(context, uri, maxParagraphs = 500)
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF1E1E2E))
            .padding(top = 70.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
    ) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        val allText = paragraphs.joinToString("\n\n")
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Docx Text", allText))
                        Toast.makeText(context, "Copied document text", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy text", tint = Color(0xFF4B5563))
                    }
                }

                Text(
                    text = "${paragraphs.size} paragraphs",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6B7280)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                SelectionContainer(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        paragraphs.forEach { paragraph ->
                            Text(
                                text = paragraph,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF1F2937),
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/* =========================================================================
 * 5. PPTX VIEWER
 * ========================================================================= */
@Composable
private fun PptxInAppViewer(
    uri: Uri,
    displayName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var slides by remember { mutableStateOf<List<DocumentParsers.SlideInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(uri) {
        isLoading = true
        withContext(Dispatchers.IO) {
            slides = DocumentParsers.extractPptxSlides(context, uri, maxSlides = 100)
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF1E1E2E))
            .padding(top = 70.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
    ) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(slides) { slide ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFF7ED),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Slideshow, contentDescription = null, tint = Color(0xFFEA580C))
                                Text(
                                    text = "Slide ${slide.slideNumber} of ${slides.size}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC2410C)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = slide.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF1F2937),
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/* =========================================================================
 * 6. XLSX VIEWER
 * ========================================================================= */
@Composable
private fun XlsxInAppViewer(
    uri: Uri,
    displayName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sheetData by remember { mutableStateOf<DocumentParsers.SheetPreview?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(uri) {
        isLoading = true
        withContext(Dispatchers.IO) {
            sheetData = DocumentParsers.extractXlsxSheet(context, uri, maxRows = 200, maxCols = 15)
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF12121A))
            .padding(top = 70.dp, bottom = 20.dp, start = 12.dp, end = 12.dp)
    ) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (sheetData != null) {
            val data = sheetData!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(12.dp)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${data.totalRows} rows previewed",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6B7280)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                val hScroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(hScroll)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxHeight()) {
                        items(data.rows.size) { rowIndex ->
                            val row = data.rows[rowIndex]
                            Row(
                                modifier = Modifier
                                    .background(if (rowIndex == 0) Color(0xFFDCFCE7) else if (rowIndex % 2 == 0) Color(0xFFF9FAFB) else Color.White)
                                    .padding(vertical = 6.dp)
                            ) {
                                // Row number
                                Text(
                                    text = "${rowIndex + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9CA3AF),
                                    modifier = Modifier.width(36.dp).padding(horizontal = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                                row.forEach { cell ->
                                    Text(
                                        text = cell,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                        color = Color(0xFF1F2937),
                                        modifier = Modifier
                                            .width(130.dp)
                                            .padding(horizontal = 8.dp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E7EB))
                        }
                    }
                }
            }
        }
    }
}

/* =========================================================================
 * 7. ARCHIVE (ZIP / APK) VIEWER
 * ========================================================================= */
@Composable
private fun ArchiveInAppViewer(
    uri: Uri,
    displayName: String,
    fileSize: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var archiveInfo by remember { mutableStateOf<DocumentParsers.ArchiveInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(uri) {
        isLoading = true
        withContext(Dispatchers.IO) {
            archiveInfo = DocumentParsers.inspectArchive(context, uri, maxEntries = 500)
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF12121A))
            .padding(top = 70.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
    ) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (archiveInfo != null) {
            val info = archiveInfo!!
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Info Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1E2E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (info.isApk && info.apkIcon != null) {
                            Image(
                                bitmap = info.apkIcon.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = info.apkAppName ?: displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (info.isApk && info.apkPackageName != null) {
                                Text(
                                    text = "${info.apkPackageName} (v${info.apkVersionName ?: "1.0"})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${info.totalFiles} files • Compressed: ${Formatters.fileSize(fileSize)} • Uncompressed: ${Formatters.fileSize(info.totalUncompressedBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                val filteredEntries = remember(searchQuery, info.entries) {
                    if (searchQuery.isBlank()) info.entries
                    else info.entries.filter { it.name.contains(searchQuery, ignoreCase = true) }
                }

                // File Tree List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A1A28))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredEntries) { entry ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = null,
                                tint = if (entry.isDirectory) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = entry.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (!entry.isDirectory) {
                                Text(
                                    text = Formatters.fileSize(entry.uncompressedSize),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = Color.White.copy(alpha = 0.08f))
                    }
                }
            }
        }
    }
}

/* =========================================================================
 * 8. TEXT VIEWER
 * ========================================================================= */
@Composable
private fun TextInAppViewer(
    uri: Uri,
    displayName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(uri) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val stream = if (uri.scheme == "file") File(uri.path!!).inputStream() else context.contentResolver.openInputStream(uri)
                stream?.use { s ->
                    content = s.bufferedReader().use { it.readText() }
                }
            } catch (_: Exception) {
                content = null
            }
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF0F0F17))
            .padding(top = 70.dp, bottom = 20.dp, start = 12.dp, end = 12.dp)
    ) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (content != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF181824))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Text Content", content))
                        Toast.makeText(context, "Copied text content", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy text", tint = Color.White.copy(alpha = 0.8f))
                    }
                }

                val lineCount = remember(content) { content!!.lines().size }
                Text(
                    text = "$lineCount lines • ${content!!.length} characters",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.15f))

                val isMarkdown = remember(displayName) {
                    displayName.endsWith(".md", ignoreCase = true) || displayName.endsWith(".markdown", ignoreCase = true)
                }

                if (isMarkdown) {
                    MarkdownViewer(
                        markdown = content!!,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    )
                } else {
                    SelectionContainer(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = content!!,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

/* =========================================================================
 * 9. IMAGE VIEWER
 * ========================================================================= */
@Composable
private fun ImageInAppViewer(
    uri: Uri,
    displayName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(uri)
            .crossfade(true)
            .build(),
        contentDescription = displayName,
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    offsetX = if (newScale > 1f) (offsetX + centroid.x * (scale - newScale) + pan.x) else 0f
                    offsetY = if (newScale > 1f) (offsetY + centroid.y * (scale - newScale) + pan.y) else 0f
                    scale = newScale
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                })
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            ),
        contentScale = ContentScale.Fit,
        loading = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        },
        error = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    )
}

/* =========================================================================
 * 10. GENERIC FILE VIEWER
 * ========================================================================= */
@Composable
private fun GenericFileInAppViewer(
    uri: Uri,
    displayName: String,
    fileSize: Long,
    dateModified: Long,
    mimeType: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF0F0F17))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E2E),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Size: ${Formatters.fileSize(fileSize)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Type: ${mimeType.ifEmpty { "Unknown" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Modified: ${Formatters.date(dateModified)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}
