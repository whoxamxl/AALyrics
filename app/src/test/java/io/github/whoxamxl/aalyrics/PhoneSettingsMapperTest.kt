package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.translation.api.TranslationModelPhase
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelState
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import io.github.whoxamxl.aalyrics.ui.phone.settings.AndroidAutoCompatibilityUiStatus
import io.github.whoxamxl.aalyrics.ui.phone.settings.AppUpdateUiPhase
import io.github.whoxamxl.aalyrics.ui.phone.settings.ChangelogUiPhase
import io.github.whoxamxl.aalyrics.ui.phone.settings.TranslationModelUiState
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class PhoneSettingsMapperTest {
    @Test
    fun `runtime Settings exposes supported translation state and unavailable release actions`() {
        val state = mapPhoneSettingsState(
            translationSettings = TranslationSettings(
                enabled = true,
                targetLanguage = "ja",
            ),
            translationModelStates = mapOf(
                "ja" to TranslationModelState(
                    languageTag = "ja",
                    phase = TranslationModelPhase.READY,
                ),
                "fr" to TranslationModelState(
                    languageTag = "fr",
                    phase = TranslationModelPhase.FAILED,
                    error = "download failed",
                ),
            ),
            verboseDetailsEnabled = true,
            plainLyricsAutoScrollEnabled = false,
            androidAutoStatus = AndroidAutoCompatibilityUiStatus.ENABLED,
            appVersionName = "0.1.0-dev",
            currentYear = 2026,
            displayLocale = Locale.ENGLISH,
        )

        assertEquals("ja", state.translationTarget.id)
        assertEquals(TranslationModelUiState.READY, state.translationTarget.modelState)
        assertEquals(
            TranslationModelUiState.BUILT_IN,
            state.translationTargets.first { it.id == "en" }.modelState,
        )
        assertEquals(
            "download failed",
            state.translationTargets.first { it.id == "fr" }.modelFailureReason,
        )
        assertEquals(true, state.verboseDetailsEnabled)
        assertEquals(false, state.plainLyricsAutoScrollEnabled)
        assertEquals(AppUpdateUiPhase.UNAVAILABLE, state.appUpdate.phase)
        assertEquals(ChangelogUiPhase.UNAVAILABLE, state.changelog.phase)
    }
}
