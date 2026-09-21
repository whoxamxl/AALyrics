package io.github.whoxamxl.aalyrics.ui.phone.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsColors
import io.github.whoxamxl.aalyrics.ui.designsystem.theme.AALyricsTheme
import io.github.whoxamxl.aalyrics.ui.phone.settings.AdvancedSettingsScreen
import io.github.whoxamxl.aalyrics.ui.phone.settings.SettingsConfirmationDialog
import io.github.whoxamxl.aalyrics.ui.phone.settings.TranslationModelCleanupUiState

@Preview(name = "Verbose off", group = "AdvancedSettings", widthDp = 412, heightDp = 760)
@Composable
private fun AdvancedVerboseOffPreview() {
    AdvancedSettingsPreview(initialVerboseDetailsEnabled = false)
}

@Preview(name = "Verbose on", group = "AdvancedSettings", widthDp = 412, heightDp = 760)
@Composable
private fun AdvancedVerboseOnPreview() {
    AdvancedSettingsPreview(initialVerboseDetailsEnabled = true)
}

@Preview(
    name = "Narrow · 320dp",
    group = "AdvancedSettings",
    widthDp = 320,
    heightDp = 700,
)
@Composable
private fun AdvancedNarrowPreview() {
    AdvancedSettingsPreview(initialVerboseDetailsEnabled = false)
}

@Preview(
    name = "Clear models confirmation",
    group = "AdvancedSettings",
    widthDp = 412,
    heightDp = 760,
)
@Composable
private fun AdvancedClearModelsDialogPreview() {
    AALyricsTheme {
        SettingsConfirmationDialog(
            title = "Clear translation models?",
            text = "Additional downloaded translation models will be removed. Translation will be turned off and the target language will return to English. The default English model will remain available.",
            confirmLabel = "Clear",
            dismissLabel = "Cancel",
            confirmColor = AALyricsColors.Error,
            onConfirm = {},
            onDismissRequest = {},
        )
    }
}

@Preview(
    name = "Clear models failure",
    group = "AdvancedSettings",
    widthDp = 412,
    heightDp = 760,
)
@Composable
private fun AdvancedClearModelsFailureDialogPreview() {
    AALyricsTheme {
        SettingsConfirmationDialog(
            title = "Could not clear translation models",
            text = "Some downloaded translation models could not be removed. Try again when the model download or system operation has finished.",
            confirmLabel = "Retry",
            dismissLabel = "Close",
            confirmColor = AALyricsColors.Error,
            onConfirm = {},
            onDismissRequest = {},
        )
    }
}

@Preview(
    name = "Reset confirmation",
    group = "AdvancedSettings",
    widthDp = 412,
    heightDp = 760,
)
@Composable
private fun AdvancedResetDialogPreview() {
    AALyricsTheme {
        SettingsConfirmationDialog(
            title = "Reset AALyrics?",
            text = "AALyrics settings and onboarding state will be restored to their defaults. Downloaded translation models and system settings will not be changed.",
            confirmLabel = "Reset",
            dismissLabel = "Cancel",
            confirmColor = AALyricsColors.Error,
            onConfirm = {},
            onDismissRequest = {},
        )
    }
}

@Composable
private fun AdvancedSettingsPreview(initialVerboseDetailsEnabled: Boolean) {
    AALyricsTheme {
        var verboseDetailsEnabled by remember(initialVerboseDetailsEnabled) {
            mutableStateOf(initialVerboseDetailsEnabled)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AALyricsColors.BackgroundBase),
        ) {
            AdvancedSettingsScreen(
                verboseDetailsEnabled = verboseDetailsEnabled,
                cleanupState = TranslationModelCleanupUiState.IDLE,
                onVerboseDetailsChanged = { verboseDetailsEnabled = it },
                onClearTranslationModels = {},
                onDismissTranslationModelCleanupFailure = {},
                onResetAALyrics = {},
                onBack = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
