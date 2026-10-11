#!/usr/bin/env bash
# Reproducible Dailer X build for Debian 12+, Ubuntu, or GitHub Codespaces.
# Builds the Android debug APK and runs backend tests. It does not require Android Studio.
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CACHE_ROOT="${DAILERX_BUILD_CACHE:-$HOME/.cache/dailerx-build}"
SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
GRADLE_VERSION="8.9"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

say() { printf '\n\033[1;33m[Dailer X]\033[0m %s\n' "$*"; }
fail() { printf '\n[Dailer X ERROR] %s\n' "$*" >&2; exit 1; }

if [[ "${INSTALL_DEBIAN_DEPS:-0}" == "1" ]]; then
  command -v apt-get >/dev/null || fail 'INSTALL_DEBIAN_DEPS=1 requires a Debian/Ubuntu image.'
  if [[ "$(id -u)" -eq 0 ]]; then SUDO=; else SUDO=sudo; fi
  $SUDO apt-get update
  $SUDO apt-get install -y --no-install-recommends ca-certificates curl unzip zip git python3 python3-venv python3-pip openjdk-17-jdk-headless
fi

command -v curl >/dev/null || fail 'curl missing. Install curl or run with INSTALL_DEBIAN_DEPS=1.'
command -v unzip >/dev/null || fail 'unzip missing. Install unzip or run with INSTALL_DEBIAN_DEPS=1.'
command -v python3 >/dev/null || fail 'python3 missing.'

# AGP 8.7.x is built for JDK 17. Prefer an installed JDK 17 if present.
if [[ -z "${JAVA_HOME:-}" ]]; then
  for candidate in /usr/lib/jvm/java-17-openjdk-amd64 /usr/lib/jvm/temurin-17-jdk-amd64; do
    if [[ -x "$candidate/bin/java" ]]; then export JAVA_HOME="$candidate"; break; fi
  done
fi
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
java -version 2>&1 | head -n 1 || fail 'Java is required.'

mkdir -p "$CACHE_ROOT" "$SDK_ROOT/cmdline-tools"
if [[ ! -x "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]]; then
  say 'Downloading Android command-line tools…'
  tmp="$CACHE_ROOT/android-cmdline-tools.zip"
  curl --fail --location --retry 3 --retry-delay 2 "$CMDLINE_TOOLS_URL" -o "$tmp"
  mkdir -p "$CACHE_ROOT/cmdline-tools-unpack"
  unzip -q -o "$tmp" -d "$CACHE_ROOT/cmdline-tools-unpack"
  rm -rf "$SDK_ROOT/cmdline-tools/latest"
  mkdir -p "$SDK_ROOT/cmdline-tools/latest"
  cp -a "$CACHE_ROOT/cmdline-tools-unpack/cmdline-tools/." "$SDK_ROOT/cmdline-tools/latest/"
  rm -rf "$CACHE_ROOT/cmdline-tools-unpack"
fi
export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="$SDK_ROOT/platform-tools:$SDK_ROOT/cmdline-tools/latest/bin:$PATH"

say 'Installing pinned Android SDK platform/build tools…'
yes | sdkmanager --licenses >/dev/null
sdkmanager --install 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'

GRADLE_HOME="$CACHE_ROOT/gradle-${GRADLE_VERSION}"
if [[ ! -x "$GRADLE_HOME/bin/gradle" ]]; then
  say "Downloading Gradle ${GRADLE_VERSION}…"
  tmp="$CACHE_ROOT/gradle-${GRADLE_VERSION}.zip"
  curl --fail --location --retry 3 --retry-delay 2 "$GRADLE_URL" -o "$tmp"
  unzip -q -o "$tmp" -d "$CACHE_ROOT"
fi

say 'Running backend tests…'
python3 -m venv "$CACHE_ROOT/backend-venv"
"$CACHE_ROOT/backend-venv/bin/python" -m pip install --disable-pip-version-check -q -r "$ROOT/backend/requirements.txt"
(
  cd "$ROOT/backend"
  "$CACHE_ROOT/backend-venv/bin/python" -m pytest -q
)

say 'Compiling Android debug APK…'
(
  cd "$ROOT"
  "$GRADLE_HOME/bin/gradle" --no-daemon --stacktrace :android:app:assembleDebug
)
APK="$ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
[[ -s "$APK" ]] || fail "Gradle finished but APK was not found at $APK"
say "Build successful: $APK"
ls -lh "$APK"
