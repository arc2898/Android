# FT-music

FT-music is an open-source study project for building a music application with a shared Go backend and Android clients. The backend is layered so storage, application logic, streaming providers, and client transport can evolve independently.
Download apk file from releases

## Current layers

- `go/internal/domain` — stable music entities.
- `go/internal/repository` — storage contracts plus memory and SQLite implementations.
- `go/internal/service` — application use cases and orchestration.
- `go/internal/streaming` — provider boundary for authorized/licensed streaming integrations.
- `go/internal/server` — JSON HTTP API shared by Android and future clients.
- `brand/theme.json` — centralized FT-music palette, spacing, and logo direction.

## Persistence

The server uses SQLite by default and creates the `tracks` schema automatically on startup. Configure the database path with `FT_MUSIC_DB`.

```bash
cd go
go test ./...
FT_MUSIC_DB=./ft-music.db go run ./cmd/ft-music
```

The backend listens on `:8080` by default. Set `FT_MUSIC_ADDR` to change the address.

## Streaming policy

The shared backend must use official or otherwise authorized provider APIs. FT-music will not scrape services, bypass access controls, remove DRM, or distribute music without permission. Each provider adapter must document its terms, licensing basis, attribution requirements, rate limits, and regional restrictions. Open-source licensing covers the code, not third-party music, artwork, trademarks, or provider access.