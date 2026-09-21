package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.translation.api.TranslationModelPhase
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelState
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import io.github.whoxamxl.aalyrics.ui.phone.settings.AndroidAutoCompatibilityUiStatus
import io.github.whoxamxl.aalyrics.ui.phone.settings.AppUpdateUiPhase
import io.github.whoxamxl.aalyrics.ui.phone.settings.TranslationModelCleanupUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.TranslationModelUiState
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class PhoneSettingsMapperTest {
    @Test
    fun `default Translation stays off while English remains built in`() {
        val state = mapPhoneSettingsState(
            translationSettings = TranslationSettings(enabled = false),
            translationModelStates = emptyMap(),
            verboseDetailsEnabled = false,
            plainLyricsAutoScrollEnabled = true,
            androidAutoStatus = AndroidAutoCompatibilityUiStatus.ENABLED,
            appVersionName = "0.1.0-dev",
            currentYear = 2026,
            noticeText = "Required Notice: © 2026 Yuta Miura",
            licenseText = "demo license",
            changelogText = "demo changelog",
            displayLocale = Locale.ENGLISH,
        )

        assertEquals(false, state.translationEnabled)
        assertEquals("en", state.translationTarget.id)
        assertEquals(TranslationModelUiState.BUILT_IN, state.translationTarget.modelState)
        assertEquals("Required Notice: © 2026 Yuta Miura", state.noticeText)
        assertEquals("demo license", state.licenseText)
        assertEquals("demo changelog", state.changelogText)
    }

    @Test
    fun `model inventory check does not appear as a download action`() {
        val state = mapPhoneSettingsState(
            translationSettings = TranslationSettings(
                enabled = false,
                targetLanguage = "ja",
            ),
            translationModelStates = mapOf(
                "ja" to TranslationModelState(
                    languageTag = "ja",
                    phase = TranslationModelPhase.CHECKING,
                ),
            ),
            verboseDetailsEnabled = false,
            plainLyricsAutoScrollEnabled = true,
            androidAutoStatus = AndroidAutoCompatibilityUiStatus.ENABLED,
            appVersionName = "0.1.0-dev",
            currentYear = 2026,
            noticeText = "Required Notice: © 2026 Yuta Miura",
            licenseText = "demo license",
            changelogText = "demo changelog",
            displayLocale = Locale.ENGLISH,
        )

        assertEquals("ja", state.translationTarget.id)
        assertEquals(
            TranslationModelUiState.CHECKING,
            state.translationTarget.modelState,
        )
    }

    @Test
    fun `cleanup failure is exposed to Settings presentation`() {
        val state = mapPhoneSettingsState(
            translationSettings = TranslationSettings(enabled = false),
            translationModelStates = emptyMap(),
            verboseDetailsEnabled = false,
            plainLyricsAutoScrollEnabled = true,
            androidAutoStatus = AndroidAutoCompatibilityUiStatus.ENABLED,
            appVersionName = "0.1.0-dev",
            currentYear = 2026,
            noticeText = "Required Notice: © 2026 Yuta Miura",
            licenseText = "demo license",
            changelogText = "demo changelog",
            translationModelCleanupState = TranslationModelCleanupState.FAILED,
            displayLocale = Locale.ENGLISH,
        )

        assertEquals(
            TranslationModelCleanupUiState.FAILED,
            state.translationModelCleanup,
        )
    }

    @Test
    fun `runtime Settings exposes supported translation state and unavailable update action`() {
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
            noticeText = "Required Notice: © 2026 Yuta Miura",
            licenseText = "demo license",
            changelogText = "demo changelog",
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
        assertEquals("demo changelog", state.changelogText)
    }
}
