package com.cleanify.data.cleaner

import android.content.Context

class CleanerPreferences(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("cleaner_prefs", Context.MODE_PRIVATE)

    var autoWhite: Boolean
        get() = prefs.getBoolean("auto_white", true)
        set(v) = prefs.edit().putBoolean("auto_white", v).apply()
    var blacklist: Set<String>
        get() = prefs.getStringSet("blacklist", Constants.blacklistDefault) ?: Constants.blacklistDefault
        set(v) = prefs.edit().putStringSet("blacklist", v).apply()
    var blacklistOn: Set<String>
        get() = prefs.getStringSet("blacklistOn", Constants.blacklistOnDefault) ?: Constants.blacklistOnDefault
        set(v) = prefs.edit().putStringSet("blacklistOn", v).apply()
    var cleanApk: Boolean
        get() = prefs.getBoolean("clean_apk", true)
        set(v) = prefs.edit().putBoolean("clean_apk", v).apply()
    var cleanCorpse: Boolean
        get() = prefs.getBoolean("clean_corpse", false)
        set(v) = prefs.edit().putBoolean("clean_corpse", v).apply()
    var cleanEmptyFile: Boolean
        get() = prefs.getBoolean("clean_empty_file", true)
        set(v) = prefs.edit().putBoolean("clean_empty_file", v).apply()
    var cleanEmptyFolder: Boolean
        get() = prefs.getBoolean("clean_empty_folder", true)
        set(v) = prefs.edit().putBoolean("clean_empty_folder", v).apply()
    var cleanGeneric: Boolean
        get() = prefs.getBoolean("clean_generic", true)
        set(v) = prefs.edit().putBoolean("clean_generic", v).apply()
    var multiRun: Int
        get() = prefs.getInt("multi_run", 1)
        set(v) = prefs.edit().putInt("multi_run", v).apply()
    var whitelist: Set<String>
        get() = prefs.getStringSet("whitelist", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("whitelist", v).apply()
    var whitelistOn: Set<String>
        get() = prefs.getStringSet("whitelistOn", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("whitelistOn", v).apply()
    var cleanClipboard: Boolean
        get() = prefs.getBoolean("clean_clipboard", false)
        set(v) = prefs.edit().putBoolean("clean_clipboard", v).apply()
    var cleanOnBoot: Boolean
        get() = prefs.getBoolean("clean_on_boot", false)
        set(v) = prefs.edit().putBoolean("clean_on_boot", v).apply()
    var cleanEvery: Int
        get() = prefs.getInt("clean_every", -1)
        set(v) = prefs.edit().putInt("clean_every", v).apply()
    var stopBackgroundApps: Boolean
        get() = prefs.getBoolean("stop_background_apps", false)
        set(v) = prefs.edit().putBoolean("stop_background_apps", v).apply()
}
