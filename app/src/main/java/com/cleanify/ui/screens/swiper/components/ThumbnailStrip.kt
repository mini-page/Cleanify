package com.cleanify.ui.screens.swiper.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cleanify.data.model.MediaItem
import com.cleanify.util.DocumentParsers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Horizontal filmstrip of all items in the batch.
 * Selected item gets a primary-color border. Decided items get a checkmark overlay.
 * Each thumbnail renders extracted artwork/preview and a bottom-left format badge.
 */
@Composable
internal fun ThumbnailStrip(
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
            ThumbnailStripItem(
                item = item,
                isActive = isActive,
                isDecided = isDecided,
                onTap = { onTap(index) },
                imageLoader = imageLoader
            )
        }
    }
}

@Composable
private fun ThumbnailStripItem(
    item: MediaItem,
    isActive: Boolean,
    isDecided: Boolean,
    onTap: () -> Unit,
    imageLoader: ImageLoader
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E1E2E))
            .then(
                if (isActive) Modifier.border(
                    width = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(10.dp)
                ) else Modifier
            )
            .clickable { onTap() }
    ) {
        // Thumbnail content based on file type
        when {
            item.isImage -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.uri)
                        .size(128)
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = item.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            item.isVideo -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.uri)
                        .size(128)
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = item.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            item.isAudio -> {
                var albumArt by remember(item.id) { mutableStateOf<Bitmap?>(null) }
                LaunchedEffect(item.id) {
                    withContext(Dispatchers.IO) {
                        val meta = DocumentParsers.extractAudioMetadata(context, item.uri)
                        albumArt = meta.albumArt
                    }
                }
                if (albumArt != null) {
                    Image(
                        bitmap = albumArt!!.asImageBitmap(),
                        contentDescription = item.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1E1E2E), Color(0xFF0F172A))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF06B6D4).copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            item.isPdf -> {
                var pdfThumb by remember(item.id) { mutableStateOf<Bitmap?>(null) }
                LaunchedEffect(item.id) {
                    withContext(Dispatchers.IO) {
                        try {
                            val pfd = if (item.uri.scheme == "file") {
                                val f = File(item.uri.path ?: item.filePath)
                                if (f.exists()) ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
                                else context.contentResolver.openFileDescriptor(item.uri, "r")
                            } else {
                                context.contentResolver.openFileDescriptor(item.uri, "r")
                            }
                            pfd?.use { fd ->
                                PdfRenderer(fd).use { renderer ->
                                    if (renderer.pageCount > 0) {
                                        val page = renderer.openPage(0)
                                        try {
                                            val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
                                            val canvas = Canvas(bmp)
                                            canvas.drawColor(android.graphics.Color.WHITE)
                                            val matrix = Matrix()
                                            val scale = 120f / maxOf(page.width, page.height)
                                            matrix.postScale(scale, scale)
                                            page.render(bmp, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                            pdfThumb = bmp
                                        } finally {
                                            page.close()
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
                if (pdfThumb != null) {
                    Image(
                        bitmap = pdfThumb!!.asImageBitmap(),
                        contentDescription = item.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2B1B1B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            item.isArchive -> {
                var apkIcon by remember(item.id) { mutableStateOf<Bitmap?>(null) }
                LaunchedEffect(item.id) {
                    withContext(Dispatchers.IO) {
                        val archive = DocumentParsers.inspectArchive(context, item.uri, maxEntries = 5)
                        if (archive.isApk) apkIcon = archive.apkIcon
                    }
                }
                if (apkIcon != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E1E2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = apkIcon!!.asImageBitmap(),
                            contentDescription = item.displayName,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2D241E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            item.isDocx -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E2638)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            item.isPptx -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2F2119)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Slideshow,
                        contentDescription = null,
                        tint = Color(0xFFF97316),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            item.isXlsx -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A2B20)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        tint = Color(0xFF22C55E),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            item.isText -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1F2430)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Article,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E2E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Bottom-left format symbol badge matching the swiper card badge
        val (badgeIcon, badgeColor) = when {
            item.isPdf -> Icons.Default.PictureAsPdf to Color(0xFFEF4444)
            item.isVideo -> Icons.Default.Videocam to Color(0xFFA855F7)
            item.isAudio -> Icons.Default.MusicNote to Color(0xFF06B6D4)
            item.isImage -> Icons.Default.Photo to Color(0xFF10B981)
            item.isDocx -> Icons.Default.Description to Color(0xFF3B82F6)
            item.isPptx -> Icons.Default.Slideshow to Color(0xFFF97316)
            item.isXlsx -> Icons.Default.TableChart to Color(0xFF22C55E)
            item.isArchive -> Icons.Default.Archive to Color(0xFFF59E0B)
            item.isText -> Icons.AutoMirrored.Filled.Article to Color(0xFF94A3B8)
            else -> Icons.AutoMirrored.Filled.InsertDriveFile to Color(0xFF64748B)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(3.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = badgeIcon,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(10.dp)
            )
        }

        // Decided checkmark overlay
        if (isDecided) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
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
    }
}
