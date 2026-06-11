# Cleanify — Feature Roadmap

**Legend:** 🟢 Done  🟡 Working  🔵 Planned  ⚫ Untouched

---

## 📸 Photo Tools

| Feature | Status |
|---------|:------:|
| Swipe-to-sort (keep / delete) | 🟢 |
| Inline photo preview (pinch-to-zoom) | 🟢 |
| Move to folder | 🟢 |
| Share / Open with external app | 🟢 |
| Duplicate photo detection | 🟢 |
| Similar photo detection (dHash + color histogram) | 🟢 |
| Screenshot cleaner (inline card type) | 🟢 |
| Burst shot grouping | ⚫ |
| Blurry photo detection | ⚫ |
| Dark / poor-quality photo detection | ⚫ |
| Meme / WhatsApp image cleaner | ⚫ |
| Old photo finder | ⚫ |
| Photo compression / resize | ⚫ |
| HEIC ↔ JPEG / PNG ↔ WebP conversion | ⚫ |
| Metadata viewer / remover | ⚫ |
| Image editor (crop, rotate, filter) | ⚫ |

---

## 🎬 Video Tools

| Feature | Status |
|---------|:------:|
| Swipe-to-sort (keep / delete) | 🟢 |
| Inline video playback (ExoPlayer) | 🟢 |
| Mute / Unmute | 🟢 |
| Playback speed control | 🟢 |
| Loop toggle | 🟢 |
| Screenshot frame extraction | 🟢 |
| Duplicate video detection (SHA-256) | 🟢 |
| Similar video detection (frame sampling) | 🟢 |
| Large video finder | ⚫ |
| Video compression | ⚫ |
| Video trim / crop | ⚫ |
| Video format conversion | ⚫ |
| Resolution / codec analysis | ⚫ |
| Unwatched video finder | ⚫ |
| Broken / corrupted video detection | ⚫ |

---

## 🎵 Audio Tools

| Feature | Status |
|---------|:------:|
| Swipe-to-sort (keep / delete) | 🟢 |
| Inline audio playback (ExoPlayer) | 🟢 |
| Toggle scan audio files | 🟢 |
| Duplicate audio detection (SHA-256) | 🟢 |
| Voice-note cleaner | ⚫ |
| Unused recordings detector | ⚫ |
| Audio compression | ⚫ |
| Audio format conversion (MP3, FLAC, AAC, OGG, WMA) | ⚫ |
| Audio metadata editor (ID3 tags) | ⚫ |
| Large audio file finder | ⚫ |

---

## 📄 Document Tools

| Feature | Status |
|---------|:------:|
| Swipe-to-sort (keep / delete) | 🟢 |
| Dynamic file icon by MIME type | 🟢 |
| Toggle scan documents | 🟢 |
| Duplicate document detection (SHA-256) | 🟢 |
| Dedicated document management UI | ⚫ |
| Inline document viewer | ⚫ |
| PDF viewer | ⚫ |
| Document content search | ⚫ |
| Old document archive suggestion | ⚫ |
| Large PDF finder | ⚫ |
| Document categorization (auto-sort by type) | ⚫ |
| Batch rename | ⚫ |
| Document compression | ⚫ |

---

## ♻️ Duplicate & Similar Media Finder

| Feature | Status |
|---------|:------:|
| Exact duplicate detection (hash-based) | 🟢 |
| Similar media detection (dHash + histogram) | 🟢 |
| Video similarity (frame sampling) | 🟢 |
| Adjustable similarity thresholds | 🟢 |
| Scan scope filtering (all / include / exclude) | 🟢 |
| Keep Oldest / Keep Newest group actions | 🟢 |
| Delete all exact duplicates | 🟢 |
| Flag as incorrect (permanent denial) | 🟢 |
| Hide group | 🟢 |
| Result persistence (Room cache) | 🟢 |
| Background scanning (foreground service) | 🟢 |
| List / Grid view with pinch-to-zoom | 🟢 |
| Stale results warning with timestamp | 🟢 |
| Unscannable files reporting | 🟢 |
| Auto-clear stale results | ⚫ |
| Scheduled duplicate scans | ⚫ |

---

## 🧠 AI & Smart Features

| Feature | Status |
|---------|:------:|
| dHash perceptual hashing | 🟢 |
| RGB color histogram (64-bin) | 🟢 |
| Storage prediction | ⚫ |
| Smart cleanup recommendations | ⚫ |
| AI duplicate selection (best-photo recommendation) | ⚫ |
| Similar-content clustering | ⚫ |
| One-tap cleanup suggestions | ⚫ |
| AI categorization of files | ⚫ |
| Object / scene recognition (ML) | ⚫ |
| OCR text recognition | ⚫ |
| TensorFlow Lite / ONNX model inference | ⚫ |

---

## 🗂️ File Cleaning Tools (Tools Hub)

| Feature | Status |
|---------|:------:|
| Empty File Cleaner (0-byte files) | 🟢 |
| Empty Folder Cleaner | 🟢 |
| Generic Junk Cleaner (`.tmp`, `.log`) | 🟢 |
| APK File Cleaner (`.apk`, `.apks`, `.apkm`, `.aab`) | 🟢 |
| Corpse File Cleaner (orphaned `Android/data/`) | 🟢 |
| Quick Clean (skip scan UI) | 🟢 |
| Blacklist Editor (regex path exclusion) | 🟢 |
| Whitelist Editor (regex path protection) | 🟢 |
| Auto-whitelist (backup, important, copy) | 🟢 |
| Double-checker (multi-pass scan) | 🟢 |
| Clear Clipboard on clean | 🟢 |
| Stop background apps on clean | 🟢 |
| Export / Import cleaner settings (JSON) | 🟢 |
| Cleaner log (last 100 entries) | 🟢 |
| Scheduled clean via WorkManager | 🟢 |
| Clean on Boot (BootReceiver) | 🟢 |

---

## 📊 Storage Analysis Dashboard

| Feature | Status |
|---------|:------:|
| Total storage used | ⚫ |
| Storage by category (Photos, Videos, Audio, Docs, Apps, Downloads) | ⚫ |
| Visual breakdown (pie / bar charts) | ⚫ |
| Largest files finder | ⚫ |
| Largest folders finder | ⚫ |
| Monthly storage growth tracking | ⚫ |
| Cleanup opportunity suggestions | ⚫ |

---

## 🛡️ Safety & Privacy

| Feature | Status |
|---------|:------:|
| 100% offline (no internet permission) | 🟢 |
| No analytics / tracking SDKs | 🟢 |
| No account required | 🟢 |
| Open source (GPL-3.0) | 🟢 |
| Undo changes (pending changes review) | 🟢 |
| Summary sheet before applying changes | 🟢 |
| Confirm exit dialog | 🟢 |
| "Do not ask again" confirmations | 🟢 |
| Trash bin with recovery (undelete) | ⚫ |
| File preview before deletion | 🟢 |
| Duplicate confidence score display | ⚫ |
| Backup before deletion | ⚫ |
| Protected folders list | 🟢 |
| Protected file types list | 🟢 |
| Encrypted vault | ⚫ |

---

## ⚙️ Productivity Features

| Feature | Status |
|---------|:------:|
| Global file search | 🟢 |
| Category-based filtering | 🟢 |
| Batch delete | 🟢 |
| Batch move to folder | 🟢 |
| Batch rename | ⚫ |
| Favorites protection | 🟢 |
| Advanced filters (date, size, type) | ⚫ |
| Cloud storage integration (Drive, Dropbox) | ⚫ |
| Scheduled / automated cleanup rules | 🟢 |
| File archiver (ZIP / RAR) | ⚫ |
| Tag-based file management | ⚫ |
| NAS integration | ⚫ |
| External drive scanning | ⚫ |

---

## 🎨 Theming & UX

| Feature | Status |
|---------|:------:|
| Theme options (System / Light / Dark / Darker / AMOLED) | 🟢 |
| Dynamic Colors (Material You) | 🟢 |
| Accent color picker (curated palette) | 🟢 |
| Language switching (English, Italian) | 🟢 |
| Custom splash screen with brand logo | 🟢 |
| Compact folder view toggle | 🟢 |
| Folder bar layout (horizontal / vertical) | 🟢 |
| Folder name position (above / below / hidden) | 🟢 |
| Hide filename overlay on cards | 🟢 |
| Full-screen summary sheet | 🟢 |
| Custom launcher icon | 🟢 |
| Session setup folder icons (legacy mode) | 🟢 |

---

## 🚀 MVP Feature Set (v1.0 Goal)

| # | Feature | Status |
|:-:|---------|:------:|
| 1 | Storage Analyzer | ⚫ |
| 2 | Duplicate File Finder | 🟢 |
| 3 | Similar Photo Finder | 🟢 |
| 4 | Large File Scanner | ⚫ |
| 5 | Junk Cleaner | 🟢 |
| 6 | Screenshot Cleaner | 🟢 |
| 7 | Video Cleaner | 🟢 |
| 8 | Batch Delete | 🟢 |
| 9 | Trash Bin Recovery | ⚫ |
| 10 | Smart Cleanup Recommendations | ⚫ |

---

*Last updated: 2026-06-08*
