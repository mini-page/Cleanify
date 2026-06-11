# Changelog

All notable changes to Cleanify are documented here.

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
