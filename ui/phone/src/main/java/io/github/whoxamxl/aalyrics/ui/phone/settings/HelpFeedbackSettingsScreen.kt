package io.github.whoxamxl.aalyrics.ui.phone.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.phone.R

/** Semantic destinations emitted by the Phone Help & Feedback surface. */
enum class HelpFeedbackDestination {
    REPORT_BUG,
    ASK_QUESTION,
    SUGGEST_IDEA,
    GENERAL_DISCUSSION,
    REPORT_SECURITY_ISSUE,
}

/** Second-level Settings routing hub for support, feedback, and private security reporting. */
@Composable
internal fun HelpFeedbackSettingsScreen(
    onDestinationSelected: (HelpFeedbackDestination) -> Unit,
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
            title = stringResource(R.string.settings_help_feedback),
            backContentDescription = stringResource(R.string.settings_back),
            onBack = onBack,
        )

        Spacer(Modifier.height(AALyricsSpacing.Space12))

        SettingsSection(title = null) {
            SettingsExternalLinkRow(
                title = stringResource(R.string.settings_report_bug),
                value = stringResource(R.string.settings_github_issues),
                onClick = {
                    onDestinationSelected(HelpFeedbackDestination.REPORT_BUG)
                },
            )

            SettingsDivider()

            SettingsExternalLinkRow(
                title = stringResource(R.string.settings_ask_question),
                value = stringResource(R.string.settings_github_q_and_a),
                onClick = {
                    onDestinationSelected(HelpFeedbackDestination.ASK_QUESTION)
                },
            )

            SettingsDivider()

            SettingsExternalLinkRow(
                title = stringResource(R.string.settings_suggest_idea),
                value = stringResource(R.string.settings_github_ideas),
                onClick = {
                    onDestinationSelected(HelpFeedbackDestination.SUGGEST_IDEA)
                },
            )

            SettingsDivider()

            SettingsExternalLinkRow(
                title = stringResource(R.string.settings_general_discussion),
                value = stringResource(R.string.settings_github_general),
                onClick = {
                    onDestinationSelected(HelpFeedbackDestination.GENERAL_DISCUSSION)
                },
            )

            SettingsDivider()

            SettingsExternalLinkRow(
                title = stringResource(R.string.settings_report_security_issue),
                value = stringResource(R.string.settings_private_report),
                onClick = {
                    onDestinationSelected(HelpFeedbackDestination.REPORT_SECURITY_ISSUE)
                },
            )
        }
    }
}
