# `:core:model`

Pure Kotlin domain types shared across AALyrics.

This module must remain independent from Android framework classes, provider implementations, networking, storage, and UI code.

## Conventions

- Timeline values use milliseconds and are named with an `Ms` suffix.
- Unknown values use `null`; sentinel values such as `0` or empty strings are not used to mean "unknown".
- Lyrics timing precision is derived from the available data rather than stored redundantly.
- Provider/catalog identities use `TrackReference` namespaces instead of provider-specific fields on `Track`.
- Platform-specific playback identifiers are adapted into `PlaybackSource` before entering the domain layer.

The model intentionally validates basic invariants at construction time so malformed provider or platform data is normalized at module boundaries rather than propagated through the application.
