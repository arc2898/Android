# Dailer X roadmap

## Implemented in the current source MVP

- [x] Gold / warm-white / charcoal theme with system/light/dark modes.
- [x] Custom outlined transparent action controls.
- [x] Bottom dock and navigation sidebar.
- [x] Search UI connected to the public-business API.
- [x] Local saved numbers, tags, notes, and call/SMS shortcuts.
- [x] Saved links and notes; open/copy/delete actions.
- [x] Message templates that open the user's installed SMS application.
- [x] Local search history and clear-history control.
- [x] Settings, custom image backgrounds, privacy and terms screens.
- [x] API configurable from the UI.
- [x] Optional Android call-screening role with local block/allow rules, E.164 normalization, opt-in high-report auto-block, and fail-open network behavior.
- [x] Backend source metadata and disabled-by-default administrative import endpoint.
- [x] Report deduplication across categories, installation/network token hashing, per-token daily limits, and backend tests.
- [x] Native ringtone picker with user-approved system setting access.
- [x] Notification channel with launcher-badge support and Android 13+ permission flow.
- [x] Debian/Codespaces build script and container recipe.
- [x] Edge-to-edge insets and adaptive dock for narrow split-screen windows.
- [x] Explicit call-recording limitation documentation (no OS restriction bypass).

## Needed before a public production release

1. Build and test the Android app across supported API levels and OEM devices.
2. Replace SharedPreferences for personal records with encrypted/local database storage where appropriate; add user-controlled export and import files.
3. Add backend authentication, stronger per-user abuse controls, HTTPS deployment, secrets management, migrations, backups, monitoring, and retention jobs.
4. Add directory ingestion connectors only for data sources with licenses that permit the intended reuse; preserve provenance, update time, correction requests, and takedown workflows.
5. Add moderated community submissions with consent records, deduplication, reputation controls, and appeals. Consent toggle currently does not upload anything.
6. Validate call-screening timing, OEM behavior, Android role UX, privacy disclosures, and accessibility on physical devices. Never block calls automatically without clear user configuration.
7. Review privacy policy, terms, local law, Google Play policies, third-party licenses, and trademark/branding before release.
8. Consider Jetpack Room + migrations and Android Keystore-backed encryption for the next data-storage revision.
