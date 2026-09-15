package io.github.whoxamxl.aalyrics.provider.matching

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

class MetadataMatchingTest {

    @Test
    fun fullWidthMetadataNormalizesToSameText() {
        assertEquals(1.0, MetadataMatching.stringSimilarity("ＡＢＣ　１２３", "abc 123"), 0.0001)
    }

    @Test
    fun commonArtistSeparatorsNormalizeConsistently() {
        assertEquals(
            1.0,
            MetadataMatching.stringSimilarity("Fabolous & Jeremih", "Fabolous, Jeremih"),
            0.0001
        )
    }

    @Test
    fun exactContributorComponentCanMatchCompositeArtistMetadata() {
        val score = MetadataMatching.artistSimilarity(
            left = "Alan Menken, Howard Ashman, Samuel E. Wright, Disney",
            right = "Samuel E. Wright",
            allowContributorComponents = true
        )

        assertEquals(0.95, score, 0.0001)
    }

    @Test
    fun contributorComponentsAreNotUsedForWeakTitleMatches() {
        val score = MetadataMatching.artistSimilarity(
            left = "Alan Menken, Howard Ashman, Samuel E. Wright, Disney",
            right = "Samuel E. Wright",
            allowContributorComponents = false
        )

        assertTrue(score < 0.95)
    }

    @Test
    fun liveVersionDoesNotMatchStudioVersion() {
        assertFalse(MetadataMatching.versionsCompatible("Song (Live)", "Song"))
        assertFalse(MetadataMatching.versionsCompatible("Song", "Song - Live"))
    }

    @Test
    fun albumVersionEvidenceCanCorroborateCandidateTitle() {
        assertTrue(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Song",
                candidateTitle = "Song (Live)",
                requestedAlbum = "Live at Wembley"
            )
        )
        assertTrue(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Song",
                candidateTitle = "Song (Live)",
                requestedAlbum = "Album: Live"
            )
        )
        assertTrue(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Song",
                candidateTitle = "Song (Live)",
                requestedAlbum = "Album: Subtitle: Live"
            )
        )
        assertTrue(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Song (Remastered)",
                candidateTitle = "Song",
                candidateAlbum = "Album: Remastered"
            )
        )
        assertTrue(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Song (Live Remastered)",
                candidateTitle = "Song",
                candidateAlbum = "Album: Live: Remastered"
            )
        )
        assertTrue(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Song (Remastered)",
                candidateTitle = "Song",
                candidateAlbum = "Live Through This: Remastered"
            )
        )
        assertTrue(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Song (Remastered)",
                candidateTitle = "Song",
                candidateAlbum = "Album (Remastered)"
            )
        )
        assertTrue(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Song (Remastered)",
                candidateTitle = "Song",
                candidateAlbum = "Album (Deluxe Remastered Edition)"
            )
        )
        assertTrue(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Song (Remastered)",
                candidateTitle = "Song",
                candidateAlbum = "Album (20th Anniversary Remastered Edition)"
            )
        )
    }

    @Test
    fun albumEvidenceDoesNotEraseExplicitTitleVersionConflict() {
        assertFalse(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Song (Live)",
                candidateTitle = "Song (Remastered)",
                requestedAlbum = "Album (Live Remastered Edition)",
                candidateAlbum = "Album (Live Remastered Edition)"
            )
        )
        assertFalse(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Song (Live)",
                candidateTitle = "Song (Live Remastered)",
                requestedAlbum = "Album (Remastered Edition)",
                candidateAlbum = "Album (Remastered Edition)"
            )
        )
    }

    @Test
    fun explicitVersionSegmentBeforeOrdinaryTrailingSubtitleIsNotEvidence() {
        assertFalse(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Song",
                candidateTitle = "Song (Live)",
                requestedAlbum = "Album: Live: Subtitle"
            )
        )
    }

    @Test
    fun japaneseAnniversaryRemasterEvidenceIsRecognized() {
        assertTrue(
            MetadataMatching.versionsCompatible(
                requestedTitle = "曲名（リマスター）",
                candidateTitle = "曲名",
                candidateAlbum = "アルバム（20周年リマスター版）"
            )
        )
    }

    @Test
    fun japaneseCompoundAlbumVersionSuffixesAreRecognized() {
        assertTrue(
            MetadataMatching.versionsCompatible(
                requestedTitle = "曲名",
                candidateTitle = "曲名（ライブ・リマスター）",
                requestedAlbum = "アルバム: ライブ版: リマスター版"
            )
        )
        assertTrue(
            MetadataMatching.versionsCompatible(
                requestedTitle = "曲名",
                candidateTitle = "曲名（ライブ・リマスター）",
                requestedAlbum = "ライブ版: リマスター版"
            )
        )
        assertFalse(
            MetadataMatching.versionsCompatible(
                requestedTitle = "曲名",
                candidateTitle = "曲名（ライブ）",
                requestedAlbum = "アルバム: ライブ版: サブタイトル"
            )
        )
    }

    @Test
    fun ordinaryAlbumNameContainingLiveIsNotVersionEvidence() {
        assertFalse(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Song",
                candidateTitle = "Song (Live)",
                requestedAlbum = "Live Through This"
            )
        )
        assertFalse(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Song",
                candidateTitle = "Song (Live)",
                requestedAlbum = "Album - Live Through This"
            )
        )
    }

    @Test
    fun ordinaryBracketedAlbumSubtitleContainingLiveIsNotVersionEvidence() {
        assertFalse(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Song",
                candidateTitle = "Song (Live)",
                requestedAlbum = "Album (We Live Here)"
            )
        )
        assertFalse(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Song",
                candidateTitle = "Song (Live)",
                requestedAlbum = "Album (We Live Here — Deluxe Edition)"
            )
        )
    }

    @Test
    fun mixedScriptProseAroundEnglishMarkerIsNotVersionEvidence() {
        assertFalse(
            MetadataMatching.versionsCompatible(
                requestedTitle = "曲名",
                candidateTitle = "曲名（ライブ）",
                requestedAlbum = "アルバム - LIVE・ドア"
            )
        )
        assertFalse(
            MetadataMatching.versionsCompatible(
                requestedTitle = "曲名",
                candidateTitle = "曲名（ライブ）",
                requestedAlbum = "アルバム（LIVE・ドア）"
            )
        )
    }

    @Test
    fun fullWidthAlbumVersionPunctuationIsRecognized() {
        assertTrue(
            MetadataMatching.versionsCompatible(
                requestedTitle = "曲名",
                candidateTitle = "曲名（ライブ）",
                requestedAlbum = "アルバム（ライブ）"
            )
        )
    }

    @Test
    fun japaneseAlbumVersionSuffixIsRecognized() {
        assertTrue(
            MetadataMatching.versionsCompatible(
                requestedTitle = "曲名",
                candidateTitle = "曲名（ライブ）",
                requestedAlbum = "アルバム - ライブ版"
            )
        )
        assertTrue(
            MetadataMatching.versionsCompatible(
                requestedTitle = "曲名",
                candidateTitle = "曲名（ライブ）",
                requestedAlbum = "アルバム - ライブバージョン"
            )
        )
        assertFalse(
            MetadataMatching.versionsCompatible(
                requestedTitle = "曲名",
                candidateTitle = "曲名（ライブ）",
                requestedAlbum = "アルバム - ライブドア"
            )
        )
    }

    @Test
    fun matchingTitleVersionsIgnoreExtraAlbumEditionMarker() {
        assertTrue(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Song",
                candidateTitle = "Song",
                requestedAlbum = "Album",
                candidateAlbum = "Album (Remastered)"
            )
        )
    }

    @Test
    fun albumVersionEvidenceStillRejectsDifferentRecording() {
        assertFalse(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Song",
                candidateTitle = "Song (Live)",
                requestedAlbum = "Studio Album",
                candidateAlbum = "Live at Wembley"
            )
        )
    }

    @Test
    fun differentLiveLabelsRemainCompatible() {
        assertTrue(MetadataMatching.versionsCompatible("Song (Live at Wembley)", "Song - Live"))
    }

    @Test
    fun acousticAndRemixDoNotMatchBaseOrEachOther() {
        assertFalse(MetadataMatching.versionsCompatible("Song (Acoustic)", "Song"))
        assertFalse(MetadataMatching.versionsCompatible("Song (Remix)", "Song (Remastered)"))
    }

    @Test
    fun japaneseVersionMarkersAreRecognized() {
        assertFalse(MetadataMatching.versionsCompatible("曲名（ライブ）", "曲名"))
        assertTrue(MetadataMatching.versionsCompatible("曲名（ライブ）", "曲名 - ライブ版"))
    }

    @Test
    fun instrumentalFlagPreventsVocalMismatch() {
        assertFalse(MetadataMatching.versionsCompatible("Song", "Song", candidateInstrumental = true))
        assertTrue(MetadataMatching.versionsCompatible("Song (Instrumental)", "Song", candidateInstrumental = true))
    }

    @Test
    fun durationScoringHasExpectedBoundaries() {
        assertEquals(1.0, MetadataMatching.durationSimilarity(240_000L, 241_500L)!!, 0.0001)
        assertEquals(0.60, MetadataMatching.durationSimilarity(240_000L, 249_000L)!!, 0.0001)
        assertEquals(0.35, MetadataMatching.durationSimilarity(240_000L, 255_000L)!!, 0.0001)
        assertEquals(-1.0, MetadataMatching.durationSimilarity(240_000L, 256_000L)!!, 0.0001)
        assertNull(MetadataMatching.durationSimilarity(0L, 240_000L))
    }
    @Test
    fun nativeSecondsPreserveFractionalDurationBoundaries() {
        assertEquals(1.00, MetadataMatching.durationSimilaritySeconds(240, 242.0))
        assertEquals(0.92, MetadataMatching.durationSimilaritySeconds(240, 242.0001))
        assertEquals(0.78, MetadataMatching.durationSimilaritySeconds(240, 247.0))
        assertEquals(0.60, MetadataMatching.durationSimilaritySeconds(240, 247.0001))
        assertEquals(0.35, MetadataMatching.durationSimilaritySeconds(240, 255.0))
        assertEquals(-1.0, MetadataMatching.durationSimilaritySeconds(240, 255.0001))
        assertNull(MetadataMatching.durationSimilaritySeconds(240, Double.NaN))
        assertNull(MetadataMatching.durationSimilaritySeconds(240, Double.POSITIVE_INFINITY))
    }
}
