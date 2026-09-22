package io.github.whoxamxl.aalyrics.ui.phone.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.phone.R
import io.github.whoxamxl.aalyrics.ui.phone.component.PhoneMarkdownText

/** Child of License showing the exact THIRD_PARTY_LICENSES.md bundled with this build. */
@Composable
internal fun ThirdPartyLicensesSettingsScreen(
    thirdPartyLicensesText: String,
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
            title = stringResource(R.string.settings_third_party_licenses),
            backContentDescription = stringResource(R.string.settings_back),
            onBack = onBack,
        )

        Spacer(Modifier.height(AALyricsSpacing.Space12))

        SettingsSection(title = null) {
            SelectionContainer {
                PhoneMarkdownText(
                    markdown = thirdPartyLicensesText,
                    modifier = Modifier.padding(AALyricsSpacing.Space16),
                )
            }
        }
    }
}
