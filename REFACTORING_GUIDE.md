# Cleanify Refactoring Guide

> **Philosophy:** Every step is independently `git revert`-able. Run `./gradlew assembleDebug` after each step.
> If it breaks, revert, investigate, retry. No step depends on a future step being done first.

---

## Phase 0 — Quick Wins (Performance & Correctness)

These are **non-structural** fixes. They don't change APIs, don't move files, and can each be a single commit.

### Step 0.1: Re-enable R8 Minification

**Files:** `app/build.gradle.kts`

```kotlin
// BEFORE
release {
    isMinifyEnabled = false
    isShrinkResources = false
    signingConfig = signingConfigs.getByName("release")
}

// AFTER
release {
    isMinifyEnabled = true
    isShrinkResources = true
    signingConfig = signingConfigs.getByName("release")
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

**Why:** Release APKs are currently **2-3x larger** than necessary. R8 removes unused code/resources.

**Rollback:** `git revert <commit>`

**Test:** Build release APK, verify app launches, scan works, images load.

---

### Step 0.2: Fix `collectAsState()` → `collectAsStateWithLifecycle()` ✅ DONE

**Files (all):**
- `ui/screens/swiper/SwiperScreen.kt` (14 occurrences)
- `ui/screens/duplicates/DuplicatesScreen.kt` (3 occurrences)
- `ui/screens/settings/SettingsScreen.kt` (40+ occurrences)
- `ui/screens/session/SessionSetupScreen.kt` (3 occurrences)
- `ui/screens/onboarding/OnboardingScreen.kt` (if present)

**Pattern (apply to every `.collectAsState()` call):**

```kotlin
// BEFORE
val uiState by viewModel.uiState.collectAsState()
val currentTheme by viewModel.currentTheme.collectAsState()

// AFTER
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
```

Add the import to each file:
```kotlin
import androidx.lifecycle.compose.collectAsStateWithLifecycle
```

**Why:** `collectAsState()` keeps collecting when the screen is in background → wasted CPU/battery. `collectAsStateWithLifecycle()` pauses collection in `ON_STOP`.

**Rollback:** `git revert <commit>`

**Test:** Open each screen, background the app for 10s, return. Verify state is preserved. Check logcat for no recomposition crashes.

---

### Step 0.3: Fix raw `CoroutineScope` in OnboardingScreen ✅ DONE

**File:** `ui/screens/onboarding/OnboardingScreen.kt`

Removed the raw `CoroutineScope(Dispatchers.Main).launch` and replaced with direct `(context as? ComponentActivity)?.finish()` call since `finish()` is already main-thread-safe.

**Why:** Raw `CoroutineScope` is never cancelled → memory leak.

**Rollback:** `git revert <commit>`

**Test:** Complete onboarding flow, verify no leaked coroutines.

---

### Step 0.4: Add Logging to Silent Exception Catches

**Files:** `data/repository/DirectMediaRepositoryImpl.kt` (15+ bare catches)

**Pattern (apply everywhere `catch (_: Exception) {}` or `catch (e: Exception) { null }`):**

```kotlin
// BEFORE
} catch (_: Exception) {}

// AFTER
} catch (e: Exception) { Log.w(TAG, "Operation failed: ${e.message}") }
```

Add `private const val TAG = "DirectMediaRepo"` at top if not present.

**Why:** Silent failures make debugging impossible. At minimum, warnings should be logged.

**Rollback:** `git revert <commit>`

**Test:** Full app walkthrough. Verify no new crashes, existing behavior unchanged.

---

### Step 0.5: Fix `@Suppress("DEPRECATION")` Where Possible

**Files to audit:**
| File | Issue | Fix |
|------|-------|-----|
| `StorageVolumeProvider.kt` | `getExternalStorageDirectory()` | Use `Context.getExternalFilesDirs()` |
| `FileManager.kt` | `getUuid()` | Use `StorageVolume.getStorageVolume()` API |
| `CleanerWorker.kt` | Foreground service type | Add `foregroundServiceType` to manifest |
| `SummarySheet.kt` | `ConfigurationScreenWidthHeight` | Use `LocalConfiguration.current` |
| `MainActivity.kt` | Legacy compat | Check if `WindowCompat` is available |

**Why:** Deprecated APIs may be removed in Android 15+, causing crashes.

**Rollback:** `git revert <commit>` per file

**Test:** Each affected screen/feature individually.

---

## Phase 1 — DRY: Eliminate Repeated Patterns

### Step 1.1: Create `Preference<T>` Wrapper

**New file:** `data/repository/Preference.kt`

```kotlin
package com.cleanify.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A type-safe wrapper around a single DataStore preference.
 * Eliminates the repeated flow+setter boilerplate in PreferencesRepository.
 */
class Preference<T>(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val key: Preferences.Key<T>,
    private val default: T
) {
    val flow: Flow<T> = dataStore.data.map { prefs -> prefs[key] ?: default }

    suspend fun set(value: T) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    suspend fun get(): T = flow.value
}
```

**Why:** `PreferencesRepository` has **800+ lines** of identical flow/setter pairs. This cuts it by ~60%.

**Rollback:** `git revert <commit>` (just deletes the new file, no existing code changed yet)

**Test:** Build compiles successfully.

---

### Step 1.2: Migrate Boolean Preferences to `Preference<Boolean>`

**File:** `data/repository/PreferencesRepository.kt`

**Before (each preference):**
```kotlin
val compactFolderViewFlow: Flow<Boolean> = context.dataStore.data
    .map { preferences ->
        preferences[PreferencesKeys.COMPACT_FOLDER_VIEW] ?: false
    }

suspend fun setCompactFolderView(enabled: Boolean) {
    context.dataStore.edit { preferences ->
        preferences[PreferencesKeys.COMPACT_FOLDER_VIEW] = enabled
    }
}
```

**After:**
```kotlin
val compactFolderView = Preference(context, context.dataStore, PreferencesKeys.COMPACT_FOLDER_VIEW, false)

// Expose for backward compatibility during migration:
val compactFolderViewFlow: Flow<Boolean> get() = compactFolderView.flow
```

**Apply to ALL boolean preferences first** (they're the simplest). Do string preferences in the next step.

**Test:** Walk through every setting toggle. Verify persistence across app restarts.

---

### Step 1.3: Migrate Enum/String Preferences to `Preference<String>`

**Same file:** `PreferencesRepository.kt`

For enum preferences, create a typed wrapper:

```kotlin
class EnumPreference<T : Enum<T>>(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val key: Preferences.Key<String>,
    private val default: T,
    private val clazz: Class<T>
) {
    val flow: Flow<T> = dataStore.data.map { prefs ->
        prefs[key]?.let { try { clazz.enumConstants?.valueOf(it) } catch (_: Exception) { null } } ?: default
    }

    suspend fun set(value: T) {
        dataStore.edit { prefs -> prefs[key] = value.name }
    }
}
```

**Apply to:** `themeFlow`, `appLocaleFlow`, `summaryViewModeFlow`, `swipeSensitivityFlow`, `swipeDownActionFlow`, `tapActionFlow`, `doubleTapActionFlow`, `folderBarLayoutFlow`, `folderNameLayoutFlow`, `addFolderFocusTargetFlow`, `unselectAllInSearchScopeFlow`, `duplicateScanScopeFlow`, `similarityThresholdLevelFlow`

**Test:** Every setting picker. Verify defaults load correctly.

---

### Step 1.4: Migrate Set Preferences to `stringSetPreferencesKey`

**Files:** `PreferencesRepository.kt`

Currently, sets like `PROCESSED_MEDIA_PATHS`, `SOURCE_FAVORITE_FOLDERS`, `TARGET_FAVORITE_FOLDERS` are stored as comma-separated strings:

```kotlin
// BEFORE
val sourceFavoriteFoldersFlow: Flow<Set<String>> = context.dataStore.data
    .map { preferences ->
        val favoritesString = preferences[PreferencesKeys.SOURCE_FAVORITE_FOLDERS] ?: ""
        if (favoritesString.isEmpty()) emptySet()
        else favoritesString.split(",").filter { it.isNotBlank() }.toSet()
    }

// AFTER (with migration helper)
private val migratedKeys = mutableSetOf<Preferences.Key<*>>()

val sourceFavoriteFolders: Preference<Set<String>> by lazy {
    Preference(context, context.dataStore, stringSetPreferencesKey("source_favorite_folders_v2"), emptySet())
}

val sourceFavoriteFoldersFlow: Flow<Set<String>> get() = sourceFavoriteFolders.flow
```

**Migration strategy:**
```kotlin
suspend fun migrateStringSetKeys() {
    context.dataStore.edit { prefs ->
        // Migrate SOURCE_FAVORITE_FOLDERS
        val old = prefs[stringPreferencesKey("source_favorite_folders")]
        if (old != null) {
            val newKey = stringSetPreferencesKey("source_favorite_folders_v2")
            prefs[newKey] = old.split(",").filter { it.isNotBlank() }.toSet()
            prefs.remove(stringPreferencesKey("source_favorite_folders"))
        }
        // Repeat for TARGET_FAVORITE_FOLDERS, PROCESSED_MEDIA_PATHS, etc.
    }
}
```

Call `migrateStringSetKeys()` in `CleanifyApp.onCreate()`.

**Why:** `split(",")` is fragile (paths can contain commas in theory) and inefficient for large sets.

**Test:** Import favorites, verify they persist. Clear history, verify it clears.

---

### Step 1.5: Extract `formatFileSize()` to Shared Utility

**Files:**
- `ui/screens/swiper/SwiperScreen.kt` (has `formatFileSize`)
- `ui/screens/session/SessionSetupScreen.kt` (has identical `formatFileSize`)

**New file:** `util/FileSizeFormatter.kt`

```kotlin
package com.cleanify.util

import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

object FileSizeFormatter {
    fun format(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        val formatter = DecimalFormat("#,##0.#")
        return formatter.format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }
}
```

Delete the local `formatFileSize` functions in both screens.

**Test:** Display file sizes in swiper and session screens. Verify formatting matches.

---

### Step 1.6: Extract `formatDuration()` to Shared Utility

**New file or add to `FileSizeFormatter.kt`** → rename to `Formatters.kt`

```kotlin
object Formatters {
    fun fileSize(size: Long): String { /* ... */ }
    fun duration(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
```

**Test:** Video player seek bar shows correct durations.

---

## Phase 2 — Extract ViewModel Responsibilities

> **Key principle:** Each extraction creates a new class, updates SwiperViewModel to delegate to it, and keeps the same public API. This means the Screen composable needs **zero changes**.

### Step 2.1: Extract `TargetFolderManager`

**New file:** `ui/screens/swiper/TargetFolderManager.kt`

```kotlin
package com.cleanify.ui.screens.swiper

import com.cleanify.data.model.MediaItem
import com.cleanify.data.repository.PreferencesRepository
import com.cleanify.domain.bus.FolderUpdateEvent
import com.cleanify.domain.bus.FolderUpdateEventBus
import com.cleanify.domain.repository.MediaRepository
import com.cleanify.ui.components.FolderSearchManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages target folders (the folders users move media into).
 * Extracted from SwiperViewModel for single-responsibility.
 */
class TargetFolderManager @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val mediaRepository: MediaRepository,
    private val folderUpdateEventBus: FolderUpdateEventBus
) {
    private val _newlyAdded = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _sessionHidden = MutableStateFlow<Set<String>>(emptySet())

    data class State(
        val targetFolders: List<Pair<String, String>> = emptyList(),
        val targetFavorites: Set<String> = emptySet(),
        val folderIdToNameMap: Map<String, String> = emptyMap()
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun observe(scope: CoroutineScope) {
        scope.launch {
            combine(
                preferencesRepository.targetFavoriteFoldersFlow,
                _newlyAdded,
                _sessionHidden
            ) { favorites, newlyAdded, sessionHidden ->
                _state.value = _state.value.copy(targetFavorites = favorites)
                // ... same combine logic currently in SwiperViewModel.observeTargetFolders()
            }.collect()
        }
    }

    fun createAndAdd(name: String, parentPath: String, addToFavorites: Boolean, scope: CoroutineScope) {
        // ... extracted from SwiperViewModel.createAndAddTargetFolder()
    }

    fun import(path: String, addToFavorites: Boolean, scope: CoroutineScope) {
        // ... extracted from SwiperViewModel.importTargetFolder()
    }

    fun toggleFavorite(path: String, scope: CoroutineScope) { /* ... */ }
    fun remove(path: String) { /* ... */ }
    fun hide(path: String) { /* ... */ }
    fun rename(oldPath: String, newPath: String, newName: String) { /* ... */ }
}
```

**In `SwiperViewModel`:**
```kotlin
@Inject lateinit var targetFolderManager: TargetFolderManager

// Delegate calls:
fun showAddTargetFolderDialog() = targetFolderManager.showDialog()
fun confirmFolderSelection(name: String, fav: Boolean, move: Boolean) =
    targetFolderManager.createAndAdd(name, lastParentPath, fav, viewModelScope)
// etc.
```

**Rollback:** `git revert <commit>` (SwiperViewModel goes back to inline logic)

**Test:** Create folder, import folder, rename folder, toggle favorite, remove from bar. All from swiper screen.

---

### Step 2.2: Extract `PendingChangesManager`

**New file:** `ui/screens/swiper/PendingChangesManager.kt`

```kotlin
/**
 * Manages the list of pending changes (keep/delete/move/screenshot)
 * and pre-computes summary lists for the SummarySheet.
 */
class PendingChangesManager {
    data class State(
        val pendingChanges: List<PendingChange> = emptyList(),
        val toDelete: List<PendingChange> = emptyList(),
        val toKeep: List<PendingChange> = emptyList(),
        val toConvert: List<PendingChange> = emptyList(),
        val groupedMoves: List<Pair<String, List<PendingChange>>> = emptyList()
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun add(change: PendingChange, folderIdToNameMap: Map<String, String>) { /* ... */ }
    fun remove(change: PendingChange, folderIdToNameMap: Map<String, String>) { /* ... */ }
    fun clear() { /* ... */ }

    private fun recompute(folderIdToNameMap: Map<String, String>) {
        // Currently: processSummaryLists() in SwiperViewModel
    }
}
```

**Why:** `processSummaryLists()` is called in 10+ places in SwiperViewModel. This centralizes it.

**Test:** Swipe through items, verify summary sheet shows correct counts. Revert changes, verify sheet updates.

---

### Step 2.3: Extract `VideoPlaybackState`

**New file:** `ui/screens/swiper/VideoPlaybackState.kt`

```kotlin
/**
 * Holds video playback state: speed, mute, position.
 * Replaces the 5 separate fields in SwiperUiState.
 */
data class VideoPlaybackState(
    val speed: Float = 1.0f,
    val isMuted: Boolean = true,
    val position: Long = 0L
)
```

**In `SwiperUiState`:**
```kotlin
// BEFORE
val videoPlaybackPosition: Long = 0L,
val videoPlaybackSpeed: Float = 1.0f,
val isVideoMuted: Boolean = true,

// AFTER
val videoPlayback: VideoPlaybackState = VideoPlaybackState(),
```

**Why:** Groups related state. Makes it easy to add new video controls later (e.g., loop, fullscreen).

**Test:** Play video, change speed, mute/unmute, seek. Verify all still works.

---

### Step 2.4: Consolidate FolderState into `SwiperUiState`

**New file:** `ui/screens/swiper/FolderState.kt`

```kotlin
/**
 * All folder-bar related state, extracted from the 12+ folder fields in SwiperUiState.
 */
data class FolderState(
    val targetFolders: List<Pair<String, String>> = emptyList(),
    val targetFavorites: Set<String> = emptySet(),
    val isFolderBarExpanded: Boolean = false,
    val useLegacyFolderIcons: Boolean = false,
    val compactFoldersView: Boolean = false,
    val hideFilename: Boolean = false,
    val menuState: FolderMenuState = FolderMenuState.Hidden,
    val defaultCreationPath: String = "",
    val folderIdToNameMap: Map<String, String> = emptyMap()
)
```

**In `SwiperUiState`:**
```kotlin
// BEFORE: 12 separate fields
val targetFolders: List<Pair<String, String>> = emptyList(),
val targetFavorites: Set<String> = emptySet(),
val isFolderBarExpanded: Boolean = false,
// ... etc

// AFTER: 1 nested field
val folderState: FolderState = FolderState(),
```

**Why:** Reduces SwiperUiState from ~40 fields to ~25. Grouping related state makes the code self-documenting.

**Test:** All folder interactions: expand, collapse, long-press menu, rename, favorite.

---

## Phase 3 — Split Large Screen Composables

> **Key principle:** Each extraction moves a `@Composable` function to its own file. The original file imports and calls it. Zero behavioral changes.

### Step 3.1: Extract `MediaItemCard.kt` from `SwiperScreen.kt`

**New file:** `ui/screens/swiper/MediaItemCard.kt`

Move:
- `MediaItemCard` composable (~300 lines)
- `VideoPlayer` composable
- `VideoBottomBar` composable
- `AudioPlayerCard` composable

**In `SwiperScreen.kt`:** Replace the function bodies with imports:
```kotlin
import com.cleanify.ui.screens.swiper.components.MediaItemCard
import com.cleanify.ui.screens.swiper.components.VideoPlayer
// etc.
```

**Rollback:** `git revert <commit>`

**Test:** Swipe card, zoom, long-press, play video, play audio. All gestures work.

---

### Step 3.2: Extract `FolderComponents.kt` from `SwiperScreen.kt`

**New file:** `ui/screens/swiper/FolderComponents.kt`

Move:
- `BottomFolderBar` composable
- `FolderChip` composable
- `FolderChipWrapper` composable
- `FolderContextMenu` composable

**Test:** Expand/collapse folder bar, long-press folder chip, select folder.

---

### Step 3.3: Extract `SwiperTopBar.kt`

**New file:** `ui/screens/swiper/SwiperTopBar.kt`

Move:
- `SwiperTopBar` composable
- `InfoButton` composable
- `MediaItemContextMenu` composable

**Test:** Top bar buttons work, overflow menu opens, info sheet shows.

---

### Step 3.4: Extract `ThumbnailStrip.kt`

**New file:** `ui/screens/swiper/ThumbnailStrip.kt`

Move:
- `ThumbnailStrip` composable

**Test:** Tap thumbnails to navigate, verify active highlight and decided overlay.

---

### Step 3.5: Extract `SummarySheet` Components

**New file:** `ui/screens/swiper/SummarySheet.kt`

Move the existing `SummarySheet` composable (if not already separate).

**Test:** Open summary sheet, toggle view modes, revert individual changes.

---

### Step 3.6: Extract `ItemInfoSheet.kt`

**New file:** `ui/screens/swiper/ItemInfoSheet.kt`

Move `ItemInfoSheet` composable.

**Test:** Open info sheet, rename file, drop metadata, dismiss.

---

## Phase 4 — Split SettingsScreen

### Step 4.1: Extract `AppearanceSubPage.kt`

**New file:** `ui/screens/settings/AppearanceSubPage.kt`

Move `AppearanceSubPage`, `AccentColorSetting`, `AccentColorDialog`.

**Test:** Theme picker, dynamic colors toggle, accent color dialog.

---

### Step 4.2: Extract `SortingSubPage.kt`

**New file:** `ui/screens/settings/SortingSubPage.kt`

Move `SortingSubPage`.

**Test:** Swipe sensitivity, folder name position, layout options.

---

### Step 4.3: Extract `BehaviorSubPage.kt`

**New file:** `ui/screens/settings/BehaviorSubPage.kt`

Move `BehaviorSubPage`, `DefaultAlbumLocationSetting`, `RememberMediaSetting`, `ForgetSortedMediaSetting`.

**Test:** Folder selection mode, default path, remember media toggle.

---

### Step 4.4: Extract `AboutSubPage.kt`

**New file:** `ui/screens/settings/AboutSubPage.kt`

Move `AboutSubPage`, `UpdateCard`, `UpdateAvailableDialog`, `DownloadProgressDialog`, `InstallDialog`.

**Test:** Check for updates, social links, open source licenses.

---

### Step 4.5: Extract `HelpSupportSubPage.kt`

**New file:** `ui/screens/settings/HelpSupportSubPage.kt`

Move `HelpSupportSubPage`, `PermissionRow`, `PermissionEntry`, `UnindexedFilesDialog`.

**Test:** Replay tutorial, export/import favorites, permission manager.

---

### Step 4.6: Extract Shared Settings Components

**New file:** `ui/screens/settings/components/SettingsComponents.kt`

Move:
- `SettingSwitch`
- `SettingsPickerItem` (generic)
- `SettingsItem`
- `SectionHeader`
- `ToolAboutCard`
- `MediaIndexingStatusItem`
- `DuplicateScanScopeManagementDialog`
- `formatPathForDisplay`

**Why:** These are shared across 5+ sub-pages. One file, one import.

**Test:** Every settings sub-page renders correctly.

---

## Phase 5 — DuplicatesRepository & SharedPreferences Cleanup

### Step 5.1: Migrate Hidden Group IDs to Room

**Files:**
- `data/db/entity/HiddenGroupId.kt` (new entity)
- `data/db/dao/HiddenGroupIdDao.kt` (new DAO)
- `data/db/CleanifyDatabase.kt` (add entity + DAO)
- `domain/repository/DuplicatesRepository.kt` (replace SharedPreferences)

```kotlin
@Entity(tableName = "hidden_group_ids")
data class HiddenGroupId(
    @PrimaryKey val groupId: String
)

@Dao
interface HiddenGroupIdDao {
    @Query("SELECT groupId FROM hidden_group_ids")
    suspend fun getAll(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(groupId: HiddenGroupId)

    @Query("DELETE FROM hidden_group_ids WHERE groupId = :id")
    suspend fun delete(id: String)
}
```

In `DuplicatesRepository`:
```kotlin
// BEFORE
private val prefs: SharedPreferences by lazy {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
suspend fun getHiddenGroupIds(): Set<String> =
    prefs.getStringSet(KEY_HIDDEN_GROUP_IDS, emptySet()) ?: emptySet()

// AFTER
suspend fun getHiddenGroupIds(): Set<String> =
    hiddenGroupIdDao.getAll().toSet()
```

**Test:** Hide a duplicate group, re-scan, verify it stays hidden.

---

### Step 5.2: Remove `SharedPreferences` Dependency from `DuplicatesRepository`

After Step 5.1, `DuplicatesRepository` should have **zero** `SharedPreferences` usage.

**Test:** All duplicate features work: scan, select, delete, hide group, flag as incorrect.

---

## Phase 6 — Split DirectMediaRepositoryImpl

### Step 6.1: Extract `FileSystemScanner`

**New file:** `data/repository/FileSystemScanner.kt`

Extract:
- `performSinglePassFileSystemScan()`
- `findViableTargetFolders()`
- `getAllMediaFilePaths()`
- `queueAllVolumeRoots()`
- `isMediaFile()`, `isSafeDestination()`, `getStandardSystemDirectoryPaths()`, `getPrimarySystemDirectoryPaths()`

**Why:** File system scanning is a pure concern. Isolating it makes it testable with mock file systems.

**Test:** Session setup folder list loads correctly. Refresh works.

---

### Step 6.2: Extract `MediaStoreDataSource`

**New file:** `data/repository/MediaStoreDataSource.kt`

Extract:
- `getMediaStoreDataForBuckets()`
- `getMediaStoreKnownPaths()`
- `scanPathsAndWait()`
- `triggerFullMediaStoreScan()`

**Why:** MediaStore queries are a distinct concern from file system scanning.

**Test:** Media loading in swiper, indexing status check, full scan trigger.

---

## Phase 7 — Testing

### Step 7.1: Add Unit Tests for `DuplicateFinderUseCase`

**New file:** `app/src/test/java/com/cleanify/domain/usecase/DuplicateFinderUseCaseTest.kt`

Test:
- Empty list returns empty result
- Files with same size but different content → no group
- Files with same size and same hash → grouped
- Cache hit (existing hash) skips re-hashing

**Test:** Run `./gradlew test`

---

### Step 7.2: Add Unit Tests for `SimilarFinderUseCase`

**New file:** `app/src/test/java/com/cleanify/domain/usecase/SimilarFinderUseCaseTest.kt`

Test:
- pHash distance calculation
- Bucket grouping logic
- Denial keys filter out pairs
- Screenshot threshold is stricter

---

### Step 7.3: Add Unit Tests for `PendingChangesManager`

**New file:** `app/src/test/java/com/cleanify/ui/screens/swiper/PendingChangesManagerTest.kt`

Test:
- Add move → appears in groupedMoves
- Add delete → appears in toDelete
- Add screenshot → appears in toConvert
- Clear → all lists empty
- Revert → removed from correct list

---

### Step 7.4: Add Unit Tests for `Formatters`

**New file:** `app/src/test/java/com/cleanify/util/FormattersTest.kt`

Test:
- 0 bytes → "0 B"
- 1024 bytes → "1 KB"
- 1.5 MB → "1.5 MB"
- Duration formatting

---

## Phase 8 — UI/UX Polish & Accessibility

> Verified findings from a full app audit. Each step is independently revert-able.

### Step 8.1: Add Confirmation Dialogs for Destructive Actions

| Location | Issue | Fix |
|----------|-------|-----|
| `SwiperScreen.kt` | Delete FAB (`onDelete`) fires immediately on tap | Add confirmation `AlertDialog` before `viewModel.handleSwipeLeft()` |
| `ContactCleanerScreen.kt` | Individual contact delete has no confirmation | Add `AlertDialog` with "Delete this contact?" before `viewModel.deleteContact()` |
| `MediaPreviewDialog.kt` | Non-media preview dialog is a dead-end (no action buttons) | Add Restore/Delete buttons when called from RecycleBin context |
| `ItemInfoSheet.kt` | Info sheet is read-only dead-end | Add Share/Open/Copy-Path action buttons at bottom |
| `RecycleBinScreen.kt` | "Empty Bin" fires without confirmation | Add `AlertDialog` with count before `viewModel.emptyBin()` |

**Pattern:**
```kotlin
var showConfirmDelete by remember { mutableStateOf(false) }

// In the onClick:
showConfirmDelete = true

// Dialog:
if (showConfirmDelete) {
    AlertDialog(
        onDismissRequest = { showConfirmDelete = false },
        title = { Text("Confirm delete") },
        text = { Text("This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = { viewModel.deleteItem(); showConfirmDelete = false },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = { showConfirmDelete = false }) { Text("Cancel") }
        }
    )
}
```

**Rollback:** `git revert <commit>`

---

### Step 8.2: Replace ExoPlayer Polling Loop with Listener Callbacks

**File:** `ui/screens/swiper/SwiperScreen.kt` → `VideoBottomBar`

```kotlin
// BEFORE (polling 250ms)
LaunchedEffect(Unit) {
    while (true) {
        if (!isSeeking) currentPosition = exoPlayer.currentPosition
        delay(250)
    }
}

// AFTER (event-driven)
DisposableEffect(exoPlayer) {
    val listener = object : Player.Listener {
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (!isSeeking) currentPosition = newPosition.positionMs
        }
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) duration = exoPlayer.duration.coerceAtLeast(1L)
        }
    }
    exoPlayer.addListener(listener)
    onDispose { exoPlayer.removeListener(listener) }
}
// Also update position in the Slider's onValueChange to stay smooth:
LaunchedEffect(exoPlayer) {
    while (isActive) {
        if (!isSeeking) currentPosition = exoPlayer.currentPosition
        delay(500) // Lower frequency, listener handles jumps
    }
}
```

**Why:** Polling wastes CPU. Listeners fire only when position changes.

**Rollback:** `git revert <commit>`

---

### Step 8.3: Consolidate Dialog Styles → Use `AppDialog` Everywhere

**Files with raw `AlertDialog` that should use `AppDialog`:**
- `SwiperScreen.kt` → confirm exit dialog, forget media confirm dialog
- `DuplicatesScreen.kt` → delete confirm, delete-all confirm
- `ContactCleanerScreen.kt` → any confirmation dialogs
- `RecycleBinScreen.kt` → empty bin confirm

**Pattern:**
```kotlin
// BEFORE (raw AlertDialog, inconsistent styling)
AlertDialog(
    onDismissRequest = { ... },
    title = { Text(...) },
    text = { Text(...) },
    confirmButton = { Button(...) { Text(...) } },
    dismissButton = { TextButton(...) { Text(...) } }
)

// AFTER (consistent AppDialog from ui/components/AppDialog.kt)
AppDialog(
    onDismissRequest = { ... },
    title = { Text(...) },
    text = { Text(...) },
    buttons = {
        TextButton(onClick = { ... }) { Text("Cancel") }
        Button(onClick = { ... }) { Text("Confirm") }
    }
)
```

**Why:** `AppDialog` has consistent rounded corners, theming, and optional "Don't ask again" checkbox support.

**Rollback:** `git revert <commit>`

---

### Step 8.4: Add Haptic Feedback to Interactive Elements

**Files:**
| File | Missing Haptic | Fix |
|------|---------------|-----|
| `SwiperScreen.kt` | Folder chip tap | Add `haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)` on `onSelectFolder` |
| `SwiperScreen.kt` | Undo button tap | Add `haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)` on `onUndo` |
| `DuplicatesScreen.kt` | Checkbox toggle | Already has haptic on long-press delete, but missing on individual toggle |

**Pattern:**
```kotlin
val haptic = LocalHapticFeedback.current
// In the clickable/tap handler:
Button(onClick = {
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    viewModel.undoLastAction()
})
```

**Rollback:** `git revert <commit>`

---

### Step 8.5: Add Accessibility `contentDescription` to Interactive Icons

**Scope:** 76 icons currently have `contentDescription = null`. Focus on **interactive** icons only (decorative-only icons are fine as null).

**Priority files:**
| File | Icons needing descriptions |
|------|--------------------------|
| `SwiperScreen.kt` | Checkmark overlay, video badge, star badge, mute/speed buttons |
| `DuplicatesScreen.kt` | Scan type icons, group action icons |
| `SettingsScreen.kt` | Category row arrow, accent color circle |
| `RecycleBinScreen.kt` | Delete/restore icons, category icons |

**Pattern:**
```kotlin
// BEFORE
Icon(Icons.Default.Check, contentDescription = null)

// AFTER
Icon(Icons.Default.Check, contentDescription = stringResource(R.string.item_selected))
```

Add corresponding string resources to `strings.xml`.

**Rollback:** `git revert <commit>`

---

### Step 8.6: Replace `formatPathForDisplay` Duplication

**Files:**
- `ui/screens/settings/SettingsScreen.kt` (line ~1226)
- `ui/screens/session/SessionSetupScreen.kt` (line ~921)

Both contain identical `formatPathForDisplay(path: String): Pair<String, String>` functions.

**Move to:** `util/Formatters.kt`
```kotlin
object Formatters {
    // ... existing fileSize, duration ...

    fun pathDisplay(path: String): Pair<String, String> {
        val file = File(path)
        val name = file.name
        val parentPath = file.parent?.replace("/storage/emulated/0", "") ?: ""
        val displayParent = if (parentPath.length > 30) "...${parentPath.takeLast(27)}" else parentPath
        return Pair(name, displayParent)
    }
}
```

Delete local copies in both files. Update call sites:
```kotlin
// BEFORE
formatListItemTitle = ::formatPathForDisplay

// AFTER
formatListItemTitle = { Formatters.pathDisplay(it) }
```

**Rollback:** `git revert <commit>`

---

## Execution Order Summary

| Phase | Steps | Risk | Impact | Commit Pattern |
|-------|-------|------|--------|----------------|
| **0** | 0.1-0.5 | Low | High | `fix: quick wins` |
| **1** | 1.1-1.6 | Low | Medium | `refactor: DRY preferences and utils` |
| **2** | 2.1-2.4 | Medium | High | `refactor: extract VM managers` |
| **3** | 3.1-3.6 | Low | Medium | `refactor: split SwiperScreen` |
| **4** | 4.1-4.6 | Low | Medium | `refactor: split SettingsScreen` |
| **5** | 5.1-5.2 | Low | Low | `refactor: migrate hidden groups to Room` |
| **6** | 6.1-6.2 | Medium | Medium | `refactor: split DirectMediaRepository` |
| **7** | 7.1-7.4 | Low | High | `test: add unit tests` |
| **8** | 8.1-8.6 | Low | High | `fix: UI/UX polish and accessibility` |

---

## After Refactoring (Estimated State)

```
ui/screens/swiper/
├── SwiperScreen.kt          (~400 lines, was 2800)
├── SwiperViewModel.kt       (~500 lines, was 1400)
├── SwiperUiState.kt          (state classes only)
├── TargetFolderManager.kt   (~150 lines)
├── PendingChangesManager.kt (~100 lines)
├── VideoPlaybackState.kt    (~20 lines)
├── FolderState.kt           (~30 lines)
├── components/
│   ├── MediaItemCard.kt     (~300 lines)
│   ├── VideoPlayer.kt       (~50 lines)
│   ├── AudioPlayerCard.kt   (~80 lines)
│   ├── ThumbnailStrip.kt    (~60 lines)
│   ├── FolderComponents.kt  (~200 lines)
│   ├── SwiperTopBar.kt      (~100 lines)
│   ├── SummarySheet.kt      (~200 lines)
│   └── ItemInfoSheet.kt     (~80 lines)

ui/screens/settings/
├── SettingsScreen.kt         (~200 lines, was 2000+)
├── SettingsViewModel.kt      (unchanged)
├── AppearanceSubPage.kt
├── SortingSubPage.kt
├── BehaviorSubPage.kt
├── DuplicateFinderSubPage.kt
├── MediaStorageSubPage.kt
├── HelpSupportSubPage.kt
├── AboutSubPage.kt
└── components/
    └── SettingsComponents.kt

data/repository/
├── PreferencesRepository.kt  (~300 lines, was 800+)
├── Preference.kt             (new, generic wrapper)
├── FileSystemScanner.kt      (extracted)
├── MediaStoreDataSource.kt   (extracted)
└── DirectMediaRepositoryImpl.kt (~400 lines, was 900+)

util/
├── Formatters.kt             (new, shared formatters)
└── (existing utils unchanged)
```
