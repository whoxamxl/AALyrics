package io.github.whoxamxl.aalyrics

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    onOpenSourceCode: () -> Unit,
    onOpenLicense: () -> Unit,
) {
    val playback by application.playbackState.collectAsStateWithLifecycle()
    val lyricsState by application.lyricsState.collectAsStateWithLifecycle()
    val playbackSurface by application.phonePlaybackSurfaceState.collectAsStateWithLifecycle()
    val mediaSourceLabel by application.phoneMediaSourceLabel.collectAsStateWithLifecycle()
    val detailsState by application.phoneDetailsState.collectAsStateWithLifecycle()
    val translationSettings by application.translationSettings.collectAsStateWithLifecycle()
    val translationModelStates by application.translationModelStates.collectAsStateWithLifecycle()
    val verboseDetailsEnabled by application.verboseDetailsEnabled.collectAsStateWithLifecycle()

    var selectedDestination by rememberSaveable {
        mutableStateOf(PhoneDestination.Home)
    }
    var plainLyricsAutoScrollEnabled by rememberSaveable {
        mutableStateOf(true)
    }
    var lyricsInteractionMode by rememberSaveable {
        mutableStateOf(LyricsViewportInteractionMode.FOLLOW)
    }
    var monotonicTimeMs by rememberSaveable(playback.trackIdentity) {
        mutableStateOf(SystemClock.elapsedRealtime())
    }

    LaunchedEffect(playback.isPlaying, playback.trackIdentity) {
        monotonicTimeMs = SystemClock.elapsedRealtime()
        while (isActive && playback.isPlaying) {
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
    val settingsState = mapPhoneSettingsState(
        translationSettings = translationSettings,
        translationModelStates = translationModelStates,
        verboseDetailsEnabled = verboseDetailsEnabled,
        plainLyricsAutoScrollEnabled = plainLyricsAutoScrollEnabled,
        androidAutoStatus = androidAutoStatus,
        appVersionName = BuildConfig.VERSION_NAME,
        currentYear = Year.now().value,
    )

    PhoneAppShell(
        state = PhoneShellUiState(
            selectedDestination = selectedDestination,
            mediaSourceLabel = mediaSourceLabel,
            playbackSurface = playbackSurface,
        ),
        onDestinationSelected = { selectedDestination = it },
        onPrevious = application::skipToPrevious,
        onPlayPause = {
            if (playback.isPlaying) application.pause() else application.play()
        },
        onNext = application::skipToNext,
        onSeekTo = application::seekTo,
        onQueueItemSelected = application::skipToQueueItem,
        onOpenPlaybackApp = { application.openSelectedPlaybackApp() },
        onTranslationEnabledChanged = application::setTranslationEnabled,
    ) { destination, bottomOverlayInset ->
        when (destination) {
            PhoneDestination.Lyrics -> LyricsScreen(
                state = lyricsUiState,
                bottomOverlayInset = bottomOverlayInset,
                onViewportInteractionModeChange = { lyricsInteractionMode = it },
            )

            PhoneDestination.Sync -> SyncScreen(
                modifier = Modifier.fillMaxSize(),
                bottomOverlayInset = bottomOverlayInset,
            )

            PhoneDestination.Details -> DetailsScreen(
                state = detailsState,
                bottomOverlayInset = bottomOverlayInset,
            )

            PhoneDestination.Settings -> SettingsScreen(
                state = settingsState,
                onPlainLyricsAutoScrollChanged = { plainLyricsAutoScrollEnabled = it },
                onVerboseDetailsChanged = application::setVerboseDetailsEnabled,
                onTranslationEnabledChanged = application::setTranslationEnabled,
                onTranslationTargetSelected = application::setTranslationTargetLanguage,
                onTranslationModelDownloadRequested = application::requestTranslationModel,
                onAndroidAutoCompatibilitySetup = onAndroidAutoCompatibilitySetup,
                onCheckForUpdates = {},
                onDownloadUpdate = {},
                onChangelogRequested = {},
                onLicenseRequested = onOpenLicense,
                onSettingsEntered = {},
                onOpenGitHub = onOpenSourceCode,
                bottomOverlayInset = bottomOverlayInset,
            )
        }
    }
}
