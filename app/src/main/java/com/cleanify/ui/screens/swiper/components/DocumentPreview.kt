package com.cleanify.ui.screens.swiper.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanify.R
import com.cleanify.data.model.MediaItem
import com.cleanify.util.DocumentParsers
import com.cleanify.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun DocumentPreviewCard(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    onOpenInApp: (() -> Unit)? = null
) {
    when {
        mediaItem.isPdf -> PdfPreview(mediaItem = mediaItem, modifier = modifier, onOpenInApp = onOpenInApp)
        mediaItem.isDocx -> DocxPreview(mediaItem = mediaItem, modifier = modifier, onOpenInApp = onOpenInApp)
        mediaItem.isPptx -> PptxPreview(mediaItem = mediaItem, modifier = modifier, onOpenInApp = onOpenInApp)
        mediaItem.isXlsx -> XlsxPreview(mediaItem = mediaItem, modifier = modifier, onOpenInApp = onOpenInApp)
        mediaItem.isArchive -> ArchivePreview(mediaItem = mediaItem, modifier = modifier, onOpenInApp = onOpenInApp)
        mediaItem.isText -> TextPreview(mediaItem = mediaItem, modifier = modifier, onOpenInApp = onOpenInApp)
        else -> FileInfoCard(mediaItem = mediaItem, modifier = modifier, onOpenInApp = onOpenInApp)
    }
}

@Composable
internal fun PdfPreview(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    onOpenInApp: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(mediaItem.id) {
        pageBitmap?.recycle()
        pageBitmap = null
        failed = false
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val pfd = if (mediaItem.uri.scheme == "file") {
                    val f = File(mediaItem.uri.path ?: mediaItem.filePath)
                    if (f.exists()) ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
                    else context.contentResolver.openFileDescriptor(mediaItem.uri, "r")
                } else {
                    context.contentResolver.openFileDescriptor(mediaItem.uri, "r")
                }

                if (pfd != null) {
                    pfd.use { fd ->
                        PdfRenderer(fd).use { renderer ->
                            val total = renderer.pageCount
                            pageCount = total
                            if (total > 0) {
                                val page = renderer.openPage(0)
                                try {
                                    val metrics = context.resources.displayMetrics
                                    val targetWidth = (metrics.widthPixels * 0.9f).toInt().coerceAtLeast(400)
                                    val scale = targetWidth.toFloat() / page.width.toFloat()
                                    val targetHeight = (page.height.toFloat() * scale).toInt().coerceAtLeast(400)

                                    val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                                    val canvas = Canvas(bmp)
                                    canvas.drawColor(android.graphics.Color.WHITE)

                                    val matrix = Matrix()
                                    matrix.postScale(scale, scale)
                                    page.render(bmp, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    pageBitmap = bmp
                                } finally {
                                    page.close()
                                }
                            }
                        }
                    }
                } else {
                    failed = true
                }
            } catch (_: Exception) {
                failed = true
            }
            isLoading = false
        }
    }

    DisposableEffect(mediaItem.id) {
        onDispose {
            try {
                pageBitmap?.recycle()
            } catch (_: Exception) {}
        }
    }

    Box(modifier = modifier.background(Color(0xFF1A1A2E))) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.6f))
                }
            }
            failed || pageBitmap == null -> {
                FileInfoCard(mediaItem = mediaItem, modifier = Modifier.fillMaxSize(), onOpenInApp = onOpenInApp)
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = pageBitmap!!.asImageBitmap(),
                        contentDescription = mediaItem.displayName,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )

                    if (pageCount > 1) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "1 / $pageCount",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DocxPreview(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    onOpenInApp: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var paragraphs by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(mediaItem.id) {
        isLoading = true
        withContext(Dispatchers.IO) {
            paragraphs = DocumentParsers.extractDocxText(context, mediaItem.uri, maxParagraphs = 30)
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF1E1E2E))
    ) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.6f))
                }
            }
            paragraphs.isNotEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = mediaItem.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        paragraphs.take(8).forEach { paragraph ->
                            Text(
                                text = paragraph,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF374151),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            else -> FileInfoCard(mediaItem = mediaItem, modifier = Modifier.fillMaxSize(), onOpenInApp = onOpenInApp)
        }
    }
}

@Composable
internal fun PptxPreview(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    onOpenInApp: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var slides by remember { mutableStateOf<List<DocumentParsers.SlideInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(mediaItem.id) {
        isLoading = true
        withContext(Dispatchers.IO) {
            slides = DocumentParsers.extractPptxSlides(context, mediaItem.uri, maxSlides = 20)
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF1E1E2E))
    ) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.6f))
                }
            }
            slides.isNotEmpty() -> {
                val firstSlide = slides.first()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF7ED))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Slideshow,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFFEA580C)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Slide 1 of ${slides.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC2410C)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = firstSlide.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1F2937),
                            textAlign = TextAlign.Center,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            else -> FileInfoCard(mediaItem = mediaItem, modifier = Modifier.fillMaxSize(), onOpenInApp = onOpenInApp)
        }
    }
}

@Composable
internal fun XlsxPreview(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    onOpenInApp: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var sheetData by remember { mutableStateOf<DocumentParsers.SheetPreview?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(mediaItem.id) {
        isLoading = true
        withContext(Dispatchers.IO) {
            sheetData = DocumentParsers.extractXlsxSheet(context, mediaItem.uri, maxRows = 15, maxCols = 6)
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF1E1E2E))
    ) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.6f))
                }
            }
            sheetData != null && sheetData!!.rows.isNotEmpty() -> {
                val data = sheetData!!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = mediaItem.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        data.rows.take(6).forEachIndexed { rIndex, rowCells ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (rIndex == 0) Color(0xFFDCFCE7) else if (rIndex % 2 == 0) Color(0xFFF9FAFB) else Color.White)
                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                            ) {
                                rowCells.take(4).forEach { cell ->
                                    Text(
                                        text = cell,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF1F2937),
                                        fontWeight = if (rIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            else -> FileInfoCard(mediaItem = mediaItem, modifier = Modifier.fillMaxSize(), onOpenInApp = onOpenInApp)
        }
    }
}

@Composable
internal fun ArchivePreview(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    onOpenInApp: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var archiveInfo by remember { mutableStateOf<DocumentParsers.ArchiveInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(mediaItem.id) {
        isLoading = true
        withContext(Dispatchers.IO) {
            archiveInfo = DocumentParsers.inspectArchive(context, mediaItem.uri, maxEntries = 100)
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF1E1E2E)),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(color = Color.White.copy(alpha = 0.6f))
            }
            archiveInfo != null -> {
                val info = archiveInfo!!
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    if (info.isApk && info.apkIcon != null) {
                        Image(
                            bitmap = info.apkIcon.asImageBitmap(),
                            contentDescription = "APK Icon",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = info.apkAppName ?: mediaItem.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (info.apkVersionName != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "v${info.apkVersionName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = mediaItem.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${info.totalFiles} files • ${Formatters.fileSize(info.totalUncompressedBytes)} uncompressed",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }
            else -> FileInfoCard(mediaItem = mediaItem, modifier = Modifier.fillMaxSize(), onOpenInApp = onOpenInApp)
        }
    }
}

@Composable
internal fun TextPreview(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    onOpenInApp: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(mediaItem.id) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val stream = if (mediaItem.uri.scheme == "file") {
                    File(mediaItem.uri.path!!).inputStream()
                } else {
                    context.contentResolver.openInputStream(mediaItem.uri)
                }
                stream?.use { s ->
                    content = s.bufferedReader().use {
                        val maxChars = 20_000
                        val sb = StringBuilder(maxChars)
                        val buf = CharArray(2048)
                        var total = 0
                        var read: Int
                        while (it.read(buf).also { read = it } != -1 && total < maxChars) {
                            val toAppend = minOf(read, maxChars - total)
                            sb.append(buf, 0, toAppend)
                            total += toAppend
                        }
                        sb.toString()
                    }
                }
            } catch (_: Exception) {
                content = null
            }
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF1E1E2E))
    ) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.6f))
                }
            }
            content != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF12121A))
                        .padding(14.dp)
                ) {
                    Text(
                        text = content!!,
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFFECEFF4),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        maxLines = 20,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            else -> FileInfoCard(mediaItem = mediaItem, modifier = Modifier.fillMaxSize(), onOpenInApp = onOpenInApp)
        }
    }
}

@Composable
internal fun FileInfoCard(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    onOpenInApp: (() -> Unit)? = null
) {
    val icon = when {
        mediaItem.isPdf -> Icons.Default.PictureAsPdf
        mediaItem.isDocx || mediaItem.isText -> Icons.Default.Description
        mediaItem.isXlsx -> Icons.Default.TableChart
        mediaItem.isPptx -> Icons.Default.Slideshow
        mediaItem.isArchive -> Icons.Default.Archive
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

    Box(
        modifier = modifier
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Color.White.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = mediaItem.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = Formatters.fileSize(mediaItem.size),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = mediaItem.mimeType.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f)
            )
        }
    }
}
