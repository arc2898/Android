# Dailer X source validation report

**Source version:** 0.3.1  
**Android version code:** 4  
**Build verified:** October 11, 2026

## Verified GitHub Actions build

- [CI run: Dailer X Android CI](https://github.com/arc2898/Android/actions/runs/38131670113)
- Backend tests: **16 passed**.
- Android build: **passed** using JDK 17, Android SDK Platform 35, Build Tools 35.0.0, and Gradle 8.9.
- Gradle task: `:android:app:assembleDebug`.
- Artifact: `dailer-x-debug-apk` (debug APK, uploaded by CI; artifact retention follows GitHub Actions settings).
- The active monorepo workflow is `.github/workflows/dailer-x-ci.yml` at the repository root. GitHub does not discover workflows stored only in a subdirectory.

## Reproducible Debian / Codespaces build

From the `dailer-x` directory, use a network-enabled Debian 12, Ubuntu, or GitHub Codespaces environment:

```bash
INSTALL_DEBIAN_DEPS=1 ./scripts/build-debian.sh
```

The script provisions Java 17, Android command-line tools, SDK Platform 35, Build Tools 35.0.0, and Gradle 8.9, then runs the backend tests and creates:

`android/app/build/outputs/apk/debug/app-debug.apk`

## Platform limitations and release checklist

- Call screening must be explicitly granted via Android's call-screening role. Automatic blocking is off by default; local allow rules override block rules; worker overload and backend failures fail open.
- Backend lookups run off the Android screening callback thread. Caller-ID notifications remain best-effort and depend on notification permission, channel settings, and OEM behavior.
- Notification badges depend on launcher/OEM settings. Ringtone changes modify the device-wide default and require explicit system-settings access.
- Generic cellular call recording is not implemented. Ordinary third-party apps lack a reliable public API for capturing cellular call audio across Android/OEM variants; this project does not bypass those restrictions.
- Demo business listings are synthetic. Production caller identification still requires lawful/licensed directory data, moderation, and dispute/correction workflows.
- The APK was compiler-verified in CI, not tested on every physical device. Before a public release, test call-screening timing, split-screen/floating-window layouts, notification permission denial, ringtone changes, and Android/OEM-specific behavior on representative devices.
