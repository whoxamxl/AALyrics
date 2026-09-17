package io.github.whoxamxl.aalyrics.ui.designsystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography

@Composable
internal fun TypographyPreviewContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Typography", style = AALyricsTypography.AppTitle)
        Text("AALyrics", style = AALyricsTypography.AppTitle)
        Text("Midnight Signals", style = AALyricsTypography.TrackTitle)
        Text("Artist name", style = AALyricsTypography.TrackArtist, color = AALyricsColors.TextSecondary)
        Text("we carry the signal into the night", style = AALyricsTypography.LyricsCurrent)
        Text(
            "the city fades behind us",
            style = AALyricsTypography.LyricsSupporting,
            color = AALyricsColors.TextSecondary,
        )
        Text("WORD SYNC", style = AALyricsTypography.Label, color = AALyricsColors.AccentCyan)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF020812)
@Composable
private fun TypographyPreview() {
    AALyricsTheme {
        Surface(modifier = Modifier.padding(24.dp)) {
            TypographyPreviewContent()
        }
    }
}
