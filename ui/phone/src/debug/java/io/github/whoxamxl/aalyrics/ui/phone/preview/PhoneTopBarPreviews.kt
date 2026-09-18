package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
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
    PhoneTopBarPreview(mediaSourceLabel = "Spotify")
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

@Preview(
    name = "Narrow",
    group = "PhoneTopBar",
    widthDp = 320,
    showBackground = true,
)
@Composable
private fun PhoneTopBarNarrowPreview() {
    PhoneTopBarPreview(mediaSourceLabel = "YouTube Music")
}

@Composable
private fun PhoneTopBarPreview(mediaSourceLabel: String?) {
    AALyricsTheme {
        PhoneTopBar(mediaSourceLabel = mediaSourceLabel)
    }
}
