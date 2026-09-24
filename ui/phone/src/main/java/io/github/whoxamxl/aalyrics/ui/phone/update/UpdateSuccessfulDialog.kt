package io.github.whoxamxl.aalyrics.ui.phone.update

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsRadius
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsStroke
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.R
import io.github.whoxamxl.aalyrics.ui.phone.component.PhoneDialogHeader
import io.github.whoxamxl.aalyrics.ui.phone.component.VersionChip

@Immutable
data class UpdateSuccessfulDialogUiState(
    val versionName: String,
)

@Composable
fun UpdateSuccessfulDialog(
    state: UpdateSuccessfulDialogUiState,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = AALyricsSpacing.Space16,
                    vertical = AALyricsSpacing.Space24,
                ),
            contentAlignment = Alignment.Center,
        ) {
            UpdateSuccessfulDialogContent(
                state = state,
                onDismissRequest = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
            )
        }
    }
}

@Composable
fun UpdateSuccessfulDialogContent(
    state: UpdateSuccessfulDialogUiState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AALyricsRadius.Radius24),
        color = AALyricsColors.BackgroundSurface,
        border = BorderStroke(
            width = AALyricsStroke.Thin,
            color = AALyricsColors.BorderSoft,
        ),
    ) {
        Column(
            modifier = Modifier.padding(AALyricsSpacing.Space24),
        ) {
            PhoneDialogHeader(
                eyebrow = stringResource(R.string.update_successful_eyebrow),
                closeContentDescription = stringResource(
                    R.string.update_successful_close,
                ),
                onClose = onDismissRequest,
            )

            Spacer(Modifier.height(AALyricsSpacing.Space8))

            Text(
                text = stringResource(R.string.update_successful_title),
                style = AALyricsTypography.LyricsSupporting,
                color = AALyricsColors.TextPrimary,
            )

            Spacer(Modifier.height(AALyricsSpacing.Space16))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AALyricsSpacing.Space4),
                verticalArrangement = Arrangement.spacedBy(AALyricsSpacing.Space4),
            ) {
                Text(
                    text = stringResource(R.string.update_successful_body),
                    style = AALyricsTypography.TrackArtist,
                    color = AALyricsColors.TextSecondary,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterVertically),
                ) {
                    VersionChip(versionName = state.versionName)
                    Text(
                        text = ".",
                        style = AALyricsTypography.TrackArtist,
                        color = AALyricsColors.TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(AALyricsSpacing.Space32))

            Button(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.update_successful_done),
                )
            }
        }
    }
}
