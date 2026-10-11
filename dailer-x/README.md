# Dailer X

**Dailer X** is an independent, open-source Android phone-number workspace built in Kotlin and Jetpack Compose. This revision uses a tightly controlled **gold, warm-white, and charcoal** identity. App actions are transparent, lightly rounded, outlined controls rather than filled stock buttons.

> **Status:** build-verified source MVP (v0.3.1). On October 11, 2026, [GitHub Actions built the Android debug APK and passed all 16 backend tests](https://github.com/arc2898/Android/actions/runs/38131670113). Download the `dailer-x-debug-apk` artifact from that run. The project remains an MVP, not a production caller-ID database or a replacement for the system phone app.

## Features in this source revision

### Android app

- Search configured public-business directory and private saved numbers.
- Save / edit / delete phone numbers with private notes and comma-separated tags.
- Open the native phone dialler or native SMS app; optional draft templates are passed to the SMS app for the user to review and send.
- Save, open, copy, edit notes for, and delete links.
- Create, edit, delete, copy, and open reusable message templates; saved-number recipient shortcuts.
- Search history with an opt-out and clear-history control.
- Custom dock, navigation sidebar, dedicated Settings page, Privacy Policy, and Terms & Conditions pages.
- System, light, or dark mode; Compact/Default/Large text size; built-in Ivory Paper, Gold Haze, Charcoal, and Onyx backgrounds; a bundled charcoal-and-gold sample wallpaper or an image from device storage.
- Configurable backend URL and connection test.
- Optional Android call-screening role with caller lookup notifications, local allow/block rules, and explicit opt-in automatic blocking at five distinct report tokens in the backend 30-day window. Allowlist overrides block rules; network failures fail open. A report threshold is a heuristic and can cause false positives.
- Local storage deletion and consent settings.
- Native ringtone picker with an explicit system-settings permission flow, notification permission controls, a high-importance notification channel with badge support, and privacy-safe caller-alert notifications.
- Edge-to-edge layout, keyboard resizing, system-bar insets, scrollable screens, and a bottom dock that adapts to narrow split-screen/floating windows; all pages remain available in the sidebar.
- Call recording compatibility guidance. General cellular call recording is not implemented because ordinary third-party Android apps cannot reliably access call audio across devices; the app does not bypass OS/OEM restrictions.
- SVG brand/icon assets, PNG brand/background assets, a native Android VectorDrawable icon, and an alpha-only notification icon. The UI uses vector Material icons, not emoji glyphs.

### Backend

- `GET /health`
- `GET /v1/search?q=...`
- `GET /v1/lookup?number=...`
- `POST /v1/reports/spam`
- `POST /v1/businesses` (disabled until `DAILERX_ADMIN_KEY` is configured)
- SQLite storage by default; Docker Compose volume for persistent data.
- Business imports include source and license metadata; imported entries are unverified by default.
- Spam reports are deduplicated across categories per installation/network token and number for 24 hours, with per-installation and per-network daily caps. Only a salted one-way digest is stored instead of the raw installation token/network address. This is a coarse anti-abuse measure, not identity verification.

Demo seed entries are synthetic fixtures, not real business data. A source match is not proof that a caller owns or controls a number.

## Source layout

```text
dailer-x/
├── android/
│   └── app/src/main/
│       ├── assets/branding/        # SVG / PNG brand files
│       ├── assets/icons/           # Reusable SVG icons
│       ├── java/com/dailerx/app/   # Compose app + call-screening service
│       ├── res/drawable/           # Android VectorDrawable app mark
│       └── AndroidManifest.xml
├── backend/
│   ├── app/main.py                # FastAPI API and SQLite schema
│   └── tests/                     # API tests
├── docs/legal/                    # Editable policy / terms templates
├── docs/ANDROID_CALL_PROTECTION.md # Blocking, ringtone, notifications, recording limits
├── BUILD_REPORT.md                # Validation status and platform limitations
├── scripts/build-debian.sh        # Debian/Codespaces source-to-APK build script
├── Dockerfile.debian              # Debian build container
├── .github/workflows/ci.yml       # Android and backend validation
├── docker-compose.yml
└── README.md
```

## Run the backend locally

Requirements: Python 3.11+.

```bash
cd backend
python -m venv .venv
# macOS/Linux:
source .venv/bin/activate
# Windows PowerShell: .venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

API health: `http://localhost:8000/health`  
Interactive OpenAPI page: `http://localhost:8000/docs`

Or use Docker from the repository root:

```bash
docker compose up --build
```

The Android emulator's default backend URL is `http://10.0.2.2:8000`. The debug manifest enables cleartext traffic only for local development. The main/release manifest keeps cleartext traffic disabled; deploy an HTTPS endpoint for production. For a physical device, enter the reachable development-host URL in Settings.

## Import a licensed business listing

Business imports are intentionally **disabled** by default. Generate a strong secret and set it before starting the backend:

```bash
export DAILERX_ADMIN_KEY='replace-with-a-long-random-secret'
export DAILERX_REPORT_SALT='replace-with-a-different-long-random-secret'
docker compose up --build
```

Then send `POST /v1/businesses` with an `X-Admin-Key` header:

```json
{
  "number": "+14155550123",
  "name": "Example Business",
  "category": "Retail",
  "source": "Your licensed directory",
  "source_license": "License or permission reference"
}
```

Only import data you have the right to use and redistribute. A simple admin key is an MVP safeguard, not a complete production ingestion security architecture. Do not expose the ingestion endpoint publicly without HTTPS, secret management, audit logs, abuse limits, and access controls.

## Build the APK in Debian / GitHub Codespaces

The source includes a pinned Debian build script. It installs the Android command-line tools, Android SDK Platform 35, Build Tools 35.0.0, Gradle 8.9, runs backend tests, then compiles the debug APK. It needs outbound access to Google's Android SDK repository and Gradle's distribution host on the first run.

```bash
# In a fresh Debian 12 / Ubuntu / Codespaces terminal (installs system packages if needed)
INSTALL_DEBIAN_DEPS=1 ./scripts/build-debian.sh

# Output
ls -lh android/app/build/outputs/apk/debug/app-debug.apk
```

For a Debian container, build and run it from the repository root:

```bash
docker build -f Dockerfile.debian -t dailerx-debian-builder .
docker run --rm -v "$PWD:/workspace" -v dailerx-build-cache:/opt/dailerx-cache dailerx-debian-builder
```

The build script also runs backend tests. Use `DAILERX_BUILD_CACHE` to choose a persistent cache location. Android Studio remains an alternative, not a requirement.

## Open in Android Studio

1. Install Android Studio, JDK 17, and Android SDK Platform 35.
2. Open the extracted `dailer-x` directory as a Gradle project.
3. Sync Gradle and allow Android Studio to download project dependencies.
4. Start the API as described above, or set a backend URL you control.
5. Run `android/app` on an Android emulator or supported test device.
6. For optional call screening on Android 10+, request the call-screening role in Settings. Automatic spam blocking is off by default; manual block/allow rules only apply while Dailer X holds the role.
7. Grant notification permission if you want caller alerts. Changing the device-wide ringtone requires an explicit system-settings grant.

A Gradle wrapper is not checked in; `scripts/build-debian.sh` downloads a pinned Gradle distribution and Android SDK on first run.

## Privacy and security notes

- The Android app does not request contacts permission, upload a complete address book, or read SMS/WhatsApp/Telegram messages.
- Saved numbers, tags, notes, links, settings, and search history use app-private `SharedPreferences` in this MVP. They are not encrypted at the application layer; do not store sensitive secrets or personal details.
- Searching sends the query to the configured backend. Spam reports send the reported number, category, optional details, and a random per-installation anti-duplicate token. Optional call screening may send an incoming number to the configured API after the user grants the screening role; remote auto-blocking itself is separately opt-in.
- The local development report salt is not suitable for deployment. Set a stable random `DAILERX_REPORT_SALT` before operating a server. The installation token is stored locally in preferences and only sent with spam reports; the backend stores a salted digest.
- The `community_consent` setting currently records preference only. No automatic community contribution or contact syncing is implemented.
- Server-side reports are not removed when a user clears local app data. A public deployment needs defined retention, correction, deletion, appeals, monitoring, request limits, and jurisdiction-specific legal review.
- The call-screening service may reject calls only when a number is in the user-configured local blocklist or when the user explicitly enables the high-report auto-block rule. The allowlist always wins; backend failures fail open. Android/OEM call UI remains in control; Dailer X uses best-effort notifications rather than drawing over the system call screen.
- Call recording of cellular audio is not implemented. Standard third-party Android apps do not have a reliable, general-purpose public API for it; see `docs/ANDROID_CALL_PROTECTION.md`.

See `docs/legal/PRIVACY_POLICY.md` and `docs/legal/TERMS_AND_CONDITIONS.md`. They are starting templates, not legal advice. Before distribution, provide an operator name/contact and review applicable laws and Google Play requirements.

## License

Project code is released under Apache-2.0. Third-party libraries, Material icons, and phone-number datasets may have separate licenses. Review them independently. There is no affiliation with or endorsement by Truecaller.
