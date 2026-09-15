package io.github.whoxamxl.aalyrics.core.lyrics

import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.Track

/**
 * Identity of one lyrics lookup lifecycle.
 *
 * Track equality alone is not sufficient for stale-result rejection because the
 * same track may be looked up again (for example after a manual refresh). The
 * coordinator will therefore assign a fresh id to every lookup attempt.
 */
@JvmInline
value class LyricsLookupId(val value: Long) {
    init {
        require(value >= 0L) { "Lyrics lookup id must not be negative" }
    }
}

/** The track and unique identity belonging to one lookup lifecycle. */
data class LyricsLookup(
    val id: LyricsLookupId,
    val track: Track,
)

/**
 * Provider-independent state exposed by the lyrics core.
 *
 * Presentation layers consume this type without knowing which providers were
 * queried or how candidates were ranked.
 */
sealed interface LyricsState {
    /** No track currently owns a lyrics lookup. */
    data object Idle : LyricsState

    /** State associated with one active or completed lookup lifecycle. */
    sealed interface ForLookup : LyricsState {
        val lookup: LyricsLookup
    }

    data class Loading(
        override val lookup: LyricsLookup,
    ) : ForLookup

    data class Ready(
        override val lookup: LyricsLookup,
        val lyrics: LyricsDocument,
    ) : ForLookup {
        init {
            require(!lyrics.isEmpty) { "Ready lyrics must not be empty" }
        }
    }

    /**
     * Usable lyrics were resolved, but one or more source attempts failed.
     *
     * Provider identities and raw exceptions deliberately stay out of shared UI
     * state; diagnostics can remain internal to orchestration.
     */
    data class Degraded(
        override val lookup: LyricsLookup,
        val lyrics: LyricsDocument,
        val failedAttempts: Int,
    ) : ForLookup {
        init {
            require(!lyrics.isEmpty) { "Degraded lyrics must not be empty" }
            require(failedAttempts > 0) { "Degraded state requires at least one failed attempt" }
        }
    }

    /** All completed source attempts returned without usable lyrics. */
    data class NotFound(
        override val lookup: LyricsLookup,
    ) : ForLookup

    /** No usable lyrics could be produced because lookup attempts failed. */
    data class Failed(
        override val lookup: LyricsLookup,
        val failedAttempts: Int,
    ) : ForLookup {
        init {
            require(failedAttempts > 0) { "Failed state requires at least one failed attempt" }
        }
    }
}

/** Events accepted by the pure lyrics-state lifecycle. */
sealed interface LyricsStateEvent {
    /** Starts a new lookup and supersedes every previous lookup. */
    data class Started(
        val lookup: LyricsLookup,
    ) : LyricsStateEvent

    /** Clears ownership when there is no active track. */
    data object Cleared : LyricsStateEvent

    /** Completion events must carry the lookup id that produced them. */
    sealed interface Completion : LyricsStateEvent {
        val lookupId: LyricsLookupId
    }

    data class Resolved(
        override val lookupId: LyricsLookupId,
        val lyrics: LyricsDocument,
    ) : Completion

    data class ResolvedDegraded(
        override val lookupId: LyricsLookupId,
        val lyrics: LyricsDocument,
        val failedAttempts: Int,
    ) : Completion

    data class NoLyrics(
        override val lookupId: LyricsLookupId,
    ) : Completion

    data class Failed(
        override val lookupId: LyricsLookupId,
        val failedAttempts: Int,
    ) : Completion
}

/**
 * Applies one lifecycle event without Android, networking, coroutines, or a
 * concrete provider dependency.
 *
 * Only the currently loading lookup may complete. A completion from an older
 * lookup, or a second completion after a terminal state was published, is
 * ignored. This gives the future coordinator a small, testable stale-result
 * guard instead of relying on track equality or provider timing.
 */
fun LyricsState.reduce(event: LyricsStateEvent): LyricsState = when (event) {
    is LyricsStateEvent.Started -> LyricsState.Loading(event.lookup)
    LyricsStateEvent.Cleared -> LyricsState.Idle
    is LyricsStateEvent.Resolved -> completeIfCurrent(event.lookupId) { lookup ->
        LyricsState.Ready(lookup = lookup, lyrics = event.lyrics)
    }
    is LyricsStateEvent.ResolvedDegraded -> completeIfCurrent(event.lookupId) { lookup ->
        LyricsState.Degraded(
            lookup = lookup,
            lyrics = event.lyrics,
            failedAttempts = event.failedAttempts,
        )
    }
    is LyricsStateEvent.NoLyrics -> completeIfCurrent(event.lookupId) { lookup ->
        LyricsState.NotFound(lookup)
    }
    is LyricsStateEvent.Failed -> completeIfCurrent(event.lookupId) { lookup ->
        LyricsState.Failed(lookup = lookup, failedAttempts = event.failedAttempts)
    }
}

private inline fun LyricsState.completeIfCurrent(
    lookupId: LyricsLookupId,
    completion: (LyricsLookup) -> LyricsState,
): LyricsState {
    val loading = this as? LyricsState.Loading ?: return this
    if (loading.lookup.id != lookupId) return this
    return completion(loading.lookup)
}
