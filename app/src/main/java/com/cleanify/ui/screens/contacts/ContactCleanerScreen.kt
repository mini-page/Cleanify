package com.cleanify.ui.screens.contacts

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cleanify.ui.components.AppDialog
import com.cleanify.ui.components.BackNavigationIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactCleanerScreen(
    onNavigateUp: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: ContactCleanerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val activity = context as? Activity

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val allGranted = granted.values.all { it }
        viewModel.checkPermissions()
        if (allGranted) {
            viewModel.loadContacts()
        } else {
            val hasPermanentlyDenied = granted.any { (permission, _) ->
                activity?.shouldShowRequestPermissionRationale(permission) != true
            }
            viewModel.setPermanentlyDenied(hasPermanentlyDenied)
        }
    }

    LaunchedEffect(Unit) {
        val hasPerms = viewModel.checkPermissions()
        if (hasPerms) {
            viewModel.loadContacts()
        } else {
            val hasRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            if (hasRead != PackageManager.PERMISSION_GRANTED &&
                activity?.shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) != true
            ) {
                viewModel.setPermanentlyDenied(true)
            }
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    if (uiState.showDeleteConfirm) {
        AppDialog(
            onDismissRequest = viewModel::dismissDeleteConfirm,
            title = { Text("Delete contacts?") },
            text = { Text("This will permanently delete ${uiState.selectedIds.size} selected contact(s). This action cannot be undone.") }
        ) {
            TextButton(onClick = viewModel::dismissDeleteConfirm) { Text("Cancel") }
            Button(onClick = viewModel::confirmDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Delete")
            }
        }
    }

    if (uiState.showMergeConfirm != null) {
        val groupIdx = uiState.showMergeConfirm!!
        val group = uiState.duplicateGroups.getOrNull(groupIdx)
        AppDialog(
            onDismissRequest = viewModel::dismissMergeConfirm,
            title = { Text("Merge duplicates?") },
            text = {
                Text(
                    if (group != null) "Merge ${group.contacts.size} contacts into one? The first contact will be kept as the primary."
                    else "Merge this duplicate group?"
                )
            }
        ) {
            TextButton(onClick = viewModel::dismissMergeConfirm) { Text("Cancel") }
            Button(onClick = viewModel::confirmMerge) { Text("Merge") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Cleaner") },
                navigationIcon = { BackNavigationIcon(onClick = onNavigateUp) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    if (uiState.selectedIds.isNotEmpty()) {
                        TextButton(onClick = viewModel::selectAll) { Text("All") }
                        TextButton(onClick = viewModel::deselectAll) { Text("None") }
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.selectedIds.isNotEmpty()) {
                BottomAppBar(tonalElevation = 3.dp) {
                    Text(
                        text = "${uiState.selectedIds.size} selected",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.deleteSelected() },
                        enabled = !uiState.isOperating
                    ) {
                        if (uiState.isOperating) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!uiState.hasPermissions) {
                PermissionPrompt(
                    permanentlyDenied = uiState.permanentlyDenied,
                    onRequest = {
                        requestPermissionLauncher.launch(
                            arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
                        )
                    },
                    onOpenSettings = viewModel::openAppSettings
                )
            } else if (uiState.isLoading) {
                SkeletonLoading()
            } else {
                CategoryTabs(
                    selectedTab = uiState.currentTab,
                    contactCount = uiState.contacts.size,
                    duplicateCount = uiState.duplicateGroups.size,
                    issueCount = uiState.issues.size,
                    onTabSelected = { viewModel.setTab(it) }
                )

                PullToRefreshBox(
                    isRefreshing = uiState.isLoading,
                    onRefresh = viewModel::loadContacts,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (uiState.currentTab) {
                        0 -> AllContactsTab(
                            contacts = uiState.contacts,
                            selectedIds = uiState.selectedIds,
                            searchQuery = uiState.searchQuery,
                            isOperating = uiState.isOperating,
                            onToggle = { viewModel.toggleSelection(it) },
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onRefresh = viewModel::loadContacts
                        )
                        1 -> DuplicatesTab(
                            duplicateGroups = uiState.duplicateGroups,
                            selectedIds = uiState.selectedIds,
                            isOperating = uiState.isOperating,
                            isMergeAllOperating = uiState.isMergeAllOperating,
                            onSelectAll = { viewModel.selectAllInGroup(it) },
                            onMerge = { viewModel.mergeGroup(it) },
                            onMergeAll = viewModel::mergeAllDuplicates
                        )
                        2 -> IssuesTab(
                            contacts = uiState.issues,
                            selectedIds = uiState.selectedIds,
                            isOperating = uiState.isOperating,
                            onToggle = { viewModel.toggleSelection(it) },
                            onDelete = viewModel::deleteSingleContact,
                            onRefresh = viewModel::loadContacts
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionPrompt(
    permanentlyDenied: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(
                imageVector = Icons.Default.Contacts,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Contacts access required",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (permanentlyDenied) {
                    "Permission was permanently denied. Please enable it in Settings."
                } else {
                    "Allow access to read and manage your contacts"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            if (permanentlyDenied) {
                OutlinedButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Open Settings", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open Settings")
                }
            } else {
                Button(onClick = onRequest) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
private fun SkeletonLoading() {
    LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
        items(8) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(40.dp)
                    ) {}
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(0.6f).height(14.dp),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {}
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(0.4f).height(10.dp),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {}
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryTabs(
    selectedTab: Int,
    contactCount: Int,
    duplicateCount: Int,
    issueCount: Int,
    onTabSelected: (Int) -> Unit
) {
    PrimaryTabRow(selectedTabIndex = selectedTab) {
        Tab(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            text = { Text("All ($contactCount)") }
        )
        Tab(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            text = { Text("Duplicates ($duplicateCount)") }
        )
        Tab(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            text = { Text("Issues ($issueCount)") }
        )
    }
}

@Composable
private fun AllContactsTab(
    contacts: List<ContactItem>,
    selectedIds: Set<Long>,
    searchQuery: String,
    isOperating: Boolean,
    onToggle: (Long) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search by name or phone...") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )

        val filteredContacts by remember(contacts, searchQuery) {
            derivedStateOf {
                val q = searchQuery.trim().lowercase()
                if (q.isBlank()) contacts
                else contacts.filter { c ->
                    c.name.lowercase().contains(q) ||
                    c.phoneNumbers.any { it.replace(Regex("[^\\d]"), "").contains(q) }
                }
            }
        }

        if (filteredContacts.isEmpty()) {
            EmptyState(
                message = if (searchQuery.isNotBlank()) "No contacts match your search" else "No contacts found",
                onRefresh = onRefresh
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                items(filteredContacts, key = { it.id }) { contact ->
                    ContactRow(contact, contact.id in selectedIds, isOperating, onToggle)
                }
            }
        }
    }
}

@Composable
private fun DuplicatesTab(
    duplicateGroups: List<DuplicateGroup>,
    selectedIds: Set<Long>,
    isOperating: Boolean,
    isMergeAllOperating: Boolean,
    onSelectAll: (Int) -> Unit,
    onMerge: (Int) -> Unit,
    onMergeAll: () -> Unit
) {
    Column {
        if (duplicateGroups.isNotEmpty()) {
            Button(
                onClick = onMergeAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                enabled = !isOperating && !isMergeAllOperating
            ) {
                if (isMergeAllOperating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Merge All (${duplicateGroups.size} groups)")
            }
        }

        if (duplicateGroups.isEmpty()) {
            EmptyState("No duplicate contacts found")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(duplicateGroups) { index, group ->
                    DuplicateGroupCard(
                        group = group,
                        groupIndex = index,
                        selectedIds = selectedIds,
                        isOperating = isOperating,
                        onSelectAll = { onSelectAll(index) },
                        onMerge = { onMerge(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IssuesTab(
    contacts: List<ContactItem>,
    selectedIds: Set<Long>,
    isOperating: Boolean,
    onToggle: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onRefresh: () -> Unit
) {
    if (contacts.isEmpty()) {
        EmptyState("No issues found", onRefresh)
    } else {
        val noName = contacts.filter { it.name.isBlank() }
        val noPhone = contacts.filter { it.name.isNotBlank() && it.phoneNumbers.isEmpty() }

        LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
            if (noName.isNotEmpty()) {
                item {
                    SectionHeader("No name (${noName.size})")
                }
                items(noName, key = { it.id }) { contact ->
                    IssueCard(contact, selectedIds, isOperating, onToggle, onDelete)
                }
            }
            if (noPhone.isNotEmpty()) {
                item {
                    SectionHeader("No phone number (${noPhone.size})")
                }
                items(noPhone, key = { it.id }) { contact ->
                    IssueCard(contact, selectedIds, isOperating, onToggle, onDelete)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun IssueCard(
    contact: ContactItem,
    selectedIds: Set<Long>,
    isOperating: Boolean,
    onToggle: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val issueLabel = when {
        contact.name.isBlank() -> "No name"
        contact.phoneNumbers.isEmpty() -> "No phone number"
        else -> "Unknown issue"
    }

    if (showDeleteConfirm) {
        AppDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete contact?") },
            text = { Text("This will permanently delete \"${contact.name.ifBlank { "(unnamed)" }}\". This action cannot be undone.") }
        ) {
            TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            Button(onClick = { showDeleteConfirm = false; onDelete(contact.id) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Delete")
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = contact.id in selectedIds,
                onCheckedChange = { onToggle(contact.id) },
                enabled = !isOperating
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name.ifBlank { "(unnamed)" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = issueLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                if (contact.phoneNumbers.isNotEmpty()) {
                    Text(
                        text = contact.phoneNumbers.first(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showDeleteConfirm = true },
                enabled = !isOperating
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete contact",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    message: String,
    onRefresh: (() -> Unit)? = null
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onRefresh != null) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Refresh")
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: ContactItem,
    isSelected: Boolean,
    isOperating: Boolean = false,
    onToggle: (Long) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle(contact.id) },
                enabled = !isOperating
            )
            Spacer(Modifier.width(8.dp))
            ContactPhoto(contact.photoUri, contact.name)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (contact.phoneNumbers.isNotEmpty()) {
                    Text(
                        text = contact.phoneNumbers.first(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (contact.starred) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Starred",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactPhoto(photoUri: String?, name: String) {
    if (photoUri != null) {
        AsyncImage(
            model = photoUri,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val initial = name.firstOrNull()?.uppercase() ?: "?"
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    groupIndex: Int,
    selectedIds: Set<Long>,
    isOperating: Boolean,
    onSelectAll: () -> Unit,
    onMerge: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Duplicate group: ${group.contacts.size} contacts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = group.matchReason,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(Modifier.height(8.dp))
            group.contacts.forEach { contact ->
                ContactRow(contact, contact.id in selectedIds, isOperating) {}
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSelectAll,
                    modifier = Modifier.weight(1f),
                    enabled = !isOperating
                ) {
                    Text("Select All")
                }
                Button(
                    onClick = onMerge,
                    modifier = Modifier.weight(1f),
                    enabled = !isOperating
                ) {
                    if (isOperating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("Merge")
                }
            }
        }
    }
}
