package io.github.whoxamxl.aalyrics.ui.phone.update

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import io.github.whoxamxl.aalyrics.ui.phone.component.VersionChip
import kotlin.math.roundToInt

@Composable
fun UnifiedUpdateDialog(
    state: UpdateDialogUiState,
) {
    require(state.phase.isDownloadProgressPhase()) {
        "UnifiedUpdateDialog download checkpoint cannot render ${state.phase}"
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
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
            UnifiedUpdateDialogContent(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
            )
        }
    }
}

@Composable
internal fun UnifiedUpdateDialogContent(
    state: UpdateDialogUiState,
    modifier: Modifier = Modifier,
) {
    require(state.phase.isDownloadProgressPhase()) {
        "UnifiedUpdateDialog download checkpoint cannot render ${state.phase}"
    }

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
            Text(
                text = stringResource(R.string.update_process_eyebrow),
                style = AALyricsTypography.Label,
                color = AALyricsColors.AccentCyan,
            )

            Spacer(Modifier.height(AALyricsSpacing.Space8))

            Text(
                text = stringResource(
                    when (state.phase) {
                        UpdateDialogPhase.DOWNLOADING ->
                            R.string.update_process_downloading_title
                        UpdateDialogPhase.VERIFYING ->
                            R.string.update_process_verifying_title
                        else ->
                            R.string.update_process_preparing_title
                    },
                ),
                style = AALyricsTypography.LyricsSupporting,
                color = AALyricsColors.TextPrimary,
            )

            Spacer(Modifier.height(AALyricsSpacing.Space16))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AALyricsSpacing.Space4),
                verticalArrangement = Arrangement.spacedBy(AALyricsSpacing.Space4),
            ) {
                Text(
                    text = stringResource(R.string.update_release_available_body_prefix),
                    style = AALyricsTypography.TrackArtist,
                    color = AALyricsColors.TextSecondary,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                VersionChip(
                    versionName = state.versionName,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }

            Spacer(Modifier.height(AALyricsSpacing.Space24))

            when (state.phase) {
                UpdateDialogPhase.DOWNLOADING -> {
                    val progress = (state.downloadProgress ?: 0f).coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.update_process_downloading_label),
                            style = AALyricsTypography.TrackArtist,
                            color = AALyricsColors.TextSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${(progress * 100f).roundToInt()}%",
                            style = AALyricsTypography.TrackArtist,
                            color = AALyricsColors.TextSecondary,
                        )
                    }

                    Spacer(Modifier.height(AALyricsSpacing.Space8))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = AALyricsColors.AccentCyan,
                        trackColor = AALyricsColors.OverlaySoft,
                    )
                }

                UpdateDialogPhase.PREPARING_DOWNLOAD -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = AALyricsColors.AccentCyan,
                        trackColor = AALyricsColors.OverlaySoft,
                    )
                }

                UpdateDialogPhase.VERIFYING -> {
                    Text(
                        text = stringResource(R.string.update_process_verifying_label),
                        style = AALyricsTypography.TrackArtist,
                        color = AALyricsColors.TextSecondary,
                    )

                    Spacer(Modifier.height(AALyricsSpacing.Space8))

                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = AALyricsColors.AccentCyan,
                        trackColor = AALyricsColors.OverlaySoft,
                    )
                }

                else -> Unit
            }
        }
    }
}

internal fun UpdateDialogPhase.isDownloadProgressPhase(): Boolean =
    this == UpdateDialogPhase.PREPARING_DOWNLOAD ||
        this == UpdateDialogPhase.DOWNLOADING ||
        this == UpdateDialogPhase.VERIFYING
