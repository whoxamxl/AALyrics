package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsScreen
import io.github.whoxamxl.aalyrics.ui.phone.lyrics.LyricsScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.navigation.PhoneDestination
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsScreen
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsScreenUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.normalizedForSettingsEntry
import io.github.whoxamxl.aalyrics.ui.phone.shell.PhoneAppShell
import io.github.whoxamxl.aalyrics.ui.phone.state.PhoneShellUiState

@Preview(
    name = "Typical Lyrics shell",
    group = "PhoneAppShell",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun PhoneAppShellTypicalLyricsPreview() {
    PhoneAppShellPreview(PhonePreviewFixtures.typicalLyricsShell)
}

@Preview(
    name = "Playback controls hidden",
    group = "PhoneAppShell",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun PhoneAppShellHiddenControlsPreview() {
    PhoneAppShellPreview(PhonePreviewFixtures.lyricsWithoutControls)
}

@Preview(
    name = "Lyrics browse · playback overlay",
    group = "PhoneAppShell",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun PhoneAppShellBrowseOverlayPreview() {
    PhoneAppShellPreview(
        state = PhonePreviewFixtures.typicalLyricsShell,
        lyricsState = PhonePreviewFixtures.lyricsScreenLine.copy(
            viewport = PhonePreviewFixtures.viewportBrowsePlaybackBelow,
        ),
    )
}

@Preview(
    name = "Settings selected",
    group = "PhoneAppShell",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun PhoneAppShellSettingsPreview() {
    PhoneAppShellPreview(
        state = PhonePreviewFixtures.settingsShell,
        settingsState = PhonePreviewFixtures.settingsTypical,
    )
}

@Preview(
    name = "Sync selected",
    group = "PhoneAppShell",
    widthDp = 412,
    heightDp = 892,
    showBackground = true,
)
@Composable
private fun PhoneAppShellSyncPreview() {
    PhoneAppShellPreview(PhonePreviewFixtures.syncShell)
}

@Composable
private fun PhoneAppShellPreview(
    state: PhoneShellUiState,
    lyricsState: LyricsScreenUiState = PhonePreviewFixtures.lyricsScreenLine,
    settingsState: SettingsScreenUiState = PhonePreviewFixtures.settingsTypical,
) {
    AALyricsTheme {
        PhoneAppShell(
            state = state,
            onDestinationSelected = {},
            onPrevious = {},
            onPlayPause = {},
            onNext = {},
            onSeekTo = {},
            onQueueItemSelected = {},
            onOpenPlaybackApp = {},
            onTranslationEnabledChanged = {},
        ) { destination, bottomOverlayInset ->
            when (destination) {
                PhoneDestination.Lyrics -> {
                    PreviewLyricsDestination(
                        initialState = lyricsState,
                        bottomOverlayInset = bottomOverlayInset,
                    )
                }
                PhoneDestination.Settings -> {
                    PreviewSettingsDestination(
                        initialState = settingsState,
                        bottomOverlayInset = bottomOverlayInset,
                    )
                }
                else -> PreviewDestinationBody(destination)
            }
        }
    }
}

/** Debug-only stateful host for the production Lyrics destination. */
@Composable
internal fun PreviewLyricsDestination(
    initialState: LyricsScreenUiState,
    bottomOverlayInset: Dp = 0.dp,
) {
    var state by remember(initialState) { mutableStateOf(initialState) }

    LyricsScreen(
        state = state,
        modifier = Modifier.fillMaxSize(),
        bottomOverlayInset = bottomOverlayInset,
        onViewportInteractionModeChange = { mode ->
            state = state.copy(
                viewport = state.viewport.copy(interactionMode = mode),
            )
        },
    )
}

/** Debug-only stateful host for the production Settings destination. */
@Composable
internal fun PreviewSettingsDestination(
    initialState: SettingsScreenUiState,
    bottomOverlayInset: Dp = 0.dp,
) {
    var state by remember(initialState) { mutableStateOf(initialState) }

    SettingsScreen(
        state = state,
        onPlainLyricsAutoScrollChanged = {
            state = state.copy(plainLyricsAutoScrollEnabled = it)
        },
        onTranslationEnabledChanged = {
            state = state.copy(translationEnabled = it)
        },
        onTranslationTargetSelected = { id ->
            state.translationTargets
                .firstOrNull { it.id == id }
                ?.let { target ->
                    state = state.copy(translationTarget = target)
                }
        },
        onTranslationModelDownloadRequested = {},
        onAndroidAutoCompatibilitySetup = {},
        onCheckForUpdates = {},
        onDownloadUpdate = {},
        onChangelogRequested = {},
        onLicenseRequested = {},
        onSettingsEntered = {
            state = state.copy(
                appUpdate = state.appUpdate.normalizedForSettingsEntry(),
            )
        },
        onOpenGitHub = {},
        modifier = Modifier.fillMaxSize(),
        bottomOverlayInset = bottomOverlayInset,
    )
}

@Composable
private fun PreviewDestinationBody(destination: PhoneDestination) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(AALyricsSpacing.Space24),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = destination.label,
                style = AALyricsTypography.LyricsSupporting,
                color = AALyricsColors.TextPrimary,
            )
            Spacer(Modifier.height(AALyricsSpacing.Space8))
            Text(
                text = "Destination behavior is intentionally deferred",
                style = AALyricsTypography.TrackArtist,
                color = AALyricsColors.TextSecondary,
            )
        }
    }
}
