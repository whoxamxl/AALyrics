package io.github.whoxamxl.aalyrics.ui.phone.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
internal fun SettingsValueRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AALyricsSpacing.Space64)
            .padding(
                start = AALyricsSpacing.Space16,
                end = AALyricsSpacing.Space16,
                top = AALyricsSpacing.Space12,
                bottom = AALyricsSpacing.Space12,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = AALyricsTypography.AppTitle,
            color = AALyricsColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = value,
            style = AALyricsTypography.TrackArtist,
            color = AALyricsColors.TextSecondary,
            maxLines = 1,
            modifier = Modifier
                .widthIn(max = 160.dp)
                .padding(start = AALyricsSpacing.Space12),
        )
    }
}

@Composable
internal fun AboutDialog(
    title: String,
    body: String,
    versionLabel: String,
    versionName: String,
    githubLabel: String,
    closeLabel: String,
    onOpenGitHub: () -> Unit,
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
            Column {
                Text(
                    text = body,
                    style = AALyricsTypography.TrackArtist,
                    color = AALyricsColors.TextSecondary,
                )

                Spacer(Modifier.height(AALyricsSpacing.Space16))

                Text(
                    text = "$versionLabel $versionName",
                    style = AALyricsTypography.Label,
                    color = AALyricsColors.TextTertiary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenGitHub) {
                Text(text = githubLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = closeLabel)
            }
        },
        containerColor = AALyricsColors.BackgroundSurfaceStrong,
        titleContentColor = AALyricsColors.TextPrimary,
        textContentColor = AALyricsColors.TextPrimary,
        shape = RoundedCornerShape(AALyricsRadius.Radius16),
    )
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
            modifier = Modifier.size(AALyricsSpacing.Space48),
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
    downloadContentDescription: String,
    downloadingContentDescription: String,
    readyContentDescription: String,
    builtInContentDescription: String,
    retryContentDescription: String,
    onSelected: (String) -> Unit,
    onDownloadRequested: (String) -> Unit,
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
                    TargetLanguageRow(
                        option = option,
                        selected = option.id == selectedId,
                        downloadContentDescription = downloadContentDescription,
                        downloadingContentDescription = downloadingContentDescription,
                        readyContentDescription = readyContentDescription,
                        builtInContentDescription = builtInContentDescription,
                        retryContentDescription = retryContentDescription,
                        onSelected = {
                            onSelected(option.id)
                            onDismissRequest()
                        },
                        onDownloadRequested = {
                            onDownloadRequested(option.id)
                        },
                    )
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

@Composable
private fun TargetLanguageRow(
    option: SettingsLanguageOptionUiState,
    selected: Boolean,
    downloadContentDescription: String,
    downloadingContentDescription: String,
    readyContentDescription: String,
    builtInContentDescription: String,
    retryContentDescription: String,
    onSelected: () -> Unit,
    onDownloadRequested: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AALyricsSpacing.Space48)
            .clickable(onClick = onSelected)
            .padding(start = AALyricsSpacing.Space8),
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
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )

        TranslationModelStatusAction(
            option = option,
            downloadContentDescription = downloadContentDescription,
            downloadingContentDescription = downloadingContentDescription,
            readyContentDescription = readyContentDescription,
            builtInContentDescription = builtInContentDescription,
            retryContentDescription = retryContentDescription,
            onDownloadRequested = { onDownloadRequested() },
        )

        Box(
            modifier = Modifier.size(AALyricsSpacing.Space48),
            contentAlignment = Alignment.Center,
        ) {
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

@Composable
private fun TranslationModelStatusAction(
    option: SettingsLanguageOptionUiState,
    downloadContentDescription: String,
    downloadingContentDescription: String,
    readyContentDescription: String,
    builtInContentDescription: String,
    retryContentDescription: String,
    onDownloadRequested: (String) -> Unit,
) {
    when (option.modelState) {
        TranslationModelUiState.BUILT_IN -> {
            Box(
                modifier = Modifier.size(AALyricsSpacing.Space48),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AALyricsIcons.DownloadDone,
                    contentDescription = builtInContentDescription,
                    tint = AALyricsColors.AccentCyan,
                    modifier = Modifier.size(AALyricsSpacing.Space20),
                )
            }
        }

        TranslationModelUiState.NOT_DOWNLOADED -> {
            IconButton(
                onClick = { onDownloadRequested(option.id) },
                modifier = Modifier.size(AALyricsSpacing.Space48),
            ) {
                Icon(
                    imageVector = AALyricsIcons.Download,
                    contentDescription = downloadContentDescription,
                    tint = AALyricsColors.TextSecondary,
                    modifier = Modifier.size(AALyricsSpacing.Space20),
                )
            }
        }

        TranslationModelUiState.DOWNLOADING -> {
            Box(
                modifier = Modifier
                    .size(AALyricsSpacing.Space40)
                    .semantics {
                        contentDescription = downloadingContentDescription
                    },
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AALyricsSpacing.Space20),
                    color = AALyricsColors.AccentCyan,
                    strokeWidth = AALyricsStroke.Strong,
                )
            }
        }

        TranslationModelUiState.READY -> {
            Box(
                modifier = Modifier.size(AALyricsSpacing.Space48),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AALyricsIcons.DownloadDone,
                    contentDescription = readyContentDescription,
                    tint = AALyricsColors.Success,
                    modifier = Modifier.size(AALyricsSpacing.Space20),
                )
            }
        }

        TranslationModelUiState.FAILED -> {
            IconButton(
                onClick = { onDownloadRequested(option.id) },
                modifier = Modifier.size(AALyricsSpacing.Space48),
            ) {
                Icon(
                    imageVector = AALyricsIcons.DownloadFailed,
                    contentDescription = retryContentDescription,
                    tint = AALyricsColors.Error,
                    modifier = Modifier.size(AALyricsSpacing.Space20),
                )
            }
        }
    }
}
