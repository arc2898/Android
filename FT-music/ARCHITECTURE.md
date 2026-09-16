# FT-music architecture

FT-music is organized into layers so the Android client, future desktop clients, and the shared streaming backend can evolve independently.

```text
Android / future clients
          |
      HTTP API
          |
     service layer
       /      \
 repository   streaming catalog
   /       \       |
SQLite   memory  authorized provider adapters
```

## Layers

| Layer | Package | Responsibility |
|---|---|---|
| Domain | `internal/domain` | Stable entities and value contracts; no transport or storage dependencies |
| Repository | `internal/repository` | Persistence ports, SQLite implementation, and memory implementation for tests |
| Service | `internal/service` | Use cases and orchestration across repositories and providers |
| Streaming | `internal/streaming` | Provider interface and catalog; adapters must use official or otherwise authorized APIs |
| Transport | `internal/server` | JSON HTTP API for clients |
| Brand | `brand` | Shared product name, palette, shape, and logo direction for Android UI work |

## Persistence

SQLite is the default runtime store. The repository creates the initial `tracks` table and search index at startup. The repository interface is intentionally independent of SQLite so future migrations can add playlists, artists, albums, lyrics, downloads, and settings without coupling the service layer to a database driver.

## Legal streaming boundary

The service must not scrape, bypass access controls, remove DRM, or download content without authorization. Each provider adapter must document its API terms, licensing basis, attribution requirements, rate limits, and supported regions. The application should expose provider identity and link to provider terms where appropriate.

The repository is intended for study and open-source development. Open-source software licensing does not itself grant permission to use copyrighted music, artwork, trademarks, or proprietary streaming endpoints.
