package com.cleanify.util

import android.content.Context
import android.net.Uri

object NativeHasher {
    init {
        System.loadLibrary("cleanify_hash")
    }

    private external fun nativeHashFile(fd: Int): String?
    private external fun nativeHashFilePartial(fd: Int, bytesToRead: Int): String?
    external fun hashBytes(data: ByteArray): String?

    fun hashFile(context: Context, uri: Uri): String? {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        val fd = pfd.detachFd()
        return try {
            nativeHashFile(fd)
        } catch (e: Exception) {
            val fis = context.contentResolver.openInputStream(uri) ?: return null
            fis.use { stream -> hashBytes(stream.readBytes()) }
        }
    }

    fun hashFilePartial(context: Context, uri: Uri, bytesToRead: Int): String? {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        val fd = pfd.detachFd()
        return try {
            nativeHashFilePartial(fd, bytesToRead)
        } catch (e: Exception) {
            val fis = context.contentResolver.openInputStream(uri) ?: return null
            fis.use { stream ->
                val limited = ByteArray(bytesToRead)
                val read = stream.read(limited)
                if (read <= 0) return@use null
                hashBytes(if (read < bytesToRead) limited.copyOf(read) else limited)
            }
        }
    }
}
