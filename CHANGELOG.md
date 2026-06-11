# Changelog

All notable changes to Cleanify are documented here.

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
