package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookup
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookupId
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.LyricsAttribution
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackSource
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.core.model.TrackReference
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsLyricsUiStatus
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhoneDetailsMapperTest {
    @Test
    fun `normal Details maps user-facing current track and lyrics fields`() {
        val track = currentTrack()
        val state = mapPhoneDetailsState(
            playback = playback(track),
            lyricsState = readyLyrics(track),
            verboseDetailsEnabled = false,
            playbackSourceLabel = "Spotify",
            displayLocale = Locale.ENGLISH,
        )

        assertEquals("Midnight Signals", state.track?.title)
        assertEquals("The Northbound Lights", state.track?.artist)
        assertEquals("Afterglow Transit", state.track?.album)
        assertEquals("3:41", state.track?.durationLabel)
        assertEquals("Spotify", state.track?.playbackSourceLabel)
        assertEquals("Musixmatch", state.lyrics?.providerDisplayName)
        assertEquals(LyricsSyncType.LINE, state.lyrics?.syncType)
        assertEquals("English", state.lyrics?.languageLabel)
        assertEquals(2, state.lyrics?.lineCount)
        assertEquals(DetailsLyricsUiStatus.READY, state.lyricsStatus)
        assertNull(state.diagnostics)
    }

    @Test
    fun `Verbose Details exposes only existing diagnostic identifiers`() {
        val track = currentTrack()
        val state = mapPhoneDetailsState(
            playback = playback(track),
            lyricsState = readyLyrics(track),
            verboseDetailsEnabled = true,
            playbackSourceLabel = "Spotify",
            displayLocale = Locale.ENGLISH,
        )

        assertEquals("musixmatch", state.diagnostics?.providerId)
        assertEquals("mxm:9384756", state.diagnostics?.sourceId)
        assertEquals(
            listOf(
                "musicbrainz:demo-mbid",
                "spotify:4uLU6hMCjMI75M1A2tKUQC",
            ),
            state.diagnostics?.trackReferences,
        )
    }

    @Test
    fun `lyrics from a different track are never exposed as current Details`() {
        val current = currentTrack()
        val staleTrack = current.copy(title = "Previous Track")
        val state = mapPhoneDetailsState(
            playback = playback(current),
            lyricsState = readyLyrics(staleTrack),
            verboseDetailsEnabled = true,
            playbackSourceLabel = "Spotify",
            displayLocale = Locale.ENGLISH,
        )

        assertNull(state.lyrics)
        assertEquals(DetailsLyricsUiStatus.UNAVAILABLE, state.lyricsStatus)
        assertNull(state.diagnostics?.providerId)
        assertNull(state.diagnostics?.sourceId)
        assertEquals(
            listOf(
                "musicbrainz:demo-mbid",
                "spotify:4uLU6hMCjMI75M1A2tKUQC",
            ),
            state.diagnostics?.trackReferences,
        )
    }

    @Test
    fun `matching active lookup maps to loading without stale lyric metadata`() {
        val track = currentTrack()
        val state = mapPhoneDetailsState(
            playback = playback(track),
            lyricsState = LyricsState.Loading(
                LyricsLookup(
                    id = LyricsLookupId(12L),
                    track = track,
                ),
            ),
            verboseDetailsEnabled = true,
            playbackSourceLabel = "Spotify",
            displayLocale = Locale.ENGLISH,
        )

        assertNull(state.lyrics)
        assertEquals(DetailsLyricsUiStatus.LOADING, state.lyricsStatus)
        assertNull(state.diagnostics?.providerId)
        assertNull(state.diagnostics?.sourceId)
    }

    private fun currentTrack() = Track(
        title = "Midnight Signals",
        artists = listOf("The Northbound Lights"),
        album = "Afterglow Transit",
        durationMs = 221_000L,
        references = setOf(
            TrackReference("spotify", "4uLU6hMCjMI75M1A2tKUQC"),
            TrackReference("musicbrainz", "demo-mbid"),
        ),
    )

    private fun playback(track: Track) = PlaybackSnapshot(
        track = track,
        source = PlaybackSource(id = "com.spotify.music"),
    )

    private fun readyLyrics(track: Track) = LyricsState.Ready(
        lookup = LyricsLookup(
            id = LyricsLookupId(11L),
            track = track,
        ),
        lyrics = LyricsDocument(
            lines = listOf(
                TimedLyricLine(
                    text = "Streetlights wake along the avenue",
                    startMs = 0L,
                ),
                TimedLyricLine(
                    text = "We carry the signal into the night",
                    startMs = 4_000L,
                ),
            ),
            languageTag = "en",
            attribution = LyricsAttribution(
                providerId = "musixmatch",
                displayName = "Musixmatch",
                sourceId = "mxm:9384756",
            ),
        ),
    )
}
