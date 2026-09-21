package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookup
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookupId
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.LyricsAttribution
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.core.model.PlaybackSource
import io.github.whoxamxl.aalyrics.core.model.PlaybackTrackIdentity
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
            lyricsState = readyLyrics(track, playback(track).trackIdentity!!),
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
            lyricsState = readyLyrics(track, playback(track).trackIdentity!!),
            verboseDetailsEnabled = true,
            playbackSourceLabel = "Spotify",
            displayLocale = Locale.ENGLISH,
        )

        assertEquals("com.spotify.music", state.diagnostics?.appPackageName)
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
    fun `referenced identity keeps lyrics across corrected descriptive metadata`() {
        val reference = TrackReference("spotify", "4uLU6hMCjMI75M1A2tKUQC")
        val lookupTrack = Track(
            title = "Initial title",
            artists = listOf("Initial artist"),
            album = "Initial album",
            references = setOf(reference),
        )
        val currentTrack = lookupTrack.copy(
            title = "Corrected title",
            artists = listOf("Corrected artist"),
            album = "Corrected album",
        )
        val source = PlaybackSource(id = "com.spotify.music")
        val lookupPlayback = PlaybackSnapshot(track = lookupTrack, source = source)
        val currentPlayback = PlaybackSnapshot(track = currentTrack, source = source)

        val state = mapPhoneDetailsState(
            playback = currentPlayback,
            lyricsState = readyLyrics(
                lookupTrack,
                requireNotNull(lookupPlayback.trackIdentity),
            ),
            verboseDetailsEnabled = false,
            playbackSourceLabel = "Spotify",
            displayLocale = Locale.ENGLISH,
        )

        assertEquals("Musixmatch", state.lyrics?.providerDisplayName)
        assertEquals(DetailsLyricsUiStatus.READY, state.lyricsStatus)
    }

    @Test
    fun `source media identity keeps lyrics across corrected descriptive metadata`() {
        val lookupTrack = Track(
            title = "Initial title",
            artists = listOf("Initial artist"),
        )
        val currentTrack = lookupTrack.copy(
            title = "Corrected title",
            artists = listOf("Corrected artist"),
            album = "Corrected album",
        )
        val source = PlaybackSource(
            id = "com.example.player",
            mediaId = "stable-item-1",
        )
        val lookupPlayback = PlaybackSnapshot(track = lookupTrack, source = source)
        val currentPlayback = PlaybackSnapshot(track = currentTrack, source = source)

        val state = mapPhoneDetailsState(
            playback = currentPlayback,
            lyricsState = readyLyrics(
                lookupTrack,
                requireNotNull(lookupPlayback.trackIdentity),
            ),
            verboseDetailsEnabled = false,
            playbackSourceLabel = "Example Player",
            displayLocale = Locale.ENGLISH,
        )

        assertEquals("Musixmatch", state.lyrics?.providerDisplayName)
        assertEquals(DetailsLyricsUiStatus.READY, state.lyricsStatus)
    }

    @Test
    fun `late duration metadata does not hide current lyrics`() {
        val lookupTrack = currentTrack().copy(durationMs = null)
        val playbackTrack = lookupTrack.copy(durationMs = 221_000L)

        val state = mapPhoneDetailsState(
            playback = playback(playbackTrack),
            lyricsState = readyLyrics(lookupTrack, playback(lookupTrack).trackIdentity!!),
            verboseDetailsEnabled = false,
            playbackSourceLabel = "Spotify",
            displayLocale = Locale.ENGLISH,
        )

        assertEquals("3:41", state.track?.durationLabel)
        assertEquals("Musixmatch", state.lyrics?.providerDisplayName)
        assertEquals(DetailsLyricsUiStatus.READY, state.lyricsStatus)
    }

    @Test
    fun `lyrics from a different track are never exposed as current Details`() {
        val current = currentTrack()
        val staleTrack = current.copy(
            title = "Previous Track",
            references = setOf(
                TrackReference("spotify", "previous-track-reference"),
            ),
        )
        val state = mapPhoneDetailsState(
            playback = playback(current),
            lyricsState = readyLyrics(staleTrack, playback(staleTrack).trackIdentity!!),
            verboseDetailsEnabled = true,
            playbackSourceLabel = "Spotify",
            displayLocale = Locale.ENGLISH,
        )

        assertNull(state.lyrics)
        assertEquals(DetailsLyricsUiStatus.UNAVAILABLE, state.lyricsStatus)
        assertEquals("com.spotify.music", state.diagnostics?.appPackageName)
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
                    playbackIdentity = playback(track).trackIdentity!!,
                ),
            ),
            verboseDetailsEnabled = true,
            playbackSourceLabel = "Spotify",
            displayLocale = Locale.ENGLISH,
        )

        assertNull(state.lyrics)
        assertEquals(DetailsLyricsUiStatus.LOADING, state.lyricsStatus)
        assertEquals("com.spotify.music", state.diagnostics?.appPackageName)
        assertNull(state.diagnostics?.providerId)
        assertNull(state.diagnostics?.sourceId)
    }

    @Test
    fun `Verbose Details progress projects playback time and current synced line`() {
        val track = currentTrack()
        val playback = PlaybackSnapshot(
            track = track,
            status = PlaybackStatus.PLAYING,
            positionMs = 90_000L,
            playbackRate = 1.0f,
            source = PlaybackSource(id = "com.spotify.music"),
            positionUpdatedAtMonotonicMs = 100_000L,
        )

        val progress = mapPhoneDetailsVerboseProgress(
            playback = playback,
            lyricsState = readyLyrics(
                track,
                requireNotNull(playback.trackIdentity),
            ),
            currentMonotonicTimeMs = 105_000L,
        )

        assertEquals("1:35", progress.playbackPositionLabel)
        assertEquals(2, progress.currentLineNumber)
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

    private fun readyLyrics(
        track: Track,
        playbackIdentity: PlaybackTrackIdentity,
    ) = LyricsState.Ready(
        lookup = LyricsLookup(
            id = LyricsLookupId(11L),
            track = track,
            playbackIdentity = playbackIdentity,
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
