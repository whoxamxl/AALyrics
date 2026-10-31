package io.github.whoxamxl.aalyrics.ui.phone.sync

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.icon.AALyricsIcons
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.R

/** Compact manual controls; application callbacks retain all timing and persistence ownership. */
@Composable
internal fun SyncOffsetCard(
    state: SyncScreenUiState,
    onAdjust: (SyncAdjustment) -> Unit,
    onReset: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resetDescription = stringResource(R.string.sync_reset)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = AALyricsColors.BackgroundSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.sync_edit_offset),
                            onClick = onEdit,
                        ),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        stringResource(R.string.sync_offset_label),
                        style = AALyricsTypography.Label,
                        color = AALyricsColors.TextSecondary,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            stringResource(
                                R.string.sync_offset_value,
                                if (state.offsetMs > 0) "+${state.offsetMs}" else state.offsetMs.toString(),
                            ),
                            style = AALyricsTypography.LyricsSupporting,
                            color = AALyricsColors.AccentCyan,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Icon(
                            imageVector = AALyricsIcons.Edit,
                            contentDescription = null,
                            tint = AALyricsColors.AccentCyan,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                TextButton(
                    onClick = onReset,
                    enabled = state.offsetMs != 0L,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = resetDescription },
                ) {
                    Text(stringResource(R.string.sync_reset_short), style = AALyricsTypography.TrackArtist)
                }
            }
            BoxWithConstraints {
                val stacked = maxWidth < 240.dp || LocalDensity.current.fontScale > 1.5f
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = if (stacked) 1 else 2,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SyncAdjustmentGroup(state, onAdjust, delay = true, modifier = Modifier.weight(1f))
                    SyncAdjustmentGroup(state, onAdjust, delay = false, modifier = Modifier.weight(1f))
                }
            }
            Text(
                stringResource(R.string.sync_scope_compact),
                style = AALyricsTypography.Label,
                color = AALyricsColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun SyncAdjustmentGroup(
    state: SyncScreenUiState,
    onAdjust: (SyncAdjustment) -> Unit,
    delay: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(if (delay) R.string.sync_delay_label else R.string.sync_advance_label),
            style = AALyricsTypography.Label,
            color = AALyricsColors.TextSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Put the smaller steps in the middle: −500, −50, +50, +500.
            for (coarse in if (delay) listOf(true, false) else listOf(false, true)) {
                SyncStepButton(
                    delay = delay,
                    coarse = coarse,
                    enabled = if (delay) state.canDelay else state.canAdvance,
                    onAdjust = onAdjust,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SyncStepButton(
    delay: Boolean,
    coarse: Boolean,
    enabled: Boolean,
    onAdjust: (SyncAdjustment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        if (delay) R.string.sync_delay_description else R.string.sync_advance_description,
        if (coarse) 500 else 50,
    )
    val buttonModifier = modifier.heightIn(min = 48.dp).semantics { contentDescription = description }
    val padding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    val content: @Composable () -> Unit = {
        Text(
            stringResource(when {
                delay && coarse -> R.string.sync_delay_500
                delay -> R.string.sync_delay_50
                coarse -> R.string.sync_advance_500
                else -> R.string.sync_advance_50
            }),
            style = AALyricsTypography.TrackArtist,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
    if (delay) {
        OutlinedButton(
            onClick = { onAdjust(if (coarse) SyncAdjustment.DELAY_COARSE else SyncAdjustment.DELAY_FINE) },
            enabled = enabled,
            modifier = buttonModifier,
            contentPadding = padding,
        ) { content() }
    } else {
        Button(
            onClick = { onAdjust(if (coarse) SyncAdjustment.ADVANCE_COARSE else SyncAdjustment.ADVANCE_FINE) },
            enabled = enabled,
            modifier = buttonModifier,
            contentPadding = padding,
        ) { content() }
    }
}
