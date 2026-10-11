# Dailer X Privacy Policy (starter template)

**Effective date:** 11 October 2026  
**Operator:** The maintainer/distributor must insert a legal name and privacy contact before release.

This document describes the current Dailer X source MVP. It is a template, not legal advice. Review it against actual deployment behavior and applicable law before distribution.

## Information stored on the device

The Android app stores saved phone numbers, display labels, private notes, tags, saved links, reusable message templates, theme and background preferences, backend URL, phone-region preference, local block/allow rules, notification preference, and call-screening preference, and—when enabled—recent search terms in app-private SharedPreferences. This MVP does not encrypt these records at the application layer. Avoid placing passwords, financial data, medical data, or other sensitive secrets in notes.

## Requests sent to a server

Search queries are sent to the backend URL the user configures. Spam reports send the reported number, category, and optional text. The backend stores report information. The person operating that service is responsible for access controls, retention, backups, user rights requests, and incident handling. Use a server you trust and use HTTPS outside local development.

## Call screening

Call screening is optional. On supported Android versions, the user must grant Dailer X the system call-screening role. Incoming numbers can be sent to the configured backend for caller lookup and spam-report counts. Manual local block rules can reject matching calls. Automatic server-based blocking is separately opt-in and can reject a number with at least five distinct report tokens in the backend's 30-day window. The allowlist takes precedence and backend failures fail open. False positives are possible. Android and device manufacturers control call UI behavior; caller alert notifications are best-effort and do not replace the system incoming-call screen.

## Contacts and message access

The current app does not request permission to upload an address book and does not read SMS, WhatsApp, Telegram, or other app messages. Messaging shortcuts open the user's installed SMS app, optionally with prefilled draft content. The user reviews and sends the message.

## Community contributions

The current community-consent setting records a user preference only. It does not automatically upload contacts, saved names, or notes. Future contribution features must use clear, specific consent and must document exactly what is collected, why, how long it is retained, and how to revoke consent.

## Logs, duplicates, and report identity

For basic duplicate-report prevention, the app sends a random per-installation token with spam reports. The API derives a salted one-way hash and stores that digest rather than the raw token. If no token is provided, the API falls back to a digest of the observed network address. These digests are pseudonymous and may still be regulated personal data. The coarse controls can be bypassed by a determined attacker. Operators must set a strong, stable `DAILERX_REPORT_SALT`, restrict database access, and define retention and deletion schedules.

## Deletion and correction

Users can clear local search history or delete local app data in Settings. That action does not remove reports already sent to the server. Server-side deletion, correction, and dispute workflows must be implemented by the server operator and a contact address must be provided before public launch.

## Security and changes

The MVP uses app-private local storage without application-layer encryption, has limited anti-abuse measures, and is not a hardened production service. The maintainer should publish versioned policy updates and promptly document material changes. Add operator identity, jurisdiction-specific disclosures, lawful basis/consent analysis, data retention, children policy, international transfers, and complaint avenues before release.


## Call protection, notifications, and ringtone

If the user grants the Android call-screening role, Dailer X can inspect incoming numbers delivered by Android. The configured number may be sent to the selected backend for lookup. Manual allow/block rules remain in app-private preferences. Automatic spam blocking is off by default and, if enabled, can reject calls at the configured report threshold; network failures fail open. Caller alert notifications depend on the user's notification permission and device settings.

Changing the device call ringtone is a user-initiated system-wide setting change. Android asks for the special permission before Dailer X applies a selected ringtone. Dailer X does not record cellular call audio. The app does not attempt to bypass Android or manufacturer call-recording restrictions.
