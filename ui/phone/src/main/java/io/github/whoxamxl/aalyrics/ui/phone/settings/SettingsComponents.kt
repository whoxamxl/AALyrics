package io.github.whoxamxl.aalyrics.ui.phone.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.icon.AALyricsIcons
import io.github.whoxamxl.aalyrics.ui.phone.component.PhonePopupMenu
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsRadius
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsSpacing
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsStroke
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.R

@Composable
internal fun SettingsSubscreenHeader(
    title: String,
    backContentDescription: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(AALyricsSpacing.Space48),
        ) {
            Icon(
                imageVector = AALyricsIcons.Back,
                contentDescription = backContentDescription,
                tint = AALyricsColors.TextPrimary,
                modifier = Modifier.size(32.dp),
            )
        }

        Text(
            text = title,
            style = AALyricsTypography.LyricsSupporting,
            color = AALyricsColors.TextPrimary,
            modifier = Modifier.padding(start = AALyricsSpacing.Space4),
        )
    }
}

@Composable
internal fun SettingsSection(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        title?.let {
            Text(
                text = it,
                style = AALyricsTypography.Label,
                color = AALyricsColors.AccentCyan,
                modifier = Modifier.padding(
                    start = AALyricsSpacing.Space4,
                    bottom = AALyricsSpacing.Space8,
                ),
            )
        }

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
                overflow = TextOverflow.Ellipsis,
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
internal fun SettingsActionRow(
    title: String,
    actionLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleColor: Color = AALyricsColors.TextPrimary,
    actionColor: Color = AALyricsColors.AccentCyan,
    infoText: String? = null,
    infoContentDescription: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AALyricsSpacing.Space64)
            .clickable(
                role = Role.Button,
                onClick = onClick,
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
            color = titleColor,
            modifier = Modifier.weight(1f),
        )

        if (infoText != null && infoContentDescription != null) {
            SettingInfoTooltip(
                text = infoText,
                contentDescription = infoContentDescription,
            )
        }

        Box(
            modifier = Modifier
                .width(52.dp)
                .padding(start = AALyricsSpacing.Space4),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = actionLabel,
                style = AALyricsTypography.TrackArtist,
                color = actionColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun SettingsExternalLinkRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AALyricsSpacing.Space64)
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
            color = AALyricsColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )

        Row(
            modifier = Modifier
                .heightIn(min = AALyricsSpacing.Space48)
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                ),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                style = AALyricsTypography.TrackArtist,
                color = AALyricsColors.AccentCyan,
                maxLines = 1,
            )

            Spacer(Modifier.size(AALyricsSpacing.Space4))

            Icon(
                imageVector = AALyricsIcons.ExternalLink,
                contentDescription = null,
                tint = AALyricsColors.AccentCyan,
                modifier = Modifier.size(AALyricsSpacing.Space16),
            )
        }
    }
}

@Composable
internal fun AppUpdateRow(
    versionLabel: String,
    currentVersionName: String,
    state: AppUpdateUiState,
    checkLabel: String,
    checkingLabel: String,
    upToDateLabel: String,
    updateAvailableLabel: String,
    downloadLabel: String,
    downloadingLabel: String,
    downloadedLabel: String,
    retryLabel: String,
    checkFailedLabel: String,
    downloadFailedLabel: String,
    failureInfoContentDescription: String,
    genericFailureReason: String,
    unavailableLabel: String,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AALyricsSpacing.Space16,
                vertical = AALyricsSpacing.Space12,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = versionLabel,
                style = AALyricsTypography.AppTitle,
                color = AALyricsColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )

            Text(
                text = currentVersionName.asVersionLabel(),
                style = AALyricsTypography.TrackArtist,
                color = AALyricsColors.TextSecondary,
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(AALyricsSpacing.Space12))

        when (state.phase) {
            AppUpdateUiPhase.UNAVAILABLE -> {
                AppUpdateStatusTextRow(label = unavailableLabel)
            }

            AppUpdateUiPhase.IDLE -> {
                AppUpdateActionRow(
                    actionLabel = checkLabel,
                    onAction = onCheckForUpdates,
                )
            }

            AppUpdateUiPhase.CHECKING -> {
                AppUpdateProgressRow(label = checkingLabel)
            }

            AppUpdateUiPhase.UP_TO_DATE -> {
                AppUpdateStatusRow(
                    label = upToDateLabel,
                    icon = AALyricsIcons.Check,
                    iconTint = AALyricsColors.Success,
                )
            }

            AppUpdateUiPhase.UPDATE_AVAILABLE -> {
                AppUpdateStatusTextRow(
                    label = "${updateAvailableLabel} ${
                        state.availableVersionName?.asVersionLabel().orEmpty()
                    }".trim(),
                )
            }

            AppUpdateUiPhase.CHECK_FAILED -> {
                AppUpdateFailureRow(
                    label = checkFailedLabel,
                    reason = state.failureReason ?: genericFailureReason,
                    failureInfoContentDescription = failureInfoContentDescription,
                    retryLabel = retryLabel,
                    onRetry = onCheckForUpdates,
                )
            }

            AppUpdateUiPhase.DOWNLOADING -> {
                AppUpdateProgressRow(
                    label = if (state.availableVersionName.isNullOrBlank()) {
                        downloadingLabel
                    } else {
                        "${downloadingLabel} ${state.availableVersionName.asVersionLabel()}"
                    },
                )
            }

            AppUpdateUiPhase.DOWNLOADED -> {
                AppUpdateStatusRow(
                    label = if (state.availableVersionName.isNullOrBlank()) {
                        downloadedLabel
                    } else {
                        "${downloadedLabel} ${state.availableVersionName.asVersionLabel()}"
                    },
                    icon = AALyricsIcons.Check,
                    iconTint = AALyricsColors.Success,
                )
            }

            AppUpdateUiPhase.DOWNLOAD_FAILED -> {
                AppUpdateFailureRow(
                    label = downloadFailedLabel,
                    reason = state.failureReason ?: genericFailureReason,
                    failureInfoContentDescription = failureInfoContentDescription,
                    retryLabel = retryLabel,
                    onRetry = onDownloadUpdate,
                )
            }
        }
    }
}

@Composable
private fun AppUpdateActionRow(
    actionLabel: String,
    onAction: () -> Unit,
    status: String? = null,
    actionIcon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AALyricsSpacing.Space48),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (status != null) {
            Text(
                text = status,
                style = AALyricsTypography.TrackArtist,
                color = AALyricsColors.TextSecondary,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = AALyricsSpacing.Space12),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }

        AppUpdateInlineAction(
            label = actionLabel,
            icon = actionIcon,
            onClick = onAction,
        )
    }
}

@Composable
private fun AppUpdateInlineAction(
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .widthIn(min = AALyricsSpacing.Space48)
            .heightIn(min = AALyricsSpacing.Space48)
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AALyricsColors.AccentCyan,
                modifier = Modifier.size(AALyricsSpacing.Space16),
            )
            Spacer(Modifier.size(AALyricsSpacing.Space4))
        }

        Text(
            text = label,
            style = AALyricsTypography.TrackArtist,
            color = AALyricsColors.AccentCyan,
        )
    }
}

@Composable
private fun AppUpdateProgressRow(
    label: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AALyricsSpacing.Space48),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AALyricsTypography.TrackArtist,
            color = AALyricsColors.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        CircularProgressIndicator(
            modifier = Modifier.size(AALyricsSpacing.Space20),
            color = AALyricsColors.AccentCyan,
            strokeWidth = AALyricsStroke.Strong,
        )
    }
}

@Composable
private fun AppUpdateStatusTextRow(
    label: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AALyricsSpacing.Space48),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = label,
            style = AALyricsTypography.TrackArtist,
            color = AALyricsColors.TextTertiary,
        )
    }
}

@Composable
private fun AppUpdateStatusRow(
    label: String,
    icon: ImageVector,
    iconTint: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AALyricsSpacing.Space48),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AALyricsTypography.TrackArtist,
            color = AALyricsColors.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(AALyricsSpacing.Space20),
        )
    }
}

@Composable
private fun AppUpdateFailureRow(
    label: String,
    reason: String,
    failureInfoContentDescription: String,
    retryLabel: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AALyricsSpacing.Space48),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AALyricsTypography.TrackArtist,
            color = AALyricsColors.Error,
        )

        SettingInfoTooltip(
            text = reason,
            contentDescription = failureInfoContentDescription,
        )

        Spacer(Modifier.weight(1f))

        AppUpdateInlineAction(
            label = retryLabel,
            icon = AALyricsIcons.Retry,
            onClick = onRetry,
        )
    }
}

private fun String.asVersionLabel(): String =
    if (startsWith("v", ignoreCase = true)) this else "v$this"

@Composable
internal fun SettingsBrandFooter(
    appName: String,
    versionLabel: String,
    versionName: String,
    currentYear: Int,
    copyrightOwner: String,
    logoContentDescription: String,
    onOpenGitHub: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = AALyricsSpacing.Space32,
                bottom = AALyricsSpacing.Space24,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.aalyrics_brand_foreground),
            contentDescription = logoContentDescription,
            modifier = Modifier.size(134.4.dp),
        )

        Spacer(Modifier.height(AALyricsSpacing.Space8))

        Text(
            text = appName,
            style = AALyricsTypography.LyricsSupporting,
            color = AALyricsColors.TextPrimary,
        )

        Spacer(Modifier.height(AALyricsSpacing.Space16))

        Text(
            text = "${versionLabel}: ${versionName.asVersionLabel()}",
            style = AALyricsTypography.TrackArtist,
            color = AALyricsColors.TextSecondary,
        )

        Text(
            text = "© $currentYear $copyrightOwner",
            style = AALyricsTypography.TrackArtist,
            color = AALyricsColors.TextSecondary,
            modifier = Modifier
                .heightIn(min = AALyricsSpacing.Space48)
                .clickable(
                    role = Role.Button,
                    onClick = onOpenGitHub,
                )
                .padding(horizontal = AALyricsSpacing.Space8),
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
            modifier = Modifier.size(AALyricsSpacing.Space48),
        ) {
            Icon(
                imageVector = AALyricsIcons.Info,
                contentDescription = contentDescription,
                tint = AALyricsColors.TextSecondary,
                modifier = Modifier.size(AALyricsSpacing.Space20),
            )
        }

        PhonePopupMenu(
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
internal fun SettingsConfirmationDialog(
    title: String,
    text: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    confirmColor: Color = AALyricsColors.AccentCyan,
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
            Text(
                text = text,
                style = AALyricsTypography.TrackArtist,
                color = AALyricsColors.TextSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = confirmColor,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = dismissLabel,
                    color = AALyricsColors.TextSecondary,
                )
            }
        },
        containerColor = AALyricsColors.BackgroundSurfaceStrong,
        titleContentColor = AALyricsColors.TextPrimary,
        textContentColor = AALyricsColors.TextPrimary,
        shape = RoundedCornerShape(AALyricsRadius.Radius16),
    )
}

@Composable
internal fun TargetLanguagePicker(
    options: List<SettingsLanguageOptionUiState>,
    selectedId: String,
    title: String,
    downloadContentDescription: String,
    checkingContentDescription: String,
    downloadingContentDescription: String,
    retryContentDescription: String,
    failureInfoContentDescription: String,
    genericFailureReason: String,
    confirmLabel: String,
    dismissLabel: String,
    onSelected: (String) -> Unit,
    onDownloadRequested: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val listState = rememberLazyListState()
    val topFadeAlpha by animateFloatAsState(
        targetValue = if (listState.canScrollBackward) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "targetLanguageTopFade",
    )
    val bottomFadeAlpha by animateFloatAsState(
        targetValue = if (listState.canScrollForward) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "targetLanguageBottomFade",
    )

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
            Box(
                modifier = Modifier.heightIn(max = 312.dp),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(
                        items = options,
                        key = { option -> option.id },
                    ) { option ->
                        TargetLanguageRow(
                            option = option,
                            selected = option.id == selectedId,
                            downloadContentDescription = downloadContentDescription,
                            checkingContentDescription = checkingContentDescription,
                            downloadingContentDescription = downloadingContentDescription,
                            retryContentDescription = retryContentDescription,
                            failureInfoContentDescription = failureInfoContentDescription,
                            genericFailureReason = genericFailureReason,
                            onSelected = {
                                onSelected(option.id)
                            },
                            onDownloadRequested = {
                                onDownloadRequested(option.id)
                            },
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(24.dp)
                        .alpha(topFadeAlpha)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    AALyricsColors.BackgroundSurfaceStrong,
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(24.dp)
                        .alpha(bottomFadeAlpha)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    AALyricsColors.BackgroundSurfaceStrong,
                                ),
                            ),
                        ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = AALyricsColors.AccentCyan,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = dismissLabel,
                    color = AALyricsColors.TextSecondary,
                )
            }
        },
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
    checkingContentDescription: String,
    downloadingContentDescription: String,
    retryContentDescription: String,
    failureInfoContentDescription: String,
    genericFailureReason: String,
    onSelected: () -> Unit,
    onDownloadRequested: () -> Unit,
) {
    val selectable =
        option.modelState == TranslationModelUiState.BUILT_IN ||
            option.modelState == TranslationModelUiState.READY

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AALyricsSpacing.Space48)
            .selectable(
                selected = selected,
                enabled = selectable,
                role = Role.RadioButton,
                onClick = onSelected,
            )
            .padding(start = AALyricsSpacing.Space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = option.displayName,
            style = AALyricsTypography.AppTitle,
            color = when {
                selected -> AALyricsColors.AccentCyan
                selectable -> AALyricsColors.TextPrimary
                else -> AALyricsColors.TextTertiary
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (option.modelState == TranslationModelUiState.FAILED) {
            SettingInfoTooltip(
                text = option.modelFailureReason ?: genericFailureReason,
                contentDescription = failureInfoContentDescription,
            )
        }

        TargetLanguagePrimaryAction(
            option = option,
            selected = selected,
            downloadContentDescription = downloadContentDescription,
            checkingContentDescription = checkingContentDescription,
            downloadingContentDescription = downloadingContentDescription,
            retryContentDescription = retryContentDescription,
            onDownloadRequested = onDownloadRequested,
        )
    }
}

@Composable
private fun TargetLanguagePrimaryAction(
    option: SettingsLanguageOptionUiState,
    selected: Boolean,
    downloadContentDescription: String,
    checkingContentDescription: String,
    downloadingContentDescription: String,
    retryContentDescription: String,
    onDownloadRequested: () -> Unit,
) {
    when (option.modelState) {
        TranslationModelUiState.BUILT_IN,
        TranslationModelUiState.READY -> {
            TargetLanguageIconSlot {
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

        TranslationModelUiState.CHECKING -> {
            TargetLanguageIconSlot(
                modifier = Modifier.semantics {
                    contentDescription = checkingContentDescription
                },
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AALyricsSpacing.Space20),
                    color = AALyricsColors.TextSecondary,
                    strokeWidth = AALyricsStroke.Strong,
                )
            }
        }

        TranslationModelUiState.NOT_DOWNLOADED -> {
            TargetLanguageIconSlot(onClick = onDownloadRequested) {
                Icon(
                    imageVector = AALyricsIcons.Download,
                    contentDescription = downloadContentDescription,
                    tint = AALyricsColors.TextSecondary,
                    modifier = Modifier.size(AALyricsSpacing.Space20),
                )
            }
        }

        TranslationModelUiState.DOWNLOADING -> {
            TargetLanguageIconSlot(
                modifier = Modifier.semantics {
                    contentDescription = downloadingContentDescription
                },
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AALyricsSpacing.Space20),
                    color = AALyricsColors.AccentCyan,
                    strokeWidth = AALyricsStroke.Strong,
                )
            }
        }

        TranslationModelUiState.FAILED -> {
            TargetLanguageIconSlot(onClick = onDownloadRequested) {
                Icon(
                    imageVector = AALyricsIcons.Retry,
                    contentDescription = retryContentDescription,
                    tint = AALyricsColors.Error,
                    modifier = Modifier.size(AALyricsSpacing.Space20),
                )
            }
        }
    }
}

@Composable
private fun TargetLanguageIconSlot(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    if (onClick != null) {
        IconButton(
            onClick = onClick,
            modifier = modifier.size(AALyricsSpacing.Space48),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                content = content,
            )
        }
    } else {
        Box(
            modifier = modifier.size(AALyricsSpacing.Space48),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
