package io.github.whoxamxl.aalyrics.core.lyrics

import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.core.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class LyricsStateTest {
    private val track = Track(
        title = "Example",
        artists = listOf("Artist"),
        durationMs = 180_000L,
    )

    private val lyrics = LyricsDocument(
        lines = listOf(PlainLyricLine("Hello")),
    )

    @Test
    fun `matching completion resolves current loading lookup`() {
        val lookup = LyricsLookup(LyricsLookupId(1), track)
        val loading = LyricsState.Idle.reduce(LyricsStateEvent.Started(lookup))

        val resolved = loading.reduce(
            LyricsStateEvent.Resolved(
                lookupId = lookup.id,
                lyrics = lyrics,
            ),
        )

        assertEquals(LyricsState.Ready(lookup, lyrics), resolved)
    }

    @Test
    fun `new lookup rejects stale completion even for the same track`() {
        val first = LyricsLookup(LyricsLookupId(10), track)
        val second = LyricsLookup(LyricsLookupId(11), track)

        val current = LyricsState.Idle
            .reduce(LyricsStateEvent.Started(first))
            .reduce(LyricsStateEvent.Started(second))

        val afterStaleResult = current.reduce(
            LyricsStateEvent.Resolved(
                lookupId = first.id,
                lyrics = lyrics,
            ),
        )

        assertSame(current, afterStaleResult)
        assertEquals(LyricsState.Loading(second), afterStaleResult)
    }

    @Test
    fun `terminal state ignores a second completion`() {
        val lookup = LyricsLookup(LyricsLookupId(20), track)
        val notFound = LyricsState.Idle
            .reduce(LyricsStateEvent.Started(lookup))
            .reduce(LyricsStateEvent.NoLyrics(lookup.id))

        val lateResolved = notFound.reduce(
            LyricsStateEvent.Resolved(
                lookupId = lookup.id,
                lyrics = lyrics,
            ),
        )

        assertSame(notFound, lateResolved)
    }

    @Test
    fun `degraded state preserves usable lyrics without provider details`() {
        val lookup = LyricsLookup(LyricsLookupId(30), track)
        val degraded = LyricsState.Idle
            .reduce(LyricsStateEvent.Started(lookup))
            .reduce(
                LyricsStateEvent.ResolvedDegraded(
                    lookupId = lookup.id,
                    lyrics = lyrics,
                    failedAttempts = 2,
                ),
            )

        val state = assertIs<LyricsState.Degraded>(degraded)
        assertEquals(lookup, state.lookup)
        assertEquals(lyrics, state.lyrics)
        assertEquals(2, state.failedAttempts)
    }

    @Test
    fun `clear returns lifecycle to idle`() {
        val lookup = LyricsLookup(LyricsLookupId(40), track)
        val loading = LyricsState.Idle.reduce(LyricsStateEvent.Started(lookup))

        assertEquals(LyricsState.Idle, loading.reduce(LyricsStateEvent.Cleared))
    }

    @Test
    fun `resolved states require usable lyrics`() {
        val lookup = LyricsLookup(LyricsLookupId(50), track)
        val empty = LyricsDocument(lines = emptyList())

        assertFailsWith<IllegalArgumentException> {
            LyricsState.Ready(lookup, empty)
        }
        assertFailsWith<IllegalArgumentException> {
            LyricsState.Degraded(lookup, empty, failedAttempts = 1)
        }
    }

    @Test
    fun `failure counts and lookup ids enforce invariants`() {
        val lookup = LyricsLookup(LyricsLookupId(60), track)

        assertFailsWith<IllegalArgumentException> {
            LyricsLookupId(-1)
        }
        assertFailsWith<IllegalArgumentException> {
            LyricsState.Degraded(lookup, lyrics, failedAttempts = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            LyricsState.Failed(lookup, failedAttempts = 0)
        }
    }
}
