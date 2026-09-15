package io.github.whoxamxl.aalyrics.provider.matching

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class RecordingVersionContextTest {

    @Test
    fun namedRemixInBracketsIsExplicitTitleVersionEvidence() {
        assertEquals(
            setOf("remix"),
            RecordingVersionContext.extractTitleVersionQualifiers("I Took a Pill in Ibiza (Seeb Remix)")
        )
        assertFalse(
            MetadataMatching.versionsCompatible(
                requestedTitle = "I Took a Pill in Ibiza (Seeb Remix)",
                candidateTitle = "I Took a Pill in Ibiza"
            )
        )
    }

    @Test
    fun namedRemixAfterSeparatorIsExplicitTitleVersionEvidence() {
        assertEquals(
            setOf("remix"),
            RecordingVersionContext.extractTitleVersionQualifiers("Song - Seeb Remix")
        )
        assertTrue(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Song - Seeb Remix",
                candidateTitle = "Song (Club Remix)"
            )
        )
    }

    @Test
    fun compoundJapaneseTitleQualifiersAreExplicitVersionEvidence() {
        assertEquals(
            setOf("live", "remaster"),
            RecordingVersionContext.extractTitleVersionQualifiers("曲名（ライブ・リマスター）")
        )
        assertEquals(
            setOf("live", "remaster"),
            RecordingVersionContext.extractTitleVersionQualifiers("曲名（ライブ版・リマスター版）")
        )
        assertFalse(
            MetadataMatching.versionsCompatible(
                requestedTitle = "曲名（ライブ・リマスター）",
                candidateTitle = "曲名"
            )
        )
    }

    @Test
    fun incidentalVersionWordsInBaseTitlesRemainNonVersionEvidence() {
        assertEquals(emptySet<String>(), RecordingVersionContext.extractTitleVersionQualifiers("Live Forever"))
        assertEquals(emptySet<String>(), RecordingVersionContext.extractTitleVersionQualifiers("Remix to Ignition"))

        assertTrue(
            MetadataMatching.versionsCompatible(
                requestedTitle = "Live Forever",
                candidateTitle = "Live Forever (Remastered)",
                requestedAlbum = "Definitely Maybe (Remastered)",
                candidateAlbum = "Definitely Maybe (Remastered)"
            )
        )
    }

    @Test
    fun explicitCoreVersionConflictsStillReject() {
        assertFalse(MetadataMatching.versionsCompatible("Song (Live)", "Song (Remastered)"))
        assertFalse(MetadataMatching.versionsCompatible("Song (Acoustic)", "Song (Remix)"))
        assertFalse(MetadataMatching.versionsCompatible("Song", "Song (Instrumental)"))
    }
}

