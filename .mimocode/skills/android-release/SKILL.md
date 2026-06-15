---
name: android-release
description: "Use when the user needs to build, test, and publish a new Android release. Covers version bumping, APK/AAB building, device installation, testing, Git operations, and GitHub release creation with changelog."
---

# Android Release Workflow

Automates the full release cycle for the Cleanify Android app: version bump → build → install → test → commit → push → GitHub release.

## When to Use

- User says "build release", "release new version", "publish to GitHub", "bump version and release"
- After completing a set of features/fixes that should be packaged as a release
- When the user wants to test a release build on their device before publishing

## Prerequisites

- Android SDK and build tools installed
- ADB available for device installation
- GitHub CLI (`gh`) authenticated
- Keystore configured in `keystore.properties` or environment variables

## Workflow Steps

### 1. Determine Version Bump

Ask the user or infer from context:
- **Patch** (x.x.1): Bug fixes only
- **Minor** (x.1.0): New features, backward compatible
- **Major** (1.0.0): Breaking changes

Read current version from `app/build.gradle.kts`:
```bash
Select-String -Path "app\build.gradle.kts" -Pattern "versionCode|versionName" | Select-Object -First 2
```

### 2. Bump Version

Edit `app/build.gradle.kts`:
- Increment `versionCode` by 1
- Update `versionName` to new version string

### 3. Build Release APK

```bash
.\gradlew.bat assembleRelease 2>&1
```

Wait for build to complete. If build fails:
1. Read the error message
2. Fix the issue (common: XML syntax, dependency conflicts, KSP version mismatch)
3. Rebuild

APK location: `app/build/outputs/apk/release/app-release.apk`

### 4. Install on Device (Optional)

If user wants to test on device:
```bash
adb install -r app\build\outputs\apk\release\app-release.apk
```

Wait for user confirmation before proceeding.

### 5. Commit Changes

Stage and commit all changes:
```bash
git add -A
git commit -m "release: v{version} - {brief description}"
```

### 6. Push to GitHub

```bash
git push origin main
```

### 7. Create GitHub Release

Use `gh` CLI to create a release with the APK:
```bash
gh release create v{version} `
  --title "v{version} - {title}" `
  --notes "{changelog}" `
  app\build\outputs\apk\release\app-release.apk
```

Generate changelog from recent commits:
```bash
git log --oneline -20
```

## Common Issues & Fixes

### Build Failures

| Error | Fix |
|-------|-----|
| `XML syntax error` | Check `shortcuts.xml`, `AndroidManifest.xml` for unclosed tags |
| `KSP version mismatch` | Update KSP version in `build.gradle.kts` to match Kotlin version |
| `Could not resolve dependency` | Run `.\gradlew.bat --refresh-dependencies` |
| `Signing config not found` | Verify `keystore.properties` exists or env vars are set |

### ADB Issues

| Error | Fix |
|-------|-----|
| `device not found` | Enable USB debugging, check cable connection |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Uninstall existing app first: `adb uninstall com.cleanify` |

## Example Usage

```
User: "Build the latest release and install it on my device for testing, then push to GitHub and create a release"

Agent:
1. Reads current version (2.8.0, versionCode 14)
2. Bumps to 2.9.0 (versionCode 15)
3. Builds release APK
4. Installs on device via ADB
5. Waits for user confirmation
6. Commits changes
7. Pushes to GitHub
8. Creates GitHub release v2.9.0 with APK attached
```

## Notes

- Always wait for user confirmation after device installation before pushing to GitHub
- Include a meaningful changelog in the GitHub release
- If the build fails, fix the issue before proceeding (don't skip steps)
- The release APK is signed with the release keystore (not debug)
