package com.cleanify.util

import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

/**
 * Shared formatting utilities used across the app.
 * Replaces duplicate implementations in SwiperScreen and SessionSetupScreen.
 */
object Formatters {

    /**
     * Formats a byte count into a human-readable file size string.
     * e.g., 1536 → "1.5 KB", 1073741824 → "1 GB"
     */
    fun fileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val formatter = DecimalFormat("#,##0.#")
        return formatter.format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

    /**
     * Formats a duration in milliseconds into "M:SS" or "H:MM:SS".
     * e.g., 65000 → "1:05", 3661000 → "1:01:01"
     */
    fun duration(millis: Long): String {
        if (millis < 0) return "0:00"
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    /**
     * Splits a file path into a display name and a truncated parent path.
     * Returns a Pair of (fileName, truncatedParentPath).
     * Replaces duplicate implementations in SettingsScreen and SessionSetupScreen.
     */
    fun pathForDisplay(path: String): Pair<String, String> {
        val file = File(path)
        val name = file.name
        val parentPath = file.parent?.replace("/storage/emulated/0", "") ?: ""
        val displayParent = if (parentPath.length > 30) "...${parentPath.takeLast(27)}" else parentPath
        return Pair(name, displayParent)
    }
}
