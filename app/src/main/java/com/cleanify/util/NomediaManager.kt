package com.cleanify.util

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NomediaManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val logTag = "NomediaManager"
    private val managedDirectories: List<File> by lazy {
        listOf(
            context.getExternalFilesDir("recycle_bin"),
            context.filesDir
        ).filterNotNull()
    }

    fun applySetting(hideFromGallery: Boolean) {
        managedDirectories.forEach { dir ->
            val nomediaFile = File(dir, ".nomedia")
            if (hideFromGallery) {
                if (!nomediaFile.exists()) {
                    try {
                        nomediaFile.createNewFile()
                    } catch (e: Exception) {
                        Log.w(logTag, "Failed to create .nomedia in ${dir.absolutePath}", e)
                    }
                }
            } else {
                if (nomediaFile.exists()) {
                    try {
                        nomediaFile.delete()
                    } catch (e: Exception) {
                        Log.w(logTag, "Failed to delete .nomedia in ${dir.absolutePath}", e)
                    }
                }
            }
        }
    }
}
