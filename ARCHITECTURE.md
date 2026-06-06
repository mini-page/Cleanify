# Cleanify — Architecture & Memory

## Overview

Cleanify is an Android app (Kotlin/Jetpack Compose) for organizing photos/videos via Tinder-like swiping, plus a duplicate media finder. Fully offline, no data leaves the device.

---

## Directory Tree (Source)

```
Cleanify/
├── app/
│   ├── build.gradle.kts              # App module build: deps, namespace, signing
│   ├── proguard-rules.pro
│   ├── lint-baseline.xml
│   ├── schemas/                       # Room DB schema exports (auto-generated)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       │   ├── values/
│       │   │   ├── strings.xml        # EN strings (605 lines)
│       │   │   ├── colors.xml          # splash_background colors
│       │   │   ├── themes.xml          # Theme.Cleanify style
│       │   │   └── ic_launcher_background.xml
│       │   ├── values-night/colors.xml
│       │   ├── values-it/strings.xml  # Italian translations (638 lines)
│       │   ├── mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/
│       │   │   ├── ic_launcher.webp
│       │   │   ├── ic_launcher_round.webp
│       │   │   └── ic_launcher_foreground.webp
│       │   ├── mipmap-anydpi/
│       │   │   ├── ic_launcher.xml        # Adaptive icon (background + foreground)
│       │   │   └── ic_launcher_round.xml  # Adaptive round icon
│       │   ├── drawable/
│       │   │   ├── ic_duplicates_scan.xml  # Notification icon (vector)
│       │   │   └── ic_cancel.xml           # Notification action (vector)
│       │   └── xml/ (backup_rules, data_extraction_rules, file_paths)
│       └── java/com/cleanify/
│           ├── CleanifyApp.kt                # Application class, ImageLoader factory
│           ├── data/
│           │   ├── db/
│           │   │   ├── CleanifyDatabase.kt    # Room DB (v3, 7 entities)
│           │   │   ├── dao/ (6 DAOs)
│           │   │   ├── entity/ (7 entities)
│           │   │   └── converter/Converters.kt
│           │   ├── model/MediaItem.kt
│           │   └── repository/
│           │       ├── DirectMediaRepositoryImpl.kt
│           │       └── PreferencesRepository.kt
│           ├── di/
│           │   ├── AppModule.kt             # Hilt singletons (ImageLoader, WorkManager, etc.)
│           │   ├── DatabaseModule.kt        # Room DB & DAO providers
│           │   └── RepositoryModule.kt      # MediaRepository binding
│           ├── domain/
│           │   ├── bus/ (3 event buses: FileModification, FolderUpdate, AppLifeCycle)
│           │   ├── model/ (ScanResultGroup, FolderDetails, IndexingStatus)
│           │   ├── repository/ (MediaRepository, DuplicatesRepository)
│           │   ├── usecase/ (DuplicateFinderUseCase, SimilarFinderUseCase, DuplicateGroup)
│           │   └── util/PHashUtil.kt        # Perceptual hashing for similar image detection
│           ├── service/
│           │   ├── DuplicateScanService.kt   # Foreground service for scanning
│           │   └── DuplicateScanStateHolder.kt
│           ├── ui/
│           │   ├── BaseActivity.kt          # Hilt abstract base
│           │   ├── MainActivity.kt          # Splash Screen, locale, theme
│           │   ├── MainApp.kt               # Root composable: nav, permissions
│           │   ├── MainViewModel.kt         # Theme, locale, onboarding state
│           │   ├── components/ (7 shared composables)
│           │   ├── navigation/AppNavigation.kt  # NavHost with deep links
│           │   ├── screens/
│           │   │   ├── onboarding/          # 6-page tutorial
│           │   │   ├── session/             # Folder selection for sorting
│           │   │   ├── swiper/              # Tinder-style swipe sorting
│           │   │   ├── duplicates/          # Duplicate finder + group view
│           │   │   ├── settings/            # Full settings screen
│           │   │   └── osslicenses/         # Open source licenses
│           │   └── theme/ (Theme.kt, AccentColors.kt, Typography.kt)
│           ├── util/ (8 utility classes)
│           └── work/ProactiveIndexingWorker.kt  # WorkManager background job
├── build.gradle.kts                   # Root build: plugin declarations
├── settings.gradle.kts                # rootProject.name = "cleanify"
├── gradle.properties                  # JVM args, AndroidX, config cache
├── gradle/wrapper/
├── gradlew / gradlew.bat
├── ARCHITECTURE.md                    # ← THIS FILE
├── README.md
├── CONTRIBUTING.md
├── FUNDING.md
├── LICENSE (GPLv3)
└── .github/ISSUE_TEMPLATE/
    ├── bug_report.md
    └── feature_request.md
```

---

## Architecture: MVVM + UDF + Clean Architecture

```
┌─────────────────────────────────────────────────┐
│  UI Layer (Compose)                             │
│  ├── Screens: Onboarding, Session, Swiper,      │
│  │            Duplicates, Settings              │
│  ├── Components: Dialogs, Cards, Search, etc.   │
│  └── ViewModels: MainVM, SessionSetupVM,        │
│                  SwiperVM, SettingsVM, etc.      │
├─────────────────────────────────────────────────┤
│  Domain Layer                                   │
│  ├── UseCases: DuplicateFinderUseCase,          │
│  │              SimilarFinderUseCase            │
│  ├── Repositories: MediaRepository (interface), │
│  │                 DuplicatesRepository (iface) │
│  ├── Models: MediaItem, FolderDetails,          │
│  │            ScanResultGroup, etc.             │
│  └── Event Buses: FileMod, FolderUpdate,        │
│                   AppLifeCycle                  │
├─────────────────────────────────────────────────┤
│  Data Layer                                     │
│  ├── Room DB (7 tables, 6 DAOs, v3)            │
│  ├── DataStore Preferences                      │
│  ├── Repository Impls (DirectMediaRepo,         │
│  │   PreferencesRepo)                           │
│  └── Direct File System I/O                     │
├─────────────────────────────────────────────────┤
│  DI Layer: Hilt (3 modules)                     │
│  Background: WorkManager, Foreground Service    │
└─────────────────────────────────────────────────┘
```

### Key Principles (from CONTRIBUTING.md)
- **State Decoupling**: Composables receive only the specific params they need, never entire UiState objects
- **UDF**: State flows down, events flow up

---

## Screen Flow

```
App Launch → Splash Screen → [Permissions Check]
  ├── No Permission → PermissionRequiredScreen
  ├── Not Onboarded → Onboarding (6 pages) → SessionSetup
  └── Onboarded → SessionSetup (folder selection)
                    └── SwiperScreen (swipe to keep/delete/move)
                         ├── SummarySheet (review changes)
                         └── DuplicateFinder (scan + review groups)
                              └── GroupDetailsScreen
```

Navigation: Single NavHost with nested graph for duplicates (`duplicates_graph`).
Deep link: `app://com.cleanify/`

---

## Key Symbols & Coupling

| Symbol | File | Consumed By |
|--------|------|-------------|
| `CleanifyApp` | `CleanifyApp.kt` | `AndroidManifest.xml` |
| `CleanifyTheme()` | `ui/theme/Theme.kt` | `MainActivity.kt` |
| `CleanifyDatabase` | `data/db/CleanifyDatabase.kt` | `DatabaseModule.kt` |
| `DuplicateScanService` | `service/DuplicateScanService.kt` | `AndroidManifest.xml`, various ViewModels |
| `AppNavigation` | `ui/navigation/AppNavigation.kt` | `MainApp.kt` |
| `MainViewModel` | `ui/MainViewModel.kt` | `MainActivity.kt`, `MainApp.kt`, `BaseActivity.kt` |

---

## Recent Updates (from git log)

| Date | Commit | Description |
|------|--------|-------------|
| Recent | `2af68f2` | Fix unscannable banner, deprecation tooltips, code cleanup |
| Recent | `1023280` | Fix stale results rescan button contrast (light theme) |
| Recent | `320c486` | Localize duplicate scan progress notification |
| Recent | `b6bfc21` | Docs: translation contribution, fix table formatting |
| Recent | `71c3f77` | Runtime language switching, Splash Screen API, i18n |
| Recent | `0667755` | High-priority scan result notification |
| Recent | `7bb3a34` | Similarity denials, grouping logic, DB v3 |
| Recent | `d026dc4` | Fix settings search flow, update deps |

---

## App Icons (for replacement)

| Icon | Location | Type |
|------|----------|------|
| Launcher (all densities) | `app/src/main/res/mipmap-*/ic_launcher.webp` | WebP raster |
| Round launcher | `app/src/main/res/mipmap-*/ic_launcher_round.webp` | WebP raster |
| Foreground (adaptive) | `app/src/main/res/mipmap-*/ic_launcher_foreground.webp` | WebP raster |
| Adaptive XML | `app/src/main/res/mipmap-anydpi/ic_launcher.xml` | XML adaptive |
| Adaptive round XML | `app/src/main/res/mipmap-anydpi/ic_launcher_round.xml` | XML adaptive |
| Background color | `app/src/main/res/values/ic_launcher_background.xml` | Color resource |
| Notification scan | `app/src/main/res/drawable/ic_duplicates_scan.xml` | Vector XML |
| Notification cancel | `app/src/main/res/drawable/ic_cancel.xml` | Vector XML |
| Splash screen icon | Referenced in `themes.xml` via `@mipmap/ic_launcher` | (same as launcher) |

Replace the `.webp` files in all 6 mipmap density buckets. Use Android Studio's Image Asset Studio for easiest replacement.

---

## Build Configuration

- **minSdk**: 29 (Android 10)
- **targetSdk**: 36
- **compileSdk**: 36
- **Kotlin**: 2.2.20
- **AGP**: 8.11.2
- **Hilt**: 2.57.1
- **Room**: 2.8.4
- **Compose BOM**: 2026.01.01
- **Material3**: 1.4.0
- **Signing**: `Cleanify_RELEASE_STORE_FILE` / `_KEY_ALIAS` in gradle.properties
- **APK output**: `cleanify-v{version}-{buildType}.apk`
