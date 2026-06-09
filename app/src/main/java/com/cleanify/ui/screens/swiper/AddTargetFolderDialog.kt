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

package com.cleanify.ui.screens.swiper

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cleanify.data.repository.AddFolderFocusTarget
import com.cleanify.ui.components.FolderSearchState
import com.cleanify.ui.theme.LocalAppTheme
import com.cleanify.ui.theme.isDark
import java.io.File
import kotlin.math.min
import kotlinx.coroutines.delay

private sealed class HintState {
    object None : HintState()
    data class ExactMatch(val path: String) : HintState()
    data class SimilarMatch(val path: String) : HintState()
}

private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
    if (lhs.isEmpty()) { return rhs.length }
    if (rhs.isEmpty()) { return lhs.length }
    val lhsLength = lhs.length + 1
    val rhsLength = rhs.length + 1
    var cost = Array(lhsLength) { it }
    val newCost = Array(lhsLength) { 0 }
    for (i in 1 until rhsLength) {
        newCost[0] = i
        for (j in 1 until lhsLength) {
            val match = if (lhs[j - 1].equals(rhs[i - 1], ignoreCase = true)) 0 else 1
            val costReplace = cost[j - 1] + match
            val costInsert = cost[j] + 1
            val costDelete = newCost[j - 1] + 1
            newCost[j] = min(min(costInsert, costDelete), costReplace)
        }
        cost = newCost.clone()
    }
    return cost[lhsLength - 1]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTargetFolderDialog(
    folderSearchState: FolderSearchState,
    addFolderFocusTarget: AddFolderFocusTarget,
    addFavoriteToTargetByDefault: Boolean,
    hintOnExistingFolderName: Boolean,
    currentItemPath: String?,
    targetFavorites: Set<String>,
    onDismissRequest: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onPathSelected: (String) -> Unit,
    onSearchFocusChanged: (Boolean) -> Unit,
    onResetFolderSelection: () -> Unit,
    onConfirm: (newFolderName: String, addToFavorites: Boolean, alsoMove: Boolean) -> Unit
) {
    var newFolderName by remember { mutableStateOf("") }
    var addToFavorites by remember { mutableStateOf(false) }
    val searchPathFocusRequester = remember { FocusRequester() }
    val newFolderNameFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val warningColor = if (LocalAppTheme.current.isDark) {
        Color(0xFFFFB86F)
    } else {
        Color(0xFFC07600)
    }

    LaunchedEffect(addFavoriteToTargetByDefault) {
        addToFavorites = addFavoriteToTargetByDefault
    }

    LaunchedEffect(addFolderFocusTarget) {
        delay(50)
        when (addFolderFocusTarget) {
            AddFolderFocusTarget.FOLDER_NAME -> {
                focusManager.clearFocus()
                newFolderNameFocusRequester.requestFocus()
            }
            AddFolderFocusTarget.SEARCH_PATH -> {
                focusManager.clearFocus()
                searchPathFocusRequester.requestFocus()
            }
            AddFolderFocusTarget.NONE -> {
                focusManager.clearFocus()
            }
        }
    }

    val hintState = remember(newFolderName, folderSearchState.browsePath, hintOnExistingFolderName, folderSearchState.allFolders) {
        if (hintOnExistingFolderName && newFolderName.isNotBlank() && folderSearchState.browsePath != null) {
            val parentPath = folderSearchState.browsePath
            val newFullPath = File(parentPath, newFolderName).absolutePath
            var exactMatchPath: String? = null
            var similarMatchPath: String? = null
            var minDistance = Int.MAX_VALUE

            val allFolderPaths = folderSearchState.allFolders.map { it.first }
            for (path in allFolderPaths) {
                if (path.equals(newFullPath, ignoreCase = true)) {
                    exactMatchPath = path
                    break
                }
                val existingFile = File(path)
                if (existingFile.parent.equals(parentPath, ignoreCase = true)) {
                    val distance = levenshtein(newFolderName, existingFile.name)
                    val nameLength = newFolderName.length
                    val isSimilar = when {
                        nameLength in 1..3 -> distance == 1
                        nameLength > 3 -> distance > 0 && distance < (nameLength * 0.4)
                        else -> false
                    }
                    if (isSimilar && distance < minDistance) {
                        minDistance = distance
                        similarMatchPath = path
                    }
                }
            }
            when {
                exactMatchPath != null -> HintState.ExactMatch(exactMatchPath)
                similarMatchPath != null -> HintState.SimilarMatch(similarMatchPath)
                else -> HintState.None
            }
        } else {
            HintState.None
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Add Target Folder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Search for an existing folder or create a new one",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // ── Search field ──
            OutlinedTextField(
                value = folderSearchState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search folders...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                leadingIcon = {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchPathFocusRequester)
                    .onFocusChanged { focusState ->
                        onSearchFocusChanged(focusState.isFocused)
                    }
            )

            Spacer(Modifier.height(8.dp))

            // ── Folder results ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                when {
                    folderSearchState.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    folderSearchState.displayedResults.isNotEmpty() -> {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            folderSearchState.displayedResults.forEach { path ->
                                Surface(
                                    onClick = {
                                        onPathSelected(path)
                                        newFolderNameFocusRequester.requestFocus()
                                    },
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .padding(end = 12.dp)
                                                .size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = getFormattedPath(path),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                    folderSearchState.searchQuery.isNotBlank() && !folderSearchState.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No folders found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Selected path chip ──
            if (folderSearchState.browsePath != null && folderSearchState.searchQuery.isBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    onClick = onResetFolderSelection,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = ".../${folderSearchState.browsePath.takeLast(35)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Change",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── New folder name ──
            OutlinedTextField(
                value = newFolderName,
                onValueChange = { newFolderName = it },
                label = { Text("New folder name (optional)") },
                placeholder = { Text("Leave blank to use selected folder") },
                singleLine = true,
                supportingText = {
                    when (hintState) {
                        is HintState.ExactMatch -> {
                            val friendlyPath = ".../${File(hintState.path).parentFile?.name}/${File(hintState.path).name}"
                            Text("A folder with this name already exists: $friendlyPath")
                        }
                        is HintState.SimilarMatch -> {
                            val friendlyPath = ".../${File(hintState.path).parentFile?.name}/${File(hintState.path).name}"
                            Text("A folder with a similar name exists: $friendlyPath")
                        }
                        HintState.None -> {}
                    }
                },
                colors = if (hintState != HintState.None) {
                    TextFieldDefaults.colors(
                        focusedIndicatorColor = warningColor,
                        unfocusedIndicatorColor = warningColor.copy(alpha = 0.7f),
                        focusedLabelColor = warningColor,
                        cursorColor = warningColor,
                        focusedSupportingTextColor = warningColor,
                        unfocusedSupportingTextColor = warningColor.copy(alpha = 0.7f),
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                    )
                } else {
                    TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(newFolderNameFocusRequester)
            )

            Spacer(Modifier.height(4.dp))

            // ── Add to favorites checkbox ──
            val isSelectedFolderFavorite = folderSearchState.browsePath in targetFavorites
            val isFavoritesRowEnabled = folderSearchState.browsePath != null && !isSelectedFolderFavorite

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = isFavoritesRowEnabled,
                        onClick = { addToFavorites = !addToFavorites }
                    )
                    .padding(vertical = 2.dp)
            ) {
                Checkbox(
                    checked = addToFavorites,
                    onCheckedChange = { addToFavorites = it },
                    enabled = isFavoritesRowEnabled
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Keep as favorite folder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isFavoritesRowEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Action buttons ──
            val isNewNameEntered = newFolderName.isNotBlank()
            val isLocationValid = folderSearchState.browsePath != null || (isNewNameEntered && folderSearchState.searchQuery.isNotBlank())

            val isSameFolderAsCurrent = remember(folderSearchState.browsePath, currentItemPath) {
                val currentPath = currentItemPath ?: return@remember false
                val parentDirectory = try { File(currentPath).parent } catch (e: Exception) { null }
                parentDirectory != null && parentDirectory == folderSearchState.browsePath
            }

            val primaryButtonText = when {
                isNewNameEntered -> "Create"
                else -> "Import"
            }
            val moveButtonText = when {
                isNewNameEntered -> "Create & Move"
                else -> "Import & Move"
            }

            val isMoveActionInvalid = !isNewNameEntered && isSameFolderAsCurrent

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth > 380.dp
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        Button(
                            onClick = { onConfirm(newFolderName, addToFavorites, false) },
                            enabled = isLocationValid
                        ) {
                            Text(primaryButtonText)
                        }
                        FilledTonalButton(
                            onClick = { onConfirm(newFolderName, addToFavorites, true) },
                            enabled = isLocationValid && !isMoveActionInvalid
                        ) {
                            Text(moveButtonText, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onConfirm(newFolderName, addToFavorites, false) },
                            enabled = isLocationValid,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(primaryButtonText)
                        }
                        FilledTonalButton(
                            onClick = { onConfirm(newFolderName, addToFavorites, true) },
                            enabled = isLocationValid && !isMoveActionInvalid,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(moveButtonText, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

private fun getFormattedPath(fullPath: String): String {
    return try {
        val file = File(fullPath)
        val parent = file.parentFile
        if (parent != null) {
            "${parent.name}/${file.name}"
        } else {
            file.name
        }
    } catch (e: Exception) {
        fullPath.takeLast(40)
    }
}
