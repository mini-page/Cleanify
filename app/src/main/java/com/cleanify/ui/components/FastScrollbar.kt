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

package com.cleanify.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private const val SCROLLBAR_MIN_THUMB_HEIGHT = 0.1f
private const val SCROLLBAR_MAX_THUMB_HEIGHT = 0.6f
private const val SCROLLBAR_INACTIVE_ALPHA = 0f
private const val SCROLLBAR_ACTIVE_ALPHA = 0.8f

private data class ScrollbarRenderInfo(
    val thumbSize: Float,
    val scrollBias: Float,
    val isVisible: Boolean
)

@Composable
private fun FastScrollbarInternal(
    isScrollInProgress: Boolean,
    renderInfo: ScrollbarRenderInfo,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (isScrollInProgress && renderInfo.isVisible) SCROLLBAR_ACTIVE_ALPHA else SCROLLBAR_INACTIVE_ALPHA,
        animationSpec = tween(durationMillis = if (isScrollInProgress) 75 else 500),
        label = "ScrollbarAlpha"
    )

    if (!renderInfo.isVisible) {
        return
    }

    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(8.dp)
            .alpha(alpha)
    ) {
        val trackHeightPx = constraints.maxHeight
        val thumbHeightDp = maxHeight * renderInfo.thumbSize

        val scrollableDistPx = trackHeightPx - with(density) { thumbHeightDp.toPx() }
        val targetThumbYPx = renderInfo.scrollBias * scrollableDistPx

        val animatedThumbYPx by animateFloatAsState(
            targetValue = targetThumbYPx,
            animationSpec = spring(),
            label = "ScrollbarPosition"
        )
        val thumbYDp = with(density) { animatedThumbYPx.toDp() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbHeightDp)
                .offset(y = thumbYDp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        )
    }
}

@Composable
fun FastScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier
) {
    val isScrollInProgress by remember { derivedStateOf { state.isScrollInProgress } }

    val renderInfo by remember {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                return@derivedStateOf ScrollbarRenderInfo(0f, 0f, isVisible = false)
            }

            val totalItemsCount = layoutInfo.totalItemsCount
            val visibleItemsCount = visibleItemsInfo.size
            val isVisible = totalItemsCount > visibleItemsCount
            if (!isVisible) {
                return@derivedStateOf ScrollbarRenderInfo(1f, 0f, isVisible = false)
            }

            val thumbSize = (40f / totalItemsCount).coerceIn(SCROLLBAR_MIN_THUMB_HEIGHT, SCROLLBAR_MAX_THUMB_HEIGHT)

            val firstVisibleItemIndex = state.firstVisibleItemIndex
            val scrollableItemsCount = (totalItemsCount - visibleItemsCount).toFloat().coerceAtLeast(1f)
            val scrollBias = (firstVisibleItemIndex.toFloat() / scrollableItemsCount).coerceIn(0f, 1f)

            ScrollbarRenderInfo(thumbSize, scrollBias, isVisible = true)
        }
    }

    FastScrollbarInternal(
        isScrollInProgress = isScrollInProgress,
        renderInfo = renderInfo,
        modifier = modifier
    )
}

@Composable
fun FastScrollbar(
    state: LazyGridState,
    modifier: Modifier = Modifier
) {
    val isScrollInProgress by remember { derivedStateOf { state.isScrollInProgress } }

    val renderInfo by remember {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                return@derivedStateOf ScrollbarRenderInfo(0f, 0f, isVisible = false)
            }

            val totalItemsCount = layoutInfo.totalItemsCount
            val visibleItemsCount = visibleItemsInfo.size
            val isVisible = totalItemsCount > visibleItemsCount
            if (!isVisible) {
                return@derivedStateOf ScrollbarRenderInfo(1f, 0f, isVisible = false)
            }

            val thumbSize = (40f / totalItemsCount).coerceIn(SCROLLBAR_MIN_THUMB_HEIGHT, SCROLLBAR_MAX_THUMB_HEIGHT)

            val firstVisibleItemIndex = state.firstVisibleItemIndex
            val scrollableItemsCount = (totalItemsCount - visibleItemsCount).toFloat().coerceAtLeast(1f)
            val scrollBias = (firstVisibleItemIndex.toFloat() / scrollableItemsCount).coerceIn(0f, 1f)

            ScrollbarRenderInfo(thumbSize, scrollBias, isVisible = true)
        }
    }

    FastScrollbarInternal(
        isScrollInProgress = isScrollInProgress,
        renderInfo = renderInfo,
        modifier = modifier
    )
}
