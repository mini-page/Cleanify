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
