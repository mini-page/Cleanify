# Changelog

All notable changes to Cleanify are documented here.

## [3.0.0] — 2026-10-18

### 🚀 Major Release Highlights
Cleanify 3.0.0 introduces a complete overhaul of the media and document experience: a unified media playback engine for Audio and Video, native document viewing (PDF, DOCX, PPTX, XLSX, Archives, Text), modern bottom-left format badges, smart mute persistence across sessions, high-resolution PNG branding, and a native multi-alias bug/feedback reporting system.

### 🐛 Bug Fixes
- **Video Mute Logic & Persistence**: Fixed an issue where videos always started muted. The app now preserves your session mute/unmute state across cards and respects the user preference.
- **Video Scrubbing & Duration Bug**: Fixed the video progress bar stuck at 100% white due to uninitialized duration. `VideoBottomBar` now reactively reads duration directly from ExoPlayer and supports smooth scrubbing.
- **Fullscreen Preview for Videos**: Fixed an issue where tapping a video only toggled playback without offering fullscreen viewing. `MediaPreviewDialog` now provides an in-app fullscreen player with timeline scrubbing, speed control, and skip buttons.
- **Accidental Fullscreen on Documents/Media**: Eliminated unwanted fullscreen triggers on non-image cards. Only image cards open fullscreen on card tap; Audio/Video toggle play/pause on tap, while documents require the dedicated top-right fullscreen button.

### ✨ New Features
- **Unified Audio & Video Playback Engine**:
  - Full-card audio playback experience matching video cards.
  - Minimal animated waveform visualizer with extracted album art and ambient blur background.
  - Autoplay in sync with global mute/unmute setting (`isVideoMuted`).
  - Tap card to toggle play/pause and reveal/hide bottom timeline seekbar with timestamps.
- **Modern File Type Badges**:
  - Crisp, frosted format badge in the bottom-left corner of every preview card.
  - Tailored color accents and icons for PDF (Red), Video (Purple), Audio (Cyan), Image (Emerald), Word DOCX (Blue), PowerPoint PPTX (Orange), Excel XLSX (Green), Archives/APK (Amber), and Text/Code (Slate).
  - Automatically animates out when seekbar controls are displayed.
- **Top-Right Dedicated Fullscreen Button**:
  - Universal fullscreen button in the top-right corner across all file types.
- **Native Document & File Engine**:
  - **PDF Preview**: First-page rendering with page count indicator and multi-page full viewer.
  - **DOCX**: Native paragraph extractor with clean reading layout.
  - **PPTX**: Slide text reader with slide counters.
  - **XLSX**: Spreadsheet grid reader with formatted cells.
  - **Archive & APK Inspector**: File tree browser with uncompressed size and embedded APK icon/version extraction.
  - **Code & Text**: Highlighting viewer with monospace formatting and character counts.
- **Native Feedback & Bug Submission**:
  - Built-in Feedback Dialog with category selection routing to dedicated developer aliases:
    - 🐞 Bug Reports: `raghavans5711+Cleanify_bugReport@gmail.com`
    - 💡 Feature Requests: `raghavans5711+Cleanify_featureReq@gmail.com`
    - ✏️ Change Suggestions: `raghavans5711+Cleanify_suggestion@gmail.com`
    - 💬 General Feedback: `raghavans5711+Cleanify@gmail.com`
  - Automatically attaches device diagnostics (OS, Model, Version, ABIs).

### 🎨 Design & Polish
- **Branding Assets**: Upgraded website and documentation logos from WebP to PNG (`logo.png`).
- **Media Controls Polish**: Removed duplicate fullscreen buttons from seekbar and small cards. Seamless layout and lifecycle management for ExoPlayer.

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
