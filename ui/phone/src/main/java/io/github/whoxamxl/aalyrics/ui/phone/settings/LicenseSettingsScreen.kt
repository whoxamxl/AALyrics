package io.github.whoxamxl.aalyrics.ui.phone.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.R
import io.github.whoxamxl.aalyrics.ui.phone.component.PhoneMarkdownText

/** Second-level Settings surface showing the bundled AALyrics notice and license. */
@Composable
internal fun LicenseSettingsScreen(
    noticeText: String,
    licenseText: String,
    onThirdPartyLicensesRequested: () -> Unit,
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
        SettingsSubscreenHeader(
            title = stringResource(R.string.settings_license),
            backContentDescription = stringResource(R.string.settings_back),
            onBack = onBack,
        )

        Spacer(Modifier.height(AALyricsSpacing.Space12))

        SettingsSection(
            title = stringResource(R.string.settings_required_notice),
        ) {
            SelectionContainer {
                Column(
                    modifier = Modifier.padding(AALyricsSpacing.Space16),
                ) {
                    Text(
                        text = stringResource(R.string.settings_brand_name),
                        style = AALyricsTypography.TrackTitle,
                        color = AALyricsColors.TextPrimary,
                    )

                    Spacer(Modifier.height(AALyricsSpacing.Space4))

                    Text(
                        text = noticeText.requiredNoticeDisplayText(),
                        style = AALyricsTypography.AppTitle,
                        color = AALyricsColors.TextSecondary,
                    )
                }
            }
        }

        Spacer(Modifier.height(AALyricsSpacing.Space20))

        SettingsSection(
            title = stringResource(R.string.settings_license_terms),
        ) {
            SelectionContainer {
                PhoneMarkdownText(
                    markdown = licenseText,
                    modifier = Modifier.padding(AALyricsSpacing.Space16),
                )
            }
        }

        Spacer(Modifier.height(AALyricsSpacing.Space20))

        SettingsSection(
            title = stringResource(R.string.settings_section_third_party_software),
        ) {
            SettingsNavigationRow(
                title = stringResource(R.string.settings_third_party_licenses),
                value = null,
                onClick = onThirdPartyLicensesRequested,
            )
        }
    }
}

private fun String.requiredNoticeDisplayText(): String {
    val requiredNoticeLine = lineSequence()
        .map(String::trim)
        .firstOrNull { it.startsWith(REQUIRED_NOTICE_PREFIX) }
        ?: return trim()

    return requiredNoticeLine
        .removePrefix(REQUIRED_NOTICE_PREFIX)
        .trim()
        .ifEmpty { requiredNoticeLine }
}

private const val REQUIRED_NOTICE_PREFIX = "Required Notice:"
