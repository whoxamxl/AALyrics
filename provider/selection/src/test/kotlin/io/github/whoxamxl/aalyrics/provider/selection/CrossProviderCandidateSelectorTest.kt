package io.github.whoxamxl.aalyrics.provider.selection

import io.github.whoxamxl.aalyrics.core.lyrics.CandidateSelectionPreferences
import io.github.whoxamxl.aalyrics.core.model.LyricLine
import io.github.whoxamxl.aalyrics.core.model.LyricsAttribution
import io.github.whoxamxl.aalyrics.core.model.LyricsDocument
import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.core.model.PlainLyricLine
import io.github.whoxamxl.aalyrics.core.model.TimedLyricLine
import io.github.whoxamxl.aalyrics.core.model.TimedWord
import io.github.whoxamxl.aalyrics.core.model.Track
import io.github.whoxamxl.aalyrics.provider.api.LyricsCandidate
import io.github.whoxamxl.aalyrics.provider.api.LyricsCandidateEvidence
import io.github.whoxamxl.aalyrics.provider.api.LyricsProviderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CrossProviderCandidateSelectorTest {
    private val selector = CrossProviderCandidateSelector()

    @Test
    fun japaneseTrackPrefersPetitLyricsWhenMetadataIsEquallyStrong() {
        val track = Track(
            title = "みずいろの雨",
            artists = listOf("Junko Yagami"),
            album = "Test Album",
            durationMs = 204_000L,
        )
        val lrcLib = candidate(
            providerId = "lrclib",
            title = track.title,
            artist = "Junko Yagami",
            album = track.album,
            durationMs = 204_000L,
            syncType = LyricsSyncType.LINE,
        )
        val petit = candidate(
            providerId = "petitlyrics",
            title = track.title,
            artist = "八神純子",
            album = track.album,
            durationMs = null,
            syncType = LyricsSyncType.WORD,
        )

        val selected = selector.select(track, listOf(lrcLib, petit))

        assertEquals("petitlyrics", selected?.providerId?.value)
    }

    @Test
    fun interleavedRomanizationReceivesQualityPenalty() {
        val track = Track("アイドル", listOf("YOASOBI"), durationMs = 210_000L)
        val contaminated = candidate(
            providerId = "lrclib",
            title = track.title,
            artist = "YOASOBI",
            durationMs = 210_000L,
            syncType = LyricsSyncType.LINE,
            lines = listOf(
                timed(1_000, "これは日本語です"),
                timed(2_000, "kore wa nihongo desu"),
                timed(3_000, "次の日本語です"),
                timed(4_000, "tsugi no nihongo desu"),
                timed(5_000, "さらに日本語です"),
                timed(6_000, "sarani nihongo desu"),
                timed(7_000, "最後の日本語です"),
                timed(8_000, "saigo no nihongo desu"),
            ),
        )
        val clean = candidate(
            providerId = "petitlyrics",
            title = track.title,
            artist = "YOASOBI",
            durationMs = 210_000L,
            syncType = LyricsSyncType.LINE,
            lines = listOf(
                timed(1_000, "これは日本語です"),
                timed(2_000, "次の日本語です"),
                timed(3_000, "さらに日本語です"),
                timed(4_000, "最後の日本語です"),
                timed(5_000, "別の日本語です"),
                timed(6_000, "終わりの日本語です"),
            ),
        )

        val badScore = selector.lyricsQualityScore(contaminated, track.durationMs)
        val cleanScore = selector.lyricsQualityScore(clean, track.durationMs)

        assertTrue(badScore < cleanScore)
        assertTrue(badScore < 0.80)
    }

    @Test
    fun romanizedArtistVsJapaneseArtistIsNeutralWithAlbumEvidence() {
        val track = Track("巡恋歌", listOf("Tsuyoshi Nagabuchi"), "風は南から", 215_000L)
        val petit = candidate(
            providerId = "petitlyrics",
            title = "巡恋歌",
            artist = "長渕 剛",
            album = "風は南から",
            durationMs = null,
            syncType = LyricsSyncType.LINE,
        )

        val metadata = selector.metadataScore(track, petit)

        assertTrue(metadata != null && metadata > 0.95)
    }

    @Test
    fun artistConstrainedQueryCorroboratesCrossScriptArtist() {
        val track = Track(
            title = "君を忘れない",
            artists = listOf("Chiharu Matsuyama"),
            album = "TOUR",
            durationMs = 288_000L,
        )
        val petit = candidate(
            providerId = "petitlyrics",
            title = "君を忘れない",
            artist = "松山千春",
            album = "別アルバム",
            durationMs = null,
            syncType = LyricsSyncType.WORD,
            artistQueryCorroborated = true,
        )

        val metadata = selector.metadataScore(track, petit)

        assertTrue(metadata != null && metadata > 0.90)
    }

    @Test
    fun titleOnlyCrossScriptArtistMismatchWithoutSecondaryEvidenceIsRejected() {
        val track = Track("同じタイトル", listOf("Romanized Artist"), durationMs = 200_000L)
        val candidate = candidate(
            providerId = "petitlyrics",
            title = "同じタイトル",
            artist = "別の歌手",
            durationMs = null,
            syncType = LyricsSyncType.WORD,
        )

        assertNull(selector.metadataScore(track, candidate))
    }

    @Test
    fun compositeMediaArtistAcceptsExactContributorForExactTitle() {
        val track = Track(
            title = "Under the Sea",
            artists = listOf("Alan Menken, Howard Ashman, Samuel E. Wright, Disney"),
            album = "The Little Mermaid",
            durationMs = 195_000L,
        )
        val candidate = candidate(
            providerId = "petitlyrics",
            title = "Under the Sea",
            artist = "Samuel E. Wright",
            album = "The Little Mermaid",
            durationMs = null,
            syncType = LyricsSyncType.WORD,
        )

        val metadata = selector.metadataScore(track, candidate)

        assertTrue(metadata != null && metadata > 0.95)
    }

    @Test
    fun compositeMediaArtistStillRejectsUnrelatedArtist() {
        val track = Track(
            title = "Under the Sea",
            artists = listOf("Alan Menken, Howard Ashman, Samuel E. Wright, Disney"),
            durationMs = 195_000L,
        )
        val unrelated = candidate(
            providerId = "petitlyrics",
            title = "Under the Sea",
            artist = "Completely Different Singer",
            durationMs = null,
            syncType = LyricsSyncType.WORD,
        )

        assertNull(selector.metadataScore(track, unrelated))
    }

    @Test
    fun westernTrackKeepsLrcLibPreferenceInStandardMode() {
        val track = Track("Example Song", listOf("Example Artist"), "Example Album", 200_000L)
        val lrcLib = candidate(
            providerId = "lrclib",
            title = track.title,
            artist = "Example Artist",
            album = track.album,
            durationMs = 200_000L,
            syncType = LyricsSyncType.LINE,
        )
        val musixmatch = candidate(
            providerId = "musixmatch",
            title = track.title,
            artist = "Example Artist",
            album = track.album,
            durationMs = 200_000L,
            syncType = LyricsSyncType.WORD,
        )

        val selected = selector.select(track, listOf(lrcLib, musixmatch))

        assertEquals("lrclib", selected?.providerId?.value)
    }

    @Test
    fun westernTrackPrefersEquivalentWordSyncWhenRequested() {
        val track = Track("Example Song", listOf("Example Artist"), "Example Album", 200_000L)
        val lrcLib = candidate(
            providerId = "lrclib",
            title = track.title,
            artist = "Example Artist",
            album = track.album,
            durationMs = 200_000L,
            syncType = LyricsSyncType.LINE,
        )
        val musixmatch = candidate(
            providerId = "musixmatch",
            title = track.title,
            artist = "Example Artist",
            album = track.album,
            durationMs = 200_000L,
            syncType = LyricsSyncType.WORD,
        )

        val selected = selector.select(
            track = track,
            candidates = listOf(lrcLib, musixmatch),
            preferences = CandidateSelectionPreferences(preferredSyncType = LyricsSyncType.WORD),
        )

        assertEquals("musixmatch", selected?.providerId?.value)
    }

    @Test
    fun wordPreferenceRequiresActualWordPayload() {
        val track = Track("Example Song", listOf("Example Artist"), "Example Album", 200_000L)
        val lrcLib = candidate(
            providerId = "lrclib",
            title = track.title,
            artist = "Example Artist",
            album = track.album,
            durationMs = 200_000L,
            syncType = LyricsSyncType.LINE,
        )
        val lineOnly = candidate(
            providerId = "musixmatch",
            title = track.title,
            artist = "Example Artist",
            album = track.album,
            durationMs = 200_000L,
            syncType = LyricsSyncType.LINE,
        )

        val selected = selector.select(
            track = track,
            candidates = listOf(lrcLib, lineOnly),
            preferences = CandidateSelectionPreferences(preferredSyncType = LyricsSyncType.WORD),
        )

        assertEquals("lrclib", selected?.providerId?.value)
    }

    @Test
    fun wordPreferenceDoesNotOverrideMateriallyWorsePayload() {
        val track = Track("Example Song", listOf("Example Artist"), "Example Album", 200_000L)
        val lrcLib = candidate(
            providerId = "lrclib",
            title = track.title,
            artist = "Example Artist",
            album = track.album,
            durationMs = 200_000L,
            syncType = LyricsSyncType.LINE,
        )
        val weakLines = (0 until 10).map { index ->
            val start = 1_000L + index * 4_000L
            timedWithWords(start, "word$index")
        }
        val weakWordSync = candidate(
            providerId = "musixmatch",
            title = track.title,
            artist = "Example Artist",
            album = track.album,
            durationMs = 200_000L,
            syncType = LyricsSyncType.WORD,
            lines = weakLines,
        )

        val selected = selector.select(
            track = track,
            candidates = listOf(lrcLib, weakWordSync),
            preferences = CandidateSelectionPreferences(preferredSyncType = LyricsSyncType.WORD),
        )

        assertEquals("lrclib", selected?.providerId?.value)
    }

    @Test
    fun synchronizedCandidateAlwaysBeatsPlainLyrics() {
        val track = Track("テスト曲", listOf("テスト歌手"), durationMs = 180_000L)
        val plain = candidate(
            providerId = "lrclib",
            title = track.title,
            artist = "テスト歌手",
            durationMs = 180_000L,
            syncType = LyricsSyncType.PLAIN,
        )
        val synced = candidate(
            providerId = "petitlyrics",
            title = track.title,
            artist = "テスト歌手",
            durationMs = null,
            syncType = LyricsSyncType.LINE,
        )

        val selected = selector.select(track, listOf(plain, synced))

        assertEquals("petitlyrics", selected?.providerId?.value)
    }

    @Test
    fun explicitAlbumEvidenceCanCorroborateTitleVersionDifference() {
        val track = Track("Song", listOf("Artist"), "Live at Wembley", 200_000L)
        val candidate = candidate(
            providerId = "lrclib",
            title = "Song (Live)",
            artist = "Artist",
            album = "Live at Wembley",
            durationMs = 200_000L,
            syncType = LyricsSyncType.LINE,
        )

        assertNotNull(selector.metadataScore(track, candidate))
    }

    @Test
    fun ordinaryAlbumNameIsNotVersionEvidence() {
        val track = Track("Song", listOf("Artist"), "Live Through This", 200_000L)
        val candidate = candidate(
            providerId = "lrclib",
            title = "Song (Live)",
            artist = "Artist",
            album = "Live Through This",
            durationMs = 200_000L,
            syncType = LyricsSyncType.LINE,
        )

        assertNull(selector.metadataScore(track, candidate))
    }

    @Test
    fun bracketedAlbumProseIsNotVersionEvidence() {
        val track = Track("Song", listOf("Artist"), "Album (We Live Here)", 200_000L)
        val candidate = candidate(
            providerId = "lrclib",
            title = "Song (Live)",
            artist = "Artist",
            durationMs = 200_000L,
            syncType = LyricsSyncType.LINE,
        )

        assertNull(selector.metadataScore(track, candidate))
    }

    @Test
    fun largeDurationMismatchIsRejected() {
        val track = Track("Example Song", listOf("Example Artist"), durationMs = 200_000L)
        val candidate = candidate(
            providerId = "lrclib",
            title = track.title,
            artist = "Example Artist",
            durationMs = 230_000L,
            syncType = LyricsSyncType.LINE,
        )

        assertNull(selector.metadataScore(track, candidate))
    }

    @Test
    fun exactScoreTieIsIndependentFromCandidateOrder() {
        val track = Track("Same Song", listOf("Same Artist"), durationMs = 180_000L)
        val alpha = candidate(
            providerId = "alpha",
            title = track.title,
            artist = "Same Artist",
            durationMs = 180_000L,
            syncType = LyricsSyncType.LINE,
        )
        val beta = candidate(
            providerId = "beta",
            title = track.title,
            artist = "Same Artist",
            durationMs = 180_000L,
            syncType = LyricsSyncType.LINE,
        )

        val forward = selector.select(track, listOf(alpha, beta))
        val reverse = selector.select(track, listOf(beta, alpha))

        assertEquals(forward?.providerId, reverse?.providerId)
    }

    private fun candidate(
        providerId: String,
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long?,
        syncType: LyricsSyncType,
        artistQueryCorroborated: Boolean = false,
        lines: List<LyricLine>? = null,
    ): LyricsCandidate {
        val resolvedLines = lines ?: defaultLines(syncType)
        return LyricsCandidate(
            providerId = LyricsProviderId(providerId),
            matchedTrack = Track(
                title = title,
                artists = listOf(artist),
                album = album,
                durationMs = durationMs,
            ),
            lyrics = LyricsDocument(
                lines = resolvedLines,
                attribution = LyricsAttribution(
                    providerId = providerId,
                    sourceId = "$providerId:test",
                ),
            ),
            evidence = LyricsCandidateEvidence(
                artistQueryCorroborated = artistQueryCorroborated,
            ),
        ).also {
            assertEquals(syncType, it.lyrics.syncType)
        }
    }

    private fun defaultLines(syncType: LyricsSyncType): List<LyricLine> {
        val rows = listOf(
            1_000L to "test line one",
            10_000L to "test line two",
            20_000L to "test line three",
            30_000L to "test line four",
        )
        return when (syncType) {
            LyricsSyncType.PLAIN -> rows.map { (_, text) -> PlainLyricLine(text) }
            LyricsSyncType.LINE -> rows.map { (start, text) -> timed(start, text) }
            LyricsSyncType.WORD -> rows.map { (start, text) -> timedWithWords(start, text) }
        }
    }

    private fun timed(startMs: Long, text: String): TimedLyricLine {
        return TimedLyricLine(text = text, startMs = startMs)
    }

    private fun timedWithWords(startMs: Long, text: String): TimedLyricLine {
        return TimedLyricLine(
            text = text,
            startMs = startMs,
            words = listOf(TimedWord(text = text, startMs = startMs)),
        )
    }
}
