package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.R
import io.github.whoxamxl.aalyrics.ui.phone.component.PhonePopupMenu

@Preview(
    name = "Signing mismatch tooltip",
    group = "UpdateInstallFailure",
    widthDp = 412,
    heightDp = 420,
    showBackground = true,
)
@Composable
private fun InstallFailureSigningMismatchTooltipPreview() {
    InstallFailureTooltipPreview()
}

@Preview(
    name = "Signing mismatch tooltip · narrow",
    group = "UpdateInstallFailure",
    widthDp = 320,
    heightDp = 420,
    showBackground = true,
)
@Composable
private fun InstallFailureSigningMismatchTooltipNarrowPreview() {
    InstallFailureTooltipPreview()
}

@Preview(
    name = "Signing mismatch tooltip · large font",
    group = "UpdateInstallFailure",
    widthDp = 412,
    heightDp = 480,
    fontScale = 1.4f,
    showBackground = true,
)
@Composable
private fun InstallFailureSigningMismatchTooltipLargeFontPreview() {
    InstallFailureTooltipPreview()
}

@Composable
private fun InstallFailureTooltipPreview() {
    AALyricsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase)
                .padding(AALyricsSpacing.Space24),
            contentAlignment = Alignment.Center,
        ) {
            PhonePopupMenu(
                expanded = true,
                onDismissRequest = {},
                modifier = Modifier.widthIn(max = 280.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.settings_update_install_failure_signing_mismatch,
                    ),
                    style = AALyricsTypography.TrackArtist,
                    color = AALyricsColors.TextPrimary,
                    modifier = Modifier.padding(AALyricsSpacing.Space16),
                )
            }
        }
    }
}
