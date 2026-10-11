# Personal Caller ID (Android 8+)

An original, standalone Kotlin/Jetpack Compose starter app. It is not a modified Truecaller build and contains no Truecaller code.

## Included
- Minimum Android 8.0 / API 26
- Simple caller-ID-inspired UI
- Number entry and native Android dialer handoff
- Local saved-number list
- No account, subscription screen, spam feed, contact upload, or network backend

## Build
Open this folder in Android Studio, or run `gradle assembleDebug` with JDK 17 and Gradle 8.9. The repository GitHub Actions workflow builds the debug APK and uploads it as the `personal-caller-id-debug-apk` artifact.

This MVP does not identify unknown incoming callers by itself. That requires a lawful, trusted lookup data provider and additional testing. It does not request the call-screening role or contacts permission.
