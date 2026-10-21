package com.cleanify.ui.screens.storage

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanify.domain.treemap.TreemapEngine
import com.cleanify.domain.treemap.TreemapNode
import com.cleanify.domain.treemap.TreemapRect
import com.cleanify.util.Formatters

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f

/**
 * Interactive, hardware-accelerated squarified treemap canvas.
 * Adapted from Quarry App (Apache 2.0, Tanvir).
 */
@Composable
fun TreemapCanvas(
    rootNode: TreemapNode,
    onDrillDown: (TreemapNode) -> Unit,
    onFileClick: (TreemapNode) -> Unit = {},
    onNavigateUp: () -> Unit,
    canNavigateUp: Boolean,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current

    // Compute laid-out nodes whenever canvas size or root node changes
    val laidOutNodes = remember(rootNode, canvasSize) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0 || rootNode.children.isEmpty()) {
            emptyList()
        } else {
            val bounds = TreemapRect(
                x = 0f,
                y = 0f,
                width = canvasSize.width.toFloat(),
                height = canvasSize.height.toFloat()
            )
            TreemapEngine.layoutSquarified(rootNode.children, bounds)
        }
    }

    val textPaint = remember(isDark) {
        Paint().apply {
            color = android.graphics.Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            isFakeBoldText = true
        }
    }

    val subtitlePaint = remember(isDark) {
        Paint().apply {
            color = android.graphics.Color.argb(220, 240, 240, 240)
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Navigation Header / Breadcrumb Bar
        Surface(
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                if (canNavigateUp) {
                    IconButton(
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                            onNavigateUp()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Up",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rootNode.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${Formatters.formatFileSize(rootNode.size)} • ${rootNode.children.size} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (scale > 1.05f) {
                    TextButton(
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                        }
                    ) {
                        Text("Reset Zoom", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Treemap Visualizer Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clipToBounds()
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val pointerCount = event.changes.size
                            if (pointerCount > 1 || scale > 1.05f) {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                if (zoomChange != 1f || panChange != Offset.Zero) {
                                    scale = (scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
                                    if (scale > 1f) {
                                        val maxOffsetX = (canvasSize.width * (scale - 1f)) / 2f
                                        val maxOffsetY = (canvasSize.height * (scale - 1f)) / 2f
                                        offset = Offset(
                                            x = (offset.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                                            y = (offset.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
                                        )
                                    } else {
                                        offset = Offset.Zero
                                    }
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .pointerInput(laidOutNodes, scale, offset) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = 1f
                            offset = Offset.Zero
                        },
                        onTap = { tapOffset ->
                            val contentX = (tapOffset.x - (canvasSize.width / 2f) - offset.x) / scale + (canvasSize.width / 2f)
                            val contentY = (tapOffset.y - (canvasSize.height / 2f) - offset.y) / scale + (canvasSize.height / 2f)

                            val clickedNode = laidOutNodes.firstOrNull { it.bounds.contains(contentX, contentY) }
                            if (clickedNode != null) {
                                if (clickedNode.isDirectory && clickedNode.children.isNotEmpty()) {
                                    scale = 1f
                                    offset = Offset.Zero
                                    onDrillDown(clickedNode)
                                } else {
                                    onFileClick(clickedNode)
                                }
                            }
                        }
                    )
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            ) {
                val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                val borderStroke = Stroke(width = 1.5.dp.toPx())
                val borderColor = if (isDark) Color(0x33FFFFFF) else Color(0x22000000)

                for (node in laidOutNodes) {
                    val rect = node.bounds
                    if (rect.width <= 2f || rect.height <= 2f) continue

                    val tileColor = getCategoryColor(node)
                    val inset = 1.5.dp.toPx()
                    val drawX = rect.x + inset
                    val drawY = rect.y + inset
                    val drawW = (rect.width - inset * 2).coerceAtLeast(1f)
                    val drawH = (rect.height - inset * 2).coerceAtLeast(1f)

                    // Draw filled rounded rectangle
                    drawRoundRect(
                        color = tileColor,
                        topLeft = Offset(drawX, drawY),
                        size = Size(drawW, drawH),
                        cornerRadius = cornerRadius
                    )

                    // Draw outline border
                    drawRoundRect(
                        color = borderColor,
                        topLeft = Offset(drawX, drawY),
                        size = Size(drawW, drawH),
                        cornerRadius = cornerRadius,
                        style = borderStroke
                    )

                    // Draw text label if box has sufficient dimensions
                    if (drawW >= 65.dp.toPx() && drawH >= 36.dp.toPx()) {
                        drawContext.canvas.nativeCanvas.apply {
                            textPaint.textSize = (12.sp.toPx()).coerceAtMost(drawH * 0.35f)
                            subtitlePaint.textSize = (10.sp.toPx()).coerceAtMost(drawH * 0.28f)

                            val maxChars = ((drawW - 12.dp.toPx()) / (textPaint.textSize * 0.6f)).toInt().coerceAtLeast(3)
                            val displayName = if (node.name.length > maxChars) {
                                node.name.take(maxChars - 2) + "…"
                            } else {
                                node.name
                            }

                            val textX = drawX + 8.dp.toPx()
                            val textY = drawY + textPaint.textSize + 6.dp.toPx()

                            drawText(displayName, textX, textY, textPaint)

                            if (drawH >= 54.dp.toPx()) {
                                val sizeText = Formatters.formatFileSize(node.size)
                                drawText(sizeText, textX, textY + subtitlePaint.textSize + 4.dp.toPx(), subtitlePaint)
                            }
                        }
                    }
                }
            }

            if (laidOutNodes.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "No files or folders to display",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getCategoryColor(node: TreemapNode): Color {
    if (node.isDirectory) {
        return Color(0xFF3F51B5) // Indigo for folders
    }

    return when (node.extension) {
        "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "svg" -> Color(0xFF8E24AA) // Purple for images
        "mp4", "mkv", "webm", "avi", "mov", "3gp", "flv" -> Color(0xFF00897B) // Teal for videos
        "mp3", "m4a", "wav", "flac", "ogg", "aac", "opus" -> Color(0xFFE65100) // Deep orange for audio
        "pdf", "doc", "docx", "txt", "xlsx", "pptx", "epub" -> Color(0xFF2E7D32) // Green for documents
        "apk", "apks", "aab", "xapk" -> Color(0xFFD81B60) // Rose/pink for APKs
        "zip", "rar", "7z", "tar", "gz" -> Color(0xFF6D4C41) // Brown for archives
        else -> Color(0xFF546E7A) // Slate grey for others
    }
}
