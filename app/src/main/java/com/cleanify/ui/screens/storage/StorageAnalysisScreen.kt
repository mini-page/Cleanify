package com.cleanify.ui.screens.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.text.format.Formatter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.cleanify.ui.components.BackNavigationIcon
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
    val isScanning: Boolean = false,
    val scannedFiles: Int = 0
)

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
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(StorageAnalysisState()) }
    var selectedFile by remember { mutableStateOf<LargeFile?>(null) }
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
                var scanned = 0
                val maxScanned = 50000
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

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
                                largeFilesList.add(LargeFile(child, size))
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
                    largeFiles = largeFilesList.take(10),
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
            TopAppBar(
                title = { Text("Storage Analysis") },
                navigationIcon = { BackNavigationIcon(onClick = onNavigateUp, contentDescription = "Back") },
                actions = {
                    IconButton(onClick = { refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isScanning) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Scanning storage...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            "Largest Files",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${state.scannedFiles} files scanned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (state.largeFiles.isEmpty()) {
                    item {
                        Text(
                            "No files found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(state.largeFiles, key = { it.file.absolutePath }) { largeFile ->
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

    selectedFile?.let { file ->
        FileInfoDialog(
            file = file,
            context = context,
            onDismiss = { selectedFile = null }
        )
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
private fun FileInfoDialog(
    file: LargeFile,
    context: Context,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault()) }
    val sizeFormat = DecimalFormat("#.##")
    val kib: Long = 1024
    val mib: Long = 1048576

    val sizeStr = when {
        file.size > mib -> "${sizeFormat.format(file.size.toDouble() / mib)} MB"
        file.size > kib -> "${sizeFormat.format(file.size.toDouble() / kib)} KB"
        else -> "${file.size} B"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
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

                Text(
                    "This file is on your device's storage. Use the Swiper or Empty Cleaner tools to manage it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
