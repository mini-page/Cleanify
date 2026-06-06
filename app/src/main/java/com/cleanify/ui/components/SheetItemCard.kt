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

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cleanify.data.model.MediaItem
import com.cleanify.data.repository.SummaryViewMode

@Composable
fun SheetItemCard(
    item: MediaItem,
    viewMode: SummaryViewMode,
    modifier: Modifier = Modifier
) {
    val size = when (viewMode) {
        SummaryViewMode.GRID -> 80.dp
        SummaryViewMode.COMPACT -> 60.dp
        SummaryViewMode.LIST -> 48.dp
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(size)
            .padding(2.dp)
    ) {
        Card(
            modifier = Modifier
                .size(size * if (viewMode == SummaryViewMode.LIST) 1f else 0.78f)
                .padding(0.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // No filename display in grid or compact view modes
        // Only LIST mode shows filenames (handled by parent composable)
    }
}
