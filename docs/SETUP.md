# Setup — build & run for €0

## 1. Install Android Studio (free)
- Windows: `winget install --id Google.AndroidStudio -e` or download from developer.android.com/studio.
- First launch → **Standard** setup wizard → it downloads the Android SDK, platform tools and an
  emulator image (~3–5 GB). It bundles its own JDK 17+, so the system Java 8 is not a problem.

- In *SDK Manager* make sure **Android SDK Platform 36** and **Android SDK Platform-Tools** are
  installed (Platform-Tools provides `adb`, needed to install on your phone).

## 2. Open the project
- *File → Open* → select this `ToDoApp` folder → wait for Gradle sync.

### Toolchain (keep in sync with Android Studio)
| Tool | Version | Note |
|---|---|---|
| Android Gradle Plugin | 9.5.1 | Matches Android Studio 2026.2; older AGP shows "SDK XML version 4" warnings |
| Gradle | 9.9.1 | via `gradlew` wrapper |
| Kotlin | 2.2.10 | **Built into AGP 9** — do not add `org.jetbrains.kotlin.android`; the Compose compiler plugin version must equal this |
| KSP / Hilt / Room | 2.3.14 / 2.60.1 / 2.8.3 | |
| compileSdk / targetSdk / minSdk | 36 / 36 / 28 | |

Command line (uses Android Studio's bundled Java):
```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew :app:testDebugUnitTest :app:assembleDebug
```

## 3. Run on your own phone (no Play Store account needed)
1. On the phone: *Settings → About phone →* tap **Build number** 7× to enable Developer options.
2. *Settings → Developer options →* enable **USB debugging** (or **Wireless debugging**).
3. Connect via USB, accept the "Allow USB debugging" prompt.
4. In Android Studio choose your phone in the device dropdown → ▶ **Run**.

The app stays installed like any other app. Debug builds are fine for personal use.

## 4. Health Connect
- Android 14+: built in (*Settings → Security & privacy → Privacy → Health Connect*).
- Android 9–13: install "Health Connect" from the Play Store (free).
- Your step source (Google Fit, Samsung Health, Fitbit, phone sensor…) must write to Health Connect.

## 5. Small release APK (recommended for daily use)
```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleRelease
```
→ `app/build/outputs/apk/release/app-release.apk` (R8-shrunk, a few MB instead of ~60 MB).
It is signed with your local **debug key**, so it installs over the debug build and keeps your data.
Copy it to the phone and open it (allow "install unknown apps" once).

> The debug key lives in `%USERPROFILE%\.android\debug.keystore`. If you reinstall Windows or build on
> another PC, the key changes → Android refuses to update the app. **Save a backup first**
> (Profile → Backup & restore), uninstall, install the new APK, then restore.

## 6. Widget
Long-press the home screen → *Widgets* → **FocusFit** → drag "Today" onto the home screen.
It shows today's progress, streak and next to-dos, and opens the app when tapped.

## 7. Backup & restore
Profile → **Backup & restore** → *Save backup* writes one JSON file with all your data and settings
to a place you choose (e.g. Google Drive or Downloads). *Restore* replaces everything with a backup's data.
