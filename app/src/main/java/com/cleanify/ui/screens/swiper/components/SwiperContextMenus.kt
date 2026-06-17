package com.cleanify.ui.screens.swiper.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.cleanify.R
import com.cleanify.data.model.MediaItem
import com.cleanify.ui.components.AppDropdownMenu
import com.cleanify.ui.components.AppMenuDivider
import com.cleanify.ui.screens.swiper.FolderMenuState
import com.cleanify.util.Formatters

@Composable
internal fun FolderContextMenu(
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
internal fun MediaItemContextMenu(
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
    onInfo: () -> Unit,
    hideFilename: Boolean = false
) {
    val appContext = LocalContext.current
    val copiedString = stringResource(R.string.filename_copied)

    if (isVisible && currentItem != null) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = onDismiss,
            offset = DpOffset((-16).dp, 0.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
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
                            if (!hideFilename) {
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
                            }
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
