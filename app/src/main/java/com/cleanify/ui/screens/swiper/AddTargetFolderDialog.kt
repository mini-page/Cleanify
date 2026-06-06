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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.cleanify.data.repository.AddFolderFocusTarget
import com.cleanify.ui.components.AppDialog
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

    val warningColor = if (LocalAppTheme.current.isDark) {
        Color(0xFFFFB86F) // Light Amber for Dark/AMOLED Theme
    } else {
        Color(0xFFC07600) // Dark Amber for Light Theme
    }

    // Initialize the "Add to Favorites" checkbox from settings
    LaunchedEffect(addFavoriteToTargetByDefault) {
        addToFavorites = addFavoriteToTargetByDefault
    }

    // Handle focus based on the addFolderFocusTarget
    LaunchedEffect(addFolderFocusTarget) {
        // Workaround for a focus timing issue introduced in a recent Compose library update.
        // The tap event that triggers this dialog now clears system focus. This delay allows the
        // focus system to stabilize before we request focus, preventing the request from being ignored.
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
                // Check for exact match first
                if (path.equals(newFullPath, ignoreCase = true)) {
                    exactMatchPath = path
                    break // Exact match is highest priority, no need to check further
                }

                // Check for similar names only within the same parent directory
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

    AppDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .padding(24.dp)
        ) {
            Text("Add Target Folder", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = folderSearchState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Search or Enter Path") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchPathFocusRequester)
                        .onFocusChanged { focusState ->
                            onSearchFocusChanged(focusState.isFocused)
                        }
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
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
                                CircularProgressIndicator()
                            }
                        }
                        folderSearchState.displayedResults.isNotEmpty() -> {
                            Box(modifier = Modifier.heightIn(max = 200.dp)) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    folderSearchState.displayedResults.forEach { path ->
                                        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        onPathSelected(path)
                                                        newFolderNameFocusRequester.requestFocus()
                                                    }
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                                Text(
                                                    text = getFormattedPath(path),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1
                                                )
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        }
                                    }
                                }
                            }
                        }
                        folderSearchState.searchQuery.isNotBlank() && !folderSearchState.isLoading -> {
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
                    if (folderSearchState.browsePath != null && folderSearchState.searchQuery.isBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Text(
                            text = "Selected: .../${folderSearchState.browsePath.takeLast(35)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .fillMaxWidth()
                                .clickable { onResetFolderSelection() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("New Folder Name (optional)") },
                    placeholder = { Text("Leave blank to import folder") },
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
                            HintState.None -> { /* No text */ }
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
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = addToFavorites,
                        onCheckedChange = { addToFavorites = it },
                        enabled = isFavoritesRowEnabled
                    )
                    Text("Add to Target Favorites", color = if (isFavoritesRowEnabled) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isNewNameEntered = newFolderName.isNotBlank()
                val isLocationValid = folderSearchState.browsePath != null || (isNewNameEntered && folderSearchState.searchQuery.isNotBlank())

                // Logic to check if the selected folder is the same as the current item's parent
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

                // A move is only invalid if a new name is NOT entered AND it's the same folder.
                val isMoveActionInvalid = !isNewNameEntered && isSameFolderAsCurrent

                TextButton(onClick = onDismissRequest) { Text("Cancel") }
                Button(
                    onClick = { onConfirm(newFolderName, addToFavorites, false) },
                    enabled = isLocationValid
                ) { Text(primaryButtonText) }
                Button(
                    onClick = { onConfirm(newFolderName, addToFavorites, true) },
                    enabled = isLocationValid && !isMoveActionInvalid
                ) {
                    Text(moveButtonText, textAlign = TextAlign.Center)
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
        // Fallback for weird paths
        fullPath.takeLast(40)
    }
}
