package com.cleanify.ui.screens.swiper.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cleanify.data.model.MediaItem
import com.cleanify.util.Formatters
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer as PdfBoxRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun DocumentPreviewCard(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier
) {
    val isPdf = mediaItem.mimeType == "application/pdf" || mediaItem.extension == "pdf"
    val isText = mediaItem.mimeType.startsWith("text/") || mediaItem.extension in setOf("txt", "rtf")
    when {
        isPdf -> PdfPreview(mediaItem = mediaItem, modifier = modifier)
        isText -> TextPreview(mediaItem = mediaItem, modifier = modifier)
        else -> FileInfoCard(mediaItem = mediaItem, modifier = modifier)
    }
}

@Composable
internal fun PdfPreview(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mediaItem.id) {
        pages.forEach { it.recycle() }
        pages = emptyList()
        failed = false
        errorMessage = null
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                PDFBoxResourceLoader.init(context)
                val file = mediaItem.file
                PDDocument.load(file).use { document ->
                    val renderer = PdfBoxRenderer(document)
                    val dpi = 200f
                    val result = mutableListOf<Bitmap>()
                    for (i in 0 until document.numberOfPages) {
                        val bitmap = renderer.renderImage(i, dpi)
                        result.add(bitmap)
                    }
                    if (result.isNotEmpty()) pages = result
                }
            } catch (pdfBoxError: Exception) {
                // PDFBox failed — fall back to system PdfRenderer
                try {
                    var pfd: ParcelFileDescriptor? = null
                    val uri = mediaItem.uri
                    if (uri.scheme == "content") {
                        try { pfd = context.contentResolver.openFileDescriptor(uri, "r") } catch (_: Exception) { }
                    }
                    if (pfd == null && uri.scheme == "file") {
                        try { pfd = ParcelFileDescriptor.open(mediaItem.file, ParcelFileDescriptor.MODE_READ_ONLY) } catch (_: Exception) { }
                    }
                    if (pfd == null && uri.scheme == "file") {
                        try { pfd = context.contentResolver.openFileDescriptor(uri, "r") } catch (_: Exception) { }
                    }
                    if (pfd != null) {
                        pfd.use { fd ->
                            PdfRenderer(fd).use { renderer ->
                                val result = mutableListOf<Bitmap>()
                                for (i in 0 until renderer.pageCount) {
                                    val page = renderer.openPage(i)
                                    val bitmap = Bitmap.createBitmap(
                                        page.width.coerceAtLeast(1), page.height.coerceAtLeast(1),
                                        Bitmap.Config.ARGB_8888
                                    )
                                    page.render(bitmap, null, null, 0)
                                    page.close()
                                    result.add(bitmap)
                                }
                                if (result.isNotEmpty()) pages = result
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
            if (pages.isEmpty()) {
                failed = true
                errorMessage = "Could not render PDF"
            }
        }
        isLoading = false
    }

    DisposableEffect(mediaItem.id) {
        onDispose { pages.forEach { it.recycle() } }
    }

    Box(modifier = modifier.background(Color(0xFF1A1A2E))) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.6f))
                }
            }
            failed -> FileInfoCard(mediaItem = mediaItem, modifier = Modifier.fillMaxSize())
            else -> {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                ) {
                    pages.forEachIndexed { index, bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "${mediaItem.displayName} page ${index + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = if (index == 0) 0.dp else 2.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
internal fun TextPreview(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier
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
                        val maxChars = 100_000
                        val sb = StringBuilder(maxChars)
                        val buf = CharArray(4096)
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
        }
        isLoading = false
    }

    Box(modifier = modifier.background(Color(0xFF1A1A2E))) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.6f))
                }
            }
            content != null -> {
                val scrollState = rememberScrollState()
                SelectionContainer {
                    Text(
                        text = content!!,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            else -> FileInfoCard(mediaItem = mediaItem, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
internal fun FileInfoCard(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier
) {
    val icon = when {
        mediaItem.mimeType.contains("pdf") -> Icons.Default.PictureAsPdf
        mediaItem.mimeType.contains("text") || mediaItem.extension in setOf("txt", "rtf") -> Icons.Default.Description
        mediaItem.mimeType.contains("spreadsheet") || mediaItem.extension in setOf("xls", "xlsx") -> Icons.Default.TableChart
        mediaItem.mimeType.contains("presentation") || mediaItem.extension in setOf("ppt", "pptx") -> Icons.Default.Slideshow
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

    Box(
        modifier = modifier.background(Color(0xFF1A1A2E)),
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
                tint = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = mediaItem.displayName,
                style = MaterialTheme.typography.titleMedium,
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
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}
