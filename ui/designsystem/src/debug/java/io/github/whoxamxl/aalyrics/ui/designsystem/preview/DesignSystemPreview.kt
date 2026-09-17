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
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography

/** High-level catalog replacing a static design-system board. */
@Preview(showBackground = true, backgroundColor = 0xFF020812, widthDp = 412, heightDp = 1200)
@Composable
private fun DesignSystemPreview() {
    AALyricsTheme {
        Surface {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                Text("AALyrics Design System", style = AALyricsTypography.LyricsSupporting)
                PalettePreviewContent()
                TypographyPreviewContent()
            }
        }
    }
}
