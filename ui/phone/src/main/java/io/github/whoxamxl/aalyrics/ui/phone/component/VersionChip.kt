package io.github.whoxamxl.aalyrics.ui.phone.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsRadius
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsStroke
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography

/**
 * Shared Phone presentation for semantic application/release versions.
 *
 * Channel differences stay intentionally restrained: each channel reuses an
 * existing AALyrics semantic/accent color with a low-emphasis tinted surface.
 */
@Composable
internal fun VersionChip(
    versionName: String,
    modifier: Modifier = Modifier,
) {
    val presentation = versionChipPresentation(versionName)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AALyricsRadius.Full),
        color = presentation.accent.copy(alpha = 0.10f),
        border = BorderStroke(
            width = AALyricsStroke.Thin,
            color = presentation.accent.copy(alpha = 0.38f),
        ),
    ) {
        Text(
            text = normalizeVersionLabel(versionName),
            modifier = Modifier.padding(
                horizontal = AALyricsSpacing.Space12,
                vertical = AALyricsSpacing.Space4,
            ),
            style = AALyricsTypography.Label.copy(
                fontFamily = FontFamily.Monospace,
            ),
            color = presentation.accent,
            maxLines = 1,
        )
    }
}

@Immutable
private data class VersionChipPresentation(
    val channel: VersionChannel,
    val accent: Color,
)

private enum class VersionChannel {
    DEV,
    ALPHA,
    BETA,
    RC,
    STABLE,
}

private fun versionChipPresentation(versionName: String): VersionChipPresentation {
    val normalized = versionName
        .trim()
        .removePrefix("v")
        .lowercase()

    val channel = when {
        normalized.contains("-dev") -> VersionChannel.DEV
        normalized.contains("-alpha") -> VersionChannel.ALPHA
        normalized.contains("-beta") -> VersionChannel.BETA
        normalized.contains("-rc") -> VersionChannel.RC
        else -> VersionChannel.STABLE
    }

    val accent = when (channel) {
        VersionChannel.DEV -> AALyricsColors.AccentCyan
        VersionChannel.ALPHA -> AALyricsColors.Error
        VersionChannel.BETA -> AALyricsColors.Warning
        VersionChannel.RC -> AALyricsColors.AccentBlue
        VersionChannel.STABLE -> AALyricsColors.Success
    }

    return VersionChipPresentation(
        channel = channel,
        accent = accent,
    )
}

private fun normalizeVersionLabel(versionName: String): String {
    val trimmed = versionName.trim()
    return if (trimmed.startsWith("v")) trimmed else "v$trimmed"
}
