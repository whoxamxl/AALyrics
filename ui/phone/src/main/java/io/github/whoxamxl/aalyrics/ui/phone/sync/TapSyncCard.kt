package io.github.whoxamxl.aalyrics.ui.phone.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.R

@Composable
internal fun TapSyncCard(
    state: TapSyncUiState,
    onStart: () -> Unit,
    onTap: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(16.dp), color = AALyricsColors.BackgroundSurface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.sync_tap_title), style = AALyricsTypography.Label, color = AALyricsColors.TextSecondary)
            if (state.active) {
                Text(
                    stringResource(R.string.sync_tap_listen, state.completedTaps + 1),
                    style = AALyricsTypography.TrackArtist,
                    color = AALyricsColors.TextPrimary,
                )
                state.lines.forEachIndexed { index, line ->
                    val current = index == state.completedTaps
                    // A fixed marker column and one text size keep the tap button still as targets advance.
                    Row(
                        modifier = Modifier.fillMaxWidth().semantics { selected = current },
                    ) {
                        val color = if (current) AALyricsColors.AccentCyan else AALyricsColors.TextSecondary
                        Text(
                            text = if (index < state.completedTaps) "✓" else "${index + 1}.",
                            modifier = Modifier.width(28.dp),
                            style = AALyricsTypography.TrackArtist,
                            color = color,
                        )
                        Text(
                            text = line,
                            modifier = Modifier.weight(1f),
                            style = AALyricsTypography.TrackArtist,
                            color = color,
                        )
                    }
                }
                Button(
                    onClick = onTap,
                    enabled = state.availability == TapSyncAvailability.READY,
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text(
                        stringResource(R.string.sync_tap_now, state.completedTaps + 1),
                        style = AALyricsTypography.TrackArtist,
                        textAlign = TextAlign.Center,
                    )
                }
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.sync_tap_cancel))
                }
            } else {
                Text(stringResource(R.string.sync_tap_help), style = AALyricsTypography.TrackArtist, color = AALyricsColors.TextSecondary)
                state.savedOffsetMs?.let { value ->
                    Text(
                        stringResource(R.string.sync_tap_saved, if (value > 0) "+$value" else value.toString()),
                        style = AALyricsTypography.TrackArtist,
                        color = AALyricsColors.AccentCyan,
                    )
                }
                state.message?.let { message ->
                    Text(
                        stringResource(when (message) {
                            TapSyncMessage.SOURCE_CHANGED -> R.string.sync_tap_interrupted
                            TapSyncMessage.OUT_OF_RANGE -> R.string.sync_tap_out_of_range
                        }),
                        style = AALyricsTypography.TrackArtist,
                        color = AALyricsColors.TextSecondary,
                    )
                }
                if (state.availability != TapSyncAvailability.READY) {
                    Text(
                        stringResource(when (state.availability) {
                            TapSyncAvailability.NEEDS_PLAYBACK -> R.string.sync_tap_play_first
                            TapSyncAvailability.NOT_ENOUGH_LINES -> R.string.sync_tap_not_enough_lines
                            else -> R.string.sync_tap_timed_required
                        }),
                        style = AALyricsTypography.TrackArtist,
                        color = AALyricsColors.TextSecondary,
                    )
                }
                Button(
                    onClick = onStart,
                    enabled = state.availability == TapSyncAvailability.READY,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.sync_tap_start), textAlign = TextAlign.Center)
                }
            }
        }
    }
}
