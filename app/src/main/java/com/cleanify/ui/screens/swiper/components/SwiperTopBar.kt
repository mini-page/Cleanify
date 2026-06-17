package com.cleanify.ui.screens.swiper.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.cleanify.data.model.MediaItem
import com.cleanify.data.repository.FolderNameLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwiperTopBar(
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
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
