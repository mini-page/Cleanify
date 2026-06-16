package com.cleanify.ui.screens.swiper.components

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cleanify.data.model.FileCategory
import com.cleanify.data.model.MediaItem

/**
 * Horizontal filmstrip of all items in the batch.
 * Selected item gets a primary-color border. Decided items get a checkmark overlay.
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
