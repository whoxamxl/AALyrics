package io.github.whoxamxl.aalyrics.ui.phone.preview

import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportInteractionMode
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportLineUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.TrackCardUiState
import io.github.whoxamxl.aalyrics.ui.phone.navigation.PhoneDestination
import io.github.whoxamxl.aalyrics.ui.phone.settings.AndroidAutoCompatibilityUiStatus
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsLanguageOptionUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.TranslationModelUiState
import io.github.whoxamxl.aalyrics.ui.phone.state.PhoneShellUiState
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackControlsUiState

/** Deterministic debug-only inputs shared by Phone shell component Previews. */
internal object PhonePreviewFixtures {
    val trackCardReady = TrackCardUiState(
        title = "Midnight Signals",
        artist = "The Northbound Lights",
        providerLabel = "Musixmatch",
        syncLabel = "Word synced",
    )
    val trackCardLongTitle = TrackCardUiState(
        title = "A Track Title Long Enough to Demonstrate the Overflow Marquee Behavior",
        artist = "The Northbound Lights",
        providerLabel = "Musixmatch",
        syncLabel = "Word synced",
    )
    val trackCardLongArtist = TrackCardUiState(
        title = "Midnight Signals",
        artist = "An Artist Name That Is Deliberately Much Longer Than the Available Track Card Width",
        providerLabel = "Musixmatch",
        syncLabel = "Word synced",
    )
    val trackCardLongTitleAndArtist = TrackCardUiState(
        title = "A Track Title Long Enough to Demonstrate the Overflow Marquee Behavior",
        artist = "An Artist Name That Is Deliberately Much Longer Than the Available Track Card Width",
        providerLabel = "Musixmatch",
        syncLabel = "Word synced",
    )
    val trackCardNoMetadata = TrackCardUiState(
        title = "Midnight Signals",
        artist = "The Northbound Lights",
    )
    val trackCardNoArtist = TrackCardUiState(
        title = "Untitled Session",
        providerLabel = "LRCLIB",
        syncLabel = "Line synced",
    )

    private val viewportLines = listOf(
        LyricsViewportLineUiState("Streetlights wake along the avenue"),
        LyricsViewportLineUiState("We carry the signal into the night"),
        LyricsViewportLineUiState("Every window turns into a constellation"),
        LyricsViewportLineUiState("Hold the rhythm while the city moves"),
        LyricsViewportLineUiState(
            text = "A quiet echo follows close behind",
            words = listOf("A", "quiet", "echo", "follows", "close", "behind"),
        ),
        LyricsViewportLineUiState("The rain keeps drawing silver on the glass"),
        LyricsViewportLineUiState(
            "A deliberately long lyric line wraps naturally instead of shrinking just to preserve an arbitrary visible-line count",
        ),
        LyricsViewportLineUiState("Every step is warmer than the last"),
        LyricsViewportLineUiState("We leave the static somewhere far behind"),
        LyricsViewportLineUiState("Morning colors gather on the skyline"),
        LyricsViewportLineUiState("Until the daylight finds us here"),
    )

    val viewportLineMiddle = LyricsViewportUiState(
        lines = viewportLines,
        syncType = LyricsSyncType.LINE,
        currentLineIndex = 5,
    )
    val viewportLineFirst = viewportLineMiddle.copy(currentLineIndex = 0)
    val viewportLineLast = viewportLineMiddle.copy(currentLineIndex = viewportLines.lastIndex)

    val viewportWord = LyricsViewportUiState(
        lines = viewportLines,
        syncType = LyricsSyncType.WORD,
        currentLineIndex = 4,
        currentWordIndex = 3,
        currentWordProgress = 0.62f,
    )

    val viewportPlain = LyricsViewportUiState(
        lines = viewportLines,
        syncType = LyricsSyncType.PLAIN,
        playbackProgress = 0.46f,
        plainAutoScrollEnabled = true,
    )
    val viewportPlainNoDuration = viewportPlain.copy(playbackProgress = null)

    val viewportBrowsePlaybackBelow = viewportLineMiddle.copy(
        currentLineIndex = 8,
        interactionMode = LyricsViewportInteractionMode.BROWSE,
    )
    val viewportBrowsePlaybackAbove = viewportLineMiddle.copy(
        currentLineIndex = 2,
        interactionMode = LyricsViewportInteractionMode.BROWSE,
    )

    val lyricsScreenLine = LyricsScreenUiState(
        trackCard = trackCardReady.copy(syncLabel = "Line synced"),
        viewport = viewportLineMiddle,
    )
    val lyricsScreenWord = LyricsScreenUiState(
        trackCard = trackCardReady,
        viewport = viewportWord,
    )
    val lyricsScreenPlain = LyricsScreenUiState(
        trackCard = trackCardReady.copy(
            providerLabel = "LRCLIB",
            syncLabel = "Plain",
        ),
        viewport = viewportPlain,
    )
    val lyricsScreenLongMetadata = LyricsScreenUiState(
        trackCard = trackCardLongTitleAndArtist,
        viewport = viewportLineMiddle,
    )

    val playingControls = PlaybackControlsUiState(
        isPlaying = true,
        progressFraction = 0.42f,
    )
    val pausedControls = PlaybackControlsUiState(
        isPlaying = false,
        progressFraction = 0.61f,
    )
    val previousDisabledControls = PlaybackControlsUiState(
        isPlaying = true,
        previousEnabled = false,
        progressFraction = 0.08f,
    )
    val nextDisabledControls = PlaybackControlsUiState(
        isPlaying = false,
        nextEnabled = false,
        progressFraction = 0.93f,
    )
    val allDisabledControls = PlaybackControlsUiState(
        isPlaying = false,
        previousEnabled = false,
        playPauseEnabled = false,
        nextEnabled = false,
        progressFraction = 0.37f,
    )

    val typicalLyricsShell = PhoneShellUiState(
        mediaSourceLabel = "Spotify",
        playbackControls = playingControls,
    )
    val narrowLyricsShell = PhoneShellUiState(
        mediaSourceLabel = "YouTube Music",
        playbackControls = pausedControls,
    )
    val lyricsWithoutControls = PhoneShellUiState(mediaSourceLabel = null)
    val syncShell = PhoneShellUiState(
        selectedDestination = PhoneDestination.Sync,
        mediaSourceLabel = "Poweramp",
        playbackControls = pausedControls,
    )

    val settingsLanguages = listOf(
        SettingsLanguageOptionUiState(
            id = "en",
            displayName = "English",
            modelState = TranslationModelUiState.BUILT_IN,
        ),
        SettingsLanguageOptionUiState(
            id = "ja",
            displayName = "Japanese",
            modelState = TranslationModelUiState.NOT_DOWNLOADED,
        ),
        SettingsLanguageOptionUiState(
            id = "fr",
            displayName = "French",
            modelState = TranslationModelUiState.DOWNLOADING,
        ),
        SettingsLanguageOptionUiState(
            id = "de",
            displayName = "German",
            modelState = TranslationModelUiState.READY,
        ),
        SettingsLanguageOptionUiState(
            id = "es",
            displayName = "Spanish",
            modelState = TranslationModelUiState.FAILED,
            modelFailureReason = "Model download failed because the network request did not complete.",
        ),
        SettingsLanguageOptionUiState("ko", "Korean"),
        SettingsLanguageOptionUiState("zh", "Chinese"),
        SettingsLanguageOptionUiState("it", "Italian"),
        SettingsLanguageOptionUiState("pt", "Portuguese"),
    )
    val settingsTypical = SettingsScreenUiState(
        plainLyricsAutoScrollEnabled = true,
        translationEnabled = true,
        translationTarget = settingsLanguages.first { it.id == "en" },
        translationTargets = settingsLanguages,
        androidAutoCompatibilityStatus = AndroidAutoCompatibilityUiStatus.ENABLED,
        appVersionName = "0.1.0-dev",
    )
    val settingsTranslationOff = settingsTypical.copy(translationEnabled = false)
    val settingsAndroidAutoSkipped = settingsTypical.copy(
        androidAutoCompatibilityStatus = AndroidAutoCompatibilityUiStatus.SKIPPED,
    )
    val settingsAndroidAutoNotReviewed = settingsTypical.copy(
        androidAutoCompatibilityStatus = AndroidAutoCompatibilityUiStatus.NOT_REVIEWED,
    )
    val settingsShell = PhoneShellUiState(
        selectedDestination = PhoneDestination.Settings,
        mediaSourceLabel = "Spotify",
        playbackControls = playingControls,
    )

    val lyricsLines = listOf(
        "Streetlights wake along the avenue",
        "We carry the signal into the night",
        "Every window turns into a constellation",
        "Hold the rhythm while the city moves",
        "A quiet echo follows close behind",
        "Until the morning finds us here",
    )
}
