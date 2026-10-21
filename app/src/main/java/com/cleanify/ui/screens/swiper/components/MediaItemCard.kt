package com.cleanify.ui.screens.swiper.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInWindow
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cleanify.R
import com.cleanify.data.model.FileCategory
import com.cleanify.data.model.MediaItem
import com.cleanify.data.repository.SwipeDownAction
import com.cleanify.data.repository.SwipeUpAction
import com.cleanify.data.repository.SwipeSensitivity
import com.cleanify.ui.theme.LocalReducedAnimations
import com.cleanify.ui.screens.swiper.components.AudioPlayerCard
import com.cleanify.ui.screens.swiper.components.DocumentPreviewCard
import com.cleanify.ui.screens.swiper.components.VideoBottomBar
import com.cleanify.ui.screens.swiper.components.VideoPlayer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MediaItemCard(
    item: MediaItem,
    exoPlayer: ExoPlayer,
    imageLoader: ImageLoader,
    gifImageLoader: ImageLoader,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeDown: () -> Unit,
    onSwipeUp: () -> Unit = {},
    onLongPress: (offset: DpOffset) -> Unit,
    onTap: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    invertSwipe: Boolean = false,
    sensitivity: SwipeSensitivity,
    swipeDownAction: SwipeDownAction,
    swipeUpAction: SwipeUpAction = SwipeUpAction.NONE,
    videoPlaybackSpeed: Float,
    onSetVideoPlaybackSpeed: (Float) -> Unit,
    isVideoMuted: Boolean,
    onToggleMute: () -> Unit,
    isPendingConversion: Boolean,
    screenshotDeletesVideo: Boolean,
    fullScreenSwipe: Boolean,
    onOpenFullScreen: ((MediaItem) -> Unit)? = null
) {
    val swipeOffsetX = remember { Animatable(0f) }
    val swipeOffsetY = remember { Animatable(0f) }
    val density = LocalDensity.current
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var showVideoControls by remember { mutableStateOf(false) }
    val videoControlsScope = rememberCoroutineScope()
    val swipeScope = rememberCoroutineScope()

    val haptic = LocalHapticFeedback.current

    val swipeThreshold = when (sensitivity) {
        SwipeSensitivity.LOW -> with(density) { 60.dp.toPx() }
        SwipeSensitivity.MEDIUM -> with(density) { 80.dp.toPx() }
        SwipeSensitivity.HIGH -> with(density) { 140.dp.toPx() }
    }
    val swipeDownThreshold = swipeThreshold * 0.8f
    val velocityThreshold = with(density) { 800.dp.toPx() }

    val reduceAnimations = LocalReducedAnimations.current
    val snapBackSpec = if (reduceAnimations) snap<Float>() else spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val flyAwaySpec = if (reduceAnimations) snap<Float>() else tween<Float>(durationMillis = 220)
    val animatedScale by animateFloatAsState(targetValue = scale, label = "scale")
    val animatedPanOffset by animateOffsetAsState(targetValue = panOffset, label = "panOffset")

    val leftColor = if (invertSwipe) Color(0xFF22C55E) else Color(0xFFEF4444)
    val rightColor = if (invertSwipe) Color(0xFFEF4444) else Color(0xFF22C55E)

    val (upColor, upText, upIcon) = when (swipeUpAction) {
        SwipeUpAction.SKIP -> Triple(Color(0xFF64748B), "Skip", Icons.Default.SkipNext)
        SwipeUpAction.FAVORITE -> Triple(Color(0xFFEAB308), "Favorite", Icons.Default.Star)
        SwipeUpAction.NONE -> Triple(Color.Transparent, "", Icons.Default.SkipNext)
    }

    val (downColor, downText, downIcon) = when (swipeDownAction) {
        SwipeDownAction.SKIP_ITEM -> Triple(Color(0xFF64748B), "Skip", Icons.Default.SkipNext)
        SwipeDownAction.MOVE_TO_EDIT -> Triple(Color(0xFF3B82F6), "Edit", Icons.Default.Edit)
        SwipeDownAction.ADD_TARGET_FOLDER -> Triple(Color(0xFF3B82F6), "Move", Icons.Default.DriveFileMove)
        SwipeDownAction.SHARE -> Triple(Color(0xFF10B981), "Share", Icons.Default.Share)
        SwipeDownAction.OPEN_WITH -> Triple(Color(0xFF8B5CF6), "Open With", Icons.Default.OpenInNew)
        SwipeDownAction.NONE -> Triple(Color.Transparent, "", Icons.Default.SkipNext)
    }
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
                                    if (scale <= 1f) {
                                        swipeScope.launch {
                                            swipeOffsetX.snapTo(swipeOffsetX.value + dragAmount.x)
                                            val canSwipeUp = swipeUpAction != SwipeUpAction.NONE
                                            val canSwipeDown = swipeDownAction != SwipeDownAction.NONE
                                            val newY = when {
                                                dragAmount.y > 0 && canSwipeDown -> swipeOffsetY.value + dragAmount.y
                                                dragAmount.y < 0 && canSwipeUp -> swipeOffsetY.value + dragAmount.y
                                                dragAmount.y > 0 && !canSwipeDown -> (swipeOffsetY.value + dragAmount.y * 0.25f).coerceAtMost(35f)
                                                dragAmount.y < 0 && !canSwipeUp -> (swipeOffsetY.value + dragAmount.y * 0.25f).coerceAtLeast(-35f)
                                                else -> swipeOffsetY.value + dragAmount.y
                                            }
                                            swipeOffsetY.snapTo(newY)
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

                            val absX = abs(swipeOffsetX.value)
                            val absY = abs(swipeOffsetY.value)

                            if (absX >= absY) {
                                // Horizontal dominant swipe
                                when {
                                    swipeOffsetX.value < -swipeThreshold || (isFlingX && vx < 0) -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        swipeScope.launch {
                                            swipeOffsetX.animateTo(-cardWidth.toPx() * 1.6f, flyAwaySpec)
                                        }
                                        onSwipeLeft()
                                    }
                                    swipeOffsetX.value > swipeThreshold || (isFlingX && vx > 0) -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        swipeScope.launch {
                                            swipeOffsetX.animateTo(cardWidth.toPx() * 1.6f, flyAwaySpec)
                                        }
                                        onSwipeRight()
                                    }
                                    else -> {
                                        swipeScope.launch {
                                            launch { swipeOffsetX.animateTo(0f, snapBackSpec) }
                                            launch { swipeOffsetY.animateTo(0f, snapBackSpec) }
                                        }
                                    }
                                }
                            } else {
                                // Vertical dominant swipe
                                when {
                                    swipeDownAction != SwipeDownAction.NONE && (swipeOffsetY.value > swipeDownThreshold || (isFlingY && vy > 0)) -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        swipeScope.launch {
                                            swipeOffsetY.animateTo(cardHeight.toPx() * 1.6f, flyAwaySpec)
                                        }
                                        onSwipeDown()
                                    }
                                    swipeUpAction != SwipeUpAction.NONE && (swipeOffsetY.value < -swipeDownThreshold || (isFlingY && vy < 0)) -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        swipeScope.launch {
                                            swipeOffsetY.animateTo(-cardHeight.toPx() * 1.6f, flyAwaySpec)
                                        }
                                        onSwipeUp()
                                    }
                                    else -> {
                                        swipeScope.launch {
                                            launch { swipeOffsetX.animateTo(0f, snapBackSpec) }
                                            launch { swipeOffsetY.animateTo(0f, snapBackSpec) }
                                        }
                                    }
                                }
                            }
                        } else if (!wasTransforming && !longPressFired) {
                            if (scale > 1f) {
                                scale = 1f
                                panOffset = Offset.Zero
                            } else {
                                when {
                                    item.isVideo || item.isAudio -> {
                                        if (exoPlayer.isPlaying) {
                                            exoPlayer.pause()
                                            showVideoControls = true
                                        } else {
                                            exoPlayer.play()
                                            showVideoControls = false
                                        }
                                    }
                                    item.isImage -> {
                                        onTap(item)
                                    }
                                    else -> {
                                        // Documents/archives do NOT open on tap/swipe gesture;
                                        // only the dedicated top-right fullscreen button triggers in-app view.
                                    }
                                }
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
                        rotationZ = if (animatedScale > 1f) 0f else (swipeOffsetX.value / 35f).coerceIn(-16f, 16f)
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
                        when {
                            item.isVideo -> {
                                VideoPlayer(
                                    exoPlayer = exoPlayer,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            item.isAudio -> {
                                AudioPlayerCard(
                                    exoPlayer = exoPlayer,
                                    mediaItem = item,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            item.isImage -> {
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
                                    modifier = Modifier.fillMaxSize(),
                                    onOpenInApp = { (onOpenFullScreen ?: onTap).invoke(item) }
                                )
                            }
                        }
                        // Single hardware-accelerated Canvas for directional light gradients
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val curX = swipeOffsetX.value
                            val curY = swipeOffsetY.value
                            val leftA = if (curX < 0) (kotlin.math.abs(curX) / swipeThreshold).coerceIn(0f, 1f) else 0f
                            val rightA = if (curX > 0) (curX / swipeThreshold).coerceIn(0f, 1f) else 0f
                            val upA = if (curY < 0 && swipeUpAction != SwipeUpAction.NONE) (kotlin.math.abs(curY) / swipeDownThreshold).coerceIn(0f, 1f) else 0f
                            val downA = if (curY > 0 && swipeDownAction != SwipeDownAction.NONE) (curY / swipeDownThreshold).coerceIn(0f, 1f) else 0f

                            if (leftA > 0f) {
                                val intensity = (leftA * 0.9f).coerceAtMost(0.9f)
                                drawRect(
                                    brush = Brush.radialGradient(
                                        0.0f to leftColor.copy(alpha = intensity),
                                        0.12f to leftColor.copy(alpha = intensity * 0.5f),
                                        0.5f to leftColor.copy(alpha = intensity * 0.1f),
                                        1.0f to Color.Transparent,
                                        center = Offset(0f, size.height),
                                        radius = size.maxDimension * (1.2f + leftA)
                                    )
                                )
                            }
                            if (rightA > 0f) {
                                val intensity = (rightA * 0.9f).coerceAtMost(0.9f)
                                drawRect(
                                    brush = Brush.radialGradient(
                                        0.0f to rightColor.copy(alpha = intensity),
                                        0.12f to rightColor.copy(alpha = intensity * 0.5f),
                                        0.5f to rightColor.copy(alpha = intensity * 0.1f),
                                        1.0f to Color.Transparent,
                                        center = Offset(size.width, size.height),
                                        radius = size.maxDimension * (1.2f + rightA)
                                    )
                                )
                            }
                            if (upA > 0f) {
                                val intensity = (upA * 0.85f).coerceAtMost(0.85f)
                                drawRect(
                                    brush = Brush.radialGradient(
                                        0.0f to upColor.copy(alpha = intensity),
                                        0.15f to upColor.copy(alpha = intensity * 0.5f),
                                        0.6f to upColor.copy(alpha = intensity * 0.1f),
                                        1.0f to Color.Transparent,
                                        center = Offset(size.width / 2f, size.height),
                                        radius = size.maxDimension * (1.1f + upA)
                                    )
                                )
                            }
                            if (downA > 0f) {
                                val intensity = (downA * 0.85f).coerceAtMost(0.85f)
                                drawRect(
                                    brush = Brush.radialGradient(
                                        0.0f to downColor.copy(alpha = intensity),
                                        0.15f to downColor.copy(alpha = intensity * 0.5f),
                                        0.6f to downColor.copy(alpha = intensity * 0.1f),
                                        1.0f to Color.Transparent,
                                        center = Offset(size.width / 2f, 0f),
                                        radius = size.maxDimension * (1.1f + downA)
                                    )
                                )
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Action Pill Overlays driven via RenderThread graphicsLayer (zero touch recompositions)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = leftColor.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 16.dp)
                                    .graphicsLayer {
                                        val curX = swipeOffsetX.value
                                        val leftA = if (curX < 0) (kotlin.math.abs(curX) / swipeThreshold).coerceIn(0f, 1f) else 0f
                                        alpha = if (leftA > 0.2f) ((leftA - 0.2f) / 0.8f).coerceIn(0f, 1f) else 0f
                                        scaleX = 0.8f + 0.2f * alpha
                                        scaleY = 0.8f + 0.2f * alpha
                                    }
                            ) {
                                Text(
                                    text = if (invertSwipe) "Keep" else "Delete",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = rightColor.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 16.dp)
                                    .graphicsLayer {
                                        val curX = swipeOffsetX.value
                                        val rightA = if (curX > 0) (curX / swipeThreshold).coerceIn(0f, 1f) else 0f
                                        alpha = if (rightA > 0.2f) ((rightA - 0.2f) / 0.8f).coerceIn(0f, 1f) else 0f
                                        scaleX = 0.8f + 0.2f * alpha
                                        scaleY = 0.8f + 0.2f * alpha
                                    }
                            ) {
                                Text(
                                    text = if (invertSwipe) "Delete" else "Keep",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                )
                            }

                            if (swipeUpAction != SwipeUpAction.NONE) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = upColor.copy(alpha = 0.9f),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 24.dp)
                                        .graphicsLayer {
                                            val curY = swipeOffsetY.value
                                            val upA = if (curY < 0) (kotlin.math.abs(curY) / swipeDownThreshold).coerceIn(0f, 1f) else 0f
                                            alpha = if (upA > 0.2f) ((upA - 0.2f) / 0.8f).coerceIn(0f, 1f) else 0f
                                            scaleX = 0.8f + 0.2f * alpha
                                            scaleY = 0.8f + 0.2f * alpha
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = upIcon,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = upText,
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                }
                            }

                            if (swipeDownAction != SwipeDownAction.NONE) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = downColor.copy(alpha = 0.9f),
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 56.dp)
                                        .graphicsLayer {
                                            val curY = swipeOffsetY.value
                                            val downA = if (curY > 0) (curY / swipeDownThreshold).coerceIn(0f, 1f) else 0f
                                            alpha = if (downA > 0.2f) ((downA - 0.2f) / 0.8f).coerceIn(0f, 1f) else 0f
                                            scaleX = 0.8f + 0.2f * alpha
                                            scaleY = 0.8f + 0.2f * alpha
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = downIcon,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = downText,
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                }
                            }
                            // Top-Start Controls: Mute/Unmute for Video & Audio, plus Pending Conversion indicator
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.isVideo || item.isAudio) {
                                    val muteDesc = if (isVideoMuted) stringResource(R.string.unmute_video) else stringResource(R.string.mute_video)
                                    Box(
                                        modifier = Modifier
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
                                }
                                if (isPendingConversion && !screenshotDeletesVideo) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .size(36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Photo,
                                            contentDescription = stringResource(R.string.pending_conversion_desc),
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Top-End Controls: Speed (for Video) & Dedicated Fullscreen Button for ALL file types
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.isVideo) {
                                    Box(
                                        modifier = Modifier
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
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .size(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(onClick = { (onOpenFullScreen ?: onTap).invoke(item) }) {
                                        Icon(
                                            imageVector = Icons.Default.Fullscreen,
                                            contentDescription = stringResource(R.string.fullscreen_desc),
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Bottom Seekbar: Revealed on Tap for Video and Audio
                            if (item.isVideo || item.isAudio) {
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

                            androidx.compose.animation.AnimatedVisibility(
                                visible = !showVideoControls || (!item.isVideo && !item.isAudio),
                                enter = fadeIn(),
                                exit = fadeOut(),
                                modifier = Modifier.align(Alignment.BottomStart)
                            ) {
                                FileTypeBadge(
                                    item = item,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }


                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun FileTypeBadge(
    item: MediaItem,
    modifier: Modifier = Modifier
) {
    val (icon, label, accentColor) = when {
        item.isPdf -> Triple(Icons.Default.PictureAsPdf, "PDF", Color(0xFFEF4444))
        item.isVideo -> Triple(Icons.Default.Videocam, if (item.extension.isNotEmpty()) item.extension.uppercase() else "VIDEO", Color(0xFFA855F7))
        item.isAudio -> Triple(Icons.Default.MusicNote, if (item.extension.isNotEmpty()) item.extension.uppercase() else "AUDIO", Color(0xFF06B6D4))
        item.isImage -> Triple(Icons.Default.Photo, if (item.extension.isNotEmpty()) item.extension.uppercase() else "IMAGE", Color(0xFF10B981))
        item.isDocx -> Triple(Icons.Default.Description, "DOCX", Color(0xFF3B82F6))
        item.isPptx -> Triple(Icons.Default.Slideshow, "PPTX", Color(0xFFF97316))
        item.isXlsx -> Triple(Icons.Default.TableChart, "XLSX", Color(0xFF22C55E))
        item.isArchive -> Triple(Icons.Default.Archive, if (item.extension.isNotEmpty()) item.extension.uppercase() else "ARCHIVE", Color(0xFFF59E0B))
        item.isText -> Triple(Icons.AutoMirrored.Filled.Article, if (item.extension.isNotEmpty()) item.extension.uppercase() else "TEXT", Color(0xFF94A3B8))
        else -> Triple(Icons.AutoMirrored.Filled.InsertDriveFile, if (item.extension.isNotEmpty()) item.extension.uppercase() else "FILE", Color(0xFF64748B))
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
