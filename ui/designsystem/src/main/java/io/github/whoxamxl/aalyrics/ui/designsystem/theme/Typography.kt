package io.github.whoxamxl.aalyrics.ui.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * AALyrics uses the Android system sans-serif family (Roboto on standard Android builds).
 * No font binary is bundled in the design-system foundation.
 */
object AALyricsTypography {
    val AppTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    )

    val TrackTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    )

    val TrackArtist = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

    val LyricsCurrent = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    )

    val LyricsSupporting = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    )

    val Label = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
}

internal val AALyricsMaterialTypography = Typography(
    labelSmall = AALyricsTypography.Label,
    bodyMedium = AALyricsTypography.TrackArtist,
    titleMedium = AALyricsTypography.TrackTitle,
    headlineSmall = AALyricsTypography.LyricsSupporting,
    displaySmall = AALyricsTypography.LyricsCurrent,
)
