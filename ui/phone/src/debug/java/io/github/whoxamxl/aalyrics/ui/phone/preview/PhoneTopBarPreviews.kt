package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.shell.PhoneTopBar
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackSourceConnectionUiState
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackSourceErrorUiReason
import io.github.whoxamxl.aalyrics.ui.phone.state.PlaybackSourceUnavailableUiReason

@Preview(name = "Connecting", group = "PhoneTopBar", widthDp = 412, showBackground = true)
@Composable
private fun PhoneTopBarConnectingPreview() {
    PhoneTopBarPreview(connectionState = PlaybackSourceConnectionUiState.CONNECTING)
}

@Preview(name = "Connected", group = "PhoneTopBar", widthDp = 412, showBackground = true)
@Composable
private fun PhoneTopBarConnectedPreview() {
    PhoneTopBarPreview(
        mediaSourceLabel = "Spotify",
        connectionState = PlaybackSourceConnectionUiState.CONNECTED,
        mediaSourceCanOpenApp = true,
        mediaSourceIconPainter = ColorPainter(AALyricsColors.AccentCyan),
    )
}

@Preview(
    name = "Connected · icon unavailable",
    group = "PhoneTopBar",
    widthDp = 412,
    showBackground = true,
)
@Composable
private fun PhoneTopBarConnectedIconUnavailablePreview() {
    PhoneTopBarPreview(
        mediaSourceLabel = "Spotify",
        connectionState = PlaybackSourceConnectionUiState.CONNECTED,
        mediaSourceCanOpenApp = true,
    )
}

@Preview(name = "Disconnected", group = "PhoneTopBar", widthDp = 412, showBackground = true)
@Composable
private fun PhoneTopBarDisconnectedPreview() {
    PhoneTopBarPreview(connectionState = PlaybackSourceConnectionUiState.DISCONNECTED)
}

@Preview(name = "Unavailable", group = "PhoneTopBar", widthDp = 412, showBackground = true)
@Composable
private fun PhoneTopBarUnavailablePreview() {
    PhoneTopBarPreview(
        mediaSourceLabel = "Spotify",
        connectionState = PlaybackSourceConnectionUiState.UNAVAILABLE,
        unavailableReason = PlaybackSourceUnavailableUiReason.UNSUPPORTED_PLAYER,
        mediaSourceIconPainter = ColorPainter(AALyricsColors.AccentCyan),
    )
}

@Preview(
    name = "Unavailable · generic fallback",
    group = "PhoneTopBar",
    widthDp = 412,
    showBackground = true,
)
@Composable
private fun PhoneTopBarUnavailableFallbackPreview() {
    PhoneTopBarPreview(
        connectionState = PlaybackSourceConnectionUiState.UNAVAILABLE,
        unavailableReason = PlaybackSourceUnavailableUiReason.UNKNOWN,
    )
}

@Preview(name = "Error", group = "PhoneTopBar", widthDp = 412, showBackground = true)
@Composable
private fun PhoneTopBarErrorPreview() {
    PhoneTopBarPreview(
        connectionState = PlaybackSourceConnectionUiState.ERROR,
        errorReason = PlaybackSourceErrorUiReason.SESSION_QUERY_FAILED,
    )
}

@Preview(
    name = "Connected · long app name",
    group = "PhoneTopBar",
    widthDp = 412,
    showBackground = true,
)
@Composable
private fun PhoneTopBarLongMediaSourcePreview() {
    PhoneTopBarPreview(
        mediaSourceLabel = "Very Long Music Player Application",
        connectionState = PlaybackSourceConnectionUiState.CONNECTED,
        mediaSourceCanOpenApp = true,
    )
}

@Composable
private fun PhoneTopBarPreview(
    mediaSourceLabel: String? = null,
    connectionState: PlaybackSourceConnectionUiState,
    unavailableReason: PlaybackSourceUnavailableUiReason? = null,
    errorReason: PlaybackSourceErrorUiReason? = null,
    mediaSourceCanOpenApp: Boolean = false,
    mediaSourceIconPainter: Painter? = null,
) {
    AALyricsTheme {
        PhoneTopBar(
            mediaSourceLabel = mediaSourceLabel,
            mediaSourceConnectionState = connectionState,
            mediaSourceUnavailableReason = unavailableReason,
            mediaSourceErrorReason = errorReason,
            mediaSourceCanOpenApp = mediaSourceCanOpenApp,
            mediaSourceIconPainter = mediaSourceIconPainter,
        )
    }
}
