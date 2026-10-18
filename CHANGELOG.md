# Changelog

All notable changes to Cleanify are documented here.

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
