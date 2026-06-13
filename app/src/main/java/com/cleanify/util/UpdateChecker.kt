package com.cleanify.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val tagName: String,
    val downloadUrl: String,
    val changelog: String
) {
    val versionName: String get() = tagName.removePrefix("v")
}

sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data class Available(val info: UpdateInfo) : UpdateCheckState
    data class Downloading(val progress: Float) : UpdateCheckState
    data class Downloaded(val file: File) : UpdateCheckState
    data class Error(val message: String) : UpdateCheckState
}

object UpdateChecker {

    private const val GITHUB_API_URL = "https://api.github.com/repos/mini-page/Cleanify/releases/latest"
    private const val CONNECT_TIMEOUT = 10_000
    private const val READ_TIMEOUT = 30_000
    private const val CACHE_DURATION_MS = 5 * 60 * 1000L

    private var cachedResult: CachedResult? = null

    private data class CachedResult(
        val info: UpdateInfo?,
        val timestamp: Long
    )

    suspend fun checkForUpdate(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        cachedResult?.let { cached ->
            if (now - cached.timestamp < CACHE_DURATION_MS) {
                return@withContext cached.info
            }
        }

        val url = URL(GITHUB_API_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
        conn.setRequestProperty("User-Agent", "Cleanify")
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT

        try {
            conn.connect()
            val responseCode = conn.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                throw IOException("GitHub API error: $errorBody")
            }

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val tagName = json.getString("tag_name")
            val assets = json.optJSONArray("assets")
            val downloadUrl = assets?.optJSONObject(0)?.getString("browser_download_url") ?: return@withContext null
            val changelog = json.optString("body", "").trim()

            val info = UpdateInfo(tagName, downloadUrl, changelog)
            val isNewer = info.versionName != currentVersion

            cachedResult = CachedResult(if (isNewer) info else null, now)
            if (isNewer) info else null
        } finally {
            conn.disconnect()
        }
    }

    fun downloadApk(context: Context, downloadUrl: String, tagName: String): Flow<Float> = flow {
        val file = File(context.cacheDir, "Cleanify-$tagName.apk")
        val url = URL(downloadUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT
        conn.readTimeout = READ_TIMEOUT
        conn.setRequestProperty("User-Agent", "Cleanify")

        try {
            conn.connect()
            val contentLength = conn.contentLengthLong
            val inputStream = conn.inputStream
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (contentLength > 0) {
                    emit((totalBytesRead.toFloat() / contentLength).coerceIn(0f, 1f))
                }
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
        } finally {
            conn.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    fun installApk(context: Context, file: File) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // No activity found, let it crash silently
        }
    }
}
