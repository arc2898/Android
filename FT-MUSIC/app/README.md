# FT-music Flutter app

This directory contains the FT-music Flutter/Dart client, retained in the same language and application structure as the upstream reference project.

The current milestone is a labeled and themed rebrand:

- product labels use **FT-music**;
- Android and Web metadata use the FT-music name;
- the dark violet/cyan palette is centralized in `lib/core/theme/app_theme.dart`;
- Android adaptive icons use the FT-music vector mark;
- the source remains under the applicable GPLv2-or-later terms.

The shared Go backend is in [`../go/`](../go/). The client and backend are kept as separate projects so the Android application can call the same service as future clients.

Before distributing builds, review third-party provider terms, music/artwork rights, trademarks, and upstream attribution obligations.
