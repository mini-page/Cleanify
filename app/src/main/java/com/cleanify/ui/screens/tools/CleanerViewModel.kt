package com.cleanify.ui.screens.tools

import android.app.ActivityManager
import android.app.Application
import android.content.ClipData
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
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

data class StorageVolumeEntry(
    val directory: File,
    val description: String,
    val isPrimary: Boolean
)

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

    val storageVolumes: List<StorageVolumeEntry> by lazy(LazyThreadSafetyMode.NONE) {
        buildVolumeList()
    }

    fun startScan(volumeIndex: Int = -1) {
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
                var totalBytes = 0L
                val roots = getRootsForIndex(volumeIndex)
                roots.forEachIndexed { index, root ->
                    val scanner = FileScanner(root, getApplication())
                    scanner.delete = false
                    scanner.emptyFile = state.scanEmptyFile
                    scanner.emptyDir = state.scanEmptyFolder
                    scanner.corpse = state.scanCorpse
                    scanner.updateProgress = { percent ->
                        val overall = (index * 100.0 + percent) / roots.size
                        _uiState.value = _uiState.value.copy(progress = overall)
                    }
                    scanner.addText = { path, _ ->
                        _uiState.value = _uiState.value.copy(
                            foundFiles = _uiState.value.foundFiles + File(path),
                            log = _uiState.value.log + path
                        )
                    }
                    scanner.setFilters(generic = state.scanGeneric, apk = state.scanApk)
                    totalBytes += scanner.start()
                }
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    totalSize = totalBytes,
                    scanComplete = true
                )
            }
        }
    }

    fun executeClean(volumeIndex: Int = -1) {
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
                var totalFailed = 0
                val roots = getRootsForIndex(volumeIndex)
                roots.forEachIndexed { index, root ->
                    val scanner = FileScanner(root, getApplication())
                    scanner.delete = true
                    scanner.emptyFile = state.scanEmptyFile
                    scanner.emptyDir = state.scanEmptyFolder
                    scanner.corpse = state.scanCorpse
                    scanner.updateProgress = { percent ->
                        val overall = (index * 100.0 + percent) / roots.size
                        _uiState.value = _uiState.value.copy(progress = overall)
                    }
                    scanner.addText = { path, _ ->
                        _uiState.value = _uiState.value.copy(log = _uiState.value.log + "Deleted: $path")
                    }
                    scanner.addFailText = { path ->
                        _uiState.value = _uiState.value.copy(log = _uiState.value.log + "Failed: $path")
                    }
                    scanner.setFilters(generic = state.scanGeneric, apk = state.scanApk)
                    scanner.start()
                    totalFailed += scanner.filesFailed
                }

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
                    filesFailed = totalFailed,
                    ramFreed = ramBytes
                )
            }
        }
    }

    fun quickClean(volumeIndex: Int = -1) {
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
                var totalFailed = 0
                val roots = getRootsForIndex(volumeIndex)
                roots.forEachIndexed { index, root ->
                    val scanner = FileScanner(root, getApplication())
                    scanner.delete = true
                    scanner.emptyFile = state.scanEmptyFile
                    scanner.emptyDir = state.scanEmptyFolder
                    scanner.corpse = state.scanCorpse
                    scanner.updateProgress = { percent ->
                        val overall = (index * 100.0 + percent) / roots.size
                        _uiState.value = _uiState.value.copy(progress = overall)
                    }
                    scanner.addText = { path, _ ->
                        _uiState.value = _uiState.value.copy(log = _uiState.value.log + "Deleted: $path")
                    }
                    scanner.addFailText = { path ->
                        _uiState.value = _uiState.value.copy(log = _uiState.value.log + "Failed: $path")
                    }
                    scanner.setFilters(generic = state.scanGeneric, apk = state.scanApk)
                    scanner.start()
                    totalFailed += scanner.filesFailed
                }

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
                    filesFailed = totalFailed,
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
        val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        }
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

    private fun getRootsForIndex(volumeIndex: Int): List<File> {
        val allRoots = if (storageVolumes.isNotEmpty()) {
            storageVolumes.map { it.directory }
        } else {
            listOf(Environment.getExternalStorageDirectory())
        }
        return if (volumeIndex in allRoots.indices) {
            listOf(allRoots[volumeIndex])
        } else {
            allRoots
        }
    }

    private fun buildVolumeList(): List<StorageVolumeEntry> {
        val app = getApplication<Application>()
        val volumes = mutableListOf<StorageVolumeEntry>()
        val storageManager = app.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        for (sv: StorageVolume in storageManager.storageVolumes) {
            val dir = sv.directory ?: continue
            if (dir.isDirectory && dir.canRead()) {
                volumes.add(StorageVolumeEntry(dir, sv.getDescription(app), sv.isPrimary))
            }
        }
        if (volumes.none { it.isPrimary }) {
            val primary = Environment.getExternalStorageDirectory()
            if (primary.isDirectory && primary.canRead()) {
                volumes.add(0, StorageVolumeEntry(primary, "Internal storage", true))
            }
        }
        return volumes
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
