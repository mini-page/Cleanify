/*
 * Cleanify - Storage Management & Quarry Engine Integration
 * Copyright (c) 2025 LoopOtto & Quarry App contributors (Apache 2.0 / GPLv3)
 */

package com.cleanify.ui.screens.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.text.format.Formatter
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderDelete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cleanify.domain.treemap.TreemapEngine
import com.cleanify.domain.treemap.TreemapNode
import com.cleanify.ui.components.BackNavigationIcon
import com.cleanify.ui.components.MediaPreviewDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LargeFile(
    val file: File,
    val size: Long
)

data class CategoryUsage(
    val name: String,
    val icon: ImageVector,
    val bytes: Long,
    val color: Color
)

data class VolumeUsage(
    val description: String,
    val isPrimary: Boolean,
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long
)

data class StorageAnalysisState(
    val volumeUsages: List<VolumeUsage> = emptyList(),
    val categories: List<CategoryUsage> = emptyList(),
    val otherBytes: Long = 0,
    val largeFiles: List<LargeFile> = emptyList(),
    val largeFilesTotalBytes: Long = 0L,
    val apkFiles: List<File> = emptyList(),
    val apkFilesTotalBytes: Long = 0L,
    val screenshotFiles: List<File> = emptyList(),
    val screenshotFilesTotalBytes: Long = 0L,
    val emptyFoldersCount: Int = 0,
    val isScanning: Boolean = false,
    val scannedFiles: Int = 0
) {
    val totalRecoverableBytes: Long
        get() = largeFilesTotalBytes + apkFilesTotalBytes + screenshotFilesTotalBytes
}

private val categoryExtensions = mapOf(
    "Images" to listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "heic", "heif"),
    "Videos" to listOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp"),
    "Music" to listOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus"),
    "Documents" to listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "rtf", "odt"),
    "APK / Apps" to listOf("apk", "apks", "apkm", "aab", "xapk")
)

private val categoryColors = listOf(
    Color(0xFF2196F3), // Images - Blue
    Color(0xFFE91E63), // Videos - Pink
    Color(0xFF9C27B0), // Music - Purple
    Color(0xFFFF9800), // Documents - Orange
    Color(0xFF4CAF50), // APK - Green
    Color(0xFF607D8B), // Downloads - Blue Grey
    Color(0xFF9E9E9E)  // Other - Grey
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageAnalysisScreen(
    onNavigateUp: () -> Unit = {},
    onNavigateToEmptyCleaner: () -> Unit = {},
    onNavigateToDuplicates: () -> Unit = {},
    onNavigateToRecycleBin: () -> Unit = {},
    onNavigateToSwiper: (List<String>) -> Unit = {},
    onNavigateToSettings: (String) -> Unit = {},
    embedded: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(StorageAnalysisState()) }
    var selectedFile by remember { mutableStateOf<LargeFile?>(null) }
    val pagerState = rememberPagerState(initialPage = 0) { 3 }
    var isBottomBarVisible by remember { mutableStateOf(true) }
    var showAppStorageSheet by remember { mutableStateOf(false) }
    var showLargeFilesSheet by remember { mutableStateOf(false) }
    var treemapRoot by remember { mutableStateOf<TreemapNode?>(null) }
    var isTreemapLoading by remember { mutableStateOf(false) }
    val treemapNavStack = remember { mutableStateListOf<TreemapNode>() }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Scrolling down (finger moves up) -> hide
                if (available.y < -10f) {
                    isBottomBarVisible = false
                } else if (available.y > 10f) {
                    // Scrolling up (finger moves down) -> show
                    isBottomBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        isBottomBarVisible = true
        if (pagerState.currentPage == 1 && treemapRoot == null && !isTreemapLoading) {
            isTreemapLoading = true
            val root = withContext(Dispatchers.IO) {
                val externalDir = Environment.getExternalStorageDirectory()
                TreemapEngine.buildTree(externalDir, maxDepth = 2)
            }
            treemapRoot = root
            isTreemapLoading = false
        }
    }

    val totalBytes = state.volumeUsages.sumOf { it.totalBytes }
    val usedBytes = state.volumeUsages.sumOf { it.usedBytes }
    val freeBytes = state.volumeUsages.sumOf { it.freeBytes }

    fun refresh() {
        state = state.copy(isScanning = true, scannedFiles = 0)
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                val volumes = mutableListOf<VolumeUsage>()

                for (sv: StorageVolume in storageManager.storageVolumes) {
                    val dir = sv.directory ?: continue
                    if (!dir.isDirectory || !dir.canRead()) continue
                    val stat = StatFs(dir.absolutePath)
                    val blockSize = stat.blockSizeLong
                    val total = blockSize * stat.blockCountLong
                    val free = blockSize * stat.availableBlocksLong
                    volumes.add(VolumeUsage(sv.getDescription(context), sv.isPrimary, total, total - free, free))
                }

                if (volumes.none { it.isPrimary }) {
                    val primary = Environment.getExternalStorageDirectory()
                    if (primary.isDirectory && primary.canRead()) {
                        val stat = StatFs(primary.absolutePath)
                        val blockSize = stat.blockSizeLong
                        val total = blockSize * stat.blockCountLong
                        val free = blockSize * stat.availableBlocksLong
                        volumes.add(VolumeUsage("Internal storage", true, total, total - free, free))
                    }
                }

                val categoryMap = mutableMapOf<String, Long>()
                categoryExtensions.keys.forEach { categoryMap[it] = 0L }
                var otherTotal = 0L
                var downloadBytes = 0L
                val largeFilesList = mutableListOf<LargeFile>()
                var largeFilesTotal = 0L
                val apkFilesList = mutableListOf<File>()
                var apkFilesTotal = 0L
                val screenshotFilesList = mutableListOf<File>()
                var screenshotFilesTotal = 0L
                var emptyFolders = 0
                var scanned = 0
                val maxScanned = 50000
                val largeFileThreshold = 50L * 1024L * 1024L // 50MB

                for (vol in volumes) {
                    val dir = if (vol.isPrimary) Environment.getExternalStorageDirectory()
                    else volumes.firstOrNull { it.description == vol.description }?.let {
                        storageManager.storageVolumes.firstOrNull { sv -> sv.getDescription(context) == vol.description }?.directory
                    } ?: continue
                    if (!dir.isDirectory) continue

                    val queue = ArrayDeque<File>()
                    queue.add(dir)
                    while (queue.isNotEmpty() && scanned < maxScanned) {
                        val d = queue.removeFirst()
                        val children = d.listFiles() ?: continue
                        if (children.isEmpty() && !d.name.startsWith(".") && d != dir) {
                            emptyFolders++
                        }
                        for (child in children) {
                            if (scanned >= maxScanned) break
                            scanned++
                            if (child.isDirectory) {
                                if (!child.name.startsWith(".") && !child.name.startsWith("Android/")) {
                                    queue.add(child)
                                }
                            } else if (child.isFile && child.length() > 0) {
                                val size = child.length()
                                val ext = child.extension.lowercase()
                                var categorized = false
                                for ((cat, exts) in categoryExtensions) {
                                    if (ext in exts) {
                                        categoryMap[cat] = (categoryMap[cat] ?: 0) + size
                                        categorized = true
                                        break
                                    }
                                }
                                if (!categorized) {
                                    if (child.parent?.contains("Download") == true) {
                                        downloadBytes += size
                                    } else {
                                        otherTotal += size
                                    }
                                }

                                // Old APK Check
                                if (ext in listOf("apk", "apks", "apkm", "xapk")) {
                                    apkFilesList.add(child)
                                    apkFilesTotal += size
                                }

                                // Screenshots Check
                                val pathLower = child.absolutePath.lowercase()
                                if ((pathLower.contains("screenshot") || child.parentFile?.name?.lowercase() == "screenshots") &&
                                    ext in listOf("jpg", "jpeg", "png", "webp")
                                ) {
                                    screenshotFilesList.add(child)
                                    screenshotFilesTotal += size
                                }

                                // Large Files Check (>50MB)
                                if (size >= largeFileThreshold) {
                                    largeFilesList.add(LargeFile(child, size))
                                    largeFilesTotal += size
                                }
                            }
                        }
                    }
                }

                largeFilesList.sortByDescending { it.size }

                val catList = mutableListOf<CategoryUsage>()
                var idx = 0
                for ((name, _) in categoryExtensions) {
                    val bytes = categoryMap[name] ?: 0
                    catList.add(CategoryUsage(name, getCategoryIcon(name), bytes, categoryColors[idx]))
                    idx++
                }
                catList.add(CategoryUsage("Downloads", Icons.Filled.Storage, downloadBytes, categoryColors[5]))
                if (otherTotal > 0) {
                    catList.add(CategoryUsage("Other", Icons.Outlined.Description, otherTotal, categoryColors[6]))
                }
                catList.sortByDescending { it.bytes }

                StorageAnalysisState(
                    volumeUsages = volumes,
                    categories = catList.filter { it.bytes > 0 },
                    otherBytes = otherTotal,
                    largeFiles = largeFilesList,
                    largeFilesTotalBytes = largeFilesTotal,
                    apkFiles = apkFilesList,
                    apkFilesTotalBytes = apkFilesTotal,
                    screenshotFiles = screenshotFilesList,
                    screenshotFilesTotalBytes = screenshotFilesTotal,
                    emptyFoldersCount = emptyFolders,
                    isScanning = false,
                    scannedFiles = scanned
                )
            }
            state = result
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            if (!embedded) {
                TopAppBar(
                    title = { Text("Storage") },
                    navigationIcon = { BackNavigationIcon(onClick = onNavigateUp, contentDescription = "Back") },
                    actions = {
                        IconButton(onClick = { refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { onNavigateToSettings("storage_management") }) {
                            Icon(Icons.Default.Settings, contentDescription = "Storage Settings")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(nestedScrollConnection)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        // TAB 0: QUARRY CLEANUP HUB
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                        // Live Scanning progress bar
                        if (state.isScanning) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                "Scanning storage candidates...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                "${state.scannedFiles} files indexed across storage",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Quarry Hero Overview Card
                        item {
                            QuarryHeroCard(
                                totalRecoverableBytes = state.totalRecoverableBytes,
                                largeFilesBytes = state.largeFilesTotalBytes,
                                apkBytes = state.apkFilesTotalBytes,
                                screenshotsBytes = state.screenshotFilesTotalBytes,
                                emptyFoldersCount = state.emptyFoldersCount,
                                scannedFiles = state.scannedFiles,
                                context = context
                            )
                        }

                        // Storage Candidates Section Header
                        item {
                            Column(modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)) {
                                Text(
                                    "Storage Candidates",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Safe and targeted modules to reclaim device space",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 1. Large Files Candidate Card
                        item {
                            StorageCandidateCard(
                                title = "Large Files (>50 MB)",
                                subtitle = "${state.largeFiles.size} large files (${Formatter.formatShortFileSize(context, state.largeFilesTotalBytes)})",
                                icon = Icons.Outlined.Description,
                                badgeText = if (state.largeFiles.isNotEmpty()) Formatter.formatShortFileSize(context, state.largeFilesTotalBytes) else null,
                                actionLabel = "Inspect",
                                onClick = { showLargeFilesSheet = true }
                            )
                        }

                        // 2. Old APK Installers Candidate Card -> Opens Swiper
                        item {
                            StorageCandidateCard(
                                title = "Old APK Installers",
                                subtitle = "${state.apkFiles.size} installer packages (${Formatter.formatShortFileSize(context, state.apkFilesTotalBytes)})",
                                icon = Icons.Default.Android,
                                badgeText = if (state.apkFiles.isNotEmpty()) "${state.apkFiles.size} APKs" else null,
                                actionLabel = "Swipe Review",
                                isHighlight = state.apkFiles.isNotEmpty(),
                                onClick = { onNavigateToSwiper(listOf("virtual:apks")) }
                            )
                        }

                        // 3. Screenshots Review Candidate Card -> Opens Swiper
                        item {
                            StorageCandidateCard(
                                title = "Screenshots Review",
                                subtitle = "${state.screenshotFiles.size} screenshots (${Formatter.formatShortFileSize(context, state.screenshotFilesTotalBytes)})",
                                icon = Icons.Outlined.Image,
                                badgeText = if (state.screenshotFiles.isNotEmpty()) Formatter.formatShortFileSize(context, state.screenshotFilesTotalBytes) else null,
                                actionLabel = "Swipe Clean",
                                onClick = { onNavigateToSwiper(listOf("virtual:screenshots")) }
                            )
                        }

                        // 4. Empty Directories Candidate Card -> Opens Empty Cleaner
                        item {
                            StorageCandidateCard(
                                title = "Empty Directory Cleanup",
                                subtitle = "${state.emptyFoldersCount} folders containing zero files",
                                icon = Icons.Outlined.FolderDelete,
                                badgeText = if (state.emptyFoldersCount > 0) "${state.emptyFoldersCount} empty" else null,
                                actionLabel = "Clean",
                                onClick = onNavigateToEmptyCleaner
                            )
                        }

                        // 5. Duplicate Files Arbiter Card -> Opens Duplicates Screen
                        item {
                            StorageCandidateCard(
                                title = "Duplicate Files Arbiter",
                                subtitle = "Rule-based duplicate resolver & content hash matcher",
                                icon = Icons.Outlined.ContentCopy,
                                badgeText = "Arbiter",
                                actionLabel = "Review",
                                onClick = onNavigateToDuplicates
                            )
                        }

                        // 6. Installed App Storage Card -> Opens App Storage BottomSheet
                        item {
                            StorageCandidateCard(
                                title = "Installed Apps & Cache",
                                subtitle = "Inspect app binaries, user storage, and cache footprint",
                                icon = Icons.Filled.Apps,
                                badgeText = "Inspector",
                                actionLabel = "Analyze",
                                onClick = { showAppStorageSheet = true }
                            )
                        }

                        // 7. Recycle Bin Staging Candidate Card -> Opens Recycle Bin
                        item {
                            StorageCandidateCard(
                                title = "Recycle Bin & Trash",
                                subtitle = "Review staged deletions or permanently purge files",
                                icon = Icons.Outlined.DeleteOutline,
                                badgeText = null,
                                actionLabel = "View Bin",
                                onClick = onNavigateToRecycleBin
                            )
                        }

                        // Storage Health Banner
                        item {
                            Spacer(Modifier.height(4.dp))
                            StorageHealthBanner(
                                totalBytes = totalBytes,
                                usedBytes = usedBytes,
                                freeBytes = freeBytes
                            )
                        }
                    }
                }

                1 -> {
                    // TAB 1: TREEMAP EXPLORER
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 76.dp)
                    ) {
                        if (isTreemapLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(Modifier.height(12.dp))
                                    Text("Computing squarified treemap hierarchy...", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        } else if (treemapRoot != null) {
                            val currentNode = treemapNavStack.lastOrNull() ?: treemapRoot!!
                            TreemapCanvas(
                                rootNode = currentNode,
                                onDrillDown = { child ->
                                    if (child.isDirectory && child.children.isNotEmpty()) {
                                        treemapNavStack.add(child)
                                    }
                                },
                                onNavigateUp = {
                                    if (treemapNavStack.isNotEmpty()) {
                                        treemapNavStack.removeAt(treemapNavStack.lastIndex)
                                    }
                                },
                                onFileClick = { fileNode ->
                                    val file = File(fileNode.path)
                                    if (file.exists()) {
                                        selectedFile = LargeFile(file = file, size = fileNode.size)
                                    }
                                },
                                canNavigateUp = treemapNavStack.isNotEmpty(),
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No storage data available for Treemap", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: STORAGE BREAKDOWN
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            StorageDonutChart(
                                totalBytes = totalBytes,
                                usedBytes = usedBytes,
                                freeBytes = freeBytes
                            )
                        }

                        item {
                            Text(
                                "Storage by Volume",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        items(state.volumeUsages, key = { it.description }) { volume ->
                            VolumeCard(volume = volume, context = context)
                        }

                        if (state.categories.isNotEmpty()) {
                            item {
                                Text(
                                    "Category Breakdown",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            items(state.categories, key = { it.name }) { category ->
                                CategoryBar(
                                    category = category,
                                    totalBytes = totalBytes,
                                    context = context
                                )
                            }
                        }

                        item {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Largest Files (>50 MB)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${state.largeFiles.size} files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (state.largeFiles.isEmpty()) {
                            item {
                                Text(
                                    "No large files found (>50 MB)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(state.largeFiles.take(15), key = { it.file.absolutePath }) { largeFile ->
                                LargeFileRow(
                                    largeFile = largeFile,
                                    context = context,
                                    onClick = { selectedFile = largeFile }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Auto-Hiding Bottom Nav Bar
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically { it * 2 } + fadeIn(),
                exit = slideOutVertically { it * 2 } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = listOf(
                            Triple("Cleanup", Icons.Outlined.CleaningServices, 0),
                            Triple("Treemap", Icons.Default.AccountTree, 1),
                            Triple("Breakdown", Icons.Default.Storage, 2)
                        )
                        tabs.forEach { (label, icon, index) ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        scope.launch { pagerState.animateScrollToPage(index) }
                                    }
                                    .padding(horizontal = if (isSelected) 14.dp else 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (isSelected) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    var previewFile by remember { mutableStateOf<File?>(null) }

    // Large Files Modal Sheet
    if (showLargeFilesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLargeFilesSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Large Files (>50 MB)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${state.largeFiles.size} items • ${Formatter.formatShortFileSize(context, state.largeFilesTotalBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showLargeFilesSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.largeFiles, key = { it.file.absolutePath }) { largeFile ->
                        LargeFileRow(
                            largeFile = largeFile,
                            context = context,
                            onClick = {
                                selectedFile = largeFile
                            }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    selectedFile?.let { file ->
        FileInfoDialog(
            file = file,
            context = context,
            onPreview = {
                previewFile = it
                selectedFile = null
            },
            onDismiss = { selectedFile = null },
            onDelete = { refresh() }
        )
    }

    previewFile?.let { file ->
        val ext = file.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: when (ext) {
            in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp") -> "image/$ext"
            in listOf("mp4", "mkv", "webm", "avi", "mov", "3gp") -> "video/$ext"
            in listOf("mp3", "wav", "flac", "ogg", "m4a", "aac", "opus") -> "audio/$ext"
            "pdf" -> "application/pdf"
            in listOf("txt", "log", "json", "xml", "csv", "md") -> "text/plain"
            else -> "*/*"
        }
        MediaPreviewDialog(
            uri = Uri.fromFile(file),
            displayName = file.name,
            fileSize = file.length(),
            dateModified = file.lastModified(),
            mimeType = mimeType,
            onDelete = {
                file.delete()
                previewFile = null
                refresh()
            },
            onDismiss = { previewFile = null }
        )
    }

    if (showAppStorageSheet) {
        AppStorageBottomSheet(
            onDismissRequest = { showAppStorageSheet = false }
        )
    }
}

/**
 * Quarry-styled Cleanup Hero Card displaying total recoverable bytes prominently
 */
@Composable
private fun QuarryHeroCard(
    totalRecoverableBytes: Long,
    largeFilesBytes: Long,
    apkBytes: Long,
    screenshotsBytes: Long,
    emptyFoldersCount: Int,
    scannedFiles: Int,
    context: Context,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CleaningServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "RECOVERABLE STORAGE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "$scannedFiles items indexed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = Formatter.formatShortFileSize(context, totalRecoverableBytes),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Safe & reviewable space identified across storage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            // Sub-metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    label = "Large Files",
                    value = Formatter.formatShortFileSize(context, largeFilesBytes),
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "Old APKs",
                    value = Formatter.formatShortFileSize(context, apkBytes),
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "Screenshots",
                    value = Formatter.formatShortFileSize(context, screenshotsBytes),
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "Empty Dirs",
                    value = "$emptyFoldersCount",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Quarry-styled Storage Candidate Action Card
 */
@Composable
private fun StorageCandidateCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String?,
    actionLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (badgeText != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = actionLabel,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StorageDonutChart(
    totalBytes: Long,
    usedBytes: Long,
    freeBytes: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val usedFraction = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f
    val usedPercent = (usedFraction * 100).toInt()
    val donutColor = when {
        usedFraction > 0.9 -> MaterialTheme.colorScheme.error
        usedFraction > 0.7 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Total Storage",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 36f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val arcTopLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    drawArc(
                        color = surfaceVariant,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        topLeft = arcTopLeft,
                        size = arcSize
                    )

                    val sweep = usedFraction * 360f
                    drawArc(
                        color = donutColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        topLeft = arcTopLeft,
                        size = arcSize
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$usedPercent%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = donutColor
                    )
                    Text(
                        "used",
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Used", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        Formatter.formatShortFileSize(context, usedBytes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = donutColor
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Free", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        Formatter.formatShortFileSize(context, freeBytes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        Formatter.formatShortFileSize(context, totalBytes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun VolumeCard(
    volume: VolumeUsage,
    context: Context
) {
    val fraction = if (volume.totalBytes > 0) volume.usedBytes.toFloat() / volume.totalBytes else 0f
    val color = when {
        fraction > 0.9 -> MaterialTheme.colorScheme.error
        fraction > 0.7 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (volume.isPrimary) Icons.Default.Devices else Icons.Default.SdCard,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = color
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(volume.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${Formatter.formatShortFileSize(context, volume.usedBytes)} used of ${Formatter.formatShortFileSize(context, volume.totalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryBar(
    category: CategoryUsage,
    totalBytes: Long,
    context: Context
) {
    val fraction = if (totalBytes > 0) category.bytes.toFloat() / totalBytes else 0f
    val percent = (fraction * 100).toInt()

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = category.color
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(category.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text("$percent%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = category.color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    Formatter.formatShortFileSize(context, category.bytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LargeFileRow(
    largeFile: LargeFile,
    context: Context,
    onClick: () -> Unit
) {
    val icon: ImageVector = when {
        largeFile.file.extension.lowercase() in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "heic", "heif") -> Icons.Outlined.Image
        largeFile.file.extension.lowercase() in listOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp") -> Icons.Outlined.VideoFile
        largeFile.file.extension.lowercase() in listOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus") -> Icons.Outlined.MusicNote
        else -> Icons.Outlined.Description
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = largeFile.file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = largeFile.file.parent ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = Formatter.formatShortFileSize(context, largeFile.size),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.Info,
                contentDescription = "File info",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StorageHealthBanner(
    totalBytes: Long,
    usedBytes: Long,
    freeBytes: Long
) {
    val usedFraction = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f
    val (statusTitle, statusDesc, bannerColor, icon) = when {
        usedFraction >= 0.90f -> listOf(
            "Storage Space Critical",
            "Over 90% full. System apps and performance may lag. Reclaim space now.",
            MaterialTheme.colorScheme.error,
            Icons.Outlined.Warning
        )
        usedFraction >= 0.75f -> listOf(
            "Storage Space Moderate",
            "Over 75% full. Consider cleaning duplicate photos and empty folders.",
            MaterialTheme.colorScheme.tertiary,
            Icons.Outlined.Warning
        )
        else -> listOf(
            "Storage Space Healthy",
            "Plenty of storage remaining. Keep your device organized with Cleanify tools.",
            MaterialTheme.colorScheme.primary,
            Icons.Outlined.CheckCircle
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = (bannerColor as Color).copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(bannerColor.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon as ImageVector,
                    contentDescription = null,
                    tint = bannerColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusTitle as String,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = bannerColor
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = statusDesc as String,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FileInfoDialog(
    file: LargeFile,
    context: Context,
    onPreview: (File) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault()) }
    val sizeFormat = DecimalFormat("#.##")
    val kib: Long = 1024
    val mib: Long = 1048576

    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val sizeStr = when {
        file.size > mib -> "${sizeFormat.format(file.size.toDouble() / mib)} MB"
        file.size > kib -> "${sizeFormat.format(file.size.toDouble() / kib)} KB"
        else -> "${file.size} B"
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete File?") },
            text = { Text("Are you sure you want to permanently delete \"${file.file.name}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        try {
                            if (file.file.delete()) {
                                Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                                onDelete()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Could not delete file", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("File Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(12.dp))

                FileDetailRow("Name", file.file.name)
                FileDetailRow("Size", sizeStr)
                FileDetailRow("Path", file.file.absolutePath)
                FileDetailRow("Last Modified", dateFormat.format(Date(file.file.lastModified())))
                FileDetailRow("Type", file.file.extension.uppercase())

                Spacer(Modifier.height(16.dp))

                // Primary Internal Preview Button
                Button(
                    onClick = { onPreview(file.file) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Preview File Internally")
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file.file
                                )
                                val mimeType = MimeTypeMap.getSingleton()
                                    .getMimeTypeFromExtension(file.file.extension.lowercase()) ?: "*/*"
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Open with"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Open with", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file.file
                                )
                                val mimeType = MimeTypeMap.getSingleton()
                                    .getMimeTypeFromExtension(file.file.extension.lowercase()) ?: "*/*"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = mimeType
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share file"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot share: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share", maxLines = 1)
                    }

                    Button(
                        onClick = { showDeleteConfirmation = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun FileDetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun getCategoryIcon(name: String): ImageVector = when (name) {
    "Images" -> Icons.Outlined.Image
    "Videos" -> Icons.Outlined.VideoFile
    "Music" -> Icons.Outlined.MusicNote
    "Documents" -> Icons.Outlined.Description
    "APK / Apps" -> Icons.Outlined.Description
    "Downloads" -> Icons.Filled.Storage
    else -> Icons.Outlined.Description
}
