# Next Up — Build Day

## Priority Order

### 1. New App Icon
- [ ] Create vector/adaptive icon with clean background (replace black)
- [ ] Generate foreground/background `.webp` for all mipmap densities
- [ ] Update `ic_launcher_background.xml` color
- [ ] Keep `ic_launcher.xml` / `ic_launcher_round.xml` adaptive icon config

### 2. Empty Files & Folder Removal
New cleanup categories — find + swipe to clean.
#### Empty Files
- [ ] Scan: find all 0-byte files across selected directories
- [ ] Reuse existing swiper UI to swipe through & delete
- [ ] Show wasted space (sum of zero-byte files = 0 but count shown)

#### Empty Folders
- [ ] Scan: find directories with no files (subdirs only = empty)
- [ ] Show folder path, size (0), count of nested empty dirs
- [ ] Swipe to delete folder(s)

### 3. Expand File Type Scanning — PDF, Audio, Documents
This is the largest change. The onboarding claims support but only images/videos are scanned.

#### Data Layer
- [ ] Add new extensions to scanning:
  - **Audio**: `mp3`, `wav`, `flac`, `aac`, `ogg`, `wma`
  - **Documents**: `pdf`, `doc`, `docx`, `xls`, `xlsx`, `ppt`, `pptx`, `txt`, `rtf`
- [ ] Replace `isMediaFile()` with broader check (or split into multiple accept-lists)
- [ ] Add MediaStore query support for non-image/video types (use `MimeType` filters)

#### Model
- [ ] Replace `isVideo: Boolean` with `FileCategory` enum:
  ```kotlin
  enum class FileCategory {
      Image, Video, Audio, Document, Other
  }
  ```
- [ ] Add `mimeType`, `duration` (for audio), `pageCount` (for PDF — optional)
- [ ] Update `MediaItem` data class

#### Scanning & Duplicates
- [ ] Exact hash matching works out of the box for any file type
- [ ] Extend `SimilarFinderUseCase` to skip non-visual types (audio/docs don't have visual similarity)
- [ ] Show category icon in duplicate groups

#### Swiper UI
- [ ] For audio: show metadata (title, artist, duration, file size)
- [ ] For PDFs: show page count, file size, title
- [ ] For documents: show file name, size, modification date
- [ ] Fallback thumbnail icon per category

### 4. Onboarding Alignment
- [ ] Ensure all 6 onboarding pages match actual features
- [ ] Gate features behind implementation status (or just update text)

## Key Files to Modify

| File | Change |
|------|--------|
| `app/src/main/res/mipmap-*/` | New icon assets |
| `DirectMediaRepositoryImpl.kt` | Extend `supportedImageExtensions`, `supportedVideoExtensions` → combine into `supportedExtensions` or per-category sets; add MediaStore queries for docs/audio |
| `MediaItem.kt` | Add `FileCategory` enum, `mimeType`, update `isVideo` → category |
| `SwiperViewModel.kt` | Handle non-media file display metadata |
| `SwiperScreen.kt` | Category-aware card layout (icon + metadata vs. thumbnail) |
| `DuplicateFinderUseCase.kt` | Already handles all file types (size > 0 filter) |
| `SimilarFinderUseCase.kt` | Skip non-visual categories |
| `DuplicateScanService.kt` | Extend video-only heuristic to cover audio |
| `OnboardingScreen.kt` | Update text if features aren't ready |

## Build & Verify
- [ ] Build: `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"; ./gradlew assembleDebug`
- [ ] Test on device: `adb install -r app/build/outputs/apk/debug/*.apk`
- [ ] Verify PDFs/audio/docs appear in scan results
- [ ] Verify swiper handles all categories
- [ ] Verify empty files/folders scan correctly
