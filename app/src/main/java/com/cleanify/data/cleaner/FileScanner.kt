package com.cleanify.data.cleaner

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.widget.TextView
import java.io.File

class FileScanner(private val path: File, private val context: Context) {
    private var prefs: CleanerPreferences = CleanerPreferences(context)
    private var filesRemoved = 0
    private var kilobytesTotal: Long = 0
    var delete = false
    var autoWhite = prefs.autoWhite
    var corpse = prefs.cleanCorpse
    var emptyFile = prefs.cleanEmptyFile
    var emptyDir = prefs.cleanEmptyFolder
    var updateProgress: ((percent: Double) -> Unit)? = null
    var addText: ((path: String, type: Int) -> Unit)? = null
    var addFailText: ((path: String) -> Unit)? = null
    var filesFailed = 0
    private var installedPackages = getInstalledPackages()
    private var guiScanProgressMax = 0
    private var guiScanProgressProgress = 0
    private var foundFiles: ArrayList<File>? = null

    private var whitelist: List<String> = emptyList()
    private var blacklist: List<Regex> = emptyList()
    private var autoWhitelist: MutableList<String> = ArrayList()

    private fun getListFiles(parentDirectory: File): ArrayList<File> {
        val inFiles = ArrayList<File>()
        val files = parentDirectory.listFiles()
        if (files != null) {
            for (file in files) {
                if (file != null && !isWhiteListed(file)) {
                    if (file.isDirectory) {
                        if (autoWhite) {
                            if (!autoWhiteList(file)) inFiles.add(file)
                        } else inFiles.add(file)
                        inFiles.addAll(getListFiles(file))
                    } else inFiles.add(file)
                }
            }
        }
        return inFiles
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun getInstalledPackages(): ArrayList<String> {
        val pm = context.packageManager
        val pkgs = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val pkgsStr: ArrayList<String> = ArrayList()
        for (pkg in pkgs) {
            pkgsStr.add(pkg.packageName)
        }
        return pkgsStr
    }

    private fun isWhiteListed(file: File): Boolean {
        val absolutePath = file.absolutePath
        val name = file.name
        for (path in whitelist) {
            if (path.equals(absolutePath) || path.equals(name, ignoreCase = true)) return true
        }
        return false
    }

    private fun isBlackListed(file: File): Boolean {
        val absolutePath = file.absolutePath
        for (pattern in blacklist) {
            if (absolutePath.matches(pattern)) return true
        }
        return false
    }

    private fun autoWhiteList(file: File): Boolean {
        for (protectedFile in autoWhitelist) {
            val whiteLists = whitelist
            if (
                file.name.lowercase().contains(protectedFile) &&
                !whiteLists.contains(file.absolutePath.lowercase())
            ) {
                whiteLists
                    .toMutableList()
                    .add(file.absolutePath.lowercase())
                prefs.whitelist = HashSet(whiteLists)
                return true
            }
        }
        return false
    }

    fun filter(file: File): Boolean {
        if (
            corpse &&
            file.parentFile?.name == "data" &&
            file.parentFile?.parentFile?.name == "Android" &&
            file.name != ".nomedia" &&
            !installedPackages.contains(file.name) ||
            emptyFile && isFileEmpty(file) ||
            emptyDir && isDirectoryEmpty(file) ||
            isBlackListed(file)
        ) return true

        val absolutePath = file.absolutePath.lowercase()
        for (filter in filters) {
            if (absolutePath.matches(filter.lowercase().toRegex())) return true
        }
        return false
    }

    private fun isDirectoryEmpty(directory: File): Boolean {
        if (!directory.isDirectory) return false
        val list = directory.list()
        if (list == null) return false
        return list.isNullOrEmpty()
    }

    private fun isFileEmpty(file: File): Boolean {
        return file.isFile && file.length() == 0L
    }

    fun setFilters(generic: Boolean, apk: Boolean) {
        filters.clear()
        if (generic) {
            for (folder in Constants.filter_genericFolders) filters.add(getRegexForFolder(folder))
            for (file in Constants.filter_genericFiles) filters.add(getRegexForFile(file))
        }
        if (apk) {
            for (apkFile in Constants.filter_apkFiles) filters.add(getRegexForFile(apkFile))
        }

        whitelist = prefs.whitelistOn.mapNotNull { it }
        blacklist = prefs.blacklistOn.mapNotNull { it.toRegex() }

        if (autoWhite) {
            autoWhitelist.clear()
            autoWhitelist.addAll(Constants.filter_autoWhite)
        }
    }

    fun start(): Long {
        isRunning = true
        var cycles: Byte = 0
        val maxCycles: Byte = if (delete) prefs.multiRun.toByte() else 1

        while (cycles < maxCycles) {
            addText?.invoke("Running Cycle $cycles/$maxCycles", 0)

            if (foundFiles == null) foundFiles = getListFiles(path)
            guiScanProgressMax += foundFiles!!.size

            for (file in foundFiles!!) {
                if (filter(file)) {
                    addText?.invoke(file.absolutePath, 1)
                    kilobytesTotal += file.length()
                    if (delete) {
                        val isDeleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
                        if (isDeleted) {
                            ++filesRemoved
                        } else {
                            ++filesFailed
                            addFailText?.invoke(file.absolutePath)
                        }
                    }
                }
                guiScanProgressProgress++
                updateProgress?.invoke(guiScanProgressProgress * 100.0 / guiScanProgressMax)
            }
            foundFiles = null
            if (filesRemoved == 0) break
            filesRemoved = 0
            ++cycles
        }
        addText?.invoke("Finished!", 1)
        isRunning = false
        return kilobytesTotal
    }

    private fun getRegexForFolder(folder: String): String {
        return ".*(\\\\|/)$folder(\\\\|/|$).*"
    }

    private fun getRegexForFile(file: String): String {
        return ".+" + file.replace(".", "\\.") + "$"
    }

    companion object {
        var isRunning = false
        private val filters = ArrayList<String>()
    }
}
