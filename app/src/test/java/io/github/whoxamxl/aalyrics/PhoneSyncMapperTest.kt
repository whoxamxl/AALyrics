package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookup
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookupId
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.core.timing.LyricsTimingOffset
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportInteractionMode
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportLineUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.TrackCardLyricsStatus
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.TrackCardUiState
import io.github.whoxamxl.aalyrics.ui.phone.sync.SyncAdjustment
import io.github.whoxamxl.aalyrics.ui.phone.sync.SyncAvailability
import io.github.whoxamxl.aalyrics.ui.phone.sync.TapSyncAvailability
import io.github.whoxamxl.aalyrics.ui.phone.sync.TapSyncMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhoneSyncMapperTest {
    private val lines = listOf(
        LyricsViewportLineUiState("First", translatedText = "第一句"),
        LyricsViewportLineUiState("Second", translatedText = "第二句"),
        LyricsViewportLineUiState("Third"),
    )

    private fun lyrics(
        status: TrackCardLyricsStatus = TrackCardLyricsStatus.READY,
        sync: LyricsSyncType = LyricsSyncType.LINE,
        index: Int? = 1,
    ) = LyricsScreenUiState(
        trackCard = TrackCardUiState(title = "Song", lyricsStatus = status),
        viewport = LyricsViewportUiState(lines = lines, syncType = sync, currentLineIndex = index),
    )

    @Test
    fun `preview reuses canonical line and translation selection`() {
        val state = mapPhoneSyncState(lyrics(), LyricsTimingOffset(300L))
        assertEquals(300L, state.offsetMs)
        assertEquals(lines[0], state.previousLine)
        assertEquals(lines[1], state.currentLine)
        assertEquals(lines[2], state.nextLine)
        assertTrue(state.canAdvance && state.canDelay)
    }

    @Test
    fun `no timed lyrics disables adjustment but retains saved offset for reset`() {
        val cases = listOf(
            lyrics(status = TrackCardLyricsStatus.IDLE) to SyncAvailability.NO_MEDIA,
            lyrics(status = TrackCardLyricsStatus.LOADING) to SyncAvailability.LOADING,
            lyrics(status = TrackCardLyricsStatus.NOT_FOUND) to SyncAvailability.UNAVAILABLE,
            lyrics(status = TrackCardLyricsStatus.FAILED) to SyncAvailability.UNAVAILABLE,
            lyrics(sync = LyricsSyncType.PLAIN) to SyncAvailability.PLAIN,
        )
        for ((input, expected) in cases) {
            val state = mapPhoneSyncState(input, LyricsTimingOffset(-500L))
            assertEquals(expected, state.availability)
            assertEquals(-500L, state.offsetMs)
            assertFalse(state.canAdvance || state.canDelay)
            assertNull(state.previousLine)
            assertNull(state.currentLine)
            assertNull(state.nextLine)
        }
    }

    @Test
    fun `before first line shows upcoming line and limits only disable outward actions`() {
        val before = mapPhoneSyncState(lyrics(index = null), LyricsTimingOffset(-30_000L))
        assertNull(before.currentLine)
        assertEquals(lines.first(), before.nextLine)
        assertTrue(before.canAdvance)
        assertFalse(before.canDelay)
        val end = mapPhoneSyncState(lyrics(index = 2), LyricsTimingOffset(30_000L))
        assertNull(end.nextLine)
        assertFalse(end.canAdvance)
        assertTrue(end.canDelay)
    }

    @Test
    fun `button actions immediately move paused preview and resetting restores original line`() {
        val track = Track(title = "Song", durationMs = 20_000L)
        val playback = PlaybackSnapshot(track = track, positionMs = 4_800L, status = PlaybackStatus.PAUSED)
        val document = LyricsDocument(listOf(
            TimedLyricLine("First", 0L),
            TimedLyricLine("Second", 5_000L),
        ))
        val ready = LyricsState.Ready(
            LyricsLookup(LyricsLookupId(1L), track, requireNotNull(playback.trackIdentity)), document,
        )
        var stored = 0L
        val settings = LyricsTimingSettings({ stored }, { stored = it })
        fun preview() = mapPhoneLyricsState(
            playback = playback,
            lyricsState = ready,
            plainLyricsAutoScrollEnabled = true,
            interactionMode = LyricsViewportInteractionMode.FOLLOW,
            currentMonotonicTimeMs = 20_000L,
            lyricsTimingOffset = settings.offset.value,
        ).let { mapPhoneSyncState(it, settings.offset.value) }

        repeat(3) {
            settings.adjustBy(SyncAdjustment.ADVANCE_FINE.deltaMs())
            assertEquals("First", preview().currentLine?.text)
        }
        settings.adjustBy(SyncAdjustment.ADVANCE_FINE.deltaMs())
        assertEquals("Second", preview().currentLine?.text)
        settings.adjustBy(SyncAdjustment.DELAY_COARSE.deltaMs())
        assertEquals("First", preview().currentLine?.text)
        settings.adjustBy(SyncAdjustment.ADVANCE_COARSE.deltaMs())
        assertEquals("Second", preview().currentLine?.text)
        settings.adjustBy(SyncAdjustment.DELAY_FINE.deltaMs())
        assertEquals("First", preview().currentLine?.text)
        settings.resetToDefaults()
        assertEquals(0L, preview().offsetMs)
        assertEquals("First", preview().currentLine?.text)
        assertEquals(4_800L, playback.positionMs)
        assertEquals(5_000L, (document.lines[1] as TimedLyricLine).startMs)
    }

    @Test
    fun `tap panel uses frozen targets while existing fine controls remain available`() {
        val listening = TapSyncState.Listening(listOf(
            TapSyncTarget(0, 10_000L, "First"),
            TapSyncTarget(1, 20_000L, "Second"),
            TapSyncTarget(2, 30_000L, "Third"),
        ), completedTaps = 1)
        val ui = lyrics()
        val tapUi = mapPhoneTapSyncState(
            listening, PlaybackSnapshot(status = PlaybackStatus.PLAYING), LyricsState.Idle,
            ui, LyricsTimingOffset(500L), now = 100_000L,
        )
        val sync = mapPhoneSyncState(ui, LyricsTimingOffset(500L), tapUi)
        assertEquals(listOf("First", "Second", "Third"), sync.tapSync.lines)
        assertEquals(1, sync.tapSync.completedTaps)
        assertTrue(sync.tapSync.active)
        assertTrue(sync.canAdvance && sync.canDelay)
    }

    @Test
    fun `tap availability explains paused plain and end of song states`() {
        val track = Track(title = "Song")
        val playing = PlaybackSnapshot(track = track, status = PlaybackStatus.PLAYING, positionMs = 500L)
        val document = LyricsDocument(listOf(
            TimedLyricLine("First", 1_000L),
            TimedLyricLine("Second", 2_000L),
            TimedLyricLine("Third", 3_000L),
        ))
        val ready = LyricsState.Ready(
            LyricsLookup(LyricsLookupId(1), track, requireNotNull(playing.trackIdentity)), document,
        )
        fun availability(playback: PlaybackSnapshot = playing, ui: LyricsScreenUiState = lyrics()) =
            mapPhoneTapSyncState(TapSyncState.Idle, playback, ready, ui, LyricsTimingOffset.ZERO, 0).availability

        assertEquals(TapSyncAvailability.READY, availability())
        assertEquals(TapSyncAvailability.NEEDS_PLAYBACK, availability(playing.copy(status = PlaybackStatus.PAUSED)))
        assertEquals(TapSyncAvailability.NEEDS_TIMED_LYRICS, availability(ui = lyrics(sync = LyricsSyncType.PLAIN)))
        assertEquals(TapSyncAvailability.NOT_ENOUGH_LINES, availability(playing.copy(positionMs = 1_500L)))
    }

    @Test
    fun `completed and interrupted tap sessions explain the result`() {
        fun map(state: TapSyncState) = mapPhoneTapSyncState(
            state, PlaybackSnapshot(), LyricsState.Idle, lyrics(), LyricsTimingOffset(-500L), 0L,
        )
        assertEquals(-500L, map(TapSyncState.Completed(-500L)).savedOffsetMs)
        assertEquals(TapSyncMessage.SOURCE_CHANGED, map(TapSyncState.Stopped(TapSyncStopReason.SOURCE_CHANGED)).message)
        assertEquals(TapSyncMessage.OUT_OF_RANGE, map(TapSyncState.Stopped(TapSyncStopReason.OUT_OF_RANGE)).message)
        assertFalse(map(TapSyncState.Completed(-500L)).active)
    }
}
