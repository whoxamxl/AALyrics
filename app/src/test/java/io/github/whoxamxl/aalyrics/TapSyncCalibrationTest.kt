package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookup
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsLookupId
import io.github.whoxamxl.aalyrics.core.lyrics.LyricsState
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.core.model.PlaybackSnapshot
import io.github.whoxamxl.aalyrics.core.model.PlaybackStatus
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.core.timing.LyricsTimingOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TapSyncCalibrationTest {
    private val track = Track(title = "Song", durationMs = 120_000L)
    private var now = 1_000L
    private var playback = PlaybackSnapshot(
        track = track, status = PlaybackStatus.PLAYING,
        positionMs = 5_000L, positionUpdatedAtMonotonicMs = now,
    )
    private val document = LyricsDocument(listOf(
        TimedLyricLine("First", 10_000L),
        TimedLyricLine("Second", 15_000L),
        TimedLyricLine("Third", 20_000L),
        TimedLyricLine("Fourth", 25_000L),
    ))
    private fun ready(doc: LyricsDocument = document, id: Long = 1L) = LyricsState.Ready(
        LyricsLookup(LyricsLookupId(id), track, requireNotNull(playback.trackIdentity)), doc,
    )
    private var lyrics: LyricsState = ready()
    private var saved = 700L
    private var writes = 0
    private val settings = LyricsTimingSettings({ saved }, { saved = it; writes++ })
    private val calibration = TapSyncCalibration(settings, { playback }, { lyrics }, { now })

    @Test
    fun `three exact-time samples average and replace the old positive offset`() {
        calibration.start()
        val originalTargets = assertIs<TapSyncState.Listening>(calibration.state.value).targets
        now = 5_200L // Raw playback 9,200; offset = +800.
        calibration.tap()
        assertEquals(0, writes)
        now = 10_400L // Raw playback 14,400; offset = +600.
        calibration.tap()
        assertEquals(originalTargets, assertIs<TapSyncState.Listening>(calibration.state.value).targets)
        assertEquals(700L, saved)
        now = 15_300L // Raw playback 19,300; offset = +700.
        calibration.tap()
        assertEquals(TapSyncState.Completed(700L), calibration.state.value)
        assertEquals(700L, saved) // Not +1,400; replaces rather than adds to the old calibration.
        assertEquals(1, writes)
        assertEquals(700L, LyricsTimingSettings({ saved }, {}).offset.value.milliseconds)
        assertEquals(5_000L, playback.positionMs)
        assertEquals(document, (lyrics as LyricsState.Ready).lyrics)
    }

    @Test
    fun `hearing lines later produces a negative delay and manual fine tuning still works`() {
        calibration.start()
        listOf(6_600L, 11_400L, 16_500L).forEach { now = it; calibration.tap() }
        assertEquals(-500L, saved)
        settings.adjustBy(100L)
        assertEquals(-400L, saved)
        settings.resetToDefaults()
        assertEquals(0L, saved)
    }

    @Test
    fun `uses playback speed and sample-time fallback at the tap instant`() {
        playback = playback.copy(
            playbackRate = 2f,
            positionUpdatedAtMonotonicMs = null,
            positionSampledAtMonotonicMs = 1_000L,
        )
        calibration.start()
        listOf(3_250L, 5_750L, 8_250L).forEach { now = it; calibration.tap() }
        assertEquals(500L, saved)
    }

    @Test
    fun `regular source updates preserve samples and targets`() {
        calibration.start()
        now = 5_500L
        calibration.tap()
        playback = playback.copy(positionMs = 9_500L, positionUpdatedAtMonotonicMs = now)
        calibration.onSourceChanged()
        now = 10_500L
        calibration.tap()
        now = 15_500L
        calibration.tap()
        assertEquals(500L, saved)
    }

    @Test
    fun `cancel discards partial samples and restarting starts at zero taps`() {
        calibration.start()
        now = 5_500L
        calibration.tap()
        calibration.cancel()
        calibration.tap()
        assertEquals(TapSyncState.Idle, calibration.state.value)
        assertEquals(0, writes)
        calibration.start()
        assertEquals(0, assertIs<TapSyncState.Listening>(calibration.state.value).completedTaps)
    }

    @Test
    fun `rapid double tap does not record the next lyric`() {
        calibration.start()
        now = 5_500L
        calibration.tap()
        now += 100L
        calibration.tap()
        assertEquals(1, assertIs<TapSyncState.Listening>(calibration.state.value).completedTaps)
        assertEquals(0, writes)
    }

    @Test
    fun `pause and rate changes end an incomplete session`() {
        for (changed in listOf(playback.copy(status = PlaybackStatus.PAUSED), playback.copy(playbackRate = 2f))) {
            calibration.start()
            val original = playback
            playback = changed
            calibration.onSourceChanged()
            assertEquals(TapSyncState.Stopped(TapSyncStopReason.SOURCE_CHANGED), calibration.state.value)
            calibration.tap()
            assertEquals(0, writes)
            playback = original
        }
    }

    @Test
    fun `seek in another player is rejected even before the source collector runs`() {
        calibration.start()
        now = 5_500L
        calibration.tap()
        playback = playback.copy(positionMs = 35_000L, positionUpdatedAtMonotonicMs = now)
        calibration.tap()
        assertEquals(TapSyncState.Stopped(TapSyncStopReason.SOURCE_CHANGED), calibration.state.value)
        assertEquals(0, writes)
    }

    @Test
    fun `track changes and lyrics refresh cannot apply stale taps`() {
        calibration.start()
        playback = playback.copy(track = Track(title = "Other"))
        calibration.tap()
        assertIs<TapSyncState.Stopped>(calibration.state.value)
        playback = playback.copy(track = track)
        calibration.start()
        lyrics = ready(id = 2L)
        calibration.onSourceChanged()
        assertIs<TapSyncState.Stopped>(calibration.state.value)
        assertEquals(0, writes)
    }

    @Test
    fun `replaced lyric timestamps and loading invalidate a session`() {
        for (replacement in listOf(
            ready(document.copy(lines = document.lines.drop(1))),
            LyricsState.Loading((lyrics as LyricsState.Ready).lookup),
        )) {
            lyrics = ready()
            calibration.start()
            lyrics = replacement
            calibration.onSourceChanged()
            assertIs<TapSyncState.Stopped>(calibration.state.value)
        }
        assertEquals(0, writes)
    }

    @Test
    fun `a changed manual offset prevents an old session from overwriting it`() {
        calibration.start()
        settings.adjustBy(100L)
        now = 5_500L
        calibration.tap()
        assertIs<TapSyncState.Stopped>(calibration.state.value)
        assertEquals(800L, saved)
        assertEquals(1, writes)
    }

    @Test
    fun `out of range tap keeps the previously saved calibration`() {
        calibration.start()
        now = 50_000L
        calibration.tap()
        assertEquals(TapSyncState.Stopped(TapSyncStopReason.OUT_OF_RANGE), calibration.state.value)
        assertEquals(700L, saved)
        assertEquals(0, writes)
    }

    @Test
    fun `only distinct nonblank timed starts are selected including zero before first lyric`() {
        val mixed = LyricsDocument(listOf(
            TimedLyricLine("", 0L),
            TimedLyricLine("Zero", 0L),
            TimedLyricLine("Same timestamp", 0L),
            PlainLyricLine("Credits"),
            TimedLyricLine("Later", 5_000L),
            TimedLyricLine("   ", 7_000L),
            TimedLyricLine("Last", 10_000L),
        ))
        val targets = nextTapSyncTargets(playback.copy(positionMs = 0L), ready(mixed), LyricsTimingOffset(-500L), now)
        assertEquals(listOf("Zero", "Later", "Last"), targets.map { it.text })
        assertEquals(listOf(1, 4, 6), targets.map { it.lineIndex })
    }

    @Test
    fun `cannot start with plain lyrics paused playback or fewer than three remaining lines`() {
        lyrics = ready(LyricsDocument(listOf(PlainLyricLine("Plain"))))
        calibration.start()
        assertEquals(TapSyncState.Idle, calibration.state.value)
        lyrics = ready()
        playback = playback.copy(status = PlaybackStatus.PAUSED)
        calibration.start()
        assertEquals(TapSyncState.Idle, calibration.state.value)
        playback = playback.copy(status = PlaybackStatus.PLAYING, positionMs = 19_000L)
        calibration.start()
        assertEquals(TapSyncState.Idle, calibration.state.value)
        assertEquals(0, writes)
    }
}
