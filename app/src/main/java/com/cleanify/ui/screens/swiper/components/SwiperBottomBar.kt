package com.cleanify.ui.screens.swiper.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * Bottom workflow toolbar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwiperBottomBar(
    hasPendingChanges: Boolean,
    isSkipButtonHidden: Boolean,
    onCreateNewAlbum: () -> Unit,
    onSkip: () -> Unit,
    onUndo: () -> Unit,
    onShowSummary: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Folder
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Above,
                spacingBetweenTooltipAndAnchor = 4.dp
            ),
            tooltip = { PlainTooltip { Text("Add target folder") } },
            state = rememberTooltipState()
        ) {
            IconButton(onClick = onCreateNewAlbum) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "Add folder")
            }
        }
        // Skip
        AnimatedVisibility(visible = !isSkipButtonHidden) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    positioning = TooltipAnchorPosition.Above,
                    spacingBetweenTooltipAndAnchor = 4.dp
                ),
                tooltip = { PlainTooltip { Text("Skip") } },
                state = rememberTooltipState()
            ) {
                IconButton(onClick = onSkip) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Skip")
                }
            }
        }
        // Undo
        AnimatedVisibility(visible = hasPendingChanges) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    positioning = TooltipAnchorPosition.Above,
                    spacingBetweenTooltipAndAnchor = 4.dp
                ),
                tooltip = { PlainTooltip { Text("Undo") } },
                state = rememberTooltipState()
            ) {
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onUndo() }) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                }
            }
        }
        Spacer(Modifier.weight(1f))
        // Proceed button
        Button(
            onClick = onShowSummary,
            enabled = hasPendingChanges,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text("Proceed")
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, modifier = Modifier.size(16.dp))
        }
    }
}
