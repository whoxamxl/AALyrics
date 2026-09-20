package io.github.whoxamxl.aalyrics.ui.phone.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.icon.AALyricsIcons
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsRadius
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsStroke
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography

@Composable
internal fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = AALyricsTypography.Label,
            color = AALyricsColors.AccentCyan,
            modifier = Modifier.padding(
                start = AALyricsSpacing.Space4,
                bottom = AALyricsSpacing.Space8,
            ),
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AALyricsRadius.Radius16),
            color = AALyricsColors.BackgroundSurface,
            border = BorderStroke(
                width = AALyricsStroke.Thin,
                color = AALyricsColors.BorderSoft,
            ),
        ) {
            Column(content = content)
        }
    }
}

@Composable
internal fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    infoText: String? = null,
    infoContentDescription: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AALyricsSpacing.Space64)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(
                start = AALyricsSpacing.Space16,
                end = AALyricsSpacing.Space12,
                top = AALyricsSpacing.Space8,
                bottom = AALyricsSpacing.Space8,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = AALyricsTypography.AppTitle,
            color = if (enabled) {
                AALyricsColors.TextPrimary
            } else {
                AALyricsColors.TextTertiary
            },
            modifier = Modifier.weight(1f),
        )

        if (infoText != null && infoContentDescription != null) {
            SettingInfoTooltip(
                text = infoText,
                contentDescription = infoContentDescription,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AALyricsColors.BackgroundBase,
                checkedTrackColor = AALyricsColors.AccentCyan,
                uncheckedThumbColor = AALyricsColors.TextSecondary,
                uncheckedTrackColor = AALyricsColors.OverlaySoft,
                uncheckedBorderColor = AALyricsColors.BorderSoft,
                disabledCheckedThumbColor = AALyricsColors.TextTertiary,
                disabledCheckedTrackColor = AALyricsColors.OverlaySoft,
                disabledUncheckedThumbColor = AALyricsColors.TextTertiary,
                disabledUncheckedTrackColor = AALyricsColors.OverlaySoft,
            ),
        )
    }
}

@Composable
internal fun SettingsNavigationRow(
    title: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueColor: Color = AALyricsColors.TextSecondary,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AALyricsSpacing.Space64)
            .clickable(
                enabled = enabled,
                onClick = onClick,
            )
            .padding(
                start = AALyricsSpacing.Space16,
                end = AALyricsSpacing.Space12,
                top = AALyricsSpacing.Space12,
                bottom = AALyricsSpacing.Space12,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = AALyricsTypography.AppTitle,
            color = if (enabled) {
                AALyricsColors.TextPrimary
            } else {
                AALyricsColors.TextTertiary
            },
            modifier = Modifier.weight(1f),
        )

        value?.let {
            Text(
                text = it,
                style = AALyricsTypography.TrackArtist,
                color = if (enabled) valueColor else AALyricsColors.TextTertiary,
                maxLines = 1,
                modifier = Modifier
                    .widthIn(max = 144.dp)
                    .padding(start = AALyricsSpacing.Space12),
            )
        }

        Icon(
            imageVector = AALyricsIcons.NavigateNext,
            contentDescription = null,
            tint = if (enabled) {
                AALyricsColors.TextSecondary
            } else {
                AALyricsColors.TextTertiary
            },
            modifier = Modifier.size(AALyricsSpacing.Space24),
        )
    }
}

@Composable
internal fun SettingsDivider() {
    HorizontalDivider(
        thickness = AALyricsStroke.Thin,
        color = AALyricsColors.BorderSoft.copy(alpha = 0.72f),
        modifier = Modifier.padding(horizontal = AALyricsSpacing.Space16),
    )
}

@Composable
internal fun SettingInfoTooltip(
    text: String,
    contentDescription: String,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(AALyricsSpacing.Space40),
        ) {
            Icon(
                imageVector = AALyricsIcons.Info,
                contentDescription = contentDescription,
                tint = AALyricsColors.TextSecondary,
                modifier = Modifier.size(AALyricsSpacing.Space20),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Text(
                text = text,
                style = AALyricsTypography.TrackArtist,
                color = AALyricsColors.TextPrimary,
                modifier = Modifier.padding(AALyricsSpacing.Space16),
            )
        }
    }
}

@Composable
internal fun TargetLanguagePicker(
    options: List<SettingsLanguageOptionUiState>,
    selectedId: String,
    title: String,
    onSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                style = AALyricsTypography.TrackTitle,
                color = AALyricsColors.TextPrimary,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                options.forEach { option ->
                    val selected = option.id == selectedId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = AALyricsSpacing.Space48)
                            .clickable {
                                onSelected(option.id)
                                onDismissRequest()
                            }
                            .padding(
                                horizontal = AALyricsSpacing.Space8,
                                vertical = AALyricsSpacing.Space8,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option.displayName,
                            style = AALyricsTypography.AppTitle,
                            color = if (selected) {
                                AALyricsColors.AccentCyan
                            } else {
                                AALyricsColors.TextPrimary
                            },
                            modifier = Modifier.weight(1f),
                        )

                        if (selected) {
                            Icon(
                                imageVector = AALyricsIcons.Check,
                                contentDescription = null,
                                tint = AALyricsColors.AccentCyan,
                                modifier = Modifier.size(AALyricsSpacing.Space24),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = AALyricsColors.BackgroundSurfaceStrong,
        titleContentColor = AALyricsColors.TextPrimary,
        textContentColor = AALyricsColors.TextPrimary,
        shape = RoundedCornerShape(AALyricsRadius.Radius16),
    )
}
