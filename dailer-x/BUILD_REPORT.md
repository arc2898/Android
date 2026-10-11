# Dailer X source validation report

**Source version:** 0.3.0

## Checks completed in the source-generation environment

- Backend unit/API tests: **16 passed**.
- Backend integration smoke test: `/health`, business search, spam report submission, and lookup/reputation all responded successfully against a local Uvicorn server.
- Python bytecode compilation: passed.
- Build script shell syntax (`bash -n`): passed.
- Android manifest, drawable XML, and bundled SVG assets: parsed successfully.
- Kotlin parser scan: no syntax/parser diagnostics were found. A full Kotlin compile was not possible without Android/Compose dependencies.

## Android APK compilation status

The Debian build script was executed, but the build stopped while downloading the Android command-line tools because DNS resolution for `dl.google.com` failed in this environment. The Gradle distribution host also fails DNS resolution here. Therefore no APK build or Android type-check is claimed as successful.

Run `INSTALL_DEBIAN_DEPS=1 ./scripts/build-debian.sh` in Debian 12, Ubuntu, or GitHub Codespaces with outbound access to Google's Android SDK repository and `services.gradle.org`. It installs the pinned SDK/build tools and Gradle, runs backend tests, and builds `android/app/build/outputs/apk/debug/app-debug.apk`.

## Platform limitations

- Call screening requires the user-selected Android call-screening role. Auto-blocking is off by default, local allow rules take priority, and backend failures fail open.
- Notifications and launcher badges depend on user permission and OEM/launcher settings.
- Ringtone changes alter the device-wide default and require explicit system-settings permission.
- Generic cellular call recording is not implemented: ordinary third-party apps do not have a reliable public API for call audio across Android/OEM variants. Dailer X does not bypass those restrictions.
- The caller database contains synthetic demo records only; production requires licensed data and moderation.
