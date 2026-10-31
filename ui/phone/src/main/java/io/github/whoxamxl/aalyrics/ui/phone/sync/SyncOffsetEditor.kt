package io.github.whoxamxl.aalyrics.ui.phone.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsRadius
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTypography
import io.github.whoxamxl.aalyrics.ui.phone.R

@Composable
internal fun SyncOffsetEditor(
    initialOffsetMs: Long,
    maxOffsetMs: Long,
    onSave: (Long) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var draft by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val text = initialOffsetMs.toString()
        mutableStateOf(TextFieldValue(text, selection = TextRange(0, text.length)))
    }
    SyncOffsetEditorDialog(
        draft = draft,
        maxOffsetMs = maxOffsetMs,
        onDraftChange = { draft = it },
        onSave = onSave,
        onDismissRequest = onDismissRequest,
    )
}

@Composable
internal fun SyncOffsetEditorDialog(
    draft: TextFieldValue,
    maxOffsetMs: Long,
    onDraftChange: (TextFieldValue) -> Unit,
    onSave: (Long) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val input = parseSyncOffsetInput(draft.text, maxOffsetMs)
    val value = (input as? SyncOffsetInput.Valid)?.milliseconds
    val focusRequester = remember { FocusRequester() }
    val changeSignLabel = stringResource(R.string.sync_offset_change_sign)
    val save: () -> Unit = { value?.let(onSave) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                stringResource(R.string.sync_edit_offset),
                style = AALyricsTypography.TrackTitle,
            )
        },
        text = {
            val keyboard = LocalSoftwareKeyboardController.current
            val inspecting = LocalInspectionMode.current
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.sync_offset_edit_help),
                    style = AALyricsTypography.TrackArtist,
                    color = AALyricsColors.TextSecondary,
                )
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    label = { Text(stringResource(R.string.sync_offset_input_label)) },
                    singleLine = true,
                    isError = input !is SyncOffsetInput.Valid,
                    supportingText = {
                        Text(
                            if (input == SyncOffsetInput.NotInteger) {
                                stringResource(R.string.sync_offset_invalid_integer)
                            } else {
                                stringResource(R.string.sync_offset_range, maxOffsetMs)
                            },
                        )
                    },
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                val text = toggleSyncOffsetSign(draft.text)
                                onDraftChange(TextFieldValue(text, selection = TextRange(text.length)))
                                focusRequester.requestFocus()
                                keyboard?.show()
                            },
                            modifier = Modifier.semantics { contentDescription = changeSignLabel },
                        ) {
                            Text("±")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                )
            }
            LaunchedEffect(Unit) {
                if (!inspecting) {
                    focusRequester.requestFocus()
                    keyboard?.show()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = save, enabled = value != null) {
                Text(stringResource(R.string.sync_offset_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.settings_cancel), color = AALyricsColors.TextSecondary)
            }
        },
        containerColor = AALyricsColors.BackgroundSurfaceStrong,
        titleContentColor = AALyricsColors.TextPrimary,
        textContentColor = AALyricsColors.TextPrimary,
        shape = RoundedCornerShape(AALyricsRadius.Radius16),
    )
}
