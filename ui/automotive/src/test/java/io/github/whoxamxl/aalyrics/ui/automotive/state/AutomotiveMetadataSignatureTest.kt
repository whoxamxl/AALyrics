package io.github.whoxamxl.aalyrics.ui.automotive.state

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AutomotiveMetadataSignatureTest {
    @Test
    fun `position-only advancement does not rebuild visible metadata`() {
        val state = AutomotiveLyricsUiState(
            trackTitle = "Song",
            artist = "Artist",
            positionMs = 1_000L,
            lyrics = AutomotiveLyricPresentation("Current"),
        )

        assertEquals(
            state.metadataSignature(),
            state.copy(positionMs = 1_500L, playbackRate = 1.2f).metadataSignature(),
        )
    }

    @Test
    fun `line loading frame and translation changes rebuild visible metadata`() {
        val state = AutomotiveLyricsUiState(
            lyrics = AutomotiveLyricPresentation("Current"),
        )
        val original = state.metadataSignature()

        assertNotEquals(original, state.copy(
            lyrics = AutomotiveLyricPresentation("Next"),
        ).metadataSignature())
        assertNotEquals(original, state.copy(
            lyrics = AutomotiveLyricPresentation("Loading lyrics."),
        ).metadataSignature())
        assertNotEquals(state.copy(
            lyrics = AutomotiveLyricPresentation("Loading lyrics."),
        ).metadataSignature(), state.copy(
            lyrics = AutomotiveLyricPresentation("Loading lyrics.."),
        ).metadataSignature())
        assertNotEquals(original, state.copy(
            lyrics = AutomotiveLyricPresentation("Current", "Translating."),
        ).metadataSignature())
        assertNotEquals(original, state.copy(
            lyrics = AutomotiveLyricPresentation("Current", "Translated"),
        ).metadataSignature())
    }

    @Test
    fun `artwork and individual track fields rebuild visible metadata`() {
        val original = AutomotiveMetadataSignature(
            trackTitle = "Song",
            artist = "Artist",
            album = "Album",
            durationMs = 10_000L,
            displayTitle = "Song — Artist",
            lyricPrimary = "Current",
            lyricSecondary = null,
            artwork = "old jacket",
        )

        assertNotEquals(original, original.copy(artwork = "new jacket"))
        assertNotEquals(original, original.copy(artwork = null))
        assertNotEquals(original, original.copy(trackTitle = "Other"))
        assertNotEquals(original, original.copy(artist = "Other"))
        assertNotEquals(original, original.copy(album = "Other"))
        assertNotEquals(original, original.copy(durationMs = 11_000L))
    }
}
