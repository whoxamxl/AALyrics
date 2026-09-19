package io.github.whoxamxl.aalyrics.ui.phone.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    destinationContent: @Composable (PhoneDestination, Dp) -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = AALyricsColors.BackgroundBase,
    ) {
        Column(Modifier.fillMaxSize()) {
            PhoneTopBar(mediaSourceLabel = state.mediaSourceLabel)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val bottomOverlayInset = if (state.playbackControls != null) {
                    PlaybackControlsOverlayInset
                } else {
                    0.dp
                }
                destinationContent(state.selectedDestination, bottomOverlayInset)

                state.playbackControls?.let { playbackState ->
                    PlaybackControlsBar(
                        state = playbackState,
                        onPrevious = onPrevious,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
            PhoneNavigationBar(
                selectedDestination = state.selectedDestination,
                onDestinationSelected = onDestinationSelected,
            )
        }
    }
}
