# FT-music rebuild

This checkout contains the upstream **BloomeeTunes** application as the reference implementation and an independent Go foundation under [`go/`](go/). The new product name is **FT-music**.

## Current baseline

The reference application is a Flutter client targeting Android, iOS, Linux, macOS, Windows, and Web. Its major capabilities include music discovery through plugins, playback, lyrics, playlists and library management, local media, offline downloads, caching, and settings. The upstream code uses a Rust/WASM plugin bridge and Isar persistence.

The Go foundation intentionally starts with a small, testable domain/API layer rather than copying Flutter source. It currently provides:

- a Go 1.22 module;
- an in-memory track library with metadata search;
- JSON endpoints for health, library listing/search, track lookup, and adding tracks;
- unit tests for core library behavior.

## Run

```bash
cd go
gofmt -w ./cmd ./internal
go test ./...
go run ./cmd/ft-music
```

The API listens on `:8080` by default. Set `FT_MUSIC_ADDR` to change the bind address.

## Suggested migration sequence

1. Define stable domain contracts for tracks, albums, artists, playlists, lyrics, downloads, and plugin providers.
2. Replace the in-memory library with SQLite and migrations.
3. Add a provider interface and one compliant provider adapter; keep provider-specific code outside the domain package.
4. Implement playback/download workers behind interfaces so desktop, mobile, and web clients can share the API.
5. Add authentication, user settings, cache invalidation, and synchronization only after the single-user flow is stable.
6. Build the FT-music client separately against the versioned API.

## License and branding

The cloned upstream project is licensed under **GNU GPL v2 or later**. Any derivative work based on its covered source must preserve the applicable copyright notices, license text, and source-distribution obligations. This Go foundation is newly authored and carries GPL-2.0-or-later headers; review third-party dependencies and artwork before publishing a rebranded distribution. “BloomeeTunes” and related upstream branding should not be presented as FT-music branding.
