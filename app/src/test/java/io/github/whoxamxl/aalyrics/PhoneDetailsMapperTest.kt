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
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelPhase
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelState
import io.github.whoxamxl.aalyrics.translation.api.TranslationProviderId
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import io.github.whoxamxl.aalyrics.translation.core.LanguageProfile
import io.github.whoxamxl.aalyrics.translation.core.TranslationArtifact
import io.github.whoxamxl.aalyrics.translation.core.TranslationArtifactLine
import io.github.whoxamxl.aalyrics.translation.core.ProfiledLineRole
import io.github.whoxamxl.aalyrics.translation.core.ProfiledLyricLine
import io.github.whoxamxl.aalyrics.translation.core.SecondaryActivation
import io.github.whoxamxl.aalyrics.translation.core.TranslationFailureReason
import io.github.whoxamxl.aalyrics.translation.core.TranslationRequestId
import io.github.whoxamxl.aalyrics.translation.core.TranslationRequestIdentity
import io.github.whoxamxl.aalyrics.translation.core.TranslationState
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsLyricsUiStatus
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsTranslationModelPhaseUiState
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsTranslationRuntimeFailureUiReason
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsTranslationRuntimeUiState
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
            playbackSourceAppInfo = playbackSourceAppInfo(),
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
            playbackSourceAppInfo = playbackSourceAppInfo(),
            displayLocale = Locale.ENGLISH,
        )

        assertEquals("com.spotify.music", state.diagnostics?.appPackageName)
        assertEquals("Audio", state.diagnostics?.appCategory)
        assertEquals(26, state.diagnostics?.appMinSdkVersion)
        assertEquals(36, state.diagnostics?.appTargetSdkVersion)
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
            playbackSourceAppInfo = playbackSourceAppInfo(),
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
            playbackSourceAppInfo = playbackSourceAppInfo(
                packageName = "com.example.player",
                label = "Example Player",
            ),
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
            playbackSourceAppInfo = playbackSourceAppInfo(),
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
            playbackSourceAppInfo = playbackSourceAppInfo(),
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
            playbackSourceAppInfo = playbackSourceAppInfo(),
            displayLocale = Locale.ENGLISH,
        )

        assertNull(state.lyrics)
        assertEquals(DetailsLyricsUiStatus.LOADING, state.lyricsStatus)
        assertEquals("com.spotify.music", state.diagnostics?.appPackageName)
        assertNull(state.diagnostics?.providerId)
        assertNull(state.diagnostics?.sourceId)
    }

    @Test
    fun `Verbose Details keeps raw package when app metadata is unavailable`() {
        val track = currentTrack()
        val state = mapPhoneDetailsState(
            playback = playback(track),
            lyricsState = readyLyrics(track, playback(track).trackIdentity!!),
            verboseDetailsEnabled = true,
            playbackSourceAppInfo = null,
            displayLocale = Locale.ENGLISH,
        )

        assertEquals("com.spotify.music", state.diagnostics?.appPackageName)
        assertNull(state.diagnostics?.appCategory)
        assertNull(state.diagnostics?.appMinSdkVersion)
        assertNull(state.diagnostics?.appTargetSdkVersion)
    }

    @Test
    fun `mismatched app metadata is not exposed for the current playback source`() {
        val track = currentTrack()
        val state = mapPhoneDetailsState(
            playback = playback(track),
            lyricsState = readyLyrics(track, playback(track).trackIdentity!!),
            verboseDetailsEnabled = true,
            playbackSourceAppInfo = playbackSourceAppInfo(
                packageName = "com.other.player",
                label = "Other Player",
            ),
            displayLocale = Locale.ENGLISH,
        )

        assertNull(state.track?.playbackSourceLabel)
        assertEquals("com.spotify.music", state.diagnostics?.appPackageName)
        assertNull(state.diagnostics?.appCategory)
        assertNull(state.diagnostics?.appMinSdkVersion)
        assertNull(state.diagnostics?.appTargetSdkVersion)
    }

    @Test
    fun `Translation Details maps matching Primary and active Secondary`() {
        val track = currentTrack()
        val playback = playback(track)
        val lyrics = readyLyrics(track, requireNotNull(playback.trackIdentity))
        val request = translationRequest(lyrics, targetLanguage = "ja")
        val state = mapPhoneDetailsState(
            playback = playback,
            lyricsState = lyrics,
            verboseDetailsEnabled = false,
            translationSettings = TranslationSettings(enabled = true, targetLanguage = "ja"),
            translationState = TranslationState.Translating(
                request = request,
                profile = profile(
                    primary = "en",
                    secondary = "es",
                    secondaryActivation = SecondaryActivation.ACTIVE,
                ),
            ),
            displayLocale = Locale.ENGLISH,
        )

        assertEquals("English (Spanish)", state.translation?.sourceLanguageLabel)
        assertEquals("Japanese", state.translation?.targetLanguageLabel)
        assertNull(state.translation?.runtimeState)
        assertNull(state.translation?.sourceModel)
        assertNull(state.translation?.targetModel)
    }

    @Test
    fun `stale Translation profile is rejected while current target remains visible`() {
        val track = currentTrack()
        val playback = playback(track)
        val lyrics = readyLyrics(track, requireNotNull(playback.trackIdentity))
        val staleRequest = translationRequest(
            lyrics.copy(lookup = lyrics.lookup.copy(id = LyricsLookupId(99L))),
            targetLanguage = "ja",
        )
        val state = mapPhoneDetailsState(
            playback = playback,
            lyricsState = lyrics,
            verboseDetailsEnabled = false,
            translationSettings = TranslationSettings(enabled = true, targetLanguage = "ja"),
            translationState = TranslationState.Translating(
                request = staleRequest,
                profile = profile(primary = "es"),
            ),
            displayLocale = Locale.ENGLISH,
        )

        assertNull(state.translation?.sourceLanguageLabel)
        assertEquals("Japanese", state.translation?.targetLanguageLabel)
    }

    @Test
    fun `Verbose Translation Details maps runtime failure and aggregate models`() {
        val track = currentTrack()
        val playback = playback(track)
        val lyrics = readyLyrics(track, requireNotNull(playback.trackIdentity))
        val request = translationRequest(lyrics, targetLanguage = "ja")
        val state = mapPhoneDetailsState(
            playback = playback,
            lyricsState = lyrics,
            verboseDetailsEnabled = true,
            translationSettings = TranslationSettings(enabled = true, targetLanguage = "ja"),
            translationState = TranslationState.Failed(
                request = request,
                profile = profile(
                    primary = "en",
                    secondary = "es",
                    secondaryActivation = SecondaryActivation.ACTIVE,
                ),
                reason = TranslationFailureReason.PROVIDER_EXECUTION_FAILED,
            ),
            translationModelStates = mapOf(
                "es" to TranslationModelState(
                    languageTag = "es",
                    phase = TranslationModelPhase.FAILED,
                    error = "Model download task failed",
                ),
                "ja" to TranslationModelState(
                    languageTag = "ja",
                    phase = TranslationModelPhase.READY,
                ),
            ),
            translationModelInventoryReconciled = true,
            displayLocale = Locale.ENGLISH,
        )

        assertEquals(DetailsTranslationRuntimeUiState.FAILED, state.translation?.runtimeState)
        assertEquals(
            DetailsTranslationRuntimeFailureUiReason.PROVIDER_EXECUTION_FAILED,
            state.translation?.runtimeFailureReason,
        )
        assertEquals("EN, ES", state.translation?.sourceModel?.languageLabel)
        assertEquals(
            DetailsTranslationModelPhaseUiState.FAILED,
            state.translation?.sourceModel?.phase,
        )
        assertEquals(
            "ES: Model download task failed",
            state.translation?.sourceModel?.failureReason,
        )
        assertEquals("JA", state.translation?.targetModel?.languageLabel)
        assertEquals(
            DetailsTranslationModelPhaseUiState.READY,
            state.translation?.targetModel?.phase,
        )
    }

    @Test
    fun `Verbose Translation Details distinguishes Ready Not required and startup Checking`() {
        val track = currentTrack()
        val playback = playback(track)
        val lyrics = readyLyrics(track, requireNotNull(playback.trackIdentity))

        val english = mapPhoneDetailsState(
            playback = playback,
            lyricsState = lyrics,
            verboseDetailsEnabled = true,
            translationSettings = TranslationSettings(enabled = false, targetLanguage = "en"),
            translationState = TranslationState.Disabled,
            translationModelInventoryReconciled = true,
            displayLocale = Locale.ENGLISH,
        )
        assertEquals(DetailsTranslationRuntimeUiState.DISABLED, english.translation?.runtimeState)
        assertEquals(
            DetailsTranslationModelPhaseUiState.READY,
            english.translation?.targetModel?.phase,
        )

        val japanese = mapPhoneDetailsState(
            playback = playback,
            lyricsState = lyrics,
            verboseDetailsEnabled = true,
            translationSettings = TranslationSettings(enabled = false, targetLanguage = "ja"),
            translationState = TranslationState.Disabled,
            translationModelInventoryReconciled = true,
            displayLocale = Locale.ENGLISH,
        )
        assertEquals(
            DetailsTranslationModelPhaseUiState.NOT_REQUIRED,
            japanese.translation?.targetModel?.phase,
        )

        val reconciling = mapPhoneDetailsState(
            playback = playback,
            lyricsState = lyrics,
            verboseDetailsEnabled = true,
            translationSettings = TranslationSettings(enabled = false, targetLanguage = "ja"),
            translationState = TranslationState.Disabled,
            translationModelInventoryReconciled = false,
            displayLocale = Locale.ENGLISH,
        )
        assertEquals(
            DetailsTranslationModelPhaseUiState.CHECKING,
            reconciling.translation?.targetModel?.phase,
        )
    }


    @Test
    fun translationDetailsOmitsIncidentalSecondaryAndPreProfileSource() {
        val track = currentTrack()
        val playback = playback(track)
        val lyrics = readyLyrics(track, requireNotNull(playback.trackIdentity))
        val request = translationRequest(lyrics, targetLanguage = "ja")

        val incidental = mapPhoneDetailsState(
            playback = playback,
            lyricsState = lyrics,
            verboseDetailsEnabled = true,
            translationSettings = TranslationSettings(enabled = true, targetLanguage = "ja"),
            translationState = TranslationState.Translating(
                request = request,
                profile = profile(
                    primary = "en",
                    secondary = "es",
                    secondaryActivation = SecondaryActivation.INCIDENTAL,
                ),
            ),
            translationModelStates = mapOf(
                "ja" to TranslationModelState("ja", TranslationModelPhase.READY),
            ),
            translationModelInventoryReconciled = true,
            displayLocale = Locale.ENGLISH,
        )
        assertEquals("English", incidental.translation?.sourceLanguageLabel)
        assertEquals("EN", incidental.translation?.sourceModel?.languageLabel)

        val preProfile = mapPhoneDetailsState(
            playback = playback,
            lyricsState = lyrics,
            verboseDetailsEnabled = true,
            translationSettings = TranslationSettings(enabled = true, targetLanguage = "ja"),
            translationState = TranslationState.Translating(
                request = request,
                profile = null,
            ),
            translationModelStates = mapOf(
                "ja" to TranslationModelState("ja", TranslationModelPhase.READY),
            ),
            translationModelInventoryReconciled = true,
            displayLocale = Locale.ENGLISH,
        )
        assertNull(preProfile.translation?.sourceLanguageLabel)
        assertNull(preProfile.translation?.sourceModel)
        assertEquals("Japanese", preProfile.translation?.targetLanguageLabel)
    }

    @Test
    fun verboseTranslationDetailsMapsAllRuntimeStates() {
        val track = currentTrack()
        val playback = playback(track)
        val lyrics = readyLyrics(track, requireNotNull(playback.trackIdentity))
        val request = translationRequest(lyrics, targetLanguage = "ja")
        val englishProfile = profile(primary = "en")
        val japaneseProfile = profile(primary = "ja")

        fun mapped(
            settingsEnabled: Boolean,
            translationState: TranslationState,
        ) = mapPhoneDetailsState(
            playback = playback,
            lyricsState = lyrics,
            verboseDetailsEnabled = true,
            translationSettings = TranslationSettings(
                enabled = settingsEnabled,
                targetLanguage = "ja",
            ),
            translationState = translationState,
            translationModelStates = mapOf(
                "ja" to TranslationModelState("ja", TranslationModelPhase.READY),
            ),
            translationModelInventoryReconciled = true,
            displayLocale = Locale.ENGLISH,
        ).translation

        assertEquals(
            DetailsTranslationRuntimeUiState.DISABLED,
            mapped(false, TranslationState.Disabled)?.runtimeState,
        )
        assertEquals(
            DetailsTranslationRuntimeUiState.IDLE,
            mapped(true, TranslationState.Idle)?.runtimeState,
        )
        assertEquals(
            DetailsTranslationRuntimeUiState.TRANSLATING,
            mapped(
                true,
                TranslationState.Translating(
                    request = request,
                    profile = englishProfile,
                ),
            )?.runtimeState,
        )
        assertEquals(
            DetailsTranslationRuntimeUiState.NOT_REQUIRED,
            mapped(
                true,
                TranslationState.NotRequired(
                    request = request,
                    profile = japaneseProfile,
                ),
            )?.runtimeState,
        )
        assertEquals(
            DetailsTranslationRuntimeUiState.READY,
            mapped(
                true,
                readyTranslationState(
                    lyrics = lyrics,
                    targetLanguage = "ja",
                    languageProfile = englishProfile,
                ),
            )?.runtimeState,
        )

        val failed = mapped(
            true,
            TranslationState.Failed(
                request = request,
                profile = englishProfile,
                reason = TranslationFailureReason.PROVIDER_EXECUTION_FAILED,
            ),
        )
        assertEquals(DetailsTranslationRuntimeUiState.FAILED, failed?.runtimeState)
        assertEquals(
            DetailsTranslationRuntimeFailureUiReason.PROVIDER_EXECUTION_FAILED,
            failed?.runtimeFailureReason,
        )
    }

    @Test
    fun verboseTranslationDetailsMapsEveryModelPhaseAndFailureReason() {
        val track = currentTrack()
        val playback = playback(track)
        val lyrics = readyLyrics(track, requireNotNull(playback.trackIdentity))

        fun targetModel(
            enabled: Boolean,
            modelState: TranslationModelState?,
            reconciled: Boolean = true,
        ) = mapPhoneDetailsState(
            playback = playback,
            lyricsState = lyrics,
            verboseDetailsEnabled = true,
            translationSettings = TranslationSettings(
                enabled = enabled,
                targetLanguage = "ja",
            ),
            translationState = if (enabled) TranslationState.Idle else TranslationState.Disabled,
            translationModelStates = modelState
                ?.let { mapOf("ja" to it) }
                .orEmpty(),
            translationModelInventoryReconciled = reconciled,
            displayLocale = Locale.ENGLISH,
        ).translation?.targetModel

        assertEquals(
            DetailsTranslationModelPhaseUiState.NOT_REQUIRED,
            targetModel(enabled = false, modelState = null)?.phase,
        )
        assertEquals(
            DetailsTranslationModelPhaseUiState.CHECKING,
            targetModel(enabled = false, modelState = null, reconciled = false)?.phase,
        )
        assertEquals(
            DetailsTranslationModelPhaseUiState.CHECKING,
            targetModel(
                enabled = true,
                modelState = TranslationModelState("ja", TranslationModelPhase.CHECKING),
            )?.phase,
        )
        assertEquals(
            DetailsTranslationModelPhaseUiState.DOWNLOADING,
            targetModel(
                enabled = true,
                modelState = TranslationModelState("ja", TranslationModelPhase.DOWNLOADING),
            )?.phase,
        )
        assertEquals(
            DetailsTranslationModelPhaseUiState.WAITING_FOR_SYSTEM,
            targetModel(
                enabled = true,
                modelState = TranslationModelState(
                    "ja",
                    TranslationModelPhase.WAITING_FOR_SYSTEM,
                ),
            )?.phase,
        )
        assertEquals(
            DetailsTranslationModelPhaseUiState.READY,
            targetModel(
                enabled = false,
                modelState = TranslationModelState("ja", TranslationModelPhase.READY),
            )?.phase,
        )

        val failed = targetModel(
            enabled = true,
            modelState = TranslationModelState(
                "ja",
                TranslationModelPhase.FAILED,
                "Download task failed",
            ),
        )
        assertEquals(DetailsTranslationModelPhaseUiState.FAILED, failed?.phase)
        assertEquals("Download task failed", failed?.failureReason)

        val timedOut = targetModel(
            enabled = true,
            modelState = TranslationModelState(
                "ja",
                TranslationModelPhase.TIMED_OUT,
                "Model still unavailable after 5 min",
            ),
        )
        assertEquals(DetailsTranslationModelPhaseUiState.TIMED_OUT, timedOut?.phase)
        assertEquals("Model still unavailable after 5 min", timedOut?.failureReason)

        val missingReason = targetModel(
            enabled = true,
            modelState = TranslationModelState(
                "ja",
                TranslationModelPhase.FAILED,
                null,
            ),
        )
        assertNull(missingReason?.failureReason)
    }

    @Test
    fun verboseTranslationDetailsAggregatesSourceModelsByActionability() {
        val track = currentTrack()
        val playback = playback(track)
        val lyrics = readyLyrics(track, requireNotNull(playback.trackIdentity))
        val request = translationRequest(lyrics, targetLanguage = "ja")
        val state = mapPhoneDetailsState(
            playback = playback,
            lyricsState = lyrics,
            verboseDetailsEnabled = true,
            translationSettings = TranslationSettings(enabled = true, targetLanguage = "ja"),
            translationState = TranslationState.Translating(
                request = request,
                profile = profile(
                    primary = "en",
                    secondary = "es",
                    secondaryActivation = SecondaryActivation.ACTIVE,
                ),
            ),
            translationModelStates = mapOf(
                "es" to TranslationModelState(
                    languageTag = "es",
                    phase = TranslationModelPhase.WAITING_FOR_SYSTEM,
                ),
                "ja" to TranslationModelState(
                    languageTag = "ja",
                    phase = TranslationModelPhase.READY,
                ),
            ),
            translationModelInventoryReconciled = true,
            displayLocale = Locale.ENGLISH,
        )

        assertEquals("EN, ES", state.translation?.sourceModel?.languageLabel)
        assertEquals(
            DetailsTranslationModelPhaseUiState.WAITING_FOR_SYSTEM,
            state.translation?.sourceModel?.phase,
        )
        assertEquals(
            DetailsTranslationModelPhaseUiState.READY,
            state.translation?.targetModel?.phase,
        )
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

    private fun translationRequest(
        lyrics: LyricsState.Ready,
        targetLanguage: String,
    ) = TranslationRequestIdentity(
        id = TranslationRequestId(1L),
        canonicalLyrics = requireNotNull(lyrics.canonicalLyricsOrNull()).identity,
        targetLanguage = targetLanguage,
    )

    private fun profile(
        primary: String?,
        secondary: String? = null,
        secondaryActivation: SecondaryActivation =
            if (secondary == null) SecondaryActivation.NONE else SecondaryActivation.INCIDENTAL,
    ) = LanguageProfile(
        primary = primary,
        secondaryCandidate = secondary,
        secondaryActivation = secondaryActivation,
        lines = listOf(
            ProfiledLyricLine(
                index = 0,
                languageTag = primary,
                confidence = 0.99f,
                role = ProfiledLineRole.PRIMARY,
            ),
            ProfiledLyricLine(
                index = 1,
                languageTag = secondary ?: primary,
                confidence = 0.95f,
                role = if (secondary == null) {
                    ProfiledLineRole.PRIMARY
                } else {
                    ProfiledLineRole.SECONDARY
                },
            ),
        ),
    )


    private fun readyTranslationState(
        lyrics: LyricsState.Ready,
        targetLanguage: String,
        languageProfile: LanguageProfile,
    ) = TranslationState.Ready(
        TranslationArtifact(
            request = translationRequest(lyrics, targetLanguage),
            providerId = TranslationProviderId("mlkit"),
            profile = languageProfile,
            lines = listOf(
                TranslationArtifactLine(
                    canonicalLineIndex = 0,
                    text = "Translated line 0",
                    sourceLanguage = languageProfile.primary,
                    translated = true,
                ),
                TranslationArtifactLine(
                    canonicalLineIndex = 1,
                    text = "Translated line 1",
                    sourceLanguage = languageProfile.primary,
                    translated = true,
                ),
            ),
        ),
    )

    private fun playbackSourceAppInfo(
        packageName: String = "com.spotify.music",
        label: String = "Spotify",
    ) = PlaybackSourceAppInfo(
        packageName = packageName,
        label = label,
        icon = null,
        category = PlaybackSourceAppCategory.AUDIO,
        minSdkVersion = 26,
        targetSdkVersion = 36,
    )

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
