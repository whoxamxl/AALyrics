package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.shell.ExpandedPlayer
import io.github.whoxamxl.aalyrics.ui.phone.shell.PlaybackBar
import io.github.whoxamxl.aalyrics.ui.phone.shell.PlaybackQueueSheetContent
import io.github.whoxamxl.aalyrics.ui.phone.shell.PlaybackSurface
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackSurfaceUiState

@Preview(name = "Collapsed · Playing", group = "PlaybackSurface", widthDp = 412, showBackground = true)
@Composable
private fun PlaybackBarPlayingPreview() {
    PlaybackBarPreview(PhonePreviewFixtures.playingSurface)
}

@Preview(name = "Collapsed · Paused", group = "PlaybackSurface", widthDp = 412, showBackground = true)
@Composable
private fun PlaybackBarPausedPreview() {
    PlaybackBarPreview(PhonePreviewFixtures.pausedSurface)
}

@Preview(name = "Collapsed · Long metadata", group = "PlaybackSurface", widthDp = 412, showBackground = true)
@Composable
private fun PlaybackBarLongMetadataPreview() {
    PlaybackBarPreview(PhonePreviewFixtures.longMetadataSurface)
}

@Preview(
    name = "Expanded · Queue",
    group = "PlaybackSurface",
    widthDp = 412,
    heightDp = 240,
    showBackground = true,
)
@Composable
private fun ExpandedPlayerQueuePreview() {
    ExpandedPlayerPreview(PhonePreviewFixtures.playingSurface)
}

@Preview(
    name = "Expanded · Open app fallback",
    group = "PlaybackSurface",
    widthDp = 412,
    heightDp = 240,
    showBackground = true,
)
@Composable
private fun ExpandedPlayerOpenAppPreview() {
    ExpandedPlayerPreview(PhonePreviewFixtures.openAppFallbackSurface)
}

@Preview(
    name = "Expanded · No trailing action",
    group = "PlaybackSurface",
    widthDp = 412,
    heightDp = 240,
    showBackground = true,
)
@Composable
private fun ExpandedPlayerNoTrailingActionPreview() {
    ExpandedPlayerPreview(PhonePreviewFixtures.noTrailingActionSurface)
}

@Preview(
    name = "Expanded · Non-seekable",
    group = "PlaybackSurface",
    widthDp = 412,
    heightDp = 240,
    showBackground = true,
)
@Composable
private fun ExpandedPlayerNonSeekablePreview() {
    ExpandedPlayerPreview(PhonePreviewFixtures.nonSeekableSurface)
}

@Preview(
    name = "Expanded · Narrow",
    group = "PlaybackSurface",
    widthDp = 320,
    heightDp = 240,
    showBackground = true,
)
@Composable
private fun ExpandedPlayerNarrowPreview() {
    ExpandedPlayerPreview(PhonePreviewFixtures.longMetadataSurface)
}

@Preview(
    name = "Expanded · Large font",
    group = "PlaybackSurface",
    widthDp = 412,
    heightDp = 300,
    fontScale = 1.4f,
    showBackground = true,
)
@Composable
private fun ExpandedPlayerLargeFontPreview() {
    ExpandedPlayerPreview(PhonePreviewFixtures.longMetadataSurface)
}

@Preview(
    name = "Queue · Scrollable",
    group = "PlaybackSurface",
    widthDp = 412,
    heightDp = 520,
    showBackground = true,
)
@Composable
private fun PlaybackQueueScrollablePreview() {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundSurfaceStrong),
        ) {
            PlaybackQueueSheetContent(
                queue = PhonePreviewFixtures.playingSurface.queue,
                canOpenPlaybackApp = true,
                onOpenPlaybackApp = {},
                onQueueItemSelected = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(
    name = "Interactive · Full surface",
    group = "PlaybackSurface",
    widthDp = 412,
    heightDp = 360,
    showBackground = true,
)
@Composable
private fun PlaybackSurfaceInteractivePreview() {
    var state by remember { mutableStateOf(PhonePreviewFixtures.playingSurface) }

    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            PlaybackSurface(
                state = state,
                onPrevious = {},
                onPlayPause = {
                    state = state.copy(isPlaying = !state.isPlaying)
                },
                onNext = {},
                onSeekTo = { positionMs ->
                    state = state.copy(
                        positionMs = positionMs,
                        positionUpdatedAtMonotonicMs = null,
                    )
                },
                onQueueItemSelected = { queueItemId ->
                    state.queue.firstOrNull { it.id == queueItemId }?.let { item ->
                        state = state.copy(
                            title = item.title,
                            artist = item.subtitle,
                            positionMs = 0L,
                            positionUpdatedAtMonotonicMs = null,
                        )
                    }
                },
                onOpenPlaybackApp = {},
                onTranslationEnabledChanged = { enabled ->
                    state = state.copy(translationEnabled = enabled)
                },
            )
        }
    }
}

@Composable
private fun PlaybackBarPreview(state: PlaybackSurfaceUiState) {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AALyricsColors.BackgroundBase),
        ) {
            PlaybackBar(
                state = state,
                progressFraction = state.progressFraction,
                onExpand = {},
                onPlayPause = {},
            )
        }
    }
}

@Composable
private fun ExpandedPlayerPreview(state: PlaybackSurfaceUiState) {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AALyricsColors.BackgroundBase)
                .padding(vertical = 4.dp),
        ) {
            ExpandedPlayer(
                state = state,
                displayedPositionMs = state.positionMs,
                onCollapse = {},
                onPrevious = {},
                onPlayPause = {},
                onNext = {},
                onSeekPreview = {},
                onSeekCommit = {},
                onSeekCancel = {},
                onQueueItemSelected = {},
                onOpenPlaybackApp = {},
                onTranslationEnabledChanged = {},
            )
        }
    }
}
