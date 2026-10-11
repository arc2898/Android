# Personal Caller ID (Android 8+)

An original Kotlin/Jetpack Compose starter project for Android 8.0/API 26 and later. Source files live in [personal-caller-id-android8](./personal-caller-id-android8/).

## Build APK
Open **Actions → Build Personal Caller ID Android APK**. A push to the build branch starts the workflow automatically; manual runs are also enabled. When a build finishes, open that run and download the **personal-caller-id-debug-apk** artifact.

## Scope and limitations
The app provides a basic local number-entry/dialer handoff and locally saved numbers. It is not Truecaller, contains no Truecaller code, and does not bypass Truecaller login, subscriptions, or access controls. It does not identify unknown callers without a separately integrated lawful lookup data provider. The artifact is a debug APK, not a Play Store-signed release.
