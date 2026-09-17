package io.github.whoxamxl.aalyrics.ui.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Shared AALyrics visual foundation.
 *
 * The first UI slice is deliberately dark-first. A light scheme should be introduced only
 * when its full semantic palette has been designed and reviewed rather than auto-derived.
 */
@Composable
fun AALyricsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AALyricsDarkColorScheme,
        typography = AALyricsMaterialTypography,
        shapes = AALyricsMaterialShapes,
        content = content,
    )
}
