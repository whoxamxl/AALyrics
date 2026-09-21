package io.github.whoxamxl.aalyrics.ui.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/** Raw brand and neutral values. Prefer [AALyricsColors] in production components. */
object AALyricsPalette {
    val Cyan100 = Color(0xFF75F5FF)
    val Cyan200 = Color(0xFF49E6FB)
    val Cyan300 = Color(0xFF2FC3FF)

    val Blue400 = Color(0xFF1B8EFF)
    val Blue500 = Color(0xFF1856F4)

    val Navy700 = Color(0xFF173F69)
    val Navy800 = Color(0xFF0F2F54)
    val Navy850 = Color(0xFF091C36)
    val Navy900 = Color(0xFF071C39)
    val Navy950 = Color(0xFF020812)

    val Neutral50 = Color(0xFFF7FAFF)
    val Neutral300 = Color(0xFFA9B7C9)
    val Neutral500 = Color(0xFF66778E)

    val Success = Color(0xFF62D6A7)
    val Warning = Color(0xFFFFC857)
    val Error = Color(0xFFFF6B7A)
}

/** Semantic colors shared by phone and automotive presentation. */
object AALyricsColors {
    val BackgroundBase = AALyricsPalette.Navy950
    val BackgroundSurface = Color(0xFF071524)
    val BackgroundChrome = Color(0xFF05111F)
    val BackgroundChromeFade1 = Color(0xFF05101E)
    val BackgroundChromeFade2 = Color(0xFF040F1C)
    val BackgroundChromeTransition = Color(0xFF040D19)
    val BackgroundChromeFade3 = Color(0xFF030B16)
    val BackgroundSurfaceStrong = Color(0xFF0B2038)
    val TextPrimary = AALyricsPalette.Neutral50
    val TextSecondary = AALyricsPalette.Neutral300
    val TextTertiary = AALyricsPalette.Neutral500
    val AccentCyan = AALyricsPalette.Cyan200
    val AccentBlue = AALyricsPalette.Blue400
    val BorderSoft = Color(0xFF17304D)
    val OverlaySoft = Color(0xFF0B1D31)
    val Success = AALyricsPalette.Success
    val Warning = AALyricsPalette.Warning
    val Error = AALyricsPalette.Error
}

internal val AALyricsDarkColorScheme = darkColorScheme(
    primary = AALyricsColors.AccentCyan,
    onPrimary = AALyricsColors.BackgroundBase,
    secondary = AALyricsColors.AccentBlue,
    onSecondary = AALyricsColors.TextPrimary,
    background = AALyricsColors.BackgroundBase,
    onBackground = AALyricsColors.TextPrimary,
    surface = AALyricsColors.BackgroundSurface,
    onSurface = AALyricsColors.TextPrimary,
    surfaceVariant = AALyricsColors.BackgroundSurfaceStrong,
    onSurfaceVariant = AALyricsColors.TextSecondary,
    outline = AALyricsColors.BorderSoft,
    error = AALyricsColors.Error,
)
