# Dailer X Android call protection notes

## Call identification and blocking

Dailer X implements Android's user-selected `CallScreeningService`. The user must grant the call-screening role in system settings. The service:

1. checks the locally configured allowlist first (allow always wins);
2. rejects locally blocked numbers;
3. when the user explicitly enables automatic spam blocking, looks up the number and rejects calls with at least five distinct installation/network report tokens in the backend's 30-day window;
4. fails open on network, parsing, or backend errors;
5. returns the screening response promptly and never captures call audio.

The five-report threshold is a conservative MVP heuristic, not proof of fraud. It can create false positives; the allowlist and off-by-default setting are intentional. Do not deploy automatic blocking as a substitute for moderation, appeals, and robust anti-abuse controls.

Android screening does not reliably provide full caller details for numbers saved in contacts unless additional permissions are granted. Dailer X deliberately does not request contact access. The notification is best-effort and depends on notification permission, the OEM, and system settings; it is not guaranteed to replace or overlay the system incoming-call UI.

## Ringtone

The ringtone control opens Android's native ringtone picker and requests the special `WRITE_SETTINGS` grant only after the user selects a ringtone. Applying it changes the device-wide default phone ringtone, not just Dailer X. OEM policy and work-profile restrictions may prevent the change.

## Notifications and badges

Dailer X creates a high-importance notification channel with `setShowBadge(true)`. Android 13+ requires runtime notification permission. Launchers and OEMs can hide badges or override channel importance/sound; the app cannot force badges to appear.

## Call recording limitation

There is no reliable public Android API that lets an ordinary third-party app record cellular call audio across manufacturers and Android versions. `VOICE_CALL` audio capture is restricted to privileged/system capabilities on many devices, and OEM built-in recording behavior differs. Dailer X intentionally does not attempt to bypass these restrictions, use Accessibility to capture audio, or record calls silently. Users should use the phone's own recording controls where available and follow consent laws.

## Windowing and cutouts

The activity uses edge-to-edge window layout, `adjustResize`, scrollable content, safe system-bar padding, and an adaptive bottom dock. The sidebar remains the fallback for pages hidden from the dock in narrow split-screen/floating windows. A vendor's Dynamic Island-like UI is not a standardized Android API; Dailer X avoids drawing over camera cutouts and uses normal system notifications instead.
