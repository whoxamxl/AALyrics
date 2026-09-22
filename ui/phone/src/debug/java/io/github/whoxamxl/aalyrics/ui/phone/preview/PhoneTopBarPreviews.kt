package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.shell.PhoneTopBar

@Preview(
    name = "No media source",
    group = "PhoneTopBar",
    widthDp = 412,
    showBackground = true,
)
@Composable
private fun PhoneTopBarNoMediaSourcePreview() {
    PhoneTopBarPreview(mediaSourceLabel = null)
}

@Preview(
    name = "Spotify",
    group = "PhoneTopBar",
    widthDp = 412,
    showBackground = true,
)
@Composable
private fun PhoneTopBarSpotifyPreview() {
    PhoneTopBarPreview(
        mediaSourceLabel = "Spotify",
        mediaSourceConnected = true,
        mediaSourceIconPainter = ColorPainter(AALyricsColors.AccentCyan),
    )
}

@Preview(
    name = "Spotify · icon unavailable",
    group = "PhoneTopBar",
    widthDp = 412,
    showBackground = true,
)
@Composable
private fun PhoneTopBarSpotifyIconUnavailablePreview() {
    PhoneTopBarPreview(
        mediaSourceLabel = "Spotify",
        mediaSourceConnected = true,
    )
}

@Preview(
    name = "Spotify · source not verified",
    group = "PhoneTopBar",
    widthDp = 412,
    showBackground = true,
)
@Composable
private fun PhoneTopBarSpotifyUnverifiedPreview() {
    PhoneTopBarPreview(mediaSourceLabel = "Spotify")
}

@Preview(
    name = "Package fallback",
    group = "PhoneTopBar",
    widthDp = 412,
    showBackground = true,
)
@Composable
private fun PhoneTopBarPackageFallbackPreview() {
    PhoneTopBarPreview(mediaSourceLabel = "com.spotify.music")
}

@Preview(
    name = "YouTube Music",
    group = "PhoneTopBar",
    widthDp = 412,
    showBackground = true,
)
@Composable
private fun PhoneTopBarYouTubeMusicPreview() {
    PhoneTopBarPreview(mediaSourceLabel = "YouTube Music")
}

@Preview(
    name = "Poweramp",
    group = "PhoneTopBar",
    widthDp = 412,
    showBackground = true,
)
@Composable
private fun PhoneTopBarPowerampPreview() {
    PhoneTopBarPreview(mediaSourceLabel = "Poweramp")
}

@Preview(
    name = "Long media source",
    group = "PhoneTopBar",
    widthDp = 412,
    showBackground = true,
)
@Composable
private fun PhoneTopBarLongMediaSourcePreview() {
    PhoneTopBarPreview(mediaSourceLabel = "Very Long Music Player Application")
}

@Composable
private fun PhoneTopBarPreview(
    mediaSourceLabel: String?,
    mediaSourceConnected: Boolean = false,
    mediaSourceIconPainter: Painter? = null,
) {
    AALyricsTheme {
        PhoneTopBar(
            mediaSourceLabel = mediaSourceLabel,
            mediaSourceConnected = mediaSourceConnected,
            mediaSourceIconPainter = mediaSourceIconPainter,
        )
    }
}
