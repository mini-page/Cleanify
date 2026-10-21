# Changelog

All notable changes to Cleanify are documented here.

## [3.2.0] — 2026-10-21

### 🚀 Release Highlights
Cleanify **v3.2.0** brings a monumental modernization and performance upgrade: complete **Storage Engine Overhaul** with Quarry-inspired deep scanning, an interactive **Hardware-Accelerated Squarified Treemap** with instant in-app file preview, **Swipeable Storage Tabs** with a floating auto-hiding icon dock (YouTube-style nested scroll), **Full-Spectrum Search Indexing** for all Cleaner Settings with a quick top-bar search toggle, ultra-fluid **High-Performance Swiper Mechanics** for massive files, and a streamlined core toolset with the removal of redundant contacts management.

---

### 📦 Storage Engine Overhaul & Quarry Integration
- **Quarry Storage Architecture**: Deep, multi-threaded storage candidate analysis across internal and external volumes, surfacing exact reclaimable capacity.
- **Cleanup Hub Dashboard**: Hero overview card displaying total recoverable space and targeted candidate modules:
  - *Large Files (>50 MB)*: Instant modal sheet review and batch management.
  - *Superfluous APKs*: Scans older and cached APK/APKS/APKM/XAPK installation packages with direct swipe-to-clean flow.
  - *Screenshots*: Dedicated screenshot detection with one-tap swiper triage.
  - *Empty Folders*: Deep directory traversal discovering orphan folders with direct integration into Empty Cleaner.
  - *Duplicates Arbiter*: Direct shortcut to the high-performance media deduplicator.
  - *Installed App Storage Inspector*: Comprehensive breakdown of app binaries, user storage, and cache footprints.
- **Interactive Squarified Treemap**:
  - Visual hierarchy mapped to relative disk footprint with intuitive color coding.
  - Hardware-accelerated canvas supporting pinch-to-zoom (up to 5x), single-finger pan when zoomed, and double-tap zoom reset.
  - Deep folder drill-down with persistent path breadcrumbs.
  - **In-App File Details & Internal Preview**: Tapping any file leaf block brings up full file metadata (name, size, path, date modified, extension type) with Open With, Share, Delete, and a direct internal preview viewer (`MediaPreviewDialog`) supporting images, videos, audio, text, and PDF files.
- **Storage Breakdown View**: Volume usage cards, interactive category storage donut chart, and extension distribution (Images, Videos, Music, Documents, APKs, Downloads, Other).
- **Floating Auto-Hiding Bottom Nav Bar**:
  - Replaced top tab bars with fluid horizontal swipe switching (`HorizontalPager`) between *Cleanup Hub*, *Treemap*, and *Breakdown*.
  - Frosted floating pill dock at bottom center with clean icons and active pill indicators.
  - **YouTube-Style Nested Scroll**: Smoothly slides down and hides when scrolling down through lists; automatically slides back up into view with a slight upward swipe.
  - Extra bottom content padding ensuring no list items or buttons are ever hidden behind the dock.

---

### 🔍 Cleaner Settings Search & Top-Bar Toggle
- **Full Settings Search Indexing**: All individual Cleaner preferences (*Empty Files, Empty Folders, Generic Junk, APK Packages, Corpse Files, Auto-whitelist, Clipboard, Scheduled Clean, Double-checker multi-run, Blacklist, Whitelist*) are now indexed in the main Settings search. Searching for any keyword instantly displays direct setting results with one-tap deep navigation.
- **Search Action Toggle**: Added a search icon button in the top app bar of Cleaner Settings that smoothly expands an in-page search bar (`CleanifySearchBar`), automatically requests focus, hides the keyboard on search action, and respects back button navigation.

---

### ⚡ Performance Swiping & Gesture Enhancements
- **Fluid High-Performance Swiper**: Optimized gesture handling and view transitions inspired by SD Maid swipe mechanics for effortless sorting.
- **Heavy Media Deletion Speed**: Asynchronous, non-blocking file deletion handling large video files (>200MB) without UI stutter.
- **Visual Swipe Feedback**: Added dark overlays with distinct action badges (Green *Keep*, Red *Delete*, Slate Gray *Skip*) that dynamically preview as cards are dragged.

---

### 🧹 Streamlining & Architecture Cleanups
- **Removed Contacts Tool**: Deprecated the Contacts cleaner to focus Cleanify strictly on high-performance storage, media management, and junk cleaning.
- **Dedicated Storage Settings**: Created a standalone Storage configuration section in app settings, accessible directly from the Storage top bar and Settings index.
- **Android 15 / 16 Target Ready**: Refined edge-to-edge layouts, window insets, and gesture exclusions for Android 15 (API 35) and Android 16 (API 36).

---

## [3.1.0] — 2026-10-20

### 🚀 Release Highlights
Cleanify **v3.1.0** is a massive milestone update packed with core workflow enhancements and refined interactions: a **Unified Home Screen** with inline tool switching, **Batch Chunk Swiping** with persistent session resume, **4-Way Smart Gestures** with customizable swipe actions and dedicated favorites, **Universal Settings Search** with instant search toggle and viewport auto-scrolling, a **Quick App Tour** interactive walkthrough, direct **Settings shortcut** from the Feedback modal, customizable **Recycle Bin vs Direct Deletion** toggle, modernized **Support & Donations** hub with native UPI deep-linking, and critical stability fixes for Contact Cleaner and Settings navigation.

---

### 🎛️ Unified Home Screen & Inline Tool Switching
- **Inline Tool Rendering**: Selecting any tool pill (*Folders, Empty Cleaner, Duplicates, Contacts, Storage, Recycle Bin*) immediately renders that tool's full interface inside the home screen body without pushing a separate full-screen page.
- **Persistent Tool Strip**: Tool navigation pills remain accessible at all times at the top of the home view for frictionless multitasking.
- **Dynamic Context Actions**: Top app bar dynamically adapts its quick action shortcuts to the active tool context while maintaining one-tap access to Settings.

---

### 🔍 Universal Settings Search & Auto-Scroll
- **Instant Search Toggle**: Paired search action button allows quickly toggling the search bar directly beneath the top settings header across all sub-pages.
- **One-Tap Direct Jump**: Tapping a search result navigates instantly to the target settings section, automatically scrolls the viewport directly to the item (`BringIntoViewRequester`), and highlights the setting with a subtle visual pulse.
- **Comprehensive Indexing**: Full search coverage across all settings, including gesture controls, batch sizes, update intervals, storage toggles, and appearance preferences.
- **Unified Section Styling**: Standardized all settings sections into consistent Material 3 elevated card containers with uniform headers, padding, and accent tokens.

---

### 🔄 Batch Chunk Swiping & Session Resume
- **Customizable Batch Sizes**: Choose between **10, 20, 30, 40, or 50** items per swiping session (default: 30 items) under *Settings → Sorting*.
- **Review Sheet at Batch End**: After finishing a chunk, Cleanify presents a summary review sheet allowing you to confirm kept/deleted media before proceeding to the next batch.
- **Persistent Session State**: If you exit the app or navigate away mid-session, progress is saved automatically.
- **One-Tap Resume Card**: A prominent frosted **"Resume Swiping Session"** card appears at the top of the folder list showing pending items and allowing you to pick up exactly where you left off.

---

### 👆 4-Way Smart Gestures & Controls
- **Interchangeable Left & Right Swipes**: Swipe Left and Swipe Right are linked to *Keep* and *Delete*; selecting an action for one automatically pairs the opposite action with the other.
- **Customizable Swipe Up**: Configurable to **None**, **Skip**, or **Favorite** (moves the item to a dedicated `Favorites` directory and marks it as a favorite).
- **Customizable Swipe Down**: Configurable to **None**, **Skip**, or **Move** to an edit folder.
- **Single & Double Tap Actions**: Direct tap shortcuts on media cards for quick preview, zoom, or pause.

---

### 🗑️ Recycle Bin vs Direct Deletion Control
- **Recycle Bin Switch**: New toggle under *Settings → Sorting* allowing users to choose between moving items to Cleanify's Recycle Bin or permanently deleting them immediately.
- **Adaptive Home Pills**: Disabling the Recycle Bin automatically removes the Recycle Bin pill from the home screen tool strip to keep the interface clean and relevant.

---

### 💬 Interactive Feedback Hub & Instant Triggers
- **Shake & Screenshot Detection**:
  - Shake phone anywhere in the app to immediately summon the support dialog (`ShakeDetector` with accelerometer filtering).
  - Capturing a device screenshot automatically prompts the feedback dialog on Android 14+ and via MediaStore observation.
- **Feedback Dialog Settings Shortcut**: Top-right settings button in the feedback modal instantly jumps to *Settings → Help & Support* and flashes the Shake & Screenshot trigger toggles for instant configuration.
- **Multi-Screenshot & Media Attachments**: Attach bug screenshots or logs directly from the gallery with image previews and one-tap removal.
- **Privacy & System Diagnostics**: Automated device diagnostics (OS, brand, model, screen resolution, RAM, CPU) with a clear user privacy toggle.
- **Multi-Channel Dispatch**: Direct dispatch through WhatsApp, Telegram (@ug_5711), or categorized Email aliases.

---

### 🧭 Interactive Quick App Tour
- **5-Slide Guided Walkthrough**: Discover Cleanify's most powerful capabilities with interactive visual slides (*Unified Home Tools, Batch Swiping, 4-Way Gestures, Shake & Screenshot Feedback, Universal Search*).
- **On-Demand Access**: Accessible anytime via *Settings → Help & Support → Quick App Tour*.

---

### 💳 Redesigned Support & Payment Hub
- **Google Pay UPI Integration**: One-tap copy for developer UPI ID (`raghavans5711@okicici`) with animated visual toast confirmation.
- **Installed Payment Apps Detection**: Dynamically detects installed UPI payment applications (Google Pay, PhonePe, Paytm, BHIM, Cred, Amazon Pay) and renders them as clean interactive rows with official icons.
- **Direct Deep-Link Launch**: Tapping an installed payment app launches its native payment flow with pre-filled UPI transaction parameters.
- **De-cluttered Interface**: Removed deprecated international and cryptocurrency options in favor of fast, reliable direct support.

---

### 📊 Storage Analysis & File Management
- **Storage Health Banner**: Real-time evaluation of device storage status (*Healthy, Moderate, Critical*) with actionable recommendations.
- **Quick Cleanup Shortcuts**: Direct shortcuts to Empty Folders cleaner, Duplicate Media scanner, Recycle Bin, and Contact Cleaner.
- **File Details & Actions**: Tap any large file to inspect details, open via secure FileProvider, share, or delete with confirmation.

---

### 🐛 Bug Fixes & Stability
- **Contact Cleaner Permission Crash**: Resolved a fatal `SecurityException` on Android 10+ where `ContactCleanerViewModel` registered `ContentObserver` before runtime permissions were granted. Observation is safely deferred until permission is granted.
- **Settings Navigation Scroll Reset**: Fixed an issue where exiting settings sub-pages reset scroll position back to the top.
- **Cleaner Settings Routing**: Fixed an issue where cleaner settings showed empty screens due to improper route parameter matching.
- **Recycle Bin Media Preview**: Fixed missing parameters in `MediaPreviewDialog` invocation from the recycle bin preview dialog.
- **Video Scrubbing & Mute Persistence**: Fixed progress bar stuck at 100% white due to uninitialized duration; preserved session mute/unmute state across media cards.


## [3.0.0] — 2026-10-18

### 🚀 Major Release Highlights
Cleanify 3.0.0 is our biggest update yet, introducing a complete overhaul of the media and document experience: a unified media playback engine for Audio and Video, native document viewing (PDF, DOCX, PPTX, XLSX, Archives, Text, Markdown), thumbnail extraction across the app, top bar tool chips with animated search reveal, automated background update checks with Android notifications, expanded accent colors, fluid settings page transitions, and de-cluttered settings sections.

### 🐛 Bug Fixes
- **Video Mute Logic & Persistence**: Fixed an issue where videos always started muted. The app now preserves your session mute/unmute state across cards and respects user preference.
- **Video Scrubbing & Duration Bug**: Fixed the video progress bar stuck at 100% white due to uninitialized duration. `VideoBottomBar` now reactively reads duration directly from ExoPlayer and supports smooth scrubbing.
- **Swipe-Down Action Guard**: Fixed a bug where swiping down animated the card off-screen without any action when `swipeDownAction` was configured to `NONE`.
- **Fullscreen Preview for Videos & Documents**: Fixed an issue where tapping a video only toggled playback without offering fullscreen viewing. `MediaPreviewDialog` now provides an in-app fullscreen player with timeline scrubbing, speed control, and skip buttons.
- **Accidental Fullscreen on Documents/Media**: Eliminated unwanted fullscreen triggers on non-image cards. Only image cards open fullscreen on card tap; Audio/Video toggle play/pause on tap, while documents require the dedicated top-right fullscreen button.
- **Settings Dropdown Alignment**: Fixed dropdown menus across Settings being cramped against the right screen edge. Enhanced with generous 250–340dp width, rounded corners, and proper margins.

### ✨ New Features
- **Unified Audio & Video Playback Engine**:
  - Full-card audio playback experience matching video cards with waveform visualizer and album artwork.
  - Minimal animated waveform visualizer with extracted album art and ambient blur background.
  - Autoplay in sync with global mute/unmute setting (`isVideoMuted`).
  - Modern center control deck: Skip Previous, Rewind 5s, Circular Play/Pause, Fast-Forward 10s, Skip Next.
  - Tap card to toggle play/pause and reveal/hide bottom timeline seekbar with timestamps.
  - 24dp rounded video playback settings modal sheet for speed (0.5x–2.0x), fit modes (Fit, Crop, Stretch), loop, and mute.
- **Thumbnail Extraction in Top Preview Strip**:
  - Automatically extracts and displays thumbnails for audio album artwork, video first frames, PDF page 0, and APK app icons.
- **Modern File Type Badges**:
  - Crisp, frosted format badge in the bottom-left corner of every preview card and thumbnail.
  - Tailored color accents and icons for PDF (Red), Video (Purple), Audio (Cyan), Image (Emerald), Word DOCX (Blue), PowerPoint PPTX (Orange), Excel XLSX (Green), Archives/APK (Amber), and Text/Code (Slate).
  - Automatically animates out when seekbar controls are displayed.
- **Native Document, File & Markdown Engine**:
  - **Markdown Viewer**: Native Jetpack Compose Markdown parser and renderer (`MarkdownViewer`) for `.md` files in viewer and update changelog dialog.
  - **PDF Preview**: First-page rendering with page count indicator and multi-page full viewer.
  - **DOCX**: Native paragraph extractor with clean reading layout.
  - **PPTX**: Slide text reader with slide counters.
  - **XLSX**: Spreadsheet grid reader with formatted cells.
  - **Archive & APK Inspector**: File tree browser with uncompressed size and embedded APK icon/version extraction.
  - **Code & Text**: Highlighting viewer with monospace formatting and character counts.
- **Home Screen Tool Chips & Animated Search Bar**:
  - Horizontal quick-access chip strip on home screen (Folders, Empty Cleaner, Duplicates, Contacts, Storage, Recycle Bin, All Tools).
  - Paired `[ 🔍 Search | ⚙️ Settings ]` top-bar action with smooth expand/fade reveal animation for the search input.
- **Automated Background Update Checker & Notifications**:
  - WorkManager periodic update worker (`AppUpdateWorker`) with user-configurable intervals: Never, Daily, Every 2 days, 3 days, 5 days, or Weekly (7 days).
  - Pushes an actionable Android system notification with release title and changelog snippet when a newer GitHub release is detected.
- **Expanded Accent Colors & Fluid Transitions**:
  - Added 6 new vibrant Material accent colors: Electric Violet, Cyan Neon, Emerald Mint, Rose Coral, Deep Indigo, and Lime Zest.
  - Integrated `AnimatedContent` slide & fade animations when transitioning between settings categories.
- **Reorganized Help & Support and About Sections**:
  - De-cluttered into modern Material 3 grouped cards: App Identity, Update Management with frequency selector, Core Features, Community & Social links, Data Backup & Reset, and Permissions Health status indicator.
  - Removed duplicate feedback entry points.
- **Native Multi-Alias Feedback & Bug Submission**:
  - Built-in Feedback Dialog with category selection routing to dedicated developer aliases:
    - 🐞 Bug Reports: `raghavans5711+Cleanify_bugReport@gmail.com`
    - 💡 Feature Requests: `raghavans5711+Cleanify_featureReq@gmail.com`
    - ✏️ Change Suggestions: `raghavans5711+Cleanify_suggestion@gmail.com`
    - 💬 General Feedback: `raghavans5711+Cleanify@gmail.com`
  - Automatically attaches device diagnostics (OS, Model, Version, ABIs).

### 🎨 Design & Polish
- **Branding Assets**: Upgraded website and documentation logos from WebP to high-res PNG (`logo.png`).
- **Media Controls Polish**: Seamless layout and lifecycle management for ExoPlayer across viewports.

## [2.7.0] — 2026-06-11

### Added
- **Immersive Media Preview** — full-screen viewer with pinch-to-zoom, double-tap
  reset, minimal close/delete overlays. Accessible from Swiper Screen, Duplicate
  Finder, and Recycle Bin.
- **Storage Analysis** — new tool showing donut chart (used/free %), per-volume
  cards, category breakdown bars (Images, Videos, Music, Documents, APK, Downloads),
  and top-10 largest files with tap-to-detail info dialog.
- **Multi-Volume Empty Cleaner** — dynamic `FilterChip` selector when external
  storage is detected: scan All, Internal, or SD Card individually.
- **Recycle Bin rewrite** — select-all/deselect-all toggle, always-visible tabs,
  media preview for image/video files, per-category filtering, confirm dialogs
  for delete/empty operations.
- **Accessibility features** — Reduce Animations (snap transitions), Hide from
  Gallery (.nomedia, default ON), haptic feedback on destructive actions, full
  TalkBack content descriptions on all interactive elements.
- **App Shortcuts** — 4 static shortcuts with deep links (Empty Cleaner, Contact
  Cleaner, Duplicates Graph, Recycle Bin).
- **Quick Settings tile** — one-tap app launch from notification shade.
- **Home Screen Widget** — redesigned with rounded cards, purple-tinted buttons,
  emoji icons, and three actions: Quick Clean, Rescan, Open Recycle Bin.
- **External storage support** — cross-volume file moves (copy+delete fallback),
  multi-root scanning in all cleaner tools via StorageVolumeProvider.

### Changed
- All icon references updated to `AutoMirrored` variants for RTL support.
- Hide from Gallery description improved and default changed to `true`.
- Empty cleaner `multiRun` default increased to 2 for more thorough cleanup.
- `getScanRoots()` always includes primary storage first, then secondary volumes.
- Gradle JVM args increased to 6144m for R8 OOM mitigation.

### Fixed
- Empty cleaner now processes files in reverse depth order (deepest first) so
  children are deleted before parent directories are checked.

## [2.6.0] — 2026-06-11

### Added
- **Native xxHash64 hashing engine** — SHA-256 replaced with xxHash64 via C/NDK
  for exact duplicate detection. Uses `mmap()` for zero-copy file access, delivering
  ~10x faster hash computation on bulk media scanning.
- **CMake + NDK integration** — native code compiled for all target architectures.
- **`NativeHasher` fallback** — pure-Kotlin `hashBytes()` fallback if native
  library fails to load, ensuring no functionality regression.
- **Release keystore** — generated at `app/release.jks` for signed release builds.

### Changed
- **Duplicate scanning** — `DuplicateFinderUseCase` now calls native xxHash64
  for full file hashes, partial video hashes, and pixel hashes instead of
  `MessageDigest("SHA-256")`.

## [2.5.0] — 2026-06-11

### Added
- **Haptic feedback** on destructive actions: swipe left/right/down in the media
  swiper, delete in Contact Cleaner, Duplicate Finder, Recycle Bin, Empty Cleaner,
  and "Apply Changes" in the Summary Sheet. Provides tactile confirmation for
  all irreversible operations.
- **In-app update checker** (Settings → About → Updates). Checks
  `github.com/mini-page/Cleanify/releases/latest`, shows changelog dialog,
  downloads APK with progress bar, and launches the system package installer.
