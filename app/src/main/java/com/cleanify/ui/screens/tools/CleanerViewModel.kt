package com.cleanify.ui.screens.tools

import android.app.ActivityManager
import android.app.Application
import android.content.ClipData
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cleanify.data.cleaner.CleanerPreferences
import com.cleanify.data.cleaner.FileScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class CleanerUiState(
    val isScanning: Boolean = false,
    val progress: Double = 0.0,
    val foundFiles: List<File> = emptyList(),
    val totalSize: Long = 0,
    val log: List<String> = emptyList(),
    val scanComplete: Boolean = false,
    val deleteComplete: Boolean = false,
    val scanEmptyFile: Boolean = true,
    val scanEmptyFolder: Boolean = true,
    val scanGeneric: Boolean = true,
    val scanApk: Boolean = true,
    val scanCorpse: Boolean = false,
    val filesFailed: Int = 0,
    val ramFreed: Long = 0
)

class CleanerViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = CleanerPreferences(application)
    private val _uiState = MutableStateFlow(CleanerUiState())
    val uiState: StateFlow<CleanerUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = CleanerUiState(
            scanEmptyFile = prefs.cleanEmptyFile,
            scanEmptyFolder = prefs.cleanEmptyFolder,
            scanGeneric = prefs.cleanGeneric,
            scanApk = prefs.cleanApk,
            scanCorpse = prefs.cleanCorpse
        )
    }

    fun updateEmptyFile(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(scanEmptyFile = enabled)
        prefs.cleanEmptyFile = enabled
    }

    fun updateEmptyFolder(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(scanEmptyFolder = enabled)
        prefs.cleanEmptyFolder = enabled
    }

    fun updateGeneric(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(scanGeneric = enabled)
        prefs.cleanGeneric = enabled
    }

    fun updateApk(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(scanApk = enabled)
        prefs.cleanApk = enabled
    }

    fun updateCorpse(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(scanCorpse = enabled)
        prefs.cleanCorpse = enabled
    }

    fun startScan() {
        if (_uiState.value.isScanning) return
        val state = _uiState.value
        _uiState.value = state.copy(
            isScanning = true,
            progress = 0.0,
            foundFiles = emptyList(),
            totalSize = 0,
            log = emptyList(),
            scanComplete = false,
            deleteComplete = false,
            filesFailed = 0,
            ramFreed = 0
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val scanner = FileScanner(
                    Environment.getExternalStorageDirectory(),
                    getApplication()
                )
                scanner.delete = false
                scanner.emptyFile = state.scanEmptyFile
                scanner.emptyDir = state.scanEmptyFolder
                scanner.corpse = state.scanCorpse
                scanner.updateProgress = { percent ->
                    _uiState.value = _uiState.value.copy(progress = percent)
                }
                scanner.addText = { path, _ ->
                    _uiState.value = _uiState.value.copy(
                        foundFiles = _uiState.value.foundFiles + File(path),
                        log = _uiState.value.log + path
                    )
                }
                scanner.setFilters(generic = state.scanGeneric, apk = state.scanApk)
                val totalBytes = scanner.start()
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    totalSize = totalBytes,
                    scanComplete = true
                )
            }
        }
    }

    fun executeClean() {
        if (_uiState.value.isScanning) return
        val state = _uiState.value
        _uiState.value = state.copy(
            isScanning = true,
            progress = 0.0,
            scanComplete = false,
            deleteComplete = false,
            log = emptyList(),
            filesFailed = 0,
            ramFreed = 0
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val scanner = FileScanner(
                    Environment.getExternalStorageDirectory(),
                    getApplication()
                )
                scanner.delete = true
                scanner.emptyFile = state.scanEmptyFile
                scanner.emptyDir = state.scanEmptyFolder
                scanner.corpse = state.scanCorpse
                scanner.updateProgress = { percent ->
                    _uiState.value = _uiState.value.copy(progress = percent)
                }
                scanner.addText = { path, _ ->
                    _uiState.value = _uiState.value.copy(log = _uiState.value.log + "Deleted: $path")
                }
                scanner.addFailText = { path ->
                    _uiState.value = _uiState.value.copy(log = _uiState.value.log + "Failed: $path")
                }
                scanner.setFilters(generic = state.scanGeneric, apk = state.scanApk)
                scanner.start()

                val failed = scanner.filesFailed

                var ramBytes = 0L
                if (prefs.stopBackgroundApps) {
                    ramBytes = stopBackgroundApps()
                }

                if (prefs.cleanClipboard) {
                    clearClipboard()
                }

                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    deleteComplete = true,
                    filesFailed = failed,
                    ramFreed = ramBytes
                )
            }
        }
    }

    fun quickClean() {
        val state = _uiState.value
        _uiState.value = state.copy(
            isScanning = true,
            progress = 0.0,
            foundFiles = emptyList(),
            totalSize = 0,
            log = emptyList(),
            scanComplete = false,
            deleteComplete = false,
            filesFailed = 0,
            ramFreed = 0
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val scanner = FileScanner(
                    Environment.getExternalStorageDirectory(),
                    getApplication()
                )
                scanner.delete = true
                scanner.emptyFile = state.scanEmptyFile
                scanner.emptyDir = state.scanEmptyFolder
                scanner.corpse = state.scanCorpse
                scanner.updateProgress = { percent ->
                    _uiState.value = _uiState.value.copy(progress = percent)
                }
                scanner.addText = { path, _ ->
                    _uiState.value = _uiState.value.copy(log = _uiState.value.log + "Deleted: $path")
                }
                scanner.addFailText = { path ->
                    _uiState.value = _uiState.value.copy(log = _uiState.value.log + "Failed: $path")
                }
                scanner.setFilters(generic = state.scanGeneric, apk = state.scanApk)
                scanner.start()

                val failed = scanner.filesFailed

                var ramBytes = 0L
                if (prefs.stopBackgroundApps) {
                    ramBytes = stopBackgroundApps()
                }

                if (prefs.cleanClipboard) {
                    clearClipboard()
                }

                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    deleteComplete = true,
                    filesFailed = failed,
                    ramFreed = ramBytes
                )
            }
        }
    }

    private fun clearClipboard() {
        try {
            val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cm.clearPrimaryClip()
            } else {
                cm.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        } catch (_: Exception) {}
    }

    private fun stopBackgroundApps(): Long {
        val am = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val beforeFree = mi.availMem

        val pm = getApplication<Application>().packageManager
        @Suppress("DEPRECATION")
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in apps) {
            if (app.packageName != getApplication<Application>().packageName) {
                try {
                    @Suppress("DEPRECATION")
                    am.killBackgroundProcesses(app.packageName)
                } catch (_: Exception) {}
            }
        }

        am.getMemoryInfo(mi)
        return mi.availMem - beforeFree
    }

    fun reset() {
        _uiState.value = CleanerUiState(
            scanEmptyFile = prefs.cleanEmptyFile,
            scanEmptyFolder = prefs.cleanEmptyFolder,
            scanGeneric = prefs.cleanGeneric,
            scanApk = prefs.cleanApk,
            scanCorpse = prefs.cleanCorpse
        )
    }
}
