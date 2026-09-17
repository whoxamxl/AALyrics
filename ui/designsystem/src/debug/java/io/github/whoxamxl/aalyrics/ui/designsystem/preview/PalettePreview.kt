package io.github.whoxamxl.aalyrics.ui.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography

private data class ColorSample(val name: String, val hex: String, val color: Color)

private val semanticSamples = listOf(
    ColorSample("Background / Base", "#020812", AALyricsColors.BackgroundBase),
    ColorSample("Background / Surface", "#071524", AALyricsColors.BackgroundSurface),
    ColorSample("Background / Strong", "#0B2038", AALyricsColors.BackgroundSurfaceStrong),
    ColorSample("Text / Primary", "#F7FAFF", AALyricsColors.TextPrimary),
    ColorSample("Text / Secondary", "#A9B7C9", AALyricsColors.TextSecondary),
    ColorSample("Text / Tertiary", "#66778E", AALyricsColors.TextTertiary),
    ColorSample("Accent / Cyan", "#49E6FB", AALyricsColors.AccentCyan),
    ColorSample("Accent / Blue", "#1B8EFF", AALyricsColors.AccentBlue),
    ColorSample("Border / Soft", "#17304D", AALyricsColors.BorderSoft),
)

@Composable
internal fun PalettePreviewContent() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Semantic colors", style = AALyricsTypography.AppTitle)
        semanticSamples.forEach { sample ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(32.dp)
                        .background(sample.color),
                )
                Column {
                    Text(sample.name, style = AALyricsTypography.TrackArtist)
                    Text(sample.hex, style = AALyricsTypography.Label, color = AALyricsColors.TextSecondary)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF020812)
@Composable
private fun PalettePreview() {
    AALyricsTheme {
        Surface(modifier = Modifier.padding(24.dp)) {
            PalettePreviewContent()
        }
    }
}
