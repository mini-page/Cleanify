## Cleanify v3.0.0 — Media Engine Overhaul, Document Viewer & Polish

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
- **Native Multi-Alias Feedback & WhatsApp Integration**:
  - Built-in Feedback Dialog with direct WhatsApp support (+91 9621272014) and email category selection routing to dedicated developer aliases:
    - 🐞 Bug Reports: `raghavans5711+Cleanify_bugReport@gmail.com`
    - 💡 Feature Requests: `raghavans5711+Cleanify_featureReq@gmail.com`
    - ✏️ Change Suggestions: `raghavans5711+Cleanify_suggestion@gmail.com`
    - 💬 General Feedback: `raghavans5711+Cleanify@gmail.com`
  - Automatically attaches device diagnostics (OS, Model, Version, ABIs).

### 🎨 Design & Polish
- **Branding Assets**: Upgraded website and documentation logos from WebP to high-res PNG (`logo.png`).
- **Media Controls Polish**: Seamless layout and lifecycle management for ExoPlayer across viewports.
