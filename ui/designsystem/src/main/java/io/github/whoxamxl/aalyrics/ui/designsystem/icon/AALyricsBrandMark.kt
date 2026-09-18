package io.github.whoxamxl.aalyrics.ui.designsystem.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import io.github.whoxamxl.aalyrics.ui.designsystem.R
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsPalette
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsRadius
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing

/** Compact brand tile for persistent app chrome. */
@Composable
fun AALyricsBrandMark(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(AALyricsSpacing.Space32)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        AALyricsPalette.Cyan100,
                        AALyricsPalette.Cyan300,
                        AALyricsPalette.Blue400,
                    ),
                ),
                shape = RoundedCornerShape(AALyricsRadius.Radius8),
            )
            .padding(AALyricsSpacing.Space4),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_aalyrics_mark),
            contentDescription = contentDescription,
            tint = AALyricsColors.BackgroundBase,
        )
    }
}
