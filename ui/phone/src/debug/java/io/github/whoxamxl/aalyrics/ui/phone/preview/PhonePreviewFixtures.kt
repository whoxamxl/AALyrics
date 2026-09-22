package io.github.whoxamxl.aalyrics.ui.phone.preview

import io.github.whoxamxl.aalyrics.core.model.LyricsSyncType
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsDiagnosticsUiState
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsLyricsUiState
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsLyricsUiStatus
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsTrackUiState
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsVerboseProgressUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportInteractionMode
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportLineUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportUiState
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.TrackCardUiState
import io.github.whoxamxl.aalyrics.ui.phone.navigation.PhoneDestination
import io.github.whoxamxl.aalyrics.ui.phone.settings.AndroidAutoCompatibilityUiStatus
import io.github.whoxamxl.aalyrics.ui.phone.settings.AppUpdateUiPhase
import io.github.whoxamxl.aalyrics.ui.phone.settings.AppUpdateUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsLanguageOptionUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.TranslationModelUiState
import io.github.whoxamxl.aalyrics.ui.phone.state.PhoneShellUiState
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackQueueItemUiState
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackSurfaceUiState

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

    val playingSurface = PlaybackSurfaceUiState(
        isPlaying = true,
        title = "Midnight Signals",
        artist = "The Northbound Lights",
        positionMs = 92_000L,
        durationMs = 221_000L,
        playbackRate = 1.0f,
        canPlay = true,
        canPause = true,
        canSkipPrevious = true,
        canSkipNext = true,
        canSkipToQueueItem = true,
        canSeek = true,
        queue = listOf(
            PlaybackQueueItemUiState(101L, "Midnight Signals", "The Northbound Lights"),
            PlaybackQueueItemUiState(102L, "Afterglow Transit", "The Northbound Lights"),
            PlaybackQueueItemUiState(103L, "Static Horizon", "Northern Relay"),
            PlaybackQueueItemUiState(104L, "Glass Stations", "The Northbound Lights"),
            PlaybackQueueItemUiState(105L, "Low Orbit", "Northern Relay"),
            PlaybackQueueItemUiState(106L, "Last Train Through Neon", "The Northbound Lights"),
            PlaybackQueueItemUiState(107L, "Signal Bloom", "Polar Avenue"),
            PlaybackQueueItemUiState(108L, "A Very Long Queue Title That Should Stay on One Line", "An Equally Long Artist Name for Queue Overflow"),
            PlaybackQueueItemUiState(109L, "Blue Hour", "Northern Relay"),
            PlaybackQueueItemUiState(110L, "Terminal Lights", "The Northbound Lights"),
        ),
        canOpenPlaybackApp = true,
        translationEnabled = true,
    )
    val pausedSurface = playingSurface.copy(
        isPlaying = false,
        positionMs = 134_000L,
    )
    val previousDisabledSurface = playingSurface.copy(
        canSkipPrevious = false,
        positionMs = 18_000L,
    )
    val nextDisabledSurface = pausedSurface.copy(
        canSkipNext = false,
        positionMs = 205_000L,
    )
    val nonSeekableSurface = playingSurface.copy(
        canSeek = false,
        durationMs = null,
        queue = emptyList(),
        canOpenPlaybackApp = true,
    )
    val openAppFallbackSurface = playingSurface.copy(queue = emptyList())
    val noTrailingActionSurface = playingSurface.copy(
        queue = emptyList(),
        canOpenPlaybackApp = false,
    )
    val longTitleSurface = playingSurface.copy(
        title = "A Track Title Long Enough to Demonstrate the Playback Bar Marquee",
        artist = "The Northbound Lights",
    )
    val longArtistSurface = playingSurface.copy(
        title = "Midnight Signals",
        artist = "An Artist Name That Is Deliberately Longer Than the Available Player Width",
    )
    val longMetadataSurface = playingSurface.copy(
        title = "A Track Title Long Enough to Demonstrate the Playback Bar Marquee",
        artist = "An Artist Name That Is Also Deliberately Longer Than the Available Player Width",
    )

    val typicalLyricsShell = PhoneShellUiState(
        mediaSourceLabel = "Spotify",
        playbackSurface = playingSurface,
    )
    val narrowLyricsShell = PhoneShellUiState(
        mediaSourceLabel = "YouTube Music",
        playbackSurface = pausedSurface,
    )
    val lyricsWithoutControls = PhoneShellUiState(mediaSourceLabel = null)
    val syncShell = PhoneShellUiState(
        selectedDestination = PhoneDestination.Sync,
        mediaSourceLabel = "Poweramp",
        playbackSurface = pausedSurface,
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
    val noticeSample = "Required Notice: © 2026 Yuta Miura"

    val licenseMarkdownSample = """
        # PolyForm Noncommercial License 1.0.0

        <https://polyformproject.org/licenses/noncommercial/1.0.0>

        ## Acceptance

        This Preview fixture exercises **bold text**, *emphasis*, `inline code`, links, headings,
        paragraphs, and compact document spacing without duplicating the production LICENSE source.

        ## Notices

        > Required notices remain readable on narrow Phone surfaces.

        - First list item
        - Second list item
    """.trimIndent()

    val changelogMarkdownSample = """
        # Changelog

        ## [0.2.0-alpha.1] - 2026-09-21

        ### Added

        - Production Phone UI and Settings surfaces.
        - Persistent Playback Surface and Translation integration.

        ### Known limitations

        - Sync calibration and Karaoke mode remain in development.

        ## [0.1.0-alpha.1] - 2026-09-19

        ### Added

        - Initial AALyrics foundation and signed release distribution.
    """.trimIndent()

    val privacyPolicyMarkdownSample = """
        # Privacy Policy

        **Effective date:** September 22, 2026

        AALyrics uses Android media-session information to identify the currently playing track.
        Track metadata may be sent to lyrics providers to find synchronized lyrics.

        ## Local data

        Settings are stored on the device. AALyrics does not include analytics or advertising SDKs.

        ## External services

        Lyrics lookup may contact LRCLIB, PetitLyrics, Musixmatch, and SyncLRC.
        Translation uses on-device ML Kit after required language models are available.
    """.trimIndent()

    val settingsTypical = SettingsScreenUiState(
        plainLyricsAutoScrollEnabled = true,
        translationEnabled = false,
        translationTarget = settingsLanguages.first { it.id == "en" },
        translationTargets = settingsLanguages,
        androidAutoCompatibilityStatus = AndroidAutoCompatibilityUiStatus.ENABLED,
        appVersionName = "0.1.0-dev",
        currentYear = 2026,
        noticeText = noticeSample,
        licenseText = licenseMarkdownSample,
        changelogText = changelogMarkdownSample,
        privacyPolicyText = privacyPolicyMarkdownSample,
    )
    val settingsCheckingUpdate = settingsTypical.copy(
        appUpdate = AppUpdateUiState(phase = AppUpdateUiPhase.CHECKING),
    )
    val settingsUpToDate = settingsTypical.copy(
        appUpdate = AppUpdateUiState(phase = AppUpdateUiPhase.UP_TO_DATE),
    )
    val settingsUpdateAvailable = settingsTypical.copy(
        appUpdate = AppUpdateUiState(
            phase = AppUpdateUiPhase.UPDATE_AVAILABLE,
            availableVersionName = "0.1.2",
        ),
    )
    val settingsUpdateFailed = settingsTypical.copy(
        appUpdate = AppUpdateUiState(
            phase = AppUpdateUiPhase.CHECK_FAILED,
            failureReason = "GitHub Releases could not be reached.",
        ),
    )
    val settingsDownloadingUpdate = settingsTypical.copy(
        appUpdate = AppUpdateUiState(
            phase = AppUpdateUiPhase.DOWNLOADING,
            availableVersionName = "0.1.2",
        ),
    )
    val settingsDownloadedUpdate = settingsTypical.copy(
        appUpdate = AppUpdateUiState(
            phase = AppUpdateUiPhase.DOWNLOADED,
            availableVersionName = "0.1.2",
        ),
    )
    val settingsTranslationOn = settingsTypical.copy(translationEnabled = true)
    val settingsAndroidAutoSkipped = settingsTypical.copy(
        androidAutoCompatibilityStatus = AndroidAutoCompatibilityUiStatus.SKIPPED,
    )
    val settingsAndroidAutoNotReviewed = settingsTypical.copy(
        androidAutoCompatibilityStatus = AndroidAutoCompatibilityUiStatus.NOT_REVIEWED,
    )
    val settingsShell = PhoneShellUiState(
        selectedDestination = PhoneDestination.Settings,
        mediaSourceLabel = "Spotify",
        playbackSurface = playingSurface,
    )

    val detailsTypical = DetailsScreenUiState(
        track = DetailsTrackUiState(
            title = "Midnight Signals",
            artist = "The Northbound Lights",
            album = "Afterglow Transit",
            durationLabel = "3:41",
            playbackSourceLabel = "Spotify",
        ),
        lyrics = DetailsLyricsUiState(
            providerDisplayName = "Musixmatch",
            syncType = LyricsSyncType.LINE,
            languageLabel = "English",
            lineCount = 64,
        ),
        lyricsStatus = DetailsLyricsUiStatus.READY,
    )
    val detailsPartial = DetailsScreenUiState(
        track = DetailsTrackUiState(
            title = "Untitled Session",
            artist = "Northern Relay",
            playbackSourceLabel = "Poweramp",
        ),
        lyricsStatus = DetailsLyricsUiStatus.NOT_FOUND,
    )
    val detailsLoading = detailsTypical.copy(
        lyrics = null,
        lyricsStatus = DetailsLyricsUiStatus.LOADING,
    )
    val detailsVerbose = detailsTypical.copy(
        verboseProgress = DetailsVerboseProgressUiState(
            playbackPositionLabel = "1:32",
            currentLineNumber = 28,
        ),
        diagnostics = DetailsDiagnosticsUiState(
            appPackageName = "com.spotify.music",
            providerId = "musixmatch",
            sourceId = "mxm:track:9384756",
            trackReferences = listOf(
                "spotify:4uLU6hMCjMI75M1A2tKUQC",
                "musicbrainz:7c1c3f7e-demo-reference",
            ),
        ),
    )
    val detailsVerboseSparse = detailsPartial.copy(
        diagnostics = DetailsDiagnosticsUiState(
            trackReferences = listOf("spotify:demo-reference"),
        ),
    )
    val detailsShell = PhoneShellUiState(
        selectedDestination = PhoneDestination.Details,
        mediaSourceLabel = "Spotify",
        playbackSurface = playingSurface,
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
