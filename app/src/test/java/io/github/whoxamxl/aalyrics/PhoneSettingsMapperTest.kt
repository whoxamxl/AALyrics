package io.github.whoxamxl.aalyrics

import io.github.whoxamxl.aalyrics.translation.api.TranslationModelPhase
import io.github.whoxamxl.aalyrics.translation.api.TranslationModelState
import io.github.whoxamxl.aalyrics.translation.api.TranslationSettings
import io.github.whoxamxl.aalyrics.ui.phone.settings.AndroidAutoCompatibilityUiStatus
import io.github.whoxamxl.aalyrics.ui.phone.settings.AppUpdateInstallFailureUiReason
import io.github.whoxamxl.aalyrics.ui.phone.settings.AppUpdateUiPhase
import io.github.whoxamxl.aalyrics.ui.phone.settings.TranslationModelCleanupUiState
import io.github.whoxamxl.aalyrics.ui.phone.settings.TranslationModelUiState
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PhoneSettingsMapperTest {
    @Test
    fun `default Translation stays off while English remains built in`() {
        val state = mapPhoneSettingsState(
            translationSettings = TranslationSettings(enabled = false),
            translationModelStates = emptyMap(),
            verboseDetailsEnabled = false,
            plainLyricsAutoScrollEnabled = true,
            ignoreNonAudioApps = true,
            allowUnclassifiedApps = false,
            androidAutoStatus = AndroidAutoCompatibilityUiStatus.ENABLED,
            appVersionName = "0.1.0-dev",
            currentYear = 2026,
            noticeText = "Required Notice: © 2026 Yuta Miura",
            licenseText = "demo license",
            changelogText = "demo changelog",
            privacyPolicyText = "demo privacy",
            termsOfUseText = "demo terms",
            thirdPartyLicensesText = "demo third-party licenses",
            displayLocale = Locale.ENGLISH,
        )

        assertEquals(false, state.translationEnabled)
        assertEquals("en", state.translationTarget.id)
        assertEquals(TranslationModelUiState.BUILT_IN, state.translationTarget.modelState)
        assertEquals("Required Notice: © 2026 Yuta Miura", state.noticeText)
        assertEquals("demo license", state.licenseText)
        assertEquals("demo changelog", state.changelogText)
        assertEquals("demo privacy", state.privacyPolicyText)
        assertEquals("demo terms", state.termsOfUseText)
        assertEquals("demo third-party licenses", state.thirdPartyLicensesText)
    }

    @Test
    fun `automatic update preference passes through presentation`() {
        val state = mapPhoneSettingsState(
            translationSettings = TranslationSettings(enabled = false),
            translationModelStates = emptyMap(),
            verboseDetailsEnabled = false,
            plainLyricsAutoScrollEnabled = true,
            ignoreNonAudioApps = true,
            allowUnclassifiedApps = false,
            automaticallyCheckForUpdates = false,
            androidAutoStatus = AndroidAutoCompatibilityUiStatus.ENABLED,
            appVersionName = "0.1.0-dev",
            currentYear = 2026,
            noticeText = "",
            licenseText = "",
            changelogText = "",
            privacyPolicyText = "",
            displayLocale = Locale.ENGLISH,
        )

        assertEquals(false, state.automaticallyCheckForUpdates)
    }

    @Test
    fun `bundled legal documents pass through presentation unchanged`() {
        val terms = """
            # Terms of Use

            Exact bundled terms content.
        """.trimIndent()
        val thirdParty = """
            # Third-Party Licenses

            Exact bundled third-party notice content.
        """.trimIndent()

        val state = mapPhoneSettingsState(
            translationSettings = TranslationSettings(enabled = false),
            translationModelStates = emptyMap(),
            verboseDetailsEnabled = false,
            plainLyricsAutoScrollEnabled = true,
            ignoreNonAudioApps = true,
            allowUnclassifiedApps = false,
            androidAutoStatus = AndroidAutoCompatibilityUiStatus.ENABLED,
            appVersionName = "0.1.0-dev",
            currentYear = 2026,
            noticeText = "Required Notice: © 2026 Yuta Miura",
            licenseText = "license",
            changelogText = "changelog",
            privacyPolicyText = "privacy",
            termsOfUseText = terms,
            thirdPartyLicensesText = thirdParty,
            displayLocale = Locale.ENGLISH,
        )

        assertEquals(terms, state.termsOfUseText)
        assertEquals(thirdParty, state.thirdPartyLicensesText)
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
            ignoreNonAudioApps = true,
            allowUnclassifiedApps = false,
            androidAutoStatus = AndroidAutoCompatibilityUiStatus.ENABLED,
            appVersionName = "0.1.0-dev",
            currentYear = 2026,
            noticeText = "Required Notice: © 2026 Yuta Miura",
            licenseText = "demo license",
            changelogText = "demo changelog",
            privacyPolicyText = "demo privacy",
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
            ignoreNonAudioApps = true,
            allowUnclassifiedApps = false,
            androidAutoStatus = AndroidAutoCompatibilityUiStatus.ENABLED,
            appVersionName = "0.1.0-dev",
            currentYear = 2026,
            noticeText = "Required Notice: © 2026 Yuta Miura",
            licenseText = "demo license",
            changelogText = "demo changelog",
            privacyPolicyText = "demo privacy",
            translationModelCleanupState = TranslationModelCleanupState.FAILED,
            displayLocale = Locale.ENGLISH,
        )

        assertEquals(
            TranslationModelCleanupUiState.FAILED,
            state.translationModelCleanup,
        )
    }

    @Test
    fun `update check runtime states map into Settings presentation`() {
        fun mapped(updateState: AppUpdateCheckState) = mapPhoneSettingsState(
            translationSettings = TranslationSettings(enabled = false),
            translationModelStates = emptyMap(),
            verboseDetailsEnabled = false,
            plainLyricsAutoScrollEnabled = true,
            ignoreNonAudioApps = true,
            allowUnclassifiedApps = false,
            androidAutoStatus = AndroidAutoCompatibilityUiStatus.ENABLED,
            appVersionName = "0.2.0-alpha.1-dev+abcdef0",
            currentYear = 2026,
            noticeText = "notice",
            licenseText = "license",
            changelogText = "changelog",
            privacyPolicyText = "privacy",
            appUpdateCheckState = updateState,
            displayLocale = Locale.ENGLISH,
        ).appUpdate

        assertEquals(AppUpdateUiPhase.IDLE, mapped(AppUpdateCheckState.Idle).phase)
        assertEquals(AppUpdateUiPhase.CHECKING, mapped(AppUpdateCheckState.Checking()).phase)
        assertEquals(AppUpdateUiPhase.UP_TO_DATE, mapped(AppUpdateCheckState.UpToDate()).phase)
        assertEquals(AppUpdateUiPhase.CHECK_FAILED, mapped(AppUpdateCheckState.Failed()).phase)

        assertEquals(
            AppUpdateUiPhase.IDLE,
            mapped(AppUpdateCheckState.Checking(UpdateCheckOrigin.AUTOMATIC)).phase,
        )
        assertEquals(
            AppUpdateUiPhase.IDLE,
            mapped(AppUpdateCheckState.UpToDate(UpdateCheckOrigin.AUTOMATIC)).phase,
        )
        assertEquals(
            AppUpdateUiPhase.IDLE,
            mapped(AppUpdateCheckState.Failed(UpdateCheckOrigin.AUTOMATIC)).phase,
        )

        val manualAvailable = mapped(
            AppUpdateCheckState.UpdateAvailable(
                versionName = "0.2.0-alpha.2",
                origin = UpdateCheckOrigin.MANUAL,
            ),
        )
        assertEquals(AppUpdateUiPhase.IDLE, manualAvailable.phase)
        assertNull(manualAvailable.availableVersionName)

        val automaticAvailable = mapped(
            AppUpdateCheckState.UpdateAvailable(
                versionName = "0.2.0-alpha.2",
                origin = UpdateCheckOrigin.AUTOMATIC,
            ),
        )
        assertEquals(AppUpdateUiPhase.IDLE, automaticAvailable.phase)
        assertNull(automaticAvailable.availableVersionName)

        val installRefreshAvailable = mapped(
            AppUpdateCheckState.UpdateAvailable(
                versionName = "0.2.0-beta.1",
                origin = UpdateCheckOrigin.INSTALL_REFRESH,
            ),
        )
        assertEquals(AppUpdateUiPhase.UPDATE_AVAILABLE, installRefreshAvailable.phase)
        assertEquals("0.2.0-beta.1", installRefreshAvailable.availableVersionName)

        val preparing = mapped(AppUpdateCheckState.PreparingDownload("0.2.0-alpha.2"))
        assertEquals(AppUpdateUiPhase.IDLE, preparing.phase)
        assertNull(preparing.availableVersionName)

        val downloading = mapped(
            AppUpdateCheckState.Downloading(
                versionName = "0.2.0-alpha.2",
                downloadedBytes = 25L,
                totalBytes = 100L,
            ),
        )
        assertEquals(AppUpdateUiPhase.IDLE, downloading.phase)
        assertNull(downloading.availableVersionName)

        val verifying = mapped(AppUpdateCheckState.VerifyingDownload("0.2.0-alpha.2"))
        assertEquals(AppUpdateUiPhase.IDLE, verifying.phase)
        assertNull(verifying.availableVersionName)

        val downloaded = mapped(
            AppUpdateCheckState.Downloaded(
                versionName = "0.2.0-alpha.2",
                apkFile = java.io.File("verified.apk"),
            ),
        )
        assertEquals(AppUpdateUiPhase.DOWNLOADED, downloaded.phase)
        assertEquals("0.2.0-alpha.2", downloaded.availableVersionName)

        val downloadFailed = mapped(AppUpdateCheckState.DownloadFailed("0.2.0-alpha.2"))
        assertEquals(AppUpdateUiPhase.DOWNLOAD_FAILED, downloadFailed.phase)
        assertEquals("0.2.0-alpha.2", downloadFailed.availableVersionName)

        val preparingInstall = mapped(AppUpdateCheckState.PreparingInstall("0.2.0-alpha.2"))
        assertEquals(AppUpdateUiPhase.PREPARING_INSTALL, preparingInstall.phase)
        assertEquals("0.2.0-alpha.2", preparingInstall.availableVersionName)

        val permissionRequired =
            mapped(AppUpdateCheckState.InstallPermissionRequired("0.2.0-alpha.2"))
        assertEquals(
            AppUpdateUiPhase.INSTALL_PERMISSION_REQUIRED,
            permissionRequired.phase,
        )
        assertEquals("0.2.0-alpha.2", permissionRequired.availableVersionName)

        val installing = mapped(AppUpdateCheckState.Installing("0.2.0-alpha.2"))
        assertEquals(AppUpdateUiPhase.INSTALLING, installing.phase)
        assertEquals("0.2.0-alpha.2", installing.availableVersionName)

        val installFailed = mapped(
            AppUpdateCheckState.InstallFailed(
                versionName = "0.2.0-alpha.2",
                reason = AppUpdateInstallFailureReason.SIGNING_IDENTITY_MISMATCH,
            ),
        )
        assertEquals(AppUpdateUiPhase.INSTALL_FAILED, installFailed.phase)
        assertEquals("0.2.0-alpha.2", installFailed.availableVersionName)
        assertEquals(
            AppUpdateInstallFailureUiReason.SIGNING_IDENTITY_MISMATCH,
            installFailed.installFailureReason,
        )
    }

    @Test
    fun `permission-required runtime state is independent from dialog visibility`() {
        fun mapped(prompt: UpdateInstallPermissionPrompt?) = mapPhoneSettingsState(
            translationSettings = TranslationSettings(enabled = false),
            translationModelStates = emptyMap(),
            verboseDetailsEnabled = false,
            plainLyricsAutoScrollEnabled = true,
            ignoreNonAudioApps = true,
            allowUnclassifiedApps = false,
            androidAutoStatus = AndroidAutoCompatibilityUiStatus.ENABLED,
            appVersionName = "0.2.0-alpha.1",
            currentYear = 2026,
            noticeText = "notice",
            licenseText = "license",
            changelogText = "changelog",
            privacyPolicyText = "privacy",
            appUpdateCheckState =
                AppUpdateCheckState.InstallPermissionRequired("0.2.0-alpha.2"),
            installPermissionPrompt = prompt,
            displayLocale = Locale.ENGLISH,
        )

        val dismissed = mapped(prompt = null)
        assertEquals(
            AppUpdateUiPhase.INSTALL_PERMISSION_REQUIRED,
            dismissed.appUpdate.phase,
        )
        assertNull(dismissed.installPermissionDialog)

        val requested = mapped(
            prompt = UpdateInstallPermissionPrompt("0.2.0-alpha.2"),
        )
        assertEquals(
            AppUpdateUiPhase.INSTALL_PERMISSION_REQUIRED,
            requested.appUpdate.phase,
        )
        assertEquals(
            "0.2.0-alpha.2",
            assertNotNull(requested.installPermissionDialog).versionName,
        )
    }

    @Test
    fun `runtime Settings exposes supported translation state and idle update action`() {
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
            ignoreNonAudioApps = false,
            allowUnclassifiedApps = true,
            androidAutoStatus = AndroidAutoCompatibilityUiStatus.ENABLED,
            appVersionName = "0.1.0-dev",
            currentYear = 2026,
            noticeText = "Required Notice: © 2026 Yuta Miura",
            licenseText = "demo license",
            changelogText = "demo changelog",
            privacyPolicyText = "demo privacy",
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
        assertEquals(false, state.ignoreNonAudioApps)
        assertEquals(true, state.allowUnclassifiedApps)
        assertEquals(AppUpdateUiPhase.IDLE, state.appUpdate.phase)
        assertEquals("demo changelog", state.changelogText)
        assertEquals("demo privacy", state.privacyPolicyText)
    }
}
