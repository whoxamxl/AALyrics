# AALyrics Architecture

## Status

This document defines the initial boundaries for the new AALyrics codebase. It is intentionally about structure rather than features.

AALyrics is a greenfield implementation. The previous Auto Lyrics fork may be used as a behavioral reference during later regression work, but implementation code is not imported into this repository during the foundation phase.

## Design goals

1. Keep Android media integration out of the lyrics domain.
2. Keep provider-specific behavior out of application state management.
3. Centralize provider selection instead of allowing providers to compete through call order.
4. Expose one stable application/domain state to both phone and automotive presentation layers.
5. Make timing, translation, caching, and provider implementations independently replaceable.
6. Keep pure domain modules free of Android framework dependencies.

## Modules

### `:app`

Android application and composition root. It owns process-level wiring and application identity, but should contain very little feature logic.

### `:core:model`

Pure Kotlin domain types shared across the project. Future examples include track identity, playback snapshots, lyric documents, timed lines, timed words, and provider metadata.

### `:provider:api`

Pure Kotlin contracts implemented by lyrics providers. Provider implementations will live in separate modules later.

### `:core:lyrics`

Pure Kotlin lyrics orchestration. Future responsibilities include candidate resolution, selection policy, and domain-level state transitions. It depends on provider contracts, not concrete providers.

### `:platform:media`

Android-specific media session and playback tracking. Its job is to translate platform events into domain models.

### `:feature:phone`

Phone presentation only. It must not talk directly to provider implementations.

### `:feature:automotive`

Android Auto presentation only. It must not own lyrics fetching or provider selection.

## Intended dependency direction

```text
                         +-------------------+
                         |       :app        |
                         +---------+---------+
                                   |
                 +-----------------+-----------------+
                 |                 |                 |
                 v                 v                 v
        :feature:phone   :feature:automotive   :platform:media
                 |                 |                 |
                 +--------+--------+                 |
                          v                          v
                    :core:lyrics                :core:model
                          |
                          v
                    :provider:api
                          |
                          v
                    :core:model
```

Concrete provider modules will depend on `:provider:api` and `:core:model`. The domain must never depend on a concrete provider.

## State flow target

```text
Android MediaSession
        |
        v
playback snapshot
        |
        v
lyrics coordinator
        |
        +----> provider contracts ----> provider implementations
        |
        v
application lyrics state
        |
        +----> phone UI
        |
        +----> automotive UI
```

This is the primary architectural constraint for future implementation work.
