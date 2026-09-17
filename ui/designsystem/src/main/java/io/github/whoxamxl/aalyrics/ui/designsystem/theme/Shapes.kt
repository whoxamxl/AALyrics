package io.github.whoxamxl.aalyrics.ui.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object AALyricsRadius {
    val Radius0 = 0.dp
    val Radius4 = 4.dp
    val Radius8 = 8.dp
    val Radius12 = 12.dp
    val Radius16 = 16.dp
    val Radius24 = 24.dp
    val Full = 999.dp
}

internal val AALyricsMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(AALyricsRadius.Radius4),
    small = RoundedCornerShape(AALyricsRadius.Radius8),
    medium = RoundedCornerShape(AALyricsRadius.Radius12),
    large = RoundedCornerShape(AALyricsRadius.Radius16),
    extraLarge = RoundedCornerShape(AALyricsRadius.Radius24),
)
