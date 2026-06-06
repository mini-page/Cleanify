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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cleanify.ui.theme.AppTheme
import com.cleanify.ui.theme.LocalAppTheme

@Composable
fun FolderSearchDialog(
    state: FolderSearchState,
    title: String,
    searchLabel: String,
    confirmButtonText: String,
    autoConfirmOnSelection: Boolean,
    onDismiss: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onFolderSelected: (String) -> Unit,
    onConfirm: () -> Unit,
    onSearch: () -> Unit = {},
    formatListItemTitle: (String) -> Pair<String, String> = { path ->
        Pair(
            path.substringAfterLast('/'),
            path.removePrefix("/storage/emulated/0/")
        )
    }
) {
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val currentTheme = LocalAppTheme.current
        val cardContainerColor = if (currentTheme == AppTheme.AMOLED) {
            MaterialTheme.colorScheme.surface
        } else {
            CardDefaults.cardColors().containerColor
        }

        val cardModifier = if (currentTheme == AppTheme.AMOLED) {
            Modifier.border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                MaterialTheme.shapes.extraLarge
            )
        } else {
            Modifier
        }

        Card(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = cardModifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 400.dp),
            colors = CardDefaults.cardColors(
                containerColor = cardContainerColor
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onQueryChanged,
                    label = { Text(searchLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    modifier = Modifier
                        .focusRequester(searchFocusRequester)
                        .fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .animateContentSize()
                ) {
                    Box(
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        when {
                            state.isLoading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            state.displayedResults.isNotEmpty() -> {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    state.displayedResults.forEach { path ->
                                        val (name, displayPath) = formatListItemTitle(path)
                                        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        onFolderSelected(path)
                                                        if (autoConfirmOnSelection) {
                                                            onConfirm()
                                                        }
                                                    }
                                                    .padding(vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                                                Column {
                                                    Text(text = name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(text = displayPath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        }
                                    }
                                }
                            }
                            state.searchQuery.isNotBlank() && !state.isLoading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No folders found.",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }

                    val displayedPath = state.browsePath
                    if (displayedPath != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Text(
                            text = "Selected: .../${displayedPath.takeLast(35)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    if (!autoConfirmOnSelection) {
                        Button(
                            onClick = onConfirm,
                            enabled = state.browsePath != null
                        ) { Text(confirmButtonText) }
                    }
                }
            }
        }
    }
}
