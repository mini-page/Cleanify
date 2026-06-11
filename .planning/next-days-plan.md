# Next Days Plan — Cleanify v2.x+

## Day 1: Rich Previews & App Icon

### App Icon Redesign
- [ ] Design new adaptive icon concept: teal-to-indigo gradient background + white sparkle/spark icon
- [ ] Generate foreground/background `.webp` for all mipmap densities (mdpi–xxxhdpi)
- [ ] Update `ic_launcher_background.xml` with gradient color
- [ ] Keep `ic_launcher.xml` / `ic_launcher_round.xml` adaptive icon config
- [ ] Preview on light + dark launcher to verify contrast

### PDF Preview (Inline)
- [ ] Add `PdfRenderer`-based PDF page preview in swiper (render first page as bitmap)
- [ ] Fallback: if PDF is encrypted/corrupt, show `FileInfoCard` with icon
- [ ] Page navigation buttons (prev/next) for multi-page PDFs
- [ ] Full-screen PDF viewer (zoomable, page indicator)

### Document Thumbnails
- [ ] Use `ThumbnailUtils.createDocumentThumbnail()` for Office docs (Android 12+)
- [ ] Generate Coil-compatible thumbnails for docx/xlsx/pptx
- [ ] Show rich preview card: filename, type icon, size, modified date, page count (PDF)
- [ ] Thumbnail caching in Coil

### Plain Text Preview
- [ ] Inline text viewer for `.txt`/`.rtf` files (scrollable, monospace, first 50 lines)
- [ ] Character/word count badge

### Long-Press Quick Peek
- [ ] Long-press any card in swiper/duplicates → full-screen popup preview (iOS Quick Look style)
- [ ] Dismiss by tapping outside or pressing back
- [ ] Works for images (full-res), PDFs (page), video (thumbnail), audio (album art placeholder)

### Grid Overview Mode
- [ ] "Gallery Grid" button in swiper toolbar → 3-column thumbnail grid of all remaining items
- [ ] Tap a grid cell → jump directly to that card in swiper
- [ ] Category color dot on each thumbnail (blue=video, green=audio, orange=doc, etc.)

### File Detail Panel
- [ ] Swipe UP on any card → drawer slides up with full metadata:
  - Image: dimensions, resolution, Exif (GPS map link, camera model, aperture, ISO, date taken)
  - Video: resolution, codec, bitrate, frame rate, duration
  - Audio: title, artist, album, genre, track number, sample rate
  - Document: pages, author, creator, PDF version
- [ ] "Open in Maps" button if GPS coords present
- [ ] Copy-to-clipboard for any field

## Day 2: Better Video & Audio Player

### Video Improvements
- [ ] Gesture controls: swipe left/right = seek, swipe up/down left edge = brightness, right edge = volume
- [ ] Gesture hints overlay (first-time use tooltip)
- [ ] Double-tap left/right to seek back/forward 10s
- [ ] Picture-in-Picture (PIP) mode on home press
- [ ] Video info overlay: resolution, codec, bitrate, frame rate
- [ ] Keep existing: mute, playback speed, screenshot

### Audio Improvements
- [ ] Mini-player bar pinned to bottom when returning from full player
- [ ] Playlist queue (add multiple audio items from scan results)
- [ ] Shuffle/repeat mode for audio queue
- [ ] Audio visualization (simple FFT bars using Visualizer API)
- [ ] Seek via tap on progress bar (not just drag)
- [ ] Notification media controls (MediaSession + media style notification)
- [ ] Album art extraction from tags (cover art in ID3 metadata)
- [ ] Now-playing lock screen controls

### Side-by-Side Compare (Similar Images)
- [ ] Side-by-side layout for duplicate image pairs (sync zoom + pan)
- [ ] Overlay toggle: draggable divider to reveal one image over the other
- [ ] "Keep Left" / "Keep Right" quick action buttons
- [ ] Comparison grid: select 2–4 items to compare simultaneously

### Shared Player Refactor
- [ ] Extract `PlayerController` class shared between video and audio
- [ ] Unify play/pause/seek/position/speed state management
- [ ] Fix: ExoPlayer should not hold reference to TextureView when playing audio (avoid video decode)

## Day 3: Core Tool Upgrades

### Safety Net — Undo Trash
- [ ] Move deleted files to `.CleanifyTrash/` on device instead of permanent delete
- [ ] Trash screen: list trashed files with restore / permanently delete / empty-all actions
- [ ] Auto-purge trash after 30 days (WorkManager `PeriodicWorkRequest`)
- [ ] Settings toggle: "Use Trash" (default ON) / "Permanent Delete"
- [ ] Trash size indicator in Tools screen
- [ ] Exclude trash from scans

### Empty Files & Folder Removal (from original plan)
- [ ] Scan: find all 0-byte files across selected directories
- [ ] Scan: find empty directories (no files, subdirs only)
- [ ] Show in swiper UI with "Empty File" / "Empty Folder" badge
- [ ] Reuse existing swipe-to-delete flow
- [ ] Track count + zero space reclaimed messaging

### File Type Converter
- [ ] Convert PNG → WEBP (lossless with resize options)
- [ ] Extract audio track from video (save as MP3/M4A)
- [ ] Compress PDF (reduce quality, strip metadata)
- [ ] Batch convert selected items in duplicates/tools screen
- [ ] Progress indicator + output size comparison

### Onboarding Alignment
- [ ] Update 6 onboarding pages to match all current features
- [ ] Add page for audio/document scanning
- [ ] Add page for empty cleaner tool
- [ ] Add page for storage visualization and trash
- [ ] Gate features behind implementation flags

## Day 4: Power User Features

### Batch Multi-Select Mode in Swiper
- [ ] Long-press item → enter multi-select mode with checkboxes on cards
- [ ] Select all / invert selection / clear buttons
- [ ] Batch actions: delete all selected / move to folder / share / open
- [ ] Batch count badge ("12 selected")
- [ ] Works in duplicates grid view too

### Storage Visualization
- [ ] Pie chart / donut chart of storage by category (Images, Video, Audio, Documents, Other)
- [ ] Per-folder breakdown (largest folders ranked)
- [ ] File type distribution (extensions bar chart)
- [ ] Time-based view: files grouped by month/year (find old clutter)
- [ ] Interactive: tap a slice → drill into that category's files
- [ ] Cache the calculated stats; refresh on scan complete

### Smart Search
- [ ] Global search bar accessible from any screen (toolbar icon → slide-down search panel)
- [ ] Search modes: filename, extension, size range (`>100MB`), date range, folder path
- [ ] Search history (last 10 queries, stored in DataStore)
- [ ] Results show file card with category icon, size, date
- [ ] Bulk actions on search results (delete selected, move all)
- [ ] Real-time filtering as user types (debounced 300ms)

### Metadata Viewer Tool
- [ ] Dedicated "Inspector" tool in Tools screen
- [ ] Pick any file from device → view all metadata
- [ ] Categories:
  - **Image**: Exif tags (GPS, camera, aperture, ISO, flash, focal length, date)
  - **Video**: MediaMetadataRetriever (resolution, rotation, codec, bitrate, frame rate)
  - **Audio**: MediaMetadataRetriever + tag libraries (title, artist, album, genre, track, year, cover art)
  - **Document**: PdfDocument (page count, title, author), basic file stats
- [ ] Copy any field, share as text, open GPS in Maps, view raw hex dump option

## Day 5: Integration & Polish

### Share Extension
- [ ] Add `ACTION_SEND` / `ACTION_SEND_MULTIPLE` intent filter in manifest
- [ ] Receive images, videos, audio, docs from Gallery, Files, Camera, browser
- [ ] Quick-sort received files: open directly into swiper with received items
- [ ] Return sorted/converted files to calling app if possible
- [ ] "Send to Cleanify" option appears in system share sheet

### Scheduled Scans
- [ ] WorkManager periodic scan (daily / weekly / monthly option)
- [ ] Notification with results: "Found 1.2 GB of duplicates in Downloads"
- [ ] Settings UI: schedule picker, last scan time, next scan time
- [ ] Respect Doze / battery saver (use WorkManager constraints)

### Export Reports
- [ ] Export scan results as CSV / JSON / HTML
- [ ] Include: file name, full path, size, category, MIME type, duplicate status, deletion action, timestamp
- [ ] Preview report before exporting
- [ ] Share exported file via system share sheet
- [ ] Auto-open after export

### Before/After Slider Enhancement
- [ ] Revisit similar-image comparison with drag divider
- [ ] Hold finger and slide left/right to reveal one image vs the other
- [ ] Works on any pair of similar images in duplicates

## Future / Nice-to-Have

- [ ] **Android 14+ Photo Picker**: Granular media access without full storage permission
- [ ] **Cloud backup detector**: Find duplicates between local and Google Photos / OneDrive (requires OAuth)
- [ ] **Split APK / App Bundle**: Smaller per-architecture downloads on Google Play
- [ ] **Home screen widget**: 1×1 "Quick Clean" button, 2×1 summary with space reclaimed
- [ ] **Accessibility**: TalkBack content descriptions on all interactive elements, keyboard navigation
- [ ] **UI tests**: Compose UI testing for swiper gestures, scan flow, settings navigation
- [ ] **Per-ABI release builds**: Auto-build + attach all 4 ABIs to GitHub Releases via CI

## Versioning Convention
```
Major.Minor.Patch
Major = big feature releases (new scan types, overhauled UI)
Minor = feature additions (document preview, batch select, tools)
Patch = bug fixes, small tweaks, translation updates
```

---

## Appendix A: External Storage (SD Card / USB OTG) Integration

### Current Limitation
- The app uses `MediaStore` (internal + MediaStore volumes) and direct filesystem walk on accessible paths
- External SD cards and USB OTG drives are **not** accessible via MediaStore after Android 11
- `MANAGE_EXTERNAL_STORAGE` covers shared storage but **not** secondary volumes (SD cards)

### Integration Options

#### Option A: SAF Tree (DocumentFile) — **Recommended**
- Request user to pick SD card root via `Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)`
- Persist read/write URI permission via `contentResolver.takePersistableUriPermission()`
- Use `DocumentFile` to walk the tree and enumerate files
- Pro: no special permission, works on all Android versions, user consciously grants access
- Con: slower than direct filesystem, `DocumentFile` has overhead
- Implementation:
  - Add SD card picker in SessionSetupScreen ("Add external storage")
  - Store persisted URIs in DataStore
  - `DirectMediaRepositoryImpl` checks persisted URIs and walks them alongside MediaStore

#### Option B: Root Access (if device is rooted)
- Execute `find` / `stat` commands via `su` on external mount points
- Parse output to build file list
- Pro: full speed, no permission dialogs
- Con: only works on rooted devices, security risk

#### Option C: MANAGE_EXTERNAL_STORAGE
- Already covers `/storage/emulated/0/`
- Does **not** include `/storage/XXXX-XXXX/` (SD card) on Android 11+
- Combine with SAF tree for SD card specifically

### Files to Modify
- `DirectMediaRepositoryImpl.kt`: add SAF tree walk method, merge results with MediaStore
- `SessionSetupScreen.kt`: "Add External Storage" button, persisted URI list
- `PreferencesRepository.kt`: store list of SD card tree URIs
- `MediaRepository.kt` (interface): add `getExternalRoots(): Flow<List<Uri>>`

---

## Appendix B: Release Build Keys & Signing

### What's Needed
| Property | What it is | How to obtain |
|----------|-----------|---------------|
| `CLEANIFY_RELEASE_STORE_FILE` | Path to `.jks` keystore | `keytool -genkey` |
| `CLEANIFY_RELEASE_KEY_ALIAS` | Alias for the key | Set during keystore creation |
| `CLEANIFY_RELEASE_STORE_PASSWORD` | Keystore password | Set during keystore creation |
| `CLEANIFY_RELEASE_KEY_PASSWORD` | Key password | Set during keystore creation |

### Generate a Keystore
```bash
keytool -genkey -v -keystore release-key.jks \
  -keyalg RSA -keysize 4096 -validity 9125 \
  -alias cleanify-release-key
```

### Configure the Build

**`gradle.properties`** (store outside repo, add to `.gitignore`):
```properties
CLEANIFY_RELEASE_STORE_FILE=C:/path/to/release-key.jks
CLEANIFY_RELEASE_KEY_ALIAS=cleanify-release-key
CLEANIFY_RELEASE_STORE_PASSWORD=your-password
CLEANIFY_RELEASE_KEY_PASSWORD=your-key-password
```

### Update `build.gradle.kts` Signing
```kotlin
create("release") {
    val storeFileProp = project.properties["CLEANIFY_RELEASE_STORE_FILE"] as? String
    val keyAliasProp = project.properties["CLEANIFY_RELEASE_KEY_ALIAS"] as? String
    val storePassProp = project.properties["CLEANIFY_RELEASE_STORE_PASSWORD"] as? String
    val keyPassProp = project.properties["CLEANIFY_RELEASE_KEY_PASSWORD"] as? String
    if (storeFileProp != null && keyAliasProp != null && storePassProp != null && keyPassProp != null) {
        storeFile = file(storeFileProp)
        storePassword = storePassProp
        keyAlias = keyAliasProp
        keyPassword = keyPassProp
    }
}
```

### Safety Rules
- **NEVER** commit `.jks` or passwords to git
- Add `release-key.jks` and `local.properties` to `.gitignore`
- For CI (GitHub Actions): store in GitHub Secrets, write to `gradle.properties` at build time