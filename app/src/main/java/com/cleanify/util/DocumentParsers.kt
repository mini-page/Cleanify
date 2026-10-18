package com.cleanify.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object DocumentParsers {

    data class AudioMetadata(
        val title: String?,
        val artist: String?,
        val album: String?,
        val bitrate: String?,
        val durationMs: Long,
        val albumArt: Bitmap?
    )

    data class SlideInfo(
        val slideNumber: Int,
        val text: String
    )

    data class SheetPreview(
        val rows: List<List<String>>,
        val totalRows: Int,
        val totalCols: Int
    )

    data class ArchiveEntry(
        val name: String,
        val isDirectory: Boolean,
        val uncompressedSize: Long,
        val compressedSize: Long
    )

    data class ArchiveInfo(
        val totalFiles: Int,
        val totalDirectories: Int,
        val totalUncompressedBytes: Long,
        val totalCompressedBytes: Long,
        val entries: List<ArchiveEntry>,
        val isApk: Boolean = false,
        val apkAppName: String? = null,
        val apkPackageName: String? = null,
        val apkVersionName: String? = null,
        val apkIcon: Bitmap? = null
    )

    /**
     * Extracts audio metadata including title, artist, album, duration, bitrate, and embedded album art.
     */
    fun extractAudioMetadata(context: Context, uri: Uri): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        var pfd: ParcelFileDescriptor? = null
        try {
            pfd = openDescriptor(context, uri)
            if (pfd != null) {
                retriever.setDataSource(pfd.fileDescriptor)
            } else {
                retriever.setDataSource(context, uri)
            }

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L

            val formattedBitrate = bitrateStr?.toLongOrNull()?.let {
                val kbps = it / 1000
                "${kbps} kbps"
            }

            val pictureBytes = retriever.embeddedPicture
            val albumArt = if (pictureBytes != null && pictureBytes.isNotEmpty()) {
                try {
                    BitmapFactory.decodeByteArray(pictureBytes, 0, pictureBytes.size)
                } catch (_: Exception) {
                    null
                }
            } else null

            return AudioMetadata(
                title = title,
                artist = artist,
                album = album,
                bitrate = formattedBitrate,
                durationMs = duration,
                albumArt = albumArt
            )
        } catch (_: Exception) {
            return AudioMetadata(null, null, null, null, 0L, null)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Extracts paragraphs and body text from a DOCX file without external heavyweight dependencies.
     */
    fun extractDocxText(context: Context, uri: Uri, maxParagraphs: Int = 500): List<String> {
        val paragraphs = mutableListOf<String>()
        var inputStream: InputStream? = null
        try {
            inputStream = openInputStream(context, uri) ?: return emptyList()
            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        val parser = Xml.newPullParser()
                        parser.setInput(zis, "UTF-8")

                        var eventType = parser.eventType
                        var currentParagraph = StringBuilder()
                        var inParagraph = false

                        while (eventType != XmlPullParser.END_DOCUMENT && paragraphs.size < maxParagraphs) {
                            when (eventType) {
                                XmlPullParser.START_TAG -> {
                                    if (parser.name == "p") {
                                        inParagraph = true
                                        currentParagraph.clear()
                                    } else if (parser.name == "t" && inParagraph) {
                                        val text = parser.nextText()
                                        currentParagraph.append(text)
                                    }
                                }
                                XmlPullParser.END_TAG -> {
                                    if (parser.name == "p") {
                                        val line = currentParagraph.toString().trim()
                                        if (line.isNotEmpty()) {
                                            paragraphs.add(line)
                                        }
                                        inParagraph = false
                                    }
                                }
                            }
                            eventType = parser.next()
                        }
                        break
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (_: Exception) {
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
        }
        return paragraphs
    }

    /**
     * Extracts slide numbers and texts from a PPTX file.
     */
    fun extractPptxSlides(context: Context, uri: Uri, maxSlides: Int = 100): List<SlideInfo> {
        val slidesMap = mutableMapOf<Int, String>()
        var inputStream: InputStream? = null
        try {
            inputStream = openInputStream(context, uri) ?: return emptyList()
            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name.startsWith("ppt/slides/slide") && name.endsWith(".xml")) {
                        val slideNumberStr = name.removePrefix("ppt/slides/slide").removeSuffix(".xml")
                        val slideNumber = slideNumberStr.toIntOrNull() ?: 1

                        val parser = Xml.newPullParser()
                        parser.setInput(zis, "UTF-8")

                        var eventType = parser.eventType
                        val slideText = StringBuilder()

                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_TAG && parser.name == "t") {
                                val text = parser.nextText()
                                if (text.isNotBlank()) {
                                    if (slideText.isNotEmpty()) slideText.append(" ")
                                    slideText.append(text)
                                }
                            }
                            eventType = parser.next()
                        }

                        val result = slideText.toString().trim()
                        if (result.isNotEmpty()) {
                            slidesMap[slideNumber] = result
                        }
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (_: Exception) {
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
        }

        return slidesMap.entries
            .sortedBy { it.key }
            .take(maxSlides)
            .map { SlideInfo(slideNumber = it.key, text = it.value) }
    }

    /**
     * Extracts spreadsheet rows and cells from an XLSX file.
     */
    fun extractXlsxSheet(context: Context, uri: Uri, maxRows: Int = 100, maxCols: Int = 20): SheetPreview {
        val sharedStrings = mutableListOf<String>()
        val rows = mutableListOf<List<String>>()

        // Pass 1: Extract shared strings
        var inputStream1: InputStream? = null
        try {
            inputStream1 = openInputStream(context, uri)
            if (inputStream1 != null) {
                ZipInputStream(inputStream1).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "xl/sharedStrings.xml") {
                            val parser = Xml.newPullParser()
                            parser.setInput(zis, "UTF-8")
                            var eventType = parser.eventType
                            while (eventType != XmlPullParser.END_DOCUMENT) {
                                if (eventType == XmlPullParser.START_TAG && parser.name == "t") {
                                    sharedStrings.add(parser.nextText())
                                }
                                eventType = parser.next()
                            }
                            break
                        }
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            try { inputStream1?.close() } catch (_: Exception) {}
        }

        // Pass 2: Extract worksheet 1
        var inputStream2: InputStream? = null
        try {
            inputStream2 = openInputStream(context, uri)
            if (inputStream2 != null) {
                ZipInputStream(inputStream2).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "xl/worksheets/sheet1.xml") {
                            val parser = Xml.newPullParser()
                            parser.setInput(zis, "UTF-8")
                            var eventType = parser.eventType

                            var currentRow = mutableListOf<String>()
                            var currentCellType: String? = null
                            var inCell = false

                            while (eventType != XmlPullParser.END_DOCUMENT && rows.size < maxRows) {
                                when (eventType) {
                                    XmlPullParser.START_TAG -> {
                                        if (parser.name == "row") {
                                            currentRow = mutableListOf()
                                        } else if (parser.name == "c") {
                                            inCell = true
                                            currentCellType = parser.getAttributeValue(null, "t")
                                        } else if (parser.name == "v" && inCell) {
                                            val value = parser.nextText()
                                            val cellContent = if (currentCellType == "s") {
                                                val index = value.toIntOrNull()
                                                if (index != null && index in sharedStrings.indices) sharedStrings[index] else value
                                            } else {
                                                value
                                            }
                                            if (currentRow.size < maxCols) {
                                                currentRow.add(cellContent)
                                            }
                                        }
                                    }
                                    XmlPullParser.END_TAG -> {
                                        if (parser.name == "c") {
                                            inCell = false
                                            currentCellType = null
                                        } else if (parser.name == "row") {
                                            if (currentRow.isNotEmpty()) {
                                                rows.add(currentRow)
                                            }
                                        }
                                    }
                                }
                                eventType = parser.next()
                            }
                            break
                        }
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            try { inputStream2?.close() } catch (_: Exception) {}
        }

        val maxFoundCols = rows.maxOfOrNull { it.size } ?: 0
        return SheetPreview(rows = rows, totalRows = rows.size, totalCols = maxFoundCols)
    }

    /**
     * Inspects archives (ZIP, APK, JAR) to list entries, calculate sizes, and parse APK details.
     */
    fun inspectArchive(context: Context, uri: Uri, maxEntries: Int = 1000): ArchiveInfo {
        var totalFiles = 0
        var totalDirectories = 0
        var totalUncompressedBytes = 0L
        var totalCompressedBytes = 0L
        val entries = mutableListOf<ArchiveEntry>()

        var inputStream: InputStream? = null
        try {
            inputStream = openInputStream(context, uri)
            if (inputStream != null) {
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val isDir = entry.isDirectory || entry.name.endsWith("/")
                        val size = if (entry.size >= 0) entry.size else 0L
                        val compSize = if (entry.compressedSize >= 0) entry.compressedSize else 0L

                        if (isDir) {
                            totalDirectories++
                        } else {
                            totalFiles++
                            totalUncompressedBytes += size
                            totalCompressedBytes += compSize
                        }

                        if (entries.size < maxEntries) {
                            entries.add(
                                ArchiveEntry(
                                    name = entry.name,
                                    isDirectory = isDir,
                                    uncompressedSize = size,
                                    compressedSize = compSize
                                )
                            )
                        }
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
        }

        // Check if APK and extract app metadata
        var isApk = false
        var apkAppName: String? = null
        var apkPackageName: String? = null
        var apkVersionName: String? = null
        var apkIcon: Bitmap? = null

        val path = if (uri.scheme == "file") uri.path else null
        if (path != null && path.endsWith(".apk", ignoreCase = true)) {
            try {
                val pm = context.packageManager
                val pkgInfo = pm.getPackageArchiveInfo(path, 0)
                if (pkgInfo != null) {
                    isApk = true
                    pkgInfo.applicationInfo?.sourceDir = path
                    pkgInfo.applicationInfo?.publicSourceDir = path
                    apkPackageName = pkgInfo.packageName
                    apkVersionName = pkgInfo.versionName
                    apkAppName = pkgInfo.applicationInfo?.loadLabel(pm)?.toString()
                    val iconDrawable = pkgInfo.applicationInfo?.loadIcon(pm)
                    if (iconDrawable != null) {
                        val bmp = Bitmap.createBitmap(
                            iconDrawable.intrinsicWidth.coerceAtLeast(48),
                            iconDrawable.intrinsicHeight.coerceAtLeast(48),
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = android.graphics.Canvas(bmp)
                        iconDrawable.setBounds(0, 0, canvas.width, canvas.height)
                        iconDrawable.draw(canvas)
                        apkIcon = bmp
                    }
                }
            } catch (_: Exception) {}
        }

        return ArchiveInfo(
            totalFiles = totalFiles,
            totalDirectories = totalDirectories,
            totalUncompressedBytes = totalUncompressedBytes,
            totalCompressedBytes = totalCompressedBytes,
            entries = entries,
            isApk = isApk,
            apkAppName = apkAppName,
            apkPackageName = apkPackageName,
            apkVersionName = apkVersionName,
            apkIcon = apkIcon
        )
    }

    private fun openInputStream(context: Context, uri: Uri): InputStream? {
        return try {
            if (uri.scheme == "file") {
                File(uri.path ?: "").inputStream()
            } else {
                context.contentResolver.openInputStream(uri)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun openDescriptor(context: Context, uri: Uri): ParcelFileDescriptor? {
        return try {
            if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                if (f.exists()) ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
                else context.contentResolver.openFileDescriptor(uri, "r")
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")
            }
        } catch (_: Exception) {
            null
        }
    }
}
