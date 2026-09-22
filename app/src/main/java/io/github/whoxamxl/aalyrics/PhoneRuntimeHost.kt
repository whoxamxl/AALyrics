package io.github.whoxamxl.aalyrics

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.whoxamxl.aalyrics.ui.designsystem.component.AlbumArtwork
import io.github.whoxamxl.aalyrics.ui.phone.details.DetailsScreen
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsScreen
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsViewportInteractionMode
import io.github.whoxamxl.aalyrics.ui.phone.navigation.PhoneDestination
import io.github.whoxamxl.aalyrics.ui.phone.settings.AndroidAutoCompatibilityUiStatus
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsScreen
import io.github.whoxamxl.aalyrics.ui.phone.shell.PhoneAppShell
import io.github.whoxamxl.aalyrics.ui.phone.state.PhoneShellUiState
import io.github.whoxamxl.aalyrics.ui.phone.sync.SyncScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Year

@Composable
internal fun PhoneRuntimeHost(
    application: AALyricsApplication,
    androidAutoStatus: AndroidAutoCompatibilityUiStatus,
    onAndroidAutoCompatibilitySetup: () -> Unit,
    onResetAALyrics: () -> Unit,
    onOpenSourceCode: () -> Unit,
    onOpenSupportAALyrics: () -> Unit,
) {
    val playback by application.playbackState.collectAsStateWithLifecycle()
    val lyricsState by application.lyricsState.collectAsStateWithLifecycle()
    val playbackSurface by application.phonePlaybackSurfaceState.collectAsStateWithLifecycle()
    val mediaSourceLabel by application.phoneMediaSourceLabel.collectAsStateWithLifecycle()
    val detailsState by application.phoneDetailsState.collectAsStateWithLifecycle()
    val playbackArtwork by application.playbackArtworkState.collectAsStateWithLifecycle()
    val translationSettings by application.translationSettings.collectAsStateWithLifecycle()
    val translationModelStates by application.translationModelStates.collectAsStateWithLifecycle()
    val translationModelCleanupState by
        application.translationModelCleanupState.collectAsStateWithLifecycle()
    val verboseDetailsEnabled by application.verboseDetailsEnabled.collectAsStateWithLifecycle()

    var selectedDestination by rememberSaveable {
        mutableStateOf(PhoneDestination.Home)
    }
    var plainLyricsAutoScrollEnabled by rememberSaveable {
        mutableStateOf(true)
    }
    var destinationRootResetKey by rememberSaveable {
        mutableStateOf(0)
    }
    var lyricsInteractionMode by rememberSaveable(playback.trackIdentity) {
        mutableStateOf(LyricsViewportInteractionMode.FOLLOW)
    }
    var monotonicTimeMs by rememberSaveable(playback.trackIdentity) {
        mutableStateOf(SystemClock.elapsedRealtime())
    }

    LaunchedEffect(playback.isPlaying, playback.trackIdentity, selectedDestination) {
        monotonicTimeMs = SystemClock.elapsedRealtime()
        while (
            isActive &&
            playback.isPlaying &&
            (
                selectedDestination == PhoneDestination.Lyrics ||
                    (
                        selectedDestination == PhoneDestination.Details &&
                            verboseDetailsEnabled
                    )
                )
        ) {
            delay(250L)
            monotonicTimeMs = SystemClock.elapsedRealtime()
        }
    }

    val lyricsUiState = mapPhoneLyricsState(
        playback = playback,
        lyricsState = lyricsState,
        plainLyricsAutoScrollEnabled = plainLyricsAutoScrollEnabled,
        interactionMode = lyricsInteractionMode,
        currentMonotonicTimeMs = monotonicTimeMs,
    )
    val playbackArtworkImage = remember(playbackArtwork) {
        playbackArtwork?.asImageBitmap()
    }

    val settingsState = mapPhoneSettingsState(
        translationSettings = translationSettings,
        translationModelStates = translationModelStates,
        verboseDetailsEnabled = verboseDetailsEnabled,
        plainLyricsAutoScrollEnabled = plainLyricsAutoScrollEnabled,
        androidAutoStatus = androidAutoStatus,
        appVersionName = BuildConfig.VERSION_NAME,
        currentYear = Year.now().value,
        noticeText = application.noticeText,
        licenseText = application.licenseText,
        changelogText = application.changelogText,
        privacyPolicyText = application.privacyPolicyText,
        translationModelCleanupState = translationModelCleanupState,
    )

    PhoneAppShell(
        state = PhoneShellUiState(
            selectedDestination = selectedDestination,
            mediaSourceLabel = mediaSourceLabel,
            playbackSurface = playbackSurface,
        ),
        onDestinationSelected = { selectedDestination = it },
        onDestinationReselected = { destination ->
            destinationRootResetKey += 1
            if (destination == PhoneDestination.Lyrics) {
                lyricsInteractionMode = LyricsViewportInteractionMode.FOLLOW
            }
        },
        onPrevious = application::skipToPrevious,
        onPlayPause = {
            if (playback.isPlaying) application.pause() else application.play()
        },
        onNext = application::skipToNext,
        onSeekTo = application::seekTo,
        onQueueItemSelected = application::skipToQueueItem,
        onOpenPlaybackApp = { application.openSelectedPlaybackApp() },
        onTranslationEnabledChanged = application::setTranslationEnabled,
        playbackArtwork = {
            AlbumArtwork(image = playbackArtworkImage)
        },
    ) { destination, bottomOverlayInset ->
        when (destination) {
            PhoneDestination.Lyrics -> LyricsScreen(
                state = lyricsUiState,
                bottomOverlayInset = bottomOverlayInset,
                artwork = {
                    AlbumArtwork(image = playbackArtworkImage)
                },
                onViewportInteractionModeChange = { lyricsInteractionMode = it },
            )

            PhoneDestination.Sync -> SyncScreen(
                bottomOverlayInset = bottomOverlayInset,
            )

            PhoneDestination.Details -> DetailsScreen(
                state = if (verboseDetailsEnabled) {
                    detailsState.copy(
                        verboseProgress = mapPhoneDetailsVerboseProgress(
                            playback = playback,
                            lyricsState = lyricsState,
                            currentMonotonicTimeMs = monotonicTimeMs,
                        ),
                    )
                } else {
                    detailsState
                },
                rootResetKey = destinationRootResetKey,
                bottomOverlayInset = bottomOverlayInset,
            )

            PhoneDestination.Settings -> SettingsScreen(
                state = settingsState,
                rootResetKey = destinationRootResetKey,
                onPlainLyricsAutoScrollChanged = { plainLyricsAutoScrollEnabled = it },
                onVerboseDetailsChanged = application::setVerboseDetailsEnabled,
                onTranslationEnabledChanged = application::setTranslationEnabled,
                onTranslationTargetSelected = application::setTranslationTargetLanguage,
                onTranslationModelDownloadRequested = application::requestTranslationModel,
                onClearTranslationModels = application::clearDownloadedTranslationModels,
                onDismissTranslationModelCleanupFailure =
                    application::dismissTranslationModelCleanupFailure,
                onResetAALyrics = {
                    plainLyricsAutoScrollEnabled = true
                    application.resetAppOwnedSettings()
                    onResetAALyrics()
                },
                onAndroidAutoCompatibilitySetup = onAndroidAutoCompatibilitySetup,
                onCheckForUpdates = {},
                onDownloadUpdate = {},
                onSettingsEntered = {},
                onOpenGitHub = onOpenSourceCode,
                onSupportAALyrics = onOpenSupportAALyrics,
                bottomOverlayInset = bottomOverlayInset,
            )
        }
    }
}
