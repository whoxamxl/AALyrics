package io.github.whoxamxl.aalyrics.ui.phone.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.phone.navigation.PhoneDestination
import io.github.whoxamxl.aalyrics.ui.phone.state.PhoneShellUiState

/** Persistent Phone chrome around a caller-owned selected destination body. */
@Composable
fun PhoneAppShell(
    state: PhoneShellUiState,
    onDestinationSelected: (PhoneDestination) -> Unit,
    onDestinationReselected: (PhoneDestination) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onQueueItemSelected: (Long) -> Unit,
    onOpenPlaybackApp: () -> Unit,
    onTranslationEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    mediaSourceIconPainter: Painter? = null,
    playbackArtwork: (@Composable BoxScope.() -> Unit)? = null,
    destinationContent: @Composable (PhoneDestination, Dp) -> Unit,
) {
    BackHandler(
        enabled = shouldReturnToHomeOnSystemBack(state.selectedDestination),
    ) {
        onDestinationSelected(PhoneDestination.Home)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = AALyricsColors.BackgroundBase,
    ) {
        Column(Modifier.fillMaxSize()) {
            PhoneTopBar(
                mediaSourceLabel = state.mediaSourceLabel,
                mediaSourceConnectionState = state.mediaSourceConnectionState,
                mediaSourceUnavailableReason = state.mediaSourceUnavailableReason,
                mediaSourceErrorReason = state.mediaSourceErrorReason,
                mediaSourceCanOpenApp = state.mediaSourceCanOpenApp,
                mediaSourceIconPainter = mediaSourceIconPainter,
                onOpenPlaybackApp = onOpenPlaybackApp,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val bottomOverlayInset = if (state.playbackSurface != null) {
                    PlaybackSurfaceOverlayInset
                } else {
                    0.dp
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-5).dp)
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(
                            Brush.verticalGradient(
                                0.00f to AALyricsColors.BackgroundChrome,
                                0.12f to AALyricsColors.BackgroundChromeFade1,
                                0.24f to AALyricsColors.BackgroundChromeFade2,
                                0.36f to AALyricsColors.BackgroundChromeFade3,
                                0.48f to AALyricsColors.BackgroundChromeFade4,
                                0.60f to AALyricsColors.BackgroundChromeTransition,
                                0.72f to AALyricsColors.BackgroundChromeFade5,
                                0.86f to AALyricsColors.BackgroundChromeFade6,
                                1.00f to AALyricsColors.BackgroundBase,
                            ),
                        ),
                )

                destinationContent(state.selectedDestination, bottomOverlayInset)

                state.playbackSurface?.let { playbackState ->
                    PlaybackSurface(
                        state = playbackState,
                        onPrevious = onPrevious,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onSeekTo = onSeekTo,
                        onQueueItemSelected = onQueueItemSelected,
                        onOpenPlaybackApp = onOpenPlaybackApp,
                        onTranslationEnabledChanged = onTranslationEnabledChanged,
                        artwork = playbackArtwork,
                    )
                }
            }
            PhoneNavigationBar(
                selectedDestination = state.selectedDestination,
                onDestinationSelected = onDestinationSelected,
                onDestinationReselected = onDestinationReselected,
            )
        }
    }
}

internal fun shouldReturnToHomeOnSystemBack(
    selectedDestination: PhoneDestination,
): Boolean = selectedDestination != PhoneDestination.Home
