package io.github.whoxamxl.aalyrics.ui.phone.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.icon.AALyricsIcons
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.R

/** Second-level Settings surface for narrowly scoped debug and future features. */
@Composable
fun AdvancedSettingsScreen(
    verboseDetailsEnabled: Boolean,
    onVerboseDetailsChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomOverlayInset: Dp = 0.dp,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = AALyricsSpacing.Space16,
                top = AALyricsSpacing.Space8,
                end = AALyricsSpacing.Space16,
                bottom = bottomOverlayInset + AALyricsSpacing.Space16,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(AALyricsSpacing.Space48),
            ) {
                Icon(
                    imageVector = AALyricsIcons.Back,
                    contentDescription = stringResource(R.string.settings_back),
                    tint = AALyricsColors.TextPrimary,
                    modifier = Modifier.size(28.8.dp),
                )
            }

            Text(
                text = stringResource(R.string.settings_advanced),
                style = AALyricsTypography.LyricsSupporting,
                color = AALyricsColors.TextPrimary,
                modifier = Modifier.padding(start = AALyricsSpacing.Space4),
            )
        }

        Spacer(Modifier.height(AALyricsSpacing.Space12))

        SettingsSection(
            title = stringResource(R.string.settings_section_debug),
        ) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_verbose_details),
                checked = verboseDetailsEnabled,
                onCheckedChange = onVerboseDetailsChanged,
            )
        }

        Spacer(Modifier.height(AALyricsSpacing.Space20))

        SettingsSection(
            title = stringResource(R.string.settings_section_experimental),
        ) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_karaoke_mode),
                checked = false,
                onCheckedChange = {},
                enabled = false,
                infoText = stringResource(R.string.settings_karaoke_unavailable),
                infoContentDescription =
                    stringResource(R.string.settings_karaoke_unavailable_description),
            )
        }
    }
}
